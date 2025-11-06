FROM python:3.11-slim

WORKDIR /app

COPY scripts/requirements.txt /app/

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* \
    && pip install --no-cache-dir -r requirements.txt

COPY scripts /app/

CMD ["sh", "-c", "until curl -s http://provider-ebird:17191/api/health; do echo 'Waiting for provider-ebird...'; sleep 3; done; echo 'provider-ebird is alive! Waiting...'; sleep 15; python main.py"]