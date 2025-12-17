from dotenv import load_dotenv
import os
import json
from network_utils import send_request
import logging
import re
from pathlib import Path
import glob


logging.basicConfig(level=logging.INFO,format="%(asctime)s - %(levelname)s - %(message)s")
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


def create_entity(path, url, key, with_id=False):
    """
    Get metadata from a JSON file path and post it to an endpoint.
    """
    logging.info(f" * getting {path} {url} {key}")
    with open(path, 'r', encoding="utf8") as file:
        data = json.load(file)
    if with_id:
        id = data['@id']
        created = send_request(url, data, key, id)
    else:
        created = send_request(url, data, key)
    if not created:
        raise Exception(f"Error creating entity from path {path} at {url} ({key})")
    else:
        return data['@id']

def create_policy(path):
    return create_entity(path, POLICY_DEFINITIONS_URL, X_API_KEY, True)


def create_contract_definition(path):
    return create_entity(path, CONTRACT_DEFINITIONS_URL, X_API_KEY)


def create_asset(path):
    return create_entity(path, ASSETS_URL, X_API_KEY)


def get_json_paths(directory, filter='*.json'):
    print(directory)
    json_paths = glob.glob(directory + filter)
    return json_paths


def main():
    # list files in policies directory and post them
    logging.info("\n ======= POLICIES ==========")
    for path in get_json_paths(POLICIES_PATH):
        create_policy(path)

    # list files in contracts directory and post them
    logging.info("\n ======= CONTRACTS ==========")
    for path in get_json_paths(CONTRACTS_PATH):
        create_contract_definition(path)

    # list files in datasets directory and post them
    logging.info("\n ======= DATASETS ==========")
    for path in get_json_paths(DATASETS_PATH, '**/*.json'):
        create_asset(path)


if __name__ == "__main__":
    main()

