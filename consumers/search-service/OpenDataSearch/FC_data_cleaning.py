# fc_to_indexer_meta.py (versión corregida)
import os
import json
import requests
from typing import Any, Dict, Iterable, List

FC_QUERY_URL = os.environ.get(
    "FC_QUERY_URL",
    "http://localhost:39195/api/catalog/v1alpha/catalog/query"
)

OUT_DIR = os.environ.get(
    "OUT_DIR",
    "C:\\Users\\Selles\\git\\BaseDataSpace\\consumers\\search-service\\OpenDataSearch\\datos"
)


LIMIT = int(os.environ.get("LIMIT", "200"))     # por llamada
OFFSET_START = int(os.environ.get("OFFSET", "0"))
WRITE_SAMPLES = os.environ.get("WRITE_SAMPLES", "true").lower() == "true"

# ---------- Utilidades de extracción de texto (title/description) ----------
def extract_text(x: Any) -> str:
    if x is None:
        return ""
    if isinstance(x, str):
        return x
    if isinstance(x, dict):
        # Formato {'value': '...'}
        if 'value' in x and isinstance(x['value'], str):
            return x['value']
        # Idiomas preferidos
        for lang in ('es', 'en'):
            v = x.get(lang)
            if isinstance(v, str):
                return v
            if isinstance(v, list):
                for vv in v:
                    if isinstance(vv, str) and vv.strip():
                        return vv
                    if isinstance(vv, dict) and isinstance(vv.get('value'), str):
                        return vv['value']
        # fallback: primer string en valores
        for v in x.values():
            if isinstance(v, str) and v.strip():
                return v
            if isinstance(v, list):
                for vv in v:
                    if isinstance(vv, str) and vv.strip():
                        return vv
                    if isinstance(vv, dict) and isinstance(vv.get('value'), str):
                        return vv['value']
    if isinstance(x, list):
        for it in x:
            if isinstance(it, str) and it.strip():
                return it
            if isinstance(it, dict) and isinstance(it.get('value'), str):
                return it['value']
    return ""

def iter_datasets_from_catalog(catalog_obj: Any) -> Iterable[Dict[str, Any]]:
    """
    Devuelve los items de catalog["dcat:dataset"] (que incluyen dcat:Dataset y cbm:Sample).
    """
    if not isinstance(catalog_obj, dict):
        return
    ds = catalog_obj.get("dcat:dataset")
    if isinstance(ds, list):
        for it in ds:
            if isinstance(it, dict):
                yield it
    elif isinstance(ds, dict):
        yield ds

# ---------- Normalización a meta_*.json ----------
def build_meta_json_from_item(item: Dict[str, Any]) -> Dict[str, Any]:
    """
    Convierte un dcat:Dataset o cbm:Sample a meta_* minimalista para el indexador:
    Incluye valores dummy si no hay cabeceras o contenido real.
    Además, asegura niveles/columnas para títulos, descripciones, cabeceras y contenidos (>=1).
    """
    meta: Dict[str, Any] = {}
    meta["identifier"]  = item.get("@id") or item.get("id")
    meta["type"]        = item.get("@type")

    # Título y descripción: si vienen vacíos, poner "1" para no ser descartados por el indexador
    title = extract_text(item.get("dct:title") or item.get("title") or "")
    description = extract_text(item.get("dct:description") or item.get("description") or "")

    if not title or not str(title).strip():
        title = "1"
    if not description or not str(description).strip():
        description = "1"

    meta["title"]       = title
    meta["description"] = description

    # Si es un sample, guarda la relación
    is_sample_of = item.get("cbm:isSampleOf")
    if is_sample_of:
        meta["isSampleOf"] = is_sample_of

    # Recursos a partir de distribuciones con data dictionary
    resources: List[Dict[str, Any]] = []
    dists = item.get("dcat:distribution") or []
    if isinstance(dists, dict):
        dists = [dists]

    # Para calcular el nivel de cabeceras (>=1) a nivel dataset
    headers_level_dataset = 0

    for idx, d in enumerate(dists):
        if not isinstance(d, dict):
            continue
        dd = d.get("cbm:hasDataDictionary") or {}
        fields = dd.get("cbm:hasField") or []
        field_names = []
        for f in fields:
            if isinstance(f, dict):
                n = f.get("name")
                if isinstance(n, str) and n.strip():
                    field_names.append({"name": n})

        # Si no hay campos, crea dummy
        if not field_names:
            field_names = [{"name": "dummy_field"}]

        # Conteo de cabeceras para este recurso (siempre >=1)
        header_count = max(1, len(field_names))

        # Rellenar contenido de ejemplo (para evitar vacío)
        dummy_rows = [{"dummy_field": 1}]  # valor fijo, sin significado

        resources.append({
            "name": f"distribution_{idx+1}",
            "schema": {"fields": field_names},
            "header_count": header_count,     # <-- nuevo: nivel/columnas de cabeceras por recurso (>=1)
            "dummy_content": dummy_rows       # <-- clave adicional para indexador
        })

        headers_level_dataset = max(headers_level_dataset, header_count)

    # Si no hay recursos, crear uno vacío por defecto (con niveles mínimos)
    if not resources:
        resources = [{
            "name": "distribution_1",
            "schema": {"fields": [{"name": "dummy_field"}]},
            "header_count": 1,
            "dummy_content": [{"dummy_field": 1}]
        }]
        headers_level_dataset = max(headers_level_dataset, 1)

    meta["resources"] = resources

    # ----- Bloque de niveles/columnas a nivel dataset (siempre >=1) -----
    # Nota: "content" se fija a 1 como placeholder. Si más adelante quieres
    # reflejar número de líneas indexadas reales, podría calcularse en el script 2.
    meta["levels"] = {
        "title": 1,                     # hay algo (aunque sea "1")
        "description": 1,               # hay algo (aunque sea "1")
        "headers": max(1, headers_level_dataset),
        "content": 1                    # placeholder mínimo
    }

    return meta


def write_meta_file(meta: Dict[str, Any], out_dir: str) -> str:
    os.makedirs(out_dir, exist_ok=True)
    # Nombre de fichero seguro
    identifier = (meta.get("identifier") or "sin_id").replace("/", "_").replace("\\", "_").replace(":", "_")
    fname = f"meta_{identifier}.json"
    fpath = os.path.join(out_dir, fname)
    with open(fpath, "w", encoding="utf-8") as fh:
        json.dump(meta, fh, ensure_ascii=False, indent=2)
    return fpath

# ---------- Descarga del FC con paginación ----------
def fetch_catalog(offset: int | None = None, limit: int | None = None) -> Dict[str, Any]:
    """
    Pide el catálogo al FC. Compatible con:
      - respuesta lista [ {dcat:Catalog} ]
      - respuesta dict   {dcat:dataset: [...]}
      - respuesta con envoltorio {"results": [ {dcat:Catalog} ]}
    Si la respuesta es vacía, devuelve un catálogo vacío {'dcat:dataset': []} en lugar de lanzar.
    """
    # Cuerpo mínimo (tu Postman)
    body = {
        "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
        "@type": "QuerySpec",
    }

    # Solo añade offset/limit si realmente se pasan (tu despliegue no los necesita)
    if offset is not None:
        body["offset"] = offset
    if limit is not None:
        body["limit"] = limit

    r = requests.post(
        FC_QUERY_URL,
        json=body,
        headers={"Accept": "application/json"},
        timeout=30
    )
    r.raise_for_status()

    # Intenta parsear JSON
    try:
        resp = r.json()
    except Exception as e:
        snippet = (r.text or "")[:400]
        raise RuntimeError(f"FC respondió contenido no JSON. Muestra: {snippet}") from e

    # --- CASO A: lista con 0..n catálogos ---
    if isinstance(resp, list):
        if resp:
            return resp[0]
        # lista vacía -> devuelve catálogo vacío para que el caller pare sin excepción
        return {"dcat:dataset": []}

    # --- CASO B: dict con results o con catálogo directo ---
    if isinstance(resp, dict):
        if "results" in resp and isinstance(resp["results"], list):
            if resp["results"]:
                return resp["results"][0]
            return {"dcat:dataset": []}
        if resp.get("@type") == "dcat:Catalog" or "dcat:dataset" in resp:
            # asegura la clave dcat:dataset aunque esté ausente
            if "dcat:dataset" not in resp:
                resp["dcat:dataset"] = []
            return resp

    # --- Formato inesperado: devuelve catálogo vacío para cortar limpio ---
    return {"dcat:dataset": []}


def main():
    print(f"Consultando FC en: {FC_QUERY_URL}")
    print(f"Directorio de salida: {OUT_DIR}")
    print(f"Incluyendo samples: {WRITE_SAMPLES}")
    print("Generando meta_*.json…")

    offset = OFFSET_START
    total_written = 0
    page = 0

    while True:
        page += 1
        catalog = fetch_catalog(offset=offset, limit=LIMIT)
        items = list(iter_datasets_from_catalog(catalog))
        if not items:
            break

        for it in items:
            typ = it.get("@type", "")
            if typ == "cbm:Sample" and not WRITE_SAMPLES:
                continue
            if typ not in ("dcat:Dataset", "cbm:Sample"):
                continue

            meta = build_meta_json_from_item(it)
            path = write_meta_file(meta, OUT_DIR)
            total_written += 1

        print(f"Página {page}: escritos {total_written} ficheros (offset {offset})")

        # Si devolvió menos que LIMIT, ya no hay más
        if len(items) < LIMIT:
            break
        offset += LIMIT

    print(f"Listo. Total ficheros generados: {total_written}")

if __name__ == "__main__":
    main()
