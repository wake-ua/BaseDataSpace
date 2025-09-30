#!/usr/bin/env bash

# get the FC catalog
curl --location 'http://localhost:39195/api/catalog/v1alpha/catalog/query' \
--header 'Content-Type: application/json' \
--data-raw '{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "QuerySpec"
}'

# add one asset to base provider
curl --location 'http://localhost:19193/management/v3/assets' \
--header 'Content-Type: application/json' \
--header 'x-api-key: managementApiKeyProviderBase' \
--data-raw '{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "assetId",
  "properties": {
    "name": "Test asset",
    "contenttype": "application/json"
    },
  "dataAddress": [{
    "type": "HttpData",
    "name": "Test asset distribution",
    "baseUrl": "https://jsonplaceholder.typicode.com/users",
    "proxyPath": "true"
  }]
}
'

curl --location 'http://localhost:19193/management/v3/policydefinitions' \
--header 'Content-Type: application/json' \
--header 'x-api-key: managementApiKeyProviderBase' \
--data-raw '{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "@id": "aPolicy",
  "policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "@type": "Set",
    "permission": [],
    "prohibition": [],
    "obligation": []
  }
}
'

curl --location 'http://localhost:19193/management/v3/contractdefinitions' \
--header 'Content-Type: application/json' \
--header 'x-api-key: managementApiKeyProviderBase' \
--data-raw '{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "1",
  "accessPolicyId": "aPolicy",
  "contractPolicyId": "aPolicy",
  "assetsSelector": []
}
'
