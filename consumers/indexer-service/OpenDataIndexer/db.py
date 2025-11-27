# Script para la gestión del acceso a Qdrant
import os
import requests
from dotenv import load_dotenv
# Importación de componentes del buscador
from config import CACERT_PATH
# ============================================================================================================================== #
load_dotenv()
API_URL = os.getenv("API_URL")
API_KEY = os.getenv("API_KEY")
# ============================================================================================================================== #
# Gestión de errores de acceso o respuesta inesperada de Qdrant
class QdrantAccessException(Exception):
    def __init__(self, message, status_code=None, response=None):
        super().__init__(message)
        self.status_code = status_code
        self.response = response    

# Almacenar y gestionar colecciones
class CollectionWrapper:
    def __init__(self, name, api_url, headers, cacert_path):
        self.name = name
        self.api_url = api_url
        self.headers = headers
        self.cacert_path = cacert_path

    # Función para hacer queries a las colecciones de Qdrant mediante HTTP requests
    def query(self, query_embeddings, n_results, filters=None, with_payload=False, with_vector=False):
        try:
            vector = query_embeddings[0]
            if hasattr(vector, "tolist"):
                vector = vector.tolist()
            # Construcción de la query
            body = {
                "vector": vector,
                "limit": int(n_results),
                "with_payload": with_payload,
                "with_vector": with_vector
            }
            if filters:
                body["filter"] = filters
            # Petición
            resp = requests.post(
                f"{self.api_url}/collections/{self.name}/points/search",
                headers=self.headers,
                verify=self.cacert_path,
                json=body
            )
            if resp.status_code != 200:
                raise QdrantAccessException(
                    f"Qdrant query failed (status {resp.status_code}): {resp.text}",
                    status_code=resp.status_code,
                    response=resp.text
                )
            # Extracción de los datos recuperados
            result = resp.json().get("result", []) or []
            ids = [str(p["id"]) for p in result]
            distances = [1.0 - float(p.get("score", 0.0)) for p in result]
            payloads = [p.get("payload", {}) for p in result] if with_payload else []
            return {"ids": ids, "distances": distances, "payloads": payloads}
        except Exception as e:
            raise QdrantAccessException(f"[CollectionWrapper] Error in query for {self.name}: {e}")

