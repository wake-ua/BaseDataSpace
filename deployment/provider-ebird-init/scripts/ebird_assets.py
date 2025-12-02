
from datetime import datetime, timezone



def build_asset(url, metadata, desc, name, api_key, province=None):
    """Create asset definition dict (NOT serialized)."""
    asset_id = f"asset-{name}"
    display_name = name.format(region=province) if province else name
    current_time = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")

    return asset_id, {
        "@id": asset_id,
        "@type": "dcat:Dataset",
        "dcat:distribution": [
            {
                "@type": "dcat:Distribution",
                "dct:format": {"@id": "HttpData"},
                "dcat:accessURL": url,
                "proxyPath": "true",
                "authKey": "x-ebirdapitoken",
                "authCode": api_key,
                "cbm:hasDataDictionary": {"@type": "cbm:DataDictionary","cbm:hasField": metadata["columns"]}
            }
        ],
        "cbm:hasSample": f"sample-{asset_id}",
        "dct:title": display_name,
        "dct:description": desc,
        "edc:contenttype": "application/json",
        "dct:modified": current_time,
        "dct:issued": current_time,
        "dct:accessRights": "https://opendatacommons.org/licenses/odbl/1.0/",
        "@context": {
            "schema": "https://schema.org/",
            "cbm": "https://w3id.org/cbm/v0.0.1/ns/",
            "dcat": "http://www.w3.org/ns/dcat#",
            "dct": "http://purl.org/dc/terms/",
            "odrl": "http://www.w3.org/ns/odrl/2/",
            "dspace": "https://w3id.org/dspace/v0.8/",
            "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
            "edc": "https://w3id.org/edc/v0.0.1/ns/"
        },
    }


def build_sample_asset(metadata, name, desc, asset_id, sample_base_url, asset_type, province=None):
    """Create sample asset definition dict."""
    display_name = name.format(region=province) if province else name
    current_time = datetime.now(timezone.utc).replace(microsecond=0).isoformat()
    return {
        "@id": f"sample-{asset_id}",
        "@type": "cbm:Sample",
        "dcat:distribution": [
            {
                "@type": "dcat:Distribution",
                "dct:format": {"@id": "HttpData"},
                "dcat:accessURL": f"{sample_base_url}/provider-ebird/sample-{asset_id}.{asset_type}",
                "edc:proxyPath": "true",
                "cbm:hasDataDictionary": {"@type": "cbm:DataDictionary","cbm:hasField": metadata["columns"]}
            }
        ],
        "cbm:isSampleOf": asset_id,
        "dct:title": display_name,
        "dct:description": desc,
        "dct:issued": current_time,
        "dct:modified": current_time,
        "edc:contenttype": "application/json",
        "schema:algorithm": "clustering",
        "dct:accessRights": "https://opendatacommons.org/licenses/odbl/1.0/",
        "@context": {
            "schema": "https://schema.org/",
            "cbm": "https://w3id.org/cbm/v0.0.1/ns/",
            "dcat": "http://www.w3.org/ns/dcat#",
            "dct": "http://purl.org/dc/terms/",
            "odrl": "http://www.w3.org/ns/odrl/2/",
            "dspace": "https://w3id.org/dspace/v0.8/",
            "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
            "edc": "https://w3id.org/edc/v0.0.1/ns/"
        },

    }
