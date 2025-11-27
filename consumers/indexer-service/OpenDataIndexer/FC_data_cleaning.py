import os
import json
import csv
import requests
import traceback
from typing import Any, Dict, List
from urllib.parse import quote

# ==========================================================================================
# CONFIG
# ==========================================================================================

FC_QUERY_URL = os.getenv("FC_QUERY_URL", "http://federated-catalog:39195/api/catalog/v1alpha/catalog/query")
DATA_DIR = os.getenv("DATA_DIR", "/app/datos")
LIMIT = int(os.getenv("LIMIT", "200"))
WRITE_SAMPLES = os.getenv("WRITE_SAMPLES", "true").lower() == "true"

SAMPLES_BASE_URL = os.getenv("SAMPLES_BASE_URL", "http://samples-server:9099")  
# En local sería: "http://localhost:9099"


os.makedirs(DATA_DIR, exist_ok=True)


# ==========================================================================================
# HELPERS DE TEXTO (igual que tu script original)
# ==========================================================================================

def extract_text(x: Any) -> str:
    if x is None:
        return ""
    if isinstance(x, str):
        return x.strip()
    if isinstance(x, dict):
        if "value" in x and isinstance(x["value"], str):
            return x["value"].strip()
        for lang in ("es", "en"):
            v = x.get(lang)
            if isinstance(v, str):
                return v.strip()
            if isinstance(v, list):
                for it in v:
                    if isinstance(it, str):
                        return it
                    if isinstance(it, dict) and isinstance(it.get("value"), str):
                        return it["value"]
        for v in x.values():
            if isinstance(v, str) and v.strip():
                return v
    return ""


def iter_datasets_from_catalog(catalog_obj: Any):
    if not isinstance(catalog_obj, dict):
        return []
    ds = catalog_obj.get("dcat:dataset")
    if isinstance(ds, list):
        for it in ds:
            if isinstance(it, dict):
                yield it
    elif isinstance(ds, dict):
        yield ds


# ==========================================================================================
# DESCARGA DE SAMPLE (JSON o CSV)
# ==========================================================================================

def download_sample(sample_id: str) -> str | None:
    """
    Descarga sample JSON o CSV desde sample-server,
    con logs detallados para depuración.
    """

    json_url = f"{SAMPLES_BASE_URL}/{sample_id}.json"
    csv_url  = f"{SAMPLES_BASE_URL}/{sample_id}.csv"

    print(f"\n[SAMPLE] Intentando descargar sample '{sample_id}'")
    print(f"[SAMPLE] URL JSON: {json_url}")
    print(f"[SAMPLE] URL CSV : {csv_url}")

    # -----------------------
    # 1) Intentar JSON
    # -----------------------
    try:
        print(f"[SAMPLE] GET JSON → {json_url}")
        r = requests.get(json_url, timeout=10)

        print(f"[SAMPLE] JSON status_code = {r.status_code}")
        print(f"[SAMPLE] JSON response (primeros 300 chars):\n{r.text[:300]}")

        if r.status_code == 200 and r.text.strip():
            json_path = os.path.join(DATA_DIR, f"{sample_id}.json")
            with open(json_path, "w", encoding="utf-8") as f:
                f.write(r.text)
            print(f"[SAMPLE] JSON guardado en: {json_path}")

            # Convertir a CSV
            csv_path = convert_json_to_csv(json_path)
            print(f"[SAMPLE] JSON → CSV convertido: {csv_path}")
            return os.path.basename(csv_path)

    except Exception as e:
        print(f"[ERROR] Excepción descargando JSON sample {json_url}: {e}")
        traceback.print_exc()

    # -----------------------
    # 2) Intentar CSV
    # -----------------------
    try:
        print(f"[SAMPLE] GET CSV → {csv_url}")
        r = requests.get(csv_url, timeout=10)

        print(f"[SAMPLE] CSV status_code = {r.status_code}")
        print(f"[SAMPLE] CSV response (primeros 300 chars):\n{r.text[:300]}")

        if r.status_code == 200 and r.text.strip() and not r.text.startswith("<"):
            csv_path = os.path.join(DATA_DIR, f"{sample_id}.csv")
            with open(csv_path, "w", encoding="utf-8") as f:
                f.write(r.text)
            print(f"[SAMPLE] CSV guardado en: {csv_path}")
            return os.path.basename(csv_path)

    except Exception as e:
        print(f"[ERROR] Excepción descargando CSV sample {csv_url}: {e}")
        traceback.print_exc()

    print(f"[ERROR] No se pudo descargar sample '{sample_id}' ni en JSON ni CSV\n")
    return None



# ==========================================================================================
# CONVERSIÓN JSON → CSV
# ==========================================================================================