# Clase singleton para crear una sola instancia en la ejecución del buscador entre los diferentes componentes
class DbEngine:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(DbEngine, cls).__new__(cls)
            cls._instance._initialize()
        return cls._instance
    
    def get_collection(self, name):
        return self.collections.get(name)

    def get_all_collections(self):
        return self.collections
    
    def _initialize(self):
        self.headers = {
            "Content-Type": "application/json",
            "api-key": API_KEY
        }
        # Carga de la info de las colecciones
        self.collections = {
            'titulos': CollectionWrapper('titulos', API_URL, self.headers, CACERT_PATH),
            'descripciones': CollectionWrapper('descripciones', API_URL, self.headers, CACERT_PATH),
            'cabeceras': CollectionWrapper('cabeceras', API_URL, self.headers, CACERT_PATH),
            'contenido': CollectionWrapper('contenidos', API_URL, self.headers, CACERT_PATH)
        }
        # Debug
        for name, wrapper in self.collections.items():
            count = self._count_collection(wrapper.name)
            print(f"[QDRANT] Collection {name}: {count} documents")

    # Consulta y cuenta los elementos de una colección, usado como debug al iniciar el buscador
    def _count_collection(self, collection_name):
        try:
            # Petición
            resp = requests.get(
                f"{API_URL}/collections/{collection_name}",
                headers=self.headers,
                verify=CACERT_PATH
            )
            # Extracción de los datos recuperados
            if resp.status_code == 200:
                return resp.json().get("result", {}).get("points_count", 0)
        except Exception as e:
            print(f"[!] Error contando colección {collection_name}: {e}")
        return 0

    # Consulta una colección y devuelve un diccionario asociando una lista IDs con sus payloads 
    def get_payloads_bulk(self, collection_name, ids):
        if not ids:
            return {}
        try:
            # Petición
            resp = requests.post(
                f"{API_URL}/collections/{collection_name}/points",
                headers=self.headers,
                verify=CACERT_PATH,
                json={"ids": ids, "with_payload": True}
            )
            if resp.status_code != 200:
                raise QdrantAccessException(
                    f"Qdrant get_payloads_bulk failed (status {resp.status_code}): {resp.text}",
                    status_code=resp.status_code,
                    response=resp.text
                )
            # Extracción de los datos recuperados
            out = {}
            for p in resp.json().get("result", []):
                out[str(p.get("id"))] = p.get("payload", {}) or {}
            return out
        except Exception as e:
            raise QdrantAccessException(f"[dbEngine] Error en get_payloads_bulk({collection_name}): {e}")

    
    # Función del endpoint "dataset" devuelve todos los datos de un dataset dado su ID
    def get_item(self, dataset_uid):
        try:
            # Recupera los payloads dado un dataset_uid
            payload_title = self._get_first_payload_by_dataset('titulos', dataset_uid)
            payload_desc  = self._get_first_payload_by_dataset('descripciones', dataset_uid)
            payload_head  = self._get_first_payload_by_dataset('cabeceras', dataset_uid)
            payload_cont  = self._get_first_payload_by_dataset('contenidos', dataset_uid)

            # Preparar la salida
            if any([payload_title, payload_desc, payload_head, payload_cont]):
                meta_src = payload_title or payload_desc
                return {
                    "hits": [{
                        "_source": {
                            "title": payload_title.get("text", ""),
                            "description": payload_desc.get("text", ""),
                            "header": payload_head.get("text", ""),
                            "content": payload_cont.get("text", ""),
                            "theme": meta_src.get("theme"),
                            "temporal": meta_src.get("temporal"),
                            "geo": meta_src.get("geo"),
                            "resources": meta_src.get("resources", []),
                            "source": meta_src.get("source", ""),
                        }
                    }],
                    "total": {"value": 1}
                }
            # No encontrado no es excepción de red: devolver vacío
            return {"hits": [], "total": {"value": 0}}
        except QdrantAccessException:
            raise
        except Exception as e:
            raise QdrantAccessException(f"[dbEngine] Error en get_item: {e}")

    # Devuelve el payload del primer point que tenga ese dataset_uid en la colección
    def _get_first_payload_by_dataset(self, collection, dataset_uid):
        try:
            # Construcción de la query
            body = {
                "filter": {
                    "must": [
                        {"key": "dataset_uid", "match": {"value": dataset_uid}}
                    ]
                },
                "with_payload": True,
                "limit": 1
            }
            # Petición
            resp = requests.post(
                f"{API_URL}/collections/{collection}/points/scroll",
                headers=self.headers,
                verify=CACERT_PATH,
                json=body
            )
            if resp.status_code != 200:
                raise QdrantAccessException(
                    f"Qdrant _get_first_payload_by_dataset failed (status {resp.status_code}): {resp.text}",
                    status_code=resp.status_code,
                    response=resp.text
                )
            # Extracción de los datos recuperados
            res = resp.json().get("result", {})
            pts = res.get("points", [])
            if pts:
                return pts[0].get("payload", {}) or {}
            return {}
        except Exception as e:
            raise QdrantAccessException(f"[dbEngine] Error en _get_first_payload_by_dataset({collection}): {e}")

    # Devuelve todos los payloads de una colección para un 'dataset_uid' dado
    def get_all_resources_by_dataset_uid(self, collection, dataset_uid):
        try:
            # Construcción de la query
            body = {
                "filter": {
                    "must": [
                        {"key": "dataset_uid", "match": {"value": dataset_uid}}
                    ]
                },
                "with_payload": True,
                "limit": 1000
            }
            # Request
            resp = requests.post(
                f"{API_URL}/collections/{collection}/points/scroll",
                headers=self.headers,
                verify=CACERT_PATH,
                json=body
            )
            if resp.status_code != 200:
                raise QdrantAccessException(
                    f"Qdrant get_all_resources_by_dataset_uid failed (status {resp.status_code}): {resp.text}",
                    status_code=resp.status_code,
                    response=resp.text
                )
            # Extracción de los datos recuperados
            res = resp.json().get("result", {})
            pts = res.get("points", [])
            return [pt.get("payload", {}) or {} for pt in pts]
        except Exception as e:
            raise QdrantAccessException(f"[dbEngine] Error en get_all_resources_by_dataset_uid({collection}): {e}")

    # Función del endpoint search cuando no se especifica una query.
    # Devuelve los datasets que cumplen los filtros consultando la colección de títulos que es la única que tiene todos los payloads
    def get_filtered_items(self, filters, limit=100):
        items = []
        # Construcción de la query
        body = {
            "filter": filters,
            "with_payload": True,
            "limit": limit
        }
        try:
            # Petición
            resp = requests.post(
                f"{API_URL}/collections/titulos/points/scroll",
                headers=self.headers,
                verify=CACERT_PATH,
                json=body
            )
            if resp.status_code != 200:
                raise QdrantAccessException(
                    f"Qdrant get_filtered_items failed (status {resp.status_code}): {resp.text}",
                    status_code=resp.status_code,
                    response=resp.text
                )
            # Extracción de los datos recuperados
            res = resp.json().get("result", {})
            pts = res.get("points", [])
            for pt in pts:
                payload = pt.get("payload", {}) or {}
                items.append({
                    "dataset_uid": payload.get("dataset_uid"),
                    "title": payload.get("text", ""),
                    "theme": payload.get("theme"),
                    "temporal": payload.get("temporal"),
                    "geo": payload.get("geo"),
                    "source": payload.get("source", ""),
                })
            return items
        except Exception as e:
            raise QdrantAccessException(f"[dbEngine] Error en get_filtered_items: {e}")
        
