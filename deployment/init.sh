#!/usr/bin/env bash

# Add the base provider base to the node directory
curl --location 'http://localhost:39193/management/v1alpha/directory' \
--header 'Content-Type: application/json' \
--data-raw '  {
    "name": "Default Base Provider",
    "id": "provider-base",
    "url": "http://provider-base:19194/protocol",
    "supportedProtocols": ["dataspace-protocol-http"],
        "@context": {
            "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
        }
    }'