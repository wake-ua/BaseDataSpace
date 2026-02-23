import logging
import os
import json
import csv
from io import StringIO
import requests
from dotenv import load_dotenv



logging.basicConfig(level=logging.INFO,format="%(asctime)s - %(levelname)s - %(message)s")

class ConflictError(Exception):
    pass

load_dotenv()



SAMPLE_BASE_URL = os.getenv("SAMPLE_BASE_URL")


def build_sample_file_payload(content, mime_type):
    if content is None:
        return None

    if mime_type == "json":
        normalized_mime = "application/json"
        file_name = "sample.json"
        body = json.dumps(content, ensure_ascii=False).encode("utf-8")
    elif mime_type == "csv":
        normalized_mime = "text/csv"
        file_name = "sample.csv"
        if isinstance(content, list):
            if content:
                output = StringIO()
                writer = csv.DictWriter(output, fieldnames=content[0].keys())
                writer.writeheader()
                writer.writerows(content)
                body = output.getvalue().encode("utf-8")
            else:
                body = b""
        elif isinstance(content, str):
            body = content.encode("utf-8")
        elif isinstance(content, bytes):
            body = content
        else:
            body = str(content).encode("utf-8")
    else:
        normalized_mime = mime_type if "/" in mime_type else "text/plain"
        file_name = "sample.txt"
        if isinstance(content, bytes):
            body = content
        elif isinstance(content, str):
            body = content.encode("utf-8")
        else:
            body = json.dumps(content, ensure_ascii=False).encode("utf-8")

    return {
        "file": (file_name, body, normalized_mime)
    }


def create_sample_in_server(method, provider_token, content, mime_type, asset_id):
    files = build_sample_file_payload(content, mime_type)
    if files is None:
        return None

    payload = {"asset_id": asset_id}

    headers = {
        "Authorization": f"Bearer {provider_token}"
    }
    
    response = requests.request(
        method,
        f"{SAMPLE_BASE_URL}/samples/",
        headers=headers,
        data=payload,
        files=files
    )

    logging.info(f"\033[1;33m Sample server creation status code: {response.status_code}\033[0m")

    if response.status_code == 409:
        raise ConflictError()

    