# ============================================================================================================================== #
# Funcionalidad de clustering no definitiva.
    """
    items: lista de dicts con:
        {
        "dataset_uid": str,
        "id_tit": str|None,
        "id_des": str|None,
        "id_cab": str|None,
        "id_con": str|None,
        "score": float,
        "sim_title": float,
        "sim_description": float,
        "sim_header": float,
        "sim_rows": float
        }
    Devuelve el mismo formato que getItems().
    """
    def get_items_aggregated(self, items):
        hits = []
        for it in items:
            pid_t = it.get("id_tit")
            pid_d = it.get("id_des")
            pid_h = it.get("id_cab")
            pid_c = it.get("id_con")

            payload_title = self._get_payload('titulos', pid_t) if pid_t else {}
            payload_desc  = self._get_payload('descripciones', pid_d) if pid_d else {}
            payload_head  = self._get_payload('cabeceras', pid_h) if pid_h else {}
            payload_cont  = self._get_payload('contenidos', pid_c) if pid_c else {}

            # Prioriza theme/temporal/geo/source del título (o de la descr como fallback)
            meta_src = payload_title or payload_desc

            hits.append({
                "id": it.get("dataset_uid"),
                "title": payload_title.get("text", ""),
                "description": payload_desc.get("text", ""),
                "header": payload_head.get("text", ""),
                "content": payload_cont.get("text", ""),
                "theme": meta_src.get("theme"),
                "temporal": meta_src.get("temporal"),
                "geo": meta_src.get("geo"),
                "source": meta_src.get("source", ""),
                "score": it.get("score", 0.0),
                "sim_title": it.get("sim_title", 0.0),
                "sim_description": it.get("sim_description", 0.0),
                "sim_header": it.get("sim_header", 0.0),
                "sim_rows": it.get("sim_rows", 0.0),
            })

        return {"total": {"value": len(hits)}, "hits": hits}
    
    def _get_payload(self, collection, id):
        try:
            # Petición
            resp = requests.post(
                f"{API_URL}/collections/{collection}/points",
                headers=self.headers,
                verify=CACERT_PATH,
                json={
                    "ids": [id],
                    "with_payload": True
                }
            )
            if resp.status_code != 200:
                raise QdrantAccessException(
                    f"Qdrant _get_payload failed (status {resp.status_code}): {resp.text}",
                    status_code=resp.status_code,
                    response=resp.text
                )
            # Extracción de los datos recuperados
            data = resp.json()
            results = data.get("result", [])
            if results:
                return results[0].get("payload", {}) or {}
            return {}
        except Exception as e:
            raise QdrantAccessException(f"[dbEngine] Error en _get_payload({collection}): {e}")

