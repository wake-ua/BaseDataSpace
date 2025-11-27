# Script para gestionar la lógica de las búsquedas y accesos a "db" para la recuperación de los datos
import pickle
import time
# Importación de componentes del buscador
from config import FLAG_ADICIONAL_GENERATIVO, FLAG_NO_RESULTADOS_GENERATIVO, QUERY_CLASIFIER_PATH, SCORES_PESOS, UMBRAL_SIMILITUD, UMBRAL_SIMILITUD_GENERATIVO, TOP_N, K_MAX
from db import QdrantAccessException
# ============================================================================================================================== #
RESPUESTA_ADICIONAL = "<p>Addicionalmente existen otras fuentes que pueden ser de utilidad:</p><br><p><a class='font-semibold underline' href='https://datos.gob.es/' target='_blank'>Datos Abiertos del Gobierno de España</a>: Plataforma con múltiples datasets públicos de diferentes ámbitos en España.<p><br>\n<p><a class='font-semibold underline' href='https://opendata.aragon.es/' target='_blank'>Open Data Aragón</a>: Datos abiertos de la comunidad autónoma de Aragón.<p><br>\n<p><a class='font-semibold underline' href='https://datos.madrid.es/' target='_blank'>Datos Abiertos del Ayuntamiento de Madrid</a>: Datos abiertos de la comunidad autónoma de Madrid.<p><br>\n<p><a class='font-semibold underline' href='https://opendata.jcyl.es/' target='_blank'>Datos Abiertos de Castilla y León</a>: Datos públicos de Castilla y León en diferentes ámbitos.<p><br>\n<p><a class='font-semibold underline' href='https://opendata.bcn.cat/' target='_blank'>Ajuntament de Barcelona Open Data</a>: Datos abiertos de la provincia Barcelona.<p><br>\n"
RESPUESTA_NO_RESULTADOS = "<p> Lamentablemente no disponemos de esos datos</p>"
# ============================================================================================================================== #
class SearchEngine:
    def __init__(self, db, generative, semantic, logger):
        print('Created Search Engine')
        # Vincular otros componentes del buscador
        self.db = db
        self.generative = generative
        self.semantic = semantic
        self.logger = logger
        # Modelo clasificador de query/intención
        self.query_classifier = pickle.load(open(QUERY_CLASIFIER_PATH, 'rb'))

        self.pesos = SCORES_PESOS
        self.umbral_similitud = UMBRAL_SIMILITUD
        self.umbral_similitud_generativo = UMBRAL_SIMILITUD_GENERATIVO
        self.top_n = TOP_N
        self.k = K_MAX

    # Función del endpoint "search" para la ejecución de queries
    def search(self, text, filters=None):
        print(f"[SEARCH] Consulta recibida: {text}")
        ini_query = time.time()
        query_type = 0  # Actualmente forzamos que todo funcione como queries en lugar de intenciones
        # query_type = self.query_classifier.predict(self.semantic.model.encode([text])).tolist()[0]

        print(f"[SEARCH] Tipo de consulta clasificada: {'intent' if query_type == 1 else 'keyword'}")
        print(f"[Tiempo Keyword-Intent] {time.time() - ini_query}")
        # Ramificación keyword - intent
        if query_type == 1:
            ini_get_keywords = time.time() 
            if FLAG_ADICIONAL_GENERATIVO:
                keywords = self.generative.get_keywords(text, adicional = True)
            else:
                keywords = self.generative.get_keywords(text)

            print(f"[Tiempo Generar-Keywords] {time.time() - ini_get_keywords}")
            print(f"[SEARCH] Keywords generadas: {keywords}")
            return self.get_intent_results(keywords, text, filters)
        else:
            return self.get_keyword_results(text, filters)

    # Relaciona los identificadores de recursos de Qdrant con sus 'dataset_uid' globales.
    # Devuelve un diccionario que mapea 'dataset_uid' a un ID de qdrant único.
    def _dataset_uid_to_resource_id(self, results, payloads):
        ds_map = {}
        # Para cada recurso en 'reslts', busca su 'dataset_uid' en los payloads.
        for id in results.keys():
            ds = (payloads.get(id) or {}).get("dataset_uid")
            if ds:
                ds_map[ds] = id
        return ds_map

    # Agrupa los candidatos por recurso (dataset_uid + path), y conserva el ID con mayor similitud por cada tipo de campo.
    def _map_dataset_uid_to_data(self, sims, payloads, sim_field, id_field, cand=None):
        if cand is None:
            cand = {}
        def res_key(pl):
            rp = pl.get("resource_path") or ""
            fn = pl.get("resource_fileName") or ""
            return f"{pl.get('dataset_uid')}:::{rp or fn}"
        # Para cada resultado en 'sims', localiza su 'dataset_uid' y ruta/nombre de recurso en los payloads
        for id, sim in sims.items():
            pl = payloads.get(id) or {}
            ds = pl.get("dataset_uid")
            if not ds:
                continue
            key = res_key(pl)
            it = cand.setdefault(key, {
                "dataset_uid": ds, "id_cab": None, "sim_header": 0.0,
                "id_con": None, "sim_rows": 0.0,
                "resource_path": pl.get("resource_path"),
                "resource_fileName": pl.get("resource_fileName")
            })
            if sim > it[sim_field]:
                it[sim_field] = sim
                it[id_field] = id
        return cand

    # Calcula el score y construccion de la salida
    def _score_and_hit(self, key, it, ds_tit, ds_des, titulos, descripciones, p_tit, p_des, p_cab, p_con, threshold):
        # Recuperación de las similitudes del recurso
        ds = it["dataset_uid"]
        id_tit = ds_tit.get(ds)
        id_des = ds_des.get(ds)
        sim_tit = float(titulos.get(id_tit, 0.0)) if id_tit else 0.0
        sim_des = float(descripciones.get(id_des, 0.0)) if id_des else 0.0
        # Calculo del score en función de los pesos definidos
        score = (
            self.pesos["title"]       * sim_tit +
            self.pesos["description"] * sim_des +
            self.pesos["header"]      * it["sim_header"] +
            self.pesos["rows"]        * it["sim_rows"]
        )
        if score < (threshold or 0):
            return None
        
        # Extracción de los payloads del recurso
        pl_tit = p_tit.get(id_tit) if id_tit else {}
        pl_des = p_des.get(id_des) if id_des else {}
        pl_cab = p_cab.get(it["id_cab"]) if it["id_cab"] else {}
        pl_con = p_con.get(it["id_con"]) if it["id_con"] else {}
        meta_src = pl_tit or pl_des or {}

        return {
            "id": key,
            "title": pl_tit.get("text", ""),
            "description": pl_des.get("text", ""),
            "header": pl_cab.get("text", ""),
            "content": pl_con.get("text", ""),
            "theme": meta_src.get("theme"),
            "temporal": meta_src.get("temporal"),
            "geo": meta_src.get("geo"),
            "source": meta_src.get("source", ""),
            "metadato_fileName": meta_src.get("meta_fileName"),
            "resource_path": it["resource_path"],
            "resource_fileName": it["resource_fileName"],
            "score": score,
            "sim_title": sim_tit,
            "sim_description": sim_des,
            "sim_header": it["sim_header"],
            "sim_rows": it["sim_rows"],
        }

    # Función para efectua hace la query, filtra de similitud y rerank
    def search_single_query(self, query, threshold=None, limit=None, filters=None):
        ini_query = time.time()
        try:
            tit, des, cab, con = self.semantic.query_collections(query, k=self.k, filters=filters)
        except QdrantAccessException as e:
            print(f"[SEARCH] Error en consulta Qdrant: {e}")
            self.logger.exception(f"[SEARCH] Error en consulta Qdrant: {e}")
            raise

        print(f"[Tiempo Query] {time.time() - ini_query}")

        # Crear mapas id->payload
        p_tit = {id_: pl for id_, pl in zip(tit.get("ids", []), tit.get("payloads", []))}
        p_des = {id_: pl for id_, pl in zip(des.get("ids", []), des.get("payloads", []))}
        p_cab = {id_: pl for id_, pl in zip(cab.get("ids", []), cab.get("payloads", []))}
        p_con = {id_: pl for id_, pl in zip(con.get("ids", []), con.get("payloads", []))}

        # Mapas id->sim
        titulos = dict(zip(tit.get("ids", []), tit.get("similarities", [])))
        descripciones = dict(zip(des.get("ids", []), des.get("similarities", [])))
        cabeceras = dict(zip(cab.get("ids", []), cab.get("similarities", [])))
        contenido = dict(zip(con.get("ids", []), con.get("similarities", [])))

        # Agrupación y cálculo de score
        ds_tit = self._dataset_uid_to_resource_id(titulos, p_tit)
        ds_des = self._dataset_uid_to_resource_id(descripciones, p_des)

        # Si no hay cabeceras ni contenido, creamos un candidato vacío
        cand = {}
        if cabeceras or contenido:
            cand = self._map_dataset_uid_to_data(cabeceras, p_cab, "sim_header", "id_cab")
            cand = self._map_dataset_uid_to_data(contenido, p_con, "sim_rows", "id_con", cand=cand)
        else:
            # Creamos candidatos directamente a partir de títulos o descripciones
            all_ds = set(list(ds_tit.keys()) + list(ds_des.keys()))
            for ds in all_ds:
                cand[ds] = {
                    "dataset_uid": ds,
                    "id_cab": None,
                    "sim_header": 0.0,
                    "id_con": None,
                    "sim_rows": 0.0,
                    "resource_path": (p_tit.get(ds_tit.get(ds)) or p_des.get(ds_des.get(ds)) or {}).get("resource_path"),
                    "resource_fileName": (p_tit.get(ds_tit.get(ds)) or p_des.get(ds_des.get(ds)) or {}).get("resource_fileName")
                }

        # Construcción de resultados
        hits = []
        for key, it in cand.items():
            hit = self._score_and_hit(
                key, it, ds_tit, ds_des,
                titulos, descripciones,
                p_tit, p_des, p_cab, p_con,
                threshold
            )
            if hit:
                hits.append(hit)

        if not hits:
            print(f"[FILTRO SIMILITUD] Ningún recurso supera el umbral {threshold}")
            return []

        # Ordenar por score y rerank opcional
        hits.sort(key=lambda x: x["score"], reverse=True)
        
        # Rerank opcional
        inicio_rerank = time.time()
        if hasattr(self.semantic, "rerank_results") and getattr(self.semantic, "usar_reranker", False):
            hits = self.semantic.rerank_results(query, hits, top_n=10)
        print(f'[Tiempo Rerank: {time.time() - inicio_rerank}]')

        if limit:
            hits = hits[:limit]

        return hits
        
    # Dada una keyword realiza la búsqueda de los datos y prepara la salida para la API
    def get_keyword_results(self, query, filters=None):
        print(f"[KEYWORD] Ejecutando búsqueda manual para: '{query}'")
        try:
            reranked_hits = self.search_single_query(query, threshold=self.umbral_similitud, limit=self.top_n, filters = filters)
        except QdrantAccessException as e:
            print(f"[KEYWORD] Error accediendo a Qdrant: {e}")
            self.logger.exception(f"[SEARCH] Error accediendo a Qdrant: {e}")

            return {
                "type": "error",
                "detail": "Error accediendo a la base de datos, inténtelo más tarde.",
                "error": str(e)
            }
        # Asegurar campos json y csv
        for r in reranked_hits:
            r['json'] = r.get('json', '')
            r['csv'] = r.get('csv', '')

        if FLAG_ADICIONAL_GENERATIVO:
            adaptativo = self.generative.get_data_sources_response(query)
        else:
            adaptativo = RESPUESTA_ADICIONAL
            
        if not reranked_hits:
            if FLAG_NO_RESULTADOS_GENERATIVO:
                intro = "<p class='font-semibold'>Parece que no tenemos en nuestra base de datos la información que buscas, pero no te preocupes, hemos encontrado algo que quizá te sirva:</p><br>"
                intro = intro + self.generative.get_data_sources_response(query)
            else:
                intro = RESPUESTA_NO_RESULTADOS

            print("[KEYWORD] Sin resultados, generando respuesta vacía.")
            return {
                "type": "keywords",
                "total": {"value": 0},
                "hits": [],
                "intro": intro,
                "additional": ''
            }
        return {
            "type": "keywords",
            "total": {"value": len(reranked_hits)},
            "hits": reranked_hits,
            "intro": "",
            "additional": adaptativo
        }

    # Dada una intención se extraen varias keywords, esta función realiza una query por keyword,
    # Elimina duplicados recuperados por las diferentes keywords
    def get_intent_results(self, queries, intent, filters):
        print(f"[INTENT] Ejecutando búsqueda para la intención: '{intent}'")
        resultados_finales = []
        total_hits = 0
        ya_encontrados = set()
        
        if FLAG_ADICIONAL_GENERATIVO:
            adaptativo = queries.get('extra', RESPUESTA_ADICIONAL)
        else:
            adaptativo = RESPUESTA_ADICIONAL

        # Recorre todas las keywords generadas    
        for q in queries['keywords']:
            query = q['consulta']
            print(f"[INTENT] Ejecutando consulta para la keyword: '{query}'")
            try:
                reranked_hits = self.search_single_query(query, threshold=self.umbral_similitud_generativo, limit=self.top_n, filters=filters)
            except QdrantAccessException as e:
                print(f"[KEYWORD] Error accediendo a Qdrant: {e}")
                return {
                    "type": "error",
                    "detail": "Error accediendo a la base de datos, inténtelo más tarde.",
                    "error": str(e)
                }
            # Filtrar resultados ya encontrados
            reranked_hits = [r for r in reranked_hits if r['id'] not in ya_encontrados]
            
            if reranked_hits:
                # Si hay resultados se añaden al set de 'ya_encontrados' y a la lista de resultados
                for r in reranked_hits:
                    ya_encontrados.add(r['id'])
                q['resultados'] = {'hits': reranked_hits}
                total_hits += len(reranked_hits)
                resultados_finales.append(q)

        print(f"[INTENT] Total resultados acumulados: {total_hits}")
        
        if total_hits == 0:
            if FLAG_NO_RESULTADOS_GENERATIVO:
                intro = "<p class='font-semibold'>Parece que no tenemos en nuestra base de datos la información que buscas, pero no te preocupes, hemos encontrado algo que quizá te sirva:</p><br>"
                intro = intro + self.generative.get_data_sources_response(query)
            else:
                intro = RESPUESTA_NO_RESULTADOS
            return {
                "type": "intent",
                "total": 0,
                "hits": 0,
                "intro": intro,
                "additional": ''
            }
        return {
            "type": "intent",
            "total": total_hits,
            "hits": resultados_finales,
            "intro": queries.get('intro', ''),
            "additional": adaptativo
        }