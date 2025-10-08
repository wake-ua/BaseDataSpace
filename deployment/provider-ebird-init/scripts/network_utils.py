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


def post_json(url, payload, apikey):
    try:
        headers = {
        'Content-Type': 'application/json',
         'x-api-key': apikey
        }
        print(url)
        response = requests.post(url, headers=headers, json=payload)
        print("Status:", response.status_code)
        print("Response:", response.text)
        print(payload)
        response.raise_for_status()
        return response.json()
    except requests.RequestException as e:
        logging.exception(e)
        return None