def convert_json_to_csv(json_path: str) -> str:
    """
    Convierte un listado JSON [{...}, {...}] a CSV simple.
    Lo guarda junto al JSON.
    """
    try:
        with open(json_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        if not isinstance(data, list) or not data:
            print(f"[WARN] JSON vacío o no es lista en {json_path}. Se usará dummy CSV.")
            return create_dummy_csv(json_path)

        # cabeceras = union de todas las keys
        headers = set()
        for row in data:
            if isinstance(row, dict):
                headers.update(row.keys())

        headers = list(headers)

        csv_path = json_path.replace(".json", ".csv")

        with open(csv_path, "w", encoding="utf-8", newline="") as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=headers)
            writer.writeheader()
            for row in data:
                if isinstance(row, dict):
                    writer.writerow(row)
                else:
                    pass  # ignorar elementos no dict

        print(f"[SAMPLE] Convertido JSON → CSV: {csv_path}")
        return csv_path

    except Exception as e:
        print(f"[ERROR] Fallo convirtiendo JSON a CSV: {e}")
        return create_dummy_csv(json_path)


def create_dummy_csv(json_path: str) -> str:
    """Genera CSV con una fila dummy."""
    csv_path = json_path.replace(".json", ".csv")
    with open(csv_path, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["dummy_field"])
        w.writerow([1])
    print(f"[SAMPLE] CSV dummy generado: {csv_path}")
    return csv_path


# ==========================================================================================
# CONSTRUIR META JSON COMPLETO (EXTENDIDO CON SAMPLE)
# ==========================================================================================

def build_meta_from_item(item: Dict[str, Any]) -> Dict[str, Any]:

    meta = {}

    # IDENTIFICADOR UNIVERSAL
    asset_id = item.get("@id") or item.get("id")
    participant = item.get("dspace:participantId", "unknown")
    meta_id = f"{asset_id}::{participant}"
    meta["identifier"] = meta_id

    # Título / descripción solo si vienen
    title = extract_text(item.get("dct:title") or item.get("title") or "")
    description = extract_text(item.get("dct:description") or item.get("description") or "")
    if not title:
        title = "1"
    if not description:
        description = "1"
    meta["title"] = title
    meta["description"] = description

    # SAMPLE
    sample_id = item.get("cbm:hasSample") or item.get("cbm:sample")
    sample_path = None

    if sample_id and WRITE_SAMPLES:
        print(f"[SAMPLE] Intentando descargar sample {sample_id}")
        sample_file = download_sample(sample_id)
        if sample_file:
            sample_path = sample_file

    # Construcción de recursos
    resources = []

    if sample_path:
        # recurso REAL
        resources.append({
            "name": "sample_resource",
            "path": sample_path,  # index_limpio usará este archivo CSV
            "schema": {"fields": []}  # Rellenará index_limpio si hace falta
        })
        meta["levels"] = {
            "title": 1,
            "description": 1,
            "headers": 1,   # index_limpio lo actualizará según el CSV
            "content": 1
        }
    else:
        # recurso dummy
        resources.append({
            "name": "dummy_resource",
            "schema": {"fields": [{"name": "dummy_field"}]},
            "path": "",  # sin sample
            "dummy_content": [{"dummy_field": 1}]
        })
        meta["levels"] = {
            "title": 1,
            "description": 1,
            "headers": 1,
            "content": 1
        }

    meta["resources"] = resources
    return meta


# ==========================================================================================
# GUARDAR META JSON
# ==========================================================================================

def save_meta(meta: Dict[str, Any]):
    safe_id = meta["identifier"].replace("/", "_").replace(":", "_")
    path = os.path.join(DATA_DIR, f"meta_{safe_id}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2, ensure_ascii=False)
    return path


# ==========================================================================================
# CONSULTA AL FC
# ==========================================================================================

def fetch_catalog():
    body = {
        "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
        "@type": "QuerySpec",
        "offset": 0,
        "limit": LIMIT
    }

    r = requests.post(FC_QUERY_URL, json=body, timeout=30)
    r.raise_for_status()

    resp = r.json()

    if isinstance(resp, list):
        return resp[0] if resp else {"dcat:dataset": []}

    if "results" in resp:
        return resp["results"][0] if resp["results"] else {"dcat:dataset": []}

    return resp


# ==========================================================================================
# MAIN
# ==========================================================================================

def main():
    print("\n=== FC_data_cleaning (con soporte de samples) ===\n")

    catalog = fetch_catalog()
    datasets = list(iter_datasets_from_catalog(catalog))

    print(f"[INFO] Datasets encontrados: {len(datasets)}")

    total = 0

    for item in datasets:
        try:
            meta = build_meta_from_item(item)
            save_meta(meta)
            total += 1
        except Exception as e:
            print("[ERROR] Procesando dataset:")
            traceback.print_exc()

    print(f"\n=== FINISHED: {total} meta_*.json generados ===")


if __name__ == "__main__":
    main()
