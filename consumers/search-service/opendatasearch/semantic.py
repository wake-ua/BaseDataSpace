# Script intermedio entre search y db para gestionar las tareas semánticas
import json
import os
import pandas as pd
import torch
from collections import defaultdict
from dateutil.parser import parse
from sentence_transformers import SentenceTransformer
from sklearn.cluster import DBSCAN, OPTICS
# Importación de componentes del buscador
from utils import normalizar_texto, traducir_fecha_es
from config import USE_E5_PROMPT, USAR_PROMPT_EMBEDDING, MODEL_TO_16
# ============================================================================================================================== #
os.environ["TOKENIZERS_PARALLELISM"] = "true" # Activar paralelismo
# ============================================================================================================================== #
class SemanticEngine:
    def __init__(self, model_name, db, generative, logger):
        print('Created Semantic Engine')

        # Seleccionar GPU o CPU y cargar modelo de SentenceTransformer
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = SentenceTransformer(model_name, trust_remote_code=True, device=device)
        if MODEL_TO_16:
            self.model.to(torch.float16)

        # Vincular otros componentes del buscador
        self.db = db
        self.generative = generative
        self.logger = logger

        # Recuperar colecciónes de la DB
        self.col_titulo = db.get_collection("titulos")
        self.col_desc = db.get_collection("descripciones")
        self.col_headers = db.get_collection("cabeceras")
        self.col_data = db.get_collection("contenido")

        # Datos para clustering
        self.df_jerarquia = pd.read_csv("jerarquia_territorial.csv")
        self.df_jerarquia = self.df_jerarquia.dropna(subset=["municipio", "provincia", "comunidad_au"])
        
    # Función del endpoint "similar", utilizado para intentar buscar titulos similares (ESTÁ SIN TOCAR DESDE EL COMIENZO DE LA REMODELACIÓN)
    def similar_items(self, query, k=10):
        # Generación del embedding de la query
        emb = self.model.encode(query, normalize_embeddings=True).tolist()
        
        # Consulta el embedding en la colección de titulos y devuelve los "k" resultados mas similares
        res = self.col_titulo.query(query_embeddings=[emb], n_results=k, with_payload=True)
        
        # Soporte para distintos formatos de respuesta Qdrant
        ids = res.get('ids', [])
        dists = res.get('distances', [])
        payloads = res.get('payloads', [])
        if ids and isinstance(ids[0], list):
            ids = ids[0]
        if dists and isinstance(dists[0], list):
            dists = dists[0]
        if payloads and isinstance(payloads[0], list):
            payloads = payloads[0]
        
        # Construcción de la salida
        resultados = []
        for i, id_ in enumerate(ids):
            score = 1 - float(dists[i]) if i < len(dists) else None
            title = payloads[i].get("text", "") if i < len(payloads) else ""
            resultados.append({
                "id": id_,
                "score": score,
                "title": title,
                "payload": payloads[i] if i < len(payloads) else {}
            })
        
        return {
            "query": query,
            "results": resultados
        }

    # Generación de embeddings adaptado para funcionar con diferentes modelos
    def embed(self, text):
        text = normalizar_texto(text)

        if USE_E5_PROMPT:
            text = f"query: {text}"

        if USAR_PROMPT_EMBEDDING:
            task = "Given a user query about open data, retrieve relevant entries that best match the information need."
            text = f"Instruct: {task}\nQuery: {text}"

        return self.model.encode(text, normalize_embeddings=True)

    def _filter_results_by_allowed_ds(self, results, allowed_ds):
        for key in ("descripciones", "cabeceras", "contenido"):
            sims = results.get(key)
            if not sims:
                continue
            p = self.db.get_payloads_bulk(key, list(sims.keys()))
            results[key] = {
                _id: sim
                for _id, sim in sims.items()
                if (p.get(_id) or {}).get("dataset_uid") in allowed_ds
            }

    # Función para hacer la query a todas las colecciones. Devuelve una lista de resultados con ID y similitud por cada colección
    def query_collections(self, query, k=None, filters=None):
        emb = self.embed(query)
        collections = self.db.get_all_collections()
        results = {}
        allowed_ds = None

        for name, collection in collections.items():
            use_filters = filters if name == "titulos" and filters else None
            data = collection.query(query_embeddings=[emb], n_results=k, filters=use_filters, with_payload=True)
            ids = data["ids"]
            dists = data["distances"]
            payloads = data["payloads"]
            similarities = [1.0 - d for d in dists] if dists else []
            results[name] = {
                "ids": ids,
                "similarities": similarities,
                "payloads": payloads,
            }

            if name == "titulos" and filters:
                allowed_ds = { (payloads[i] or {}).get("dataset_uid") for i in range(len(ids)) }
                allowed_ds.discard(None)

        # Si hay filtros limitar los resultados
        if allowed_ds:
            self._filter_results_by_allowed_ds(results, allowed_ds)

        return (
            results.get("titulos", {}),
            results.get("descripciones", {}),
            results.get("cabeceras", {}),
            results.get("contenido", {}),
        )

