from dotenv import load_dotenv
import os
import json
from ebird_assets import build_asset, build_sample_asset
from network_utils import fetch_sample_data, send_request, post_json
from create_files import save_parsed_data
import logging
import re
from pathlib import Path



logging.basicConfig(level=logging.INFO,format="%(asctime)s - %(levelname)s - %(message)s")
load_dotenv()



BASE_URL = os.getenv("BASE_URL")
API_KEY = os.getenv("API_KEY")
X_API_KEY = os.getenv("X_API_KEY")
SAMPLE_BASE_URL = os.getenv("SAMPLE_BASE_URL")
POLICY_DEFINITIONS_URL = f"{BASE_URL}/management/v3/policydefinitions"
CONTRACT_DEFINITIONS_URL = f"{BASE_URL}/management/v3/contractdefinitions"
ASSETS_URL = f"{BASE_URL}/management/v3/assets-cbm"
SAMPLES_URL = f"{BASE_URL}/management/v3/assets-cbm"
ASSETS_IDS = ["1"]
SAMPLES_IDS = ["2"]
OPEN_POLICY_ID = "always-true"


ASSET_ID_MAP = {
    "ASSETS_IDS": ASSETS_IDS,
    "SAMPLES_IDS": SAMPLES_IDS,
}


POLICIES = [
    {"id": "basic-policy", "policy_file": "basic_policy.json", "contract_file": "basic_contract.json", "assets_id_key": "ASSETS_IDS"},
]


endpoints = {
    "notable_obs_in_region": {
        "name": "Recent notable observations in {region}",
        "desc": "Get the list of recent, notable observations (up to 30 days ago) of birds seen in {region}",
        "url": "https://api.ebird.org/v2/data/obs/{code}/recent/notable"
    },
    "recent_checklist_feed": {
        "name": "Recent checklist feed in {region}",
        "desc": "Get information on the most recently submitted checklists for {region}",
        "url": "https://api.ebird.org/v2/product/lists/{code}?maxResults=200"
    },
    "hotspot_by_region": {
        "name": "Hotspots in {region}",
        "desc": "Get a list of hotspots in {region}",
        "url": "https://api.ebird.org/v2/ref/hotspot/{code}"
    },
}



def get_metadata(filename):
    """
    Get metadata from a JSON file.
    """
    # Get the directory where this script is located
    current_dir = Path(__file__).parent

    # Construct the full path to the file in the 'metadata' subdirectory
    file_path = current_dir / 'metadata' / filename

    with open(file_path, 'r', encoding="utf8") as file:
        data = json.load(file)
        return data



def create_assets_for_region_code(file, metadata, url_template, name, desc, response_type="json"):
    """
    Create asset for each province in spain a JSON file.
    """
    with open(file, 'r', encoding="utf8") as file:
        data = json.load(file)
        for item in data:
            code = item["code"]
            province_name = item["name"]
            print(f"\033[32m{province_name}\033[0m")
            asset_name = name.format(region=province_name)
            asset_name = re.sub(r'\s+', '-', asset_name.lower())
            asset_desc = desc.format(region=province_name)
            print(f"\033[32m{asset_desc}\033[0m")
            url = url_template.format(code=code)
            asset_id =  create_asset_and_sample(metadata, url, asset_name, asset_desc, province_name, response_type)
            if asset_id:
                ASSETS_IDS.append(asset_id)
                SAMPLES_IDS.append(f"sample-{asset_id}")
                logging.info(f"CREATED ASSET {asset_id} AND SAMPLE sample-{asset_id}")


def create_asset_no_region_code(metadata, url, name, desc, response_type ="json"):
    """
    Create asset for no region code endpoint
    """
    asset_id =  create_asset_and_sample(metadata, url, desc, name, response_type)
    if asset_id:
        ASSETS_IDS.append(asset_id)
        SAMPLES_IDS.append(f"sample-{asset_id}")
        logging.info(f"CREATED ASSET {asset_id} AND SAMPLE sample-{asset_id}")


