import requests
import csv
from io import StringIO
import logging


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)


def fetch_sample_data(url, api_key, response_type):
    headers = {'x-ebirdapitoken': api_key}
    try:
        response = requests.get(url, headers=headers)
        response.raise_for_status()
        if response_type == "json":
            data = response.json()
            if isinstance(data, list):
                return data[:10]
            return None
        elif response_type == "csv":
            f = StringIO(response.text)
            reader = csv.DictReader(f)
            rows = list(reader)
            first_ten = rows[:10]
            return first_ten
    except requests.RequestException:
        logging.exception("Fetchdata failed %s", url)
    except ValueError:
        logging.exception("Fetchdata failed for %s", url)
    return None


def send_request(url, payload, apikey):

    try:
        headers = {
            'Content-Type': 'application/json',
            'x-api-key': apikey
        }
        response = requests.post(url, headers=headers, json=payload)
        print(f"\033[34m POST Status: {response.status_code}\033[0m")
        print("POST Response:", response.text)

        if response.status_code != 409:
            return response.json()['@id']

        if response.status_code == 409:
            print("Conflict detected (409). Retrying with PUT...")
            response = requests.put(url, headers=headers, json=payload)
            print(f"\033[34m PUT Status: {response.status_code}\033[0m")
            print("PUT Response:", response.text)
            if response.status_code == 204:
                return payload["@id"]

        response.raise_for_status()
    except requests.RequestException as e:
        logging.exception(e)
        return None



def post_json(url, payload, apikey):
    try:
        headers = {
            'Content-Type': 'application/json',
            'x-api-key': apikey
        }
        response = requests.post(url, headers=headers, json=payload)
        print("Status:", response.status_code)
        print("Response:", response.text)
        print(f"\033[34m{response.status_code}\033[0m")
        if response.status_code != 409:
            return response.json()

        if response.status_code == 409:
            print("Conflict detected (409). Retrying with PUT...")
            url = url + f"/{payload['@id']}"
            response = requests.put(url, headers=headers, json=payload)
            print(f"\033[34m PUT Status: {response.status_code}\033[0m")
            print("PUT Response:", response.text)
            if response.status_code == 204:
                return payload["@id"]



    except requests.RequestException as e:
        logging.exception(e)
        return None

