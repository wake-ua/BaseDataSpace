import json
import time
import hashlib
import pandas as pd
import numpy as np
from io import StringIO
from typing import Optional, List
# Servicios externos
import redis
from fastapi import FastAPI, Query, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.requests import Request
from fastapi import status
# Importación de componentes del buscador
from search import SearchEngine
from logger import LoggerOpenSearch, LoggerBuscador
from db import DbEngine, QdrantAccessException
from generative import GenerativeEngine
from semantic import SemanticEngine
from config import GENERATIVE_MODEL_NAME, SEMANTIC_MODEL_NAME, DESACTIVAR_CACHE_REDIS
# ============================================================================================================================== #
# Inicialización de FastAPI y de Redis
app = FastAPI()
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
# redis_client = redis.Redis(host='localhost', port=6379, db=0)
# Vincular otros componentes del buscador
log = LoggerOpenSearch()
logbus = LoggerBuscador()
generative = GenerativeEngine(GENERATIVE_MODEL_NAME, logbus)
db = DbEngine()
semantic = SemanticEngine(SEMANTIC_MODEL_NAME, db, generative, logbus)
search = SearchEngine(db, generative, semantic, logbus)

# Control de excepciones
@app.exception_handler(QdrantAccessException)
async def qdrant_exception_handler(request: Request, exc: QdrantAccessException):
    print(f"[QDRANT ERROR] {exc}")
    return JSONResponse(
        status_code=exc.status_code or 502,
        content={"detail": str(exc)}
    )
# ============================================================================================================================== #
# Enpoind principal, usado para las consultas, incluye query y filtros
@app.get("/search")
def search_api(
    request: Request,
    q: Optional[str] = None,
    theme: Optional[List[str]] = Query(None),
    geo: Optional[List[str]] = Query(None),
    temporal_start: Optional[str] = Query(None),
    temporal_end: Optional[str] = Query(None),
):
    client_ip = request.client.host
    start = time.time()
    
    # Generación de filtros según parametros de la petición
    filters = {"must": []}
    if theme:
        filters["must"].append({"key": "theme", "match": {"any": theme}})
    if geo:
        filters["must"].append({"key": "geo", "match": {"any": geo}})
    if temporal_start or temporal_end:
        if temporal_start:
            filters["must"].append({
                "key": "temporal.endDate", "range": {"gte": temporal_start}
            })
        if temporal_end:
            filters["must"].append({
                "key": "temporal.startDate", "range": {"lte": temporal_end}
            })
    if not filters["must"]:
        filters = None

    # Clave única (hash) para cada combinación de q + filtros
    cache_key = f"search:{hashlib.sha256(((q or '') + json.dumps(filters, sort_keys=True)).encode()).hexdigest()}"

    # Buscar la query en cache
    #cached_result = redis_client.get(cache_key)
    if DESACTIVAR_CACHE_REDIS:
        cached_result = None
    # Si esta en cache se recupera, si no, se ejecuta la busqueda y se almacena en cache
    if cached_result:
        r = json.loads(cached_result)
        r['cache'] = True 
    else:
        if not q:
            results = db.get_filtered_items(filters, limit=100)
            r = {
                "type": "filters_only",
                "total": {"value": len(results)},
                "hits": results,
                "intro": "",
                "additional": "",
            }
        else:
            r = search.search(q, filters=filters)
        #redis_client.setex(cache_key, 3600, json.dumps(r))
        r['cache'] = False

    end = time.time()
    response_time = end - start
    log.save_log(q, response_time, client_ip)
    r['response_time'] = round(response_time, 3)

    return r

# Enpoint para buscar un dataset dado el ID de un dataset 
@app.get("/dataset/{dataset_uid}")
def read_dataset(dataset_uid: str):
    data = db.get_item(dataset_uid)
    if not data or not data["hits"]:
        raise HTTPException(status_code=404, detail="No encontrado")
    return data["hits"][0]["_source"]

# Endpoint para generar sugerencias de análisis dado el ID de un dataset 
@app.get("/dataset/{dataset_uid}/suggestions")
def get_suggestions(dataset_uid: str):
    resp = generative.get_suggestions(dataset_uid, db)
    if not resp or "response" in resp and resp["response"] != "OK":
        # Si la generación de sugerencias falla o el dataset no existe
        logbus.error("Error durante la generación de sugerencias para dataset %s: %s", dataset_uid, resp)
        raise HTTPException(status_code=404, detail=resp.get("response", "Error generando sugerencias"))
    return resp

# Endpoint para extraer una muestra de lineas dado el ID de un recurso 
@app.get("/dataset/{dataset_uid}/sample")
def get_dataset_sample(dataset_uid: str):
    recursos = db.get_all_resources_by_dataset_uid("contenidos", dataset_uid)
    if not recursos:
        raise HTTPException(status_code=404, detail="No hay recursos CSV para este dataset")

    muestras = []
    for recurso in recursos:
        csv_text = recurso.get("text", "")
        resource_name = recurso.get("resource_fileName") or recurso.get("resource_name") or "Recurso"
        if not csv_text:
            continue
        try:
            df = pd.read_csv(StringIO(csv_text), engine='python', on_bad_lines="skip", encoding='utf-8')
            # Reemplazar NaN por None
            muestra = df.head(10).replace({np.nan: None}).to_dict(orient="records")
        except Exception as e:
            muestra = f"Error leyendo CSV: {e}"
        muestras.append({
            "resource_name": resource_name,
            "sample": muestra
        })

    if not muestras:
        raise HTTPException(status_code=404, detail="No se pudo obtener ninguna muestra")
    return {"response": muestras}

# Enpoint para buscar elementos con un titulo similar
@app.get("/similar/{titulo}")
def read_item(titulo: str):
    return semantic.similar_items(titulo)

# Control de errores
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    import traceback
    print(f"[ERROR] Unhandled exception in {request.url}: {exc}")
    logbus.error("Error inesperado en endpoint: %s", exc)
    traceback.print_exc()
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"detail": "Internal Server Error"}
    )