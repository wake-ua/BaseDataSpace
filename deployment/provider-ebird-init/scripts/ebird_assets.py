
import uuid


def build_asset(url, metadata, name, api_key, province=None):
    """Create asset definition dict (NOT serialized)."""
    asset_id = str(uuid.uuid4())
    display_name = name.format(region=province) if province else name

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
                "cbm:hasDataDictionary": {"cbm:hasField": metadata["columns"]}
            }
        ],
        "schema:name": display_name,
        "name": display_name,
        "dct:title": metadata["title"],
        "cbm:hasSample": f"sample-{asset_id}",
        "contenttype": "application/json",
        "@context": {
            "schema": "https://schema.org/",
            "cbm": "https://w3id.org/cbm/v0.0.1/ns/",
            "dcat": "http://www.w3.org/ns/dcat#",
            "dct": "http://purl.org/dc/terms/",
            "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
        }
    }

def build_sample_asset(metadata, name, asset_id, sample_base_url, asset_type, province=None):
    """Create sample asset definition dict."""
    display_name = name.format(region=province) if province else name
    return {
        "@id": f"sample-{asset_id}",
        "properties": {
            "name": display_name,
            "contenttype": "application/json",
            "schema:name": display_name,
            "dcterms:title": metadata["title"],
            "cbm:isSampleOf": asset_id,
            "schema:algorithm": "clustering",
            "cbm:hasDataDictionary": {"cbm:hasField": metadata["columns"]}
        },
        "@context": {
            "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
            "schema": "https://schema.org/",
            "dcterms": "http://purl.org/dc/terms/",
            "cbm": "https://w3id.org/cbm/v0.0.1/ns/"
        },
        "dataAddress": [{
            "type": "HttpData",
            "name": "Sample asset distribution",
            "baseUrl": f"{sample_base_url}/sample-{asset_id}.{asset_type}",
            "proxyPath": "true"
        }]
    }
