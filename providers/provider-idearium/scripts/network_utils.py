import requests
import csv
from io import StringIO
import logging


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

def send_request(url, payload, apikey, id=None):

    try:
        headers = {
        'Content-Type': 'application/json',
        'x-api-key': apikey
        }
        response = requests.post(url, headers=headers, json=payload)
        logging.info(f"\033[34m POST Status: {response.status_code}\033[0m")

        if response.status_code != 200:
            logging.info("POST Response:" + response.text)

        if response.status_code != 409:
            return response.json()['@id']

        if response.status_code == 409:
            logging.info("Conflict detected (409). Retrying with PUT... ({id})")
            if id:
                response = requests.put(url + '/' + id, headers=headers, json=payload)
            else:
                response = requests.put(url, headers=headers, json=payload)
            logging.info(f"\033[34m PUT Status: {response.status_code}\033[0m")
            if response.status_code != 200:
                logging.info("PUT Response: " + response.text)
            if response.status_code == 204:
                return payload["@id"]

        response.raise_for_status()
    except requests.RequestException as e:
        logging.exception(e)
        return None