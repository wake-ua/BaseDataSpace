from dotenv import load_dotenv
import os
import json
import uuid
from ebird_assets import build_asset, build_sample_asset
from network_utils import fetch_sample_data, post_json
from create_files import save_parsed_data
import logging
from pathlib import Path



logging.basicConfig(level=logging.INFO,format="%(asctime)s - %(levelname)s - %(message)s")
load_dotenv()



BASE_URL = os.getenv("BASE_URL")
API_KEY = os.getenv("API_KEY")
SAMPLE_BASE_URL = os.getenv("SAMPLE_BASE_URL")
POLICY_DEFINITIONS_URL = f"{BASE_URL}/management/v3/policydefinitions"
CONTRACT_DEFINITIONS_URL = f"{BASE_URL}/management/v3/contractdefinitions"
ASSETS_URL = f"{BASE_URL}/management/v3/assets-cbm"
SAMPLES_URL = f"{BASE_URL}/management/v3/assets"
ASSETS_IDS = ["1"]
SAMPLES_IDS = ["2"]



ASSET_ID_MAP = {
    "ASSETS_IDS": ASSETS_IDS,
    "SAMPLES_IDS": SAMPLES_IDS,
}


POLICIES = [
    {"policy_file": "open_policy.json", "contract_file": "open_contract.json", "assets_id_key": "SAMPLES_IDS"}, 
    {"policy_file": "basic_policy.json", "contract_file": "basic_contract.json", "assets_id_key": "ASSETS_IDS"},
]


endpoints = {
    "notable_obs_in_region": {
        "name": "Recent notable observations in {region}",
        "desc": "Get the list of recent, notable observations (up to 30 days ago) of birds seen in {region}",
        "url": "https://api.ebird.org/v2/data/obs/{code}/recent/notable"
    },
    "species_list_region": {
        "name": "Species list in {region}",
        "desc": "Get a list of species codes ever seen in {region}, in taxonomic order (species taxa only)",
        "url": "https://api.ebird.org/v2/product/spplist/{code}"
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
    "taxonomy": {
        "name": "eBird Taxonomy",
        "desc": "Get the taxonomy used by eBird.",
        "url": "https://api.ebird.org/v2/ref/taxonomy/ebird"
    },
    "taxonomic_groups": {
        "name": "Taxonomic groups by {code} order",
        "desc": "Get a list of taxonomic groups ordered by {code}",
        "url": "https://api.ebird.org/v2/ref/sppgroup/{code}"
    }
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
            url = url_template.format(code=code)
            asset_id =  create_asset_and_sample(metadata, url, name, province_name, response_type)
            if asset_id:
                    ASSETS_IDS.append(asset_id)
                    SAMPLES_IDS.append(f"sample-{asset_id}")
                    logging.info(f"CREATED ASSET {asset_id} AND SAMPLE sample-{asset_id}")


def create_asset_no_region_code(metadata, url, name, desc, response_type ="json"):
      """
      Create asset for no region code endpoint
      """
      asset_id =  create_asset_and_sample(metadata, url, name, response_type)
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
         desc = desc.format(code=group)
         logging.info(f"taxonomic {metadata},{url}, {name} {group} ")
         asset_id =  create_asset_and_sample(metadata, url, name, response_type)
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
        "metadata_file": "list_species_metadata.json",
        "endpoint_key": "species_list_region",
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
    {
        "metadata_file": "taxonomy_metadata.json",
        "endpoint_key": "taxonomy",
        "creator_func": create_asset_no_region_code,
        "response_type": "csv"
    },
    {
        "metadata_file": "taxonomic_groups_metadata.json",
        "endpoint_key": "taxonomic_groups",
        "creator_func": create_asset_taxonomic_groups,
        "response_type": "json"
    }
]


def create_policy(filename):
   policy_id = str(uuid.uuid4())
   filepath = os.path.join("policies", filename)
   with open(filepath) as f:
    data = json.load(f)
    data['@id'] = policy_id
   created = post_json(POLICY_DEFINITIONS_URL, data)
   if not created:
        return None
   else:
        return policy_id


def create_contract_definition(filename, access_policy_id, contract_policy_id, asset_ids):
   contract_id = str(uuid.uuid4())
   filepath = os.path.join("contracts", filename)
   with open(filepath) as f:
    data = json.load(f)
    data['@id'] = contract_id
    data["accessPolicyId"] = access_policy_id
    data["contractPolicyId"] = contract_policy_id
    data["assetsSelector"][0]["operandRight"] = asset_ids
    json.dumps(data)
    created = post_json(CONTRACT_DEFINITIONS_URL, data)
    if not created:
        return None
    else:
        return contract_id



def create_asset_and_sample(metadata, url, name,province=None, response_type="json"):
    asset_id, asset_payload = build_asset(url, metadata, name, API_KEY, province)
    created = post_json(ASSETS_URL, asset_payload)
    if not created:
        return None
    
    sample = fetch_sample_data(url, API_KEY, response_type)
    if sample:
        save_parsed_data(f"sample-{asset_id}", response_type, sample)
        sample_payload = build_sample_asset(metadata, name, asset_id, SAMPLE_BASE_URL, response_type, province)
        post_json(SAMPLES_URL, sample_payload)
    return created["@id"]


def main():
    for config in ASSET_CONFIGS:
        metadata = get_metadata(config["metadata_file"])
        endpoint = endpoints[config["endpoint_key"]]
        if config["creator_func"] == create_assets_for_region_code:
            config["creator_func"]("provinces.json", metadata, endpoint["url"], endpoint["name"], endpoint["desc"], config["response_type"])
        else:
            config["creator_func"](metadata, endpoint["url"], endpoint["name"], endpoint["desc"], config["response_type"])
   
    
    for policy in POLICIES:
        policy_id = create_policy(policy["policy_file"])
        logging.info(f"CREATED POLICY {policy_id}")
        
        if policy_id:
            contract_id = create_contract_definition(policy["contract_file"], policy_id, policy_id, ASSET_ID_MAP[policy["assets_id_key"]])
            logging.info(f"CREATED CONTRACT {contract_id}")
        

if __name__ == "__main__":
    main()

