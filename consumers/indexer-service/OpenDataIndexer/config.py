# Script para centralizar todos los parametros personalizables del buscador

GENERATIVE_MODEL_NAME = 'gpt-4.1-nano' # Modelo empleado para la extracción de queries dada una intent y generación de información adicional
FLAG_ADICIONAL_GENERATIVO = False # Cambiar entre la petición a la API para la generación del mensaje de información adicional o usar un texto predeterminado 
FLAG_NO_RESULTADOS_GENERATIVO = False # Cambiar entre la petición a la API para la generación del mensaje información adicional o usar un texto predeterminado 
USE_E5_PROMPT = False  # Activar solo si usas el modelo semantico intfloat/multilingual-e5
USAR_PROMPT_EMBEDDING = False # Activar solo si usas modelos que aceptan un prompt como Qwen
MODEL_TO_16 = True # Reducir modelo a 16bits para ahorar RAM
CACERT_PATH = "./tls/cacert.pem" # Ruta certificado acceso a Qdrant
QUERY_CLASIFIER_PATH = './query_classifier.pickle' # Ruta modelo clasificador query-intent
DESACTIVAR_CACHE_REDIS = True # Desactivar o activar el cache de Redis
# ==============================================================================================================================
# Parámetros del buscador
SEMANTIC_MODEL_NAME = 'sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2' # Modelo empleado para la generación de embeddings

SCORES_PESOS = {
    "title": 0.5,
    "description": 0.3,
    "header": 0.1,
    "rows": 0.1
}
UMBRAL_SIMILITUD = 0.35 # Umbral de similitud para keywords
UMBRAL_SIMILITUD_GENERATIVO = 0.50 # Umbral de similitud para intents (como generan varias queries, podría ser interesante subir el umbral)
TOP_N = 3000 # Resultados devueltos por keyword en el buscador
K_MAX = 3000 # Número de elementos máximo recuperado de la BBDD en cada query