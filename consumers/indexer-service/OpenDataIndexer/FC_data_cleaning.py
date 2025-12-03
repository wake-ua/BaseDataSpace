import os
import json
import csv
import requests
import traceback
from typing import Any, Dict

# ==========================================================================================
# CONFIG
# ==========================================================================================

FC_QUERY_URL = os.getenv("FC_QUERY_URL", "http://federated-catalog:39195/api/catalog/v1alpha/catalog/query")
DATA_DIR = os.getenv("DATA_DIR", "/app/datos")
SAMPLES_DIR = os.getenv("SAMPLES_DIR", "/app/samples")

LIMIT = int(os.getenv("LIMIT", "200"))
WRITE_SAMPLES = True  # fuerza samples ON

SAMPLES_BASE_URL = os.getenv("SAMPLES_BASE_URL", "http://samples-server:9099")

DATA_SAMPLES_DIR = os.path.join(DATA_DIR, "samples")

# ==========================================================================================
# LIMPIEZA INICIAL
# ==========================================================================================

def limpiar_directorio(path):
    print(f"[CLEAN] Limpiando {path} ...")
    if os.path.exists(path):
        for f in os.listdir(path):
            fp = os.path.join(path, f)
            try:
                if os.path.isfile(fp):
                    os.remove(fp)
                else:
                    import shutil
                    shutil.rmtree(fp)
            except Exception as e:
                print(f"[CLEAN][ERROR] {fp}: {e}")
    else:
        os.makedirs(path, exist_ok=True)

print(f"[INIT] DATA_DIR = {DATA_DIR}")
print(f"[INIT] SAMPLES_DIR = {SAMPLES_DIR}")
print(f"[INIT] DATA_SAMPLES_DIR = {DATA_SAMPLES_DIR}")

limpiar_directorio(DATA_DIR)
limpiar_directorio(SAMPLES_DIR)
os.makedirs(DATA_DIR, exist_ok=True)
os.makedirs(SAMPLES_DIR, exist_ok=True)
os.makedirs(DATA_SAMPLES_DIR, exist_ok=True)

# ==========================================================================================
# HELPERS TEXTO
# ==========================================================================================

def extract_text(x: Any) -> str:
    if x is None:
        return ""
    if isinstance(x, str):
        return x.strip()
    if isinstance(x, dict):
        if "value" in x:
            return str(x["value"]).strip()
        for lang in ("es", "en"):
            v = x.get(lang)
            if isinstance(v, str):
                return v.strip()
            if isinstance(v, list):
                for it in v:
                    if isinstance(it, str):
                        return it
                    if isinstance(it, dict) and "value" in it:
                        return str(it["value"]).strip()
        for v in x.values():
            if isinstance(v, str):
                return v.strip()
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
# DESCARGA DE SAMPLES
# ==========================================================================================

def download_sample(sample_id: str) -> str | None:
    print(f"\n[SAMPLE] Descargando {sample_id}")

    json_url = f"{SAMPLES_BASE_URL}/{sample_id}.json"
    csv_url  = f"{SAMPLES_BASE_URL}/{sample_id}.csv"

    # — INTENTAR JSON —
    try:
        r = requests.get(json_url, timeout=10)
        if r.status_code == 200 and r.text.strip():
            json_path = os.path.join(SAMPLES_DIR, f"{sample_id}.json")
            with open(json_path, "w", encoding="utf-8") as f:
                f.write(r.text)
            print(f"[SAMPLE] JSON guardado en {json_path}")

            # convertir a CSV
            csv_path = convert_json_to_csv(json_path)
            final_path = mover_csv_a_data(csv_path, sample_id)
            return os.path.basename(final_path)
    except Exception as e:
        print("[SAMPLE][JSON ERROR]")
        traceback.print_exc()

    # — INTENTAR CSV —
    try:
        r = requests.get(csv_url, timeout=10)
        if r.status_code == 200 and r.text.strip() and not r.text.startswith("<"):
            tmp_csv = os.path.join(SAMPLES_DIR, f"{sample_id}.csv")
            with open(tmp_csv, "w", encoding="utf-8") as f:
                f.write(r.text)
            print(f"[SAMPLE] CSV guardado temporalmente: {tmp_csv}")

            final = mover_csv_a_data(tmp_csv, sample_id)
            return os.path.basename(final)
    except Exception:
        traceback.print_exc()

    print(f"[SAMPLE][ERROR] No se pudo descargar {sample_id}")
    return None

# ==========================================================================================
# CONVERSIÓN JSON → CSV
# ==========================================================================================

