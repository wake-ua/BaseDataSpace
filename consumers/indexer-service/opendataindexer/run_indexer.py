import os
import sys
import traceback
import importlib
import runpy
import time
import requests


# -------------------------------------------------------------
# Esperas para asegurar que servicios externos están disponibles
# -------------------------------------------------------------
def wait_for_http(url, retries=30, delay=3):
    """
    Espera a que un servicio HTTP responda.
    Se considera OK cualquier código < 500 (200, 400, 404, etc.)
    """
    for i in range(retries):
        try:
            print(f"[Esperando] Intento {i+1}/{retries} → {url}", flush=True)
            r = requests.get(url, timeout=5)
            if r.status_code < 500:
                print(f"[OK] Servicio disponible: {url}", flush=True)
                return
        except Exception as e:
            print(f"[NO DISPONIBLE] {url} → {e}", flush=True)

        time.sleep(delay)

    raise RuntimeError(f"No se pudo conectar al servicio: {url} tras {retries} intentos")


# ----------------- etapa 1: fc_data_cleaning -----------------
def run_fc_data_cleaning():
    print("\n=== [1/2] Ejecutando FC_data_cleaning.main() ===", flush=True)

    try:
        mod = importlib.import_module("FC_data_cleaning")
        if hasattr(mod, "main"):
            mod.main()
        else:
            print("FC_data_cleaning no define main(); solo importando módulo.")
        print("FC_data_cleaning completado", flush=True)
    except Exception:
        print("Error en FC_data_cleaning", flush=True)
        traceback.print_exc()
        sys.exit(1)


# ----------------- etapa 2: index_limpio -----------------
def run_index_limpio():
    print("\n=== [2/2] Ejecutando index_limpio ===", flush=True)

    # Lee valores directamente del entorno
    QDRANT_URL   = os.getenv("QDRANT_URL", "http://qdrant:6333")
    QDRANT_KEY   = os.getenv("QDRANT_API_KEY", "")
    QDRANT_CA    = os.getenv("QDRANT_CACERT", "")
    DATA_DIR     = os.getenv("DATA_DIR", "/app/datos")

    INDEX_DEBUG              = os.getenv("INDEX_DEBUG", "true")
    INDEX_BATCH_SIZE         = os.getenv("INDEX_BATCH_SIZE", "4")
    INDEX_LINES_BATCH_SIZE   = os.getenv("INDEX_LINES_BATCH_SIZE", "4")
    INDEX_MAX_CONTENT_LINES  = os.getenv("INDEX_MAX_CONTENT_LINES", "25")
    INDEX_MAX_META_FILES     = os.getenv("INDEX_MAX_META_FILES")
    INDEX_SOLO_UN_RECURSO    = os.getenv("INDEX_SOLO_UN_RECURSO", "true")
    INDEX_MODEL_NAME         = os.getenv("INDEX_MODEL_NAME", "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")
    INDEX_MODEL_TO_16        = os.getenv("INDEX_MODEL_TO_16", "true")

    # init_globals para index_limpio
    init_globals = {
        "API_URL": QDRANT_URL,
        "API_KEY": QDRANT_KEY,
        "CACERT_PATH": QDRANT_CA,
        "DATA_PATH": DATA_DIR,

        "DEBUG": INDEX_DEBUG,
        "BATCH_SIZE": INDEX_BATCH_SIZE,
        "LINES_BATCH_SIZE": INDEX_LINES_BATCH_SIZE,
        "MAX_CONTENT_LINES": INDEX_MAX_CONTENT_LINES,
        "MAX_META_FILES": INDEX_MAX_META_FILES,
        "SOLO_UN_RECURSO": INDEX_SOLO_UN_RECURSO,

        "MODEL_NAME": INDEX_MODEL_NAME,
        "MODEL_TO_16": INDEX_MODEL_TO_16,
    }

    print("Configuración cargada correctamente. Lanzando index_limpio...", flush=True)

    try:
        print("index_limpio iniciado. Cargando modelo y procesando datos...", flush=True)
        runpy.run_module("index_limpio", run_name="__main__", init_globals=init_globals)
        print("index_limpio completado", flush=True)
    except SystemExit as e:
        code = int(e.code) if isinstance(e.code, int) else 1
        if code != 0:
            print(f"index_limpio terminó con SystemExit código {code}", flush=True)
            sys.exit(code)
        print("index_limpio completado (SystemExit 0)", flush=True)
    except Exception:
        print("Error en index_limpio", flush=True)
        traceback.print_exc()
        sys.exit(1)


# ----------------- main -----------------
def main():
    print("=== INICIANDO INDEXADOR (FC -> Index) ===", flush=True)

    data_dir = os.getenv("DATA_DIR", "/app/datos")
    os.makedirs(data_dir, exist_ok=True)

    # --------------------------------------------
    # ESPERAR SERVICIOS CRÍTICOS ANTES DE INDEXAR
    # --------------------------------------------
    print("\n=== Esperando servicios críticos ===", flush=True)

    # Esperar a Qdrant
    wait_for_http("http://qdrant:6333/collections")

    # Esperar a federated-catalog
    wait_for_http("http://federated-catalog:39195/api/catalog/v1alpha/catalog/query")

    # Delay de seguridad adicional
    print("[Extra] Esperando 10 segundos adicionales...", flush=True)
    time.sleep(10)

    print("=== Todos los servicios disponibles. Comenzando indexación ===\n", flush=True)

    # Lanzar las etapas reales
    run_fc_data_cleaning()
    run_index_limpio()

    print("\nProceso completo", flush=True)
    sys.exit(0)


if __name__ == "__main__":
    main()
