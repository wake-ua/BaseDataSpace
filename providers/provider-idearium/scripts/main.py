from dotenv import load_dotenv
import os
import json
from network_utils import send_request, get_request
import logging
import glob
from bs4 import BeautifulSoup
from urllib.parse import urljoin
import time

logging.basicConfig(level=logging.INFO,format="%(asctime)s - %(levelname)s - %(message)s")

# BASE_DIR = os.path.join(os.path.dirname(__file__))
# load_dotenv(BASE_DIR + '/../.env.pro')
load_dotenv()

BASE_URL = os.getenv("BASE_URL")
API_KEY = os.getenv("API_KEY")
X_API_KEY = os.getenv("X_API_KEY")

POLICY_DEFINITIONS_URL = f"{BASE_URL}/management/v3/policydefinitions"
CONTRACT_DEFINITIONS_URL = f"{BASE_URL}/management/v3/contractdefinitions"
ASSETS_URL = f"{BASE_URL}/management/v3/assets-cbm"

POLICIES_PATH = os.getenv("POLICIES_PATH")
CONTRACTS_PATH = os.getenv("CONTRACTS_PATH")
DATASETS_PATH = os.getenv("DATASETS_PATH")
DATASET_LIST = os.getenv("DATASET_LIST")
JSON_URL = os.getenv("JSON_URL")
TOKEN_KEY = os.getenv("TOKEN_KEY")
TOKEN_VALUE = os.getenv("TOKEN_VALUE")

def create_entity_from_file(path, url, key, with_id=False):
    """
    Get metadata from a JSON file path and post it to an endpoint.
    """
    logging.info(f" * getting {path} {url} {key}")
    with open(path, 'r', encoding="utf8") as file:
        data = json.load(file)
    return create_entity(data, url, key, with_id)

def create_entity(data, url, key, with_id=False):
    """
    Get metadata from a JSON data object and post it to an endpoint.
    """
    logging.info(f" * getting {url} {key}")
    if with_id:
        id = data['@id']
        created = send_request(url, data, key, id)
    else:
        created = send_request(url, data, key)
    if not created:
        # raise Exception(f"Error creating entity at {url} ({key})")
        logging.error(f"Error creating entity at {url} ({key})")
        return None
    else:
        return data['@id']

def create_policy(path):
    return create_entity_from_file(path, POLICY_DEFINITIONS_URL, X_API_KEY, True)


def create_contract_definition(path):
    return create_entity_from_file(path, CONTRACT_DEFINITIONS_URL, X_API_KEY)


def create_asset(text):
    return create_entity(text, ASSETS_URL, X_API_KEY)


def get_json_paths(directory, filter='*.json'):
    print(directory)
    json_paths = glob.glob(directory + filter)
    return json_paths

def get_dataset_json_urls(dataset_url):
    if not dataset_url.endswith('/'):
        dataset_url += '/'
    html = get_request(dataset_url)
    parsed_html = BeautifulSoup(html,features="html.parser")
    content_list = parsed_html.find('ul').find_all('li')
    return [urljoin(dataset_url, item.text) for item in content_list if item.text.endswith('.json')]

def get_dataset_json_list(dataset_list_url):
    csv = get_request(dataset_list_url).decode("utf-8")
    return [c.strip() for c in csv.splitlines()]

def main():
    # list files in policies directory and post them
    logging.info("\n ======= POLICIES ==========")
    for path in get_json_paths(POLICIES_PATH):
        logging.info(' --------- ' + path + ' --------- ')
        create_policy(path)

    # list files in contracts directory and post them
    logging.info("\n ======= CONTRACTS ==========")
    for path in get_json_paths(CONTRACTS_PATH):
        logging.info(' --------- ' + path + ' --------- ')
        create_contract_definition(path)

    # list files in datasets directory and post them
    logging.info("")
    logging.info(" ======= DATASETS ==========")
    logging.info("**** " + DATASET_LIST + " ****")
    json_names = get_dataset_json_list(DATASET_LIST)
    for json_name in json_names:
        json_url = JSON_URL + json_name
        # sleep
        time.sleep(1)
        logging.info('\n\t\t --------- ' + json_name + ' --------- ')
        json_text = get_request(json_url, TOKEN_KEY, TOKEN_VALUE).decode("utf-8")
        data = json.loads(json_text)
        id = create_asset(data)
        if not id:
            logging.error(f"Error creating asset at {json_url}")
    logging.info("**********************")



if __name__ == "__main__":
    main()