def convert_json_to_csv(json_path: str) -> str:
    try:
        with open(json_path, "r", encoding="utf-8") as f:
            rows = json.load(f)

        if not isinstance(rows, list) or not rows:
            return create_dummy_csv(json_path)

        headers = set()
        for row in rows:
            if isinstance(row, dict):
                headers.update(row.keys())

        csv_path = json_path.replace(".json", ".csv")

        with open(csv_path, "w", encoding="utf-8", newline="") as f:
            w = csv.DictWriter(f, fieldnames=list(headers))
            w.writeheader()
            for row in rows:
                if isinstance(row, dict):
                    w.writerow(row)

        print(f"[SAMPLE] JSON→CSV convertido en {csv_path}")
        return csv_path

    except Exception as e:
        print("[SAMPLE][ERROR] Convertir JSON→CSV falló")
        traceback.print_exc()
        return create_dummy_csv(json_path)

def create_dummy_csv(json_path: str) -> str:
    csv_path = json_path.replace(".json", ".csv")
    with open(csv_path, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["dummy_field"])
        w.writerow([1])
    print(f"[SAMPLE] Dummy CSV creado en {csv_path}")
    return csv_path

# ==========================================================================================
# MOVER CSV A /app/datos/samples
# ==========================================================================================

def mover_csv_a_data(csv_path, sample_id):
    """
    Mueve el CSV final desde /app/samples/ → /app/datos/samples/
    usando shutil.move(), compatible con Docker (cross-device).
    """
    import shutil

    final_path = os.path.join(DATA_DIR, "samples", f"{sample_id}.csv")
    os.makedirs(os.path.dirname(final_path), exist_ok=True)

    print(f"[MOVE] Moviendo CSV de {csv_path}")
    print(f"[MOVE] Hacia {final_path}")

    try:
        shutil.move(csv_path, final_path)
        print(f"[MOVE] OK → {final_path}")
        return final_path
    except Exception as e:
        print(f"[MOVE ERROR] No se pudo mover CSV: {e}")
        return None


# ==========================================================================================
# META JSON
# ==========================================================================================

def build_meta_from_item(item: Dict[str, Any]) -> Dict[str, Any]:
    asset_id = item.get("@id") or item.get("id")
    participant = item.get("dspace:participantId", "unknown")
    meta_id = f"{asset_id}::{participant}"

    print(f"\n=== DATASET {meta_id} ===")

    meta = {"identifier": meta_id}

    meta["title"] = extract_text(item.get("dct:title") or item.get("title") or "") or "1"
    meta["description"] = extract_text(item.get("dct:description") or item.get("description") or "") or "1"

    sample_id = item.get("cbm:hasSample") or item.get("cbm:sample")

    sample_path = None
    if sample_id and WRITE_SAMPLES:
        fname = download_sample(sample_id)
        if fname:
            sample_path = f"samples/{fname}"

    if sample_path:
        resources = [{
            "name": "sample_resource",
            "path": sample_path,
            "schema": {"fields": []}
        }]
    else:
        resources = [{
            "name": "dummy_resource",
            "path": "",
            "schema": {"fields": [{"name": "dummy_field"}]},
            "dummy_content": [{"dummy_field": 1}]
        }]

    meta["resources"] = resources
    meta["levels"] = {"title": 1, "description": 1, "headers": 1, "content": 1}
    return meta

# ==========================================================================================
# GUARDAR META
# ==========================================================================================

def save_meta(meta: Dict[str, Any]):
    safe = meta["identifier"].replace("/", "_").replace(":", "_")
    path = os.path.join(DATA_DIR, f"meta_{safe}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2, ensure_ascii=False)
    print(f"[SAVE] meta guardado: {path}")
    return path

# ==========================================================================================
# CONSULTA AL FC
# ==========================================================================================

def fetch_catalog():
    print("[FC] Descargando catálogo…")

    body = {
        "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
        "@type": "QuerySpec",
        "offset": 0,
        "limit": LIMIT
    }

    r = requests.post(FC_QUERY_URL, json=body, timeout=30)
    r.raise_for_status()
    resp = r.json()

    # soportar varios formatos
    if isinstance(resp, list):
        return resp[0] if resp else {"dcat:dataset": []}
    if "results" in resp:
        return resp["results"][0] if resp["results"] else {"dcat:dataset": []}
    return resp

# ==========================================================================================
# MAIN
# ==========================================================================================

def main():
    print("\n=== FC_data_cleaning (samples + limpieza + paths correctos) ===\n")

    catalog = fetch_catalog()
    datasets = list(iter_datasets_from_catalog(catalog))

    print(f"[INFO] Datasets encontrados: {len(datasets)}")

    total = 0
    for item in datasets:
        try:
            meta = build_meta_from_item(item)
            save_meta(meta)
            total += 1
        except:
            traceback.print_exc()

    print(f"\n=== FINISHED: {total} meta_*.json generados ===\n")

if __name__ == "__main__":
    main()