def create_asset_taxonomic_groups(metadata, url_template, name, desc, response_type ="json"):
    """
    Create asset for taxonomic groups endpoint
    """
    groups = ["ebird", "merlin"]
    for group in groups:
        url = url_template.format(code=group)
        name = name.format(code=group)
        asset_name = re.sub(r'\s+', '-', name.lower())
        desc = desc.format(code=group)

        logging.info(f"taxonomic {metadata},{url}, {name} {group} ")
        asset_id =  create_asset_and_sample(metadata, url, asset_name, desc, response_type)
        if asset_id:
            ASSETS_IDS.append(asset_id)
            SAMPLES_IDS.append(f"sample-{asset_id}")
            logging.info(f"CREATED ASSET {asset_id} AND SAMPLE sample-{asset_id}")


ASSET_CONFIGS = [
    {
        "metadata_file": "observation_metadata.json",
        "endpoint_key": "notable_obs_in_region",
        "creator_func": create_assets_for_region_code,
        "response_type": "json"
    },
    {
        "metadata_file": "recent_checklist_feed_metadata.json",
        "endpoint_key": "recent_checklist_feed",
        "creator_func": create_assets_for_region_code,
        "response_type": "json"
    },
    {
        "metadata_file": "hotspot_metadata.json",
        "endpoint_key": "hotspot_by_region",
        "creator_func": create_assets_for_region_code,
        "response_type": "csv"
    },
]


def create_policy(filename):
    filepath = os.path.join("policies", filename)
    with open(filepath) as f:
        data = json.load(f)
    created = post_json(POLICY_DEFINITIONS_URL, data, X_API_KEY)
    if not created:
        return None
    else:
        return data['@id']


def create_contract_definition(filename, access_policy_id, contract_policy_id, asset_ids):
    filepath = os.path.join("contracts", filename)
    with open(filepath) as f:
        data = json.load(f)
        data["accessPolicyId"] = access_policy_id
        data["contractPolicyId"] = contract_policy_id
        data["assetsSelector"][0]["operandRight"] = asset_ids
        json.dumps(data)
        created = send_request(CONTRACT_DEFINITIONS_URL, data, X_API_KEY)
        if not created:
            return None
        else:
            return data['@id']



def create_asset_and_sample(metadata, url, name, desc, province=None, response_type="json"):
    asset_id, asset_payload = build_asset(url, metadata, desc, name, API_KEY, province)
    created = send_request(ASSETS_URL, asset_payload, X_API_KEY)
    print(created)
    if not created:
        return None

    sample = fetch_sample_data(url, API_KEY, response_type)
    if sample:
        save_parsed_data(f"sample-{asset_id}", response_type, sample)
        sample_payload = build_sample_asset(metadata, name, desc, asset_id, SAMPLE_BASE_URL, response_type, province)
        send_request(SAMPLES_URL, sample_payload, X_API_KEY)
    return created


def main():
    for config in ASSET_CONFIGS:
        metadata = get_metadata(config["metadata_file"])
        endpoint = endpoints[config["endpoint_key"]]
        if config["creator_func"] == create_assets_for_region_code:
            config["creator_func"]("provinces.json", metadata, endpoint["url"], endpoint["name"], endpoint["desc"], config["response_type"])
        else:
            config["creator_func"](metadata, endpoint["url"], endpoint["name"], endpoint["desc"], config["response_type"])



    #Contract for samples
    contract_sample_id = create_contract_definition("open_contract.json", OPEN_POLICY_ID, OPEN_POLICY_ID, SAMPLES_IDS)
    logging.info(f"\033[1;34mCREATED CONTRACT FOR SAMPLES {contract_sample_id}\033[0m")

    for policy in POLICIES:
        policy_id = create_policy(policy["policy_file"])
        logging.info(f"\033[1;34mCREATED POLICY {policy_id}\033[0m")

        if policy_id:
            contract_id = create_contract_definition(policy["contract_file"], OPEN_POLICY_ID, policy_id, ASSET_ID_MAP[policy["assets_id_key"]])
            logging.info(f"\033[1;34mCREATED CONTRACT {contract_id}\033[0m")




if __name__ == "__main__":
    main()

