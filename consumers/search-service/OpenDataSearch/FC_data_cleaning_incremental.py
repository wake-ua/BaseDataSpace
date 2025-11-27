import os
import json
import requests
from typing import Any, Dict, Iterable, List
from datetime import datetime, timedelta, timezone

# ============================================================
# CONFIG
# ============================================================

FC_QUERY_URL = os.environ.get(
    "FC_QUERY_URL",
    "http://federated-catalog:39195/api/catalog/v1alpha/catalog/query"
)

OUT_DIR = os.environ.get("OUT_DIR", "/app/datos")

LIMIT = int(os.environ.get("LIMIT", "200"))
OFFSET_START = int(os.environ.get("OFFSET", "0"))
WRITE_SAMPLES = os.environ.get("WRITE_SAMPLES", "true").lower() == "true"


# ============================================================
# TIEMPO: última hora + 10 minutos
# ============================================================

def get_recent_timestamp():
    now = datetime.now(timezone.utc)
    delta = timedelta(hours=1, minutes=10)
    return (now - delta).isoformat()


# ============================================================
# Helpers de extracción
# ============================================================

def extract_text(x: Any) -> str:
    if x is None:
        return ""
    if isinstance(x, str):
        return x
    if isinstance(x, dict):
        if "value" in x and isinstance(x["value"], str):
            return x["value"]
        for lang in ("es", "en"):
            v = x.get(lang)
            if isinstance(v, str):
                return v
            if isinstance(v, list):
                for it in v:
                    if isinstance(it, str) and it.strip():
                        return it
                    if isinstance(it, dict) and isinstance(it.get("value"), str):
                        return it["value"]
        for v in x.values():
            if isinstance(v, str) and v.strip():
                return v
            if isinstance(v, list):
                for it in v:
                    if isinstance(it, str) and it.strip():
                        return it
                    if isinstance(it, dict) and isinstance(it.get("value"), str):
                        return it["value"]
    if isinstance(x, list):
        for it in x:
            if isinstance(it, str) and it.strip():
                return it
            if isinstance(it, dict) and isinstance(it.get("value"), str):
                return it["value"]
    return ""


def iter_datasets_from_catalog(catalog_obj: Any) -> Iterable[Dict[str, Any]]:
    if not isinstance(catalog_obj, dict):
        return []
    ds = catalog_obj.get("dcat:dataset")
    if isinstance(ds, list):
        for it in ds:
            if isinstance(it, dict):
                yield it
    elif isinstance(ds, dict):
        yield ds


# ============================================================
# Construcción de meta_*.json (IGUAL que script original)
# ============================================================

def build_meta_json_from_item(item: Dict[str, Any]) -> Dict[str, Any]:
    meta: Dict[str, Any] = {}

    asset_id = item.get("@id") or item.get("id")
    participant = item.get("dspace:participantId", "")
    meta["identifier"] = f"{asset_id}::{participant}"

    meta["type"] = item.get("@type")

    title = extract_text(item.get("dct:title") or item.get("title"))
    description = extract_text(item.get("dct:description") or item.get("description"))
    if not title:
        title = "1"
    if not description:
        description = "1"

    meta["title"] = title
    meta["description"] = description

    # samples
    is_sample_of = item.get("cbm:isSampleOf")
    if is_sample_of:
        meta["isSampleOf"] = is_sample_of

    dists = item.get("dcat:distribution") or []
    if isinstance(dists, dict):
        dists = [dists]

    resources = []
    headers_level_dataset = 0

    for idx, d in enumerate(dists):
        if not isinstance(d, dict):
            continue

        dd = d.get("cbm:hasDataDictionary") or {}
        fields = dd.get("cbm:hasField") or []

        field_names = []

        for f in fields:
            if isinstance(f, dict):
                n = f.get("schema:name") or f.get("name")
                if isinstance(n, str) and n.strip():
                    field_names.append({"name": n})

        if not field_names:
            field_names = [{"name": "dummy_field"}]

        header_count = max(1, len(field_names))

        dummy_rows = [{"dummy_field": 1}]

        resources.append({
            "name": f"distribution_{idx+1}",
            "schema": {"fields": field_names},
            "header_count": header_count,
            "dummy_content": dummy_rows
        })

        headers_level_dataset = max(headers_level_dataset, header_count)

    if not resources:
        resources = [{
            "name": "distribution_1",
            "schema": {"fields": [{"name": "dummy_field"}]},
            "header_count": 1,
            "dummy_content": [{"dummy_field": 1}]
        }]
        headers_level_dataset = max(headers_level_dataset, 1)

    meta["resources"] = resources

    meta["levels"] = {
        "title": 1,
        "description": 1,
        "headers": max(1, headers_level_dataset),
        "content": 1
    }

    return meta


# ============================================================
# Escritura del meta_*.json
# ============================================================

def write_meta_file(meta: Dict[str, Any], out_dir: str) -> str:
    os.makedirs(out_dir, exist_ok=True)
    identifier = meta["identifier"].replace("/", "_").replace(":", "_").replace("\\", "_")
    fname = f"meta_{identifier}.json"
    fpath = os.path.join(out_dir, fname)
    with open(fpath, "w", encoding="utf-8") as fh:
        json.dump(meta, fh, ensure_ascii=False, indent=2)
    return fpath


# ============================================================
# Limpieza del OUT_DIR
# ============================================================

def clear_out_dir(out_dir: str):
    if not os.path.isdir(out_dir):
        return
    for name in os.listdir(out_dir):
        if name.startswith("meta_") and name.endswith(".json"):
            try:
                os.remove(os.path.join(out_dir, name))
            except:
                pass


# ============================================================
# Consulta incremental al FC
# ============================================================

def fetch_catalog_incremental(offset=None, limit=None):
    body = {
        "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/", "dct": "http://purl.org/dc/terms/"},
        "@type": "QuerySpec",
        "filterExpression": [
            {
                "@type": "Criterion",
                "operandLeft": "dct:modified",
                "operator": ">=",
                "operandRight": get_recent_timestamp()
            }
        ]
    }

    if offset is not None:
        body["offset"] = offset
    if limit is not None:
        body["limit"] = limit

    r = requests.post(FC_QUERY_URL, json=body, timeout=30)
    r.raise_for_status()
    resp = r.json()

    # formatos flexibles como el script oficial
    if isinstance(resp, list):
        if resp:
            return resp[0]
        return {"dcat:dataset": []}

    if isinstance(resp, dict):
        if "results" in resp:
            return resp["results"][0] if resp["results"] else {"dcat:dataset": []}
        if "dcat:dataset" in resp:
            return resp

    return {"dcat:dataset": []}


# ============================================================
# MAIN
# ============================================================

def main():
    print(f"[INFO] FC incremental: {FC_QUERY_URL}")
    print(f"[INFO] Output dir:     {OUT_DIR}")
    print(f"[INFO] Limit:           {LIMIT}")

    clear_out_dir(OUT_DIR)

    offset = OFFSET_START
    total_written = 0
    page = 0

    while True:
        page += 1
        catalog = fetch_catalog_incremental(offset=offset, limit=LIMIT)
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
            write_meta_file(meta, OUT_DIR)
            total_written += 1

        print(f"[INFO] Página {page}: generados {total_written} ficheros")

        if len(items) < LIMIT:
            break

        offset += LIMIT

    print(f"[OK] Incremental completo. Total: {total_written}")


if __name__ == "__main__":
    main()
