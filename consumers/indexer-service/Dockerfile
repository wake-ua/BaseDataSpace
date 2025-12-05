# consumers/indexer-service/Dockerfile
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    TOKENIZERS_PARALLELISM=false

WORKDIR /app

# Dependencias de sistema mínimas (solo runtime)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# Requisitos (aprovecha caché)
COPY opendataindexer/requirements.txt /app/requirements.txt
RUN python -m pip install --upgrade pip \
 && pip install -r /app/requirements.txt

# Código del indexador
COPY opendataindexer/ /app/

# Asegura la carpeta de salida de meta_*.json (y posible mapeo de volumen)
RUN mkdir -p /app/datos

# Variables por defecto (puedes sobreescribirlas en compose/run)
ENV DATA_DIR=/app/datos \
    QDRANT_URL=http://qdrant:6333 \
    QDRANT_API_KEY= \
    INDEX_DEBUG=true \
    INDEX_BATCH_SIZE=4 \
    INDEX_LINES_BATCH_SIZE=4 \
    INDEX_MAX_CONTENT_LINES=25 \
    INDEX_MAX_META_FILES=None \
    INDEX_SOLO_UN_RECURSO=true \
    INDEX_MODEL_NAME=sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2 \
    INDEX_MODEL_TO_16=true \
    FC_QUERY_URL=http://localhost:39195/api/catalog/v1alpha/catalog/query \
    OUT_DIR=/app/datos \
    LIMIT=200 \
    OFFSET=0 \
    WRITE_SAMPLES=true

# Es un job batch: ejecuta limpieza + indexado y sale
#CMD ["python", "run_indexer.py"]
CMD ["tail", "-f", "/dev/null"]
