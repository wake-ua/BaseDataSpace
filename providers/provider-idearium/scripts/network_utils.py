import requests
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

        if response.status_code == 400:
            logging.error("Bad Request (400). Validation Failed")

        if response.status_code == 200:
            return response.json()['@id']

        if response.status_code == 409:
            logging.info(f"Conflict detected (409). Retrying with PUT... ")
            if id:
                response = requests.put(url + '/' + id, headers=headers, json=payload)
            else:
                response = requests.put(url, headers=headers, json=payload)
            logging.info(f"\033[34m PUT Status: {response.status_code}\033[0m")
            if response.status_code != 200:
                logging.info("PUT Response: " + response.text)
            if response.status_code == 204:
                return payload["@id"]
        # return None
        response.raise_for_status()
    except requests.RequestException as e:
        logging.exception(e)
        return None

def get_request(url, token_key=None, token_value=None):

    try:
        headers = {}
        if token_key and token_value:
            headers[token_key] = token_value
        response = requests.get(url, headers=headers)
        logging.info(f"\033[34m GET {url} Status: {response.status_code}\033[0m")
        response.raise_for_status()
        return response.content
    except requests.RequestException as e:
        logging.exception(e)
        return None