# ============================================================================================================================== #
# Funcionalidad de clustering no definitiva
    """
        Agrupa resultados que tengan títulos semánticamente similares.
        :param items: lista de diccionarios con al menos el campo 'title'
        :param eps: umbral de agrupación para DBSCAN (distancia coseno)
        :param min_samples: mínimo de elementos por grupo
        :return: lista de grupos, cada uno es un dict con 'representative_title' e 'items'
    """
    def group_by_similar_titles(self, items, eps=0.20, min_samples=2):   
        if not items:
            print("[CLUSTER] Lista de items vacía, no hay nada que agrupar.")
            return []

        print(f"[CLUSTER] Iniciando agrupación de {len(items)} elementos")

        # Extraer títulos
        titulos = [item['title'] for item in items]
        titulos = [normalizar_texto(titulo) for titulo in titulos]

        # Obtener embeddings normalizados
        embeddings = self.model.encode(titulos, normalize_embeddings=True)

        # Clustering por similitud semántica
        clustering = DBSCAN(eps=eps, min_samples=min_samples, metric='cosine')
        labels = clustering.fit_predict(embeddings)

        print(f"[CLUSTER] Etiquetas asignadas por clustering: {labels}")
        n_clusters = len(set(labels)) - (1 if -1 in labels else 0)
        n_outliers = list(labels).count(-1)

        print(f"[CLUSTER] Clusters formados (excluyendo outliers): {n_clusters}")
        print(f"[CLUSTER] Items no agrupados (outliers): {n_outliers}")

        # Agrupar por etiqueta
        grupos = defaultdict(list)
        for label, item in zip(labels, items):
            grupos[label].append(item)

        # Estructura de salida agrupada
        agrupado = []
        for label, grupo in grupos.items():
            titulos_grupo = [item["title"] for item in grupo]

            if label == -1:
                for item in grupo:
                    agrupado.append({
                        "representative_title": item["title"],
                        "items": [item]
                    })
                continue

            # Generar título representativo con LLM
            # titulo_representativo = self.generative.generate_group_title(titulos_grupo)
            titulo_representativo = titulos_grupo[0]
            
            print(f"[CLUSTER] Grupo {label} con {len(grupo)} elementos.")
            print(f"[CLUSTER] Título representativo generado: '{titulo_representativo}'")

            # Extraer fechas
            fechas = []
            geos = []
            for item in grupo:
                json_object = json.loads(item["json"])
                temporal = json_object.get("temporal")
                geo = json_object.get("geo")
                if geo:
                    geos.append(geo)

                # Normaliza: si es dict, lo meto en una lista
                if isinstance(temporal, dict):
                    temporal = [temporal]
                elif not isinstance(temporal, list):
                    temporal = []

                for t in temporal:
                    if not isinstance(t, dict):
                        print(f"[WARNING] Formato inesperado en temporal: {t}")
                        continue
                    # Sacar fechas
                    start = t.get("startDate")
                    end = t.get("endDate")
                    try:
                        if start:
                            fechas.append(parse(self.traducir_fecha_es(start)))
                        if end:
                            fechas.append(parse(self.traducir_fecha_es(end)))
                    except Exception as e:
                        self.logger.exception(f"[WARNING] Error parseando fechas: {start}, {end} → {e}")
                        print(f"[WARNING] Error parseando fechas: {start}, {end} → {e}")

                # Seleccionar máximo y mínmimo
                fecha_min = min(fechas).strftime("%Y-%m-%d") if fechas else None
                fecha_max = max(fechas).strftime("%Y-%m-%d") if fechas else None

            print(f"[CLUSTER] Meta Temp: [{fecha_min} -> {fecha_max}]")
            if len(geos) > 0:
                # Extrae comunidad, provincia y municipio por cada geo
                comun_set = set()
                prov_set = set()
                muni_set = set()
                for geo in geos: 
                    geo_str = str(geo).strip()
                    matches = self.df_jerarquia[
                        (self.df_jerarquia["municipio"] == geo_str) |
                        (self.df_jerarquia["provincia"] == geo_str) |
                        (self.df_jerarquia["comunidad_au"] == geo_str)
                    ]
                    if not matches.empty: # Añade nuevos geos únicos
                        comun_set.update(matches["comunidad_au"].unique())
                        prov_set.update(matches["provincia"].unique())
                        muni_set.update(matches["municipio"].unique())
                    else:
                        print(f"[GEO] No se encontró '{geo_str}' en jerarquía")

                # Lógica de fusión jerárquica
                geo_final = None
                if len(comun_set) == 1 and len(prov_set) >= 1: # Si coinciden los geos a nivel de comunidad
                    geo_final = list(comun_set)[0].title()
                elif len(prov_set) == 1: # Si coinciden los geos a nivel de provincia
                    geo_final = list(prov_set)[0].title()
                elif len(muni_set) == 1: # Si coinciden los geos a nivel de municipio
                    geo_final = list(muni_set)[0].title()
                else:
                    geo_final = "España"

                print(f"[CLUSTER] Meta Geo: [{geo_final}]")

            print(" - Títulos en el grupo:")
            for item in grupo:
                json_object = json.loads(item["json"])

                print(f"{item['title']} [{json_object.get('geo')}] [{json_object.get('temporal')}]")

            agrupado.append({
                "representative_title": titulo_representativo,
                "items": grupo
            })
            print("------------------------")
        return agrupado