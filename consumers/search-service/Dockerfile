# Contenedor del buscador (OpenDataSearch) ejecutado dentro de search-service
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

# Copiamos solo requirements primero (aprovecha caché)
COPY OpenDataSearch/requirements.txt /app/requirements.txt

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential curl ca-certificates && \
    pip install --no-cache-dir -r /app/requirements.txt && \
    apt-get purge -y build-essential && \
    rm -rf /var/lib/apt/lists/*

# Copiamos el código del buscador (sin datos gigantes)
COPY OpenDataSearch/ /app/

# Puerto interno
EXPOSE 8080

# Variables por defecto (se pueden sobreescribir en docker-compose)
ENV API_URL=http://qdrant:6333
ENV API_KEY="123"
ENV OPENAI_API_KEY=""
ENV FC_QUERY_URL=http://base-provider:39195/api/catalog/v1alpha/catalog/query
ENV OUT_DIR=/app/datos

# Asegurar carpeta de datos
RUN mkdir -p /app/datos

# Lanzar FastAPI con Uvicorn (main.py debe tener `app`)
CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8002", "--log-level", "debug"]

