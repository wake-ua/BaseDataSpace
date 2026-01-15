
e-bird Provider Execution Guide
---

# 1. Execution Options (choose one)

The e-bird provider can be executed using either of the following methods:
- A. Local execution.
- B. Docker-based execution (recommended).

If you choose Docker, you may proceed directly to step B.4 after starting the environment.

## A. Local execution
> [ **⚠️ Do not attempt to run this on Windows.** ]

**A.1. Execute the e-bird provider**

```
java -Dedc.fs.config=providers/provider-ebird/resources/configuration/provider-ebird-configuration.properties -jar providers/provider-ebird/build/libs/provider-ebird.jar --log-level=DEBUG
```

**A.2. Initialize the PostgreSQL schema**

The provider stores its catalog in a PostgreSQL database. It is necessary to create the tables beforehand with thus schema creation script:

```
psql -f providers/provider-ebird/src/main/resources/META-INF.services/database.sql
```

> [ **⚠️ After starting the needed services, you may continue directly with step 2 (Federated Catalog)** ]

## B. Docker-based execution (recommended).

**B.1. Build the project**
```./gradlew clean build ```


**B.2. Build Docker images for main components**
```docker compose build --no cache ```

**B.3. Start the main components environment**
```docker compose up -d ```

**B.4. Build Docker images for ebird components** (from providers/provider-ebird/ directory )
```docker compose build --no cache ```

**B.4. Start the provider ebird environment** (from providers/provider-ebird/ directory )
```docker compose up -d ```


**B.5. [ ⚠️ Before continuing ensure: ]**
- **B.4.1. The dataspace is running**:
```docker ps ```
- **B.4.2. You imported the latest Postman collection.**
- **B.4.3. The e-bird participant appears registered in the participant directory** (see [`init-docker-dev.sh`](init-docker-dev.sh)).

---

**2 Retrieve the Federated Catalog (Postman)**
Use the following Postman request, which returns the complete catalog of the e-bird provider:
```GET BaseDS-EDC / Federated Catalog / Catalog Query / Get Provider Catalog from FC Docker ```

Ensure that the request body is defined as follows:
```json 
{
  "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
  "counterPartyAddress": "http://provider-ebird:17194/protocol",
  "protocol": "dataspace-protocol-http"
}
```

From the catalog response, identify and select one dataset entry. Each dataset includes several fields, but the two essential elements for the next steps are:
- The dataset identifier (`@id`), which acts as the `assetId`.
- The contract offer identifier, located inside `odrl:hasPolicy["@id"]`.

A typical dataset object looks like this:
``` json
{
 "@id": "assetId",
    "@type": "dcat:Dataset",
    "odrl:hasPolicy": {
        "@id": "Y29udHJhY3Q=:YXNzZXRJZA==:YjMyNzEzMTQtNWI0NC00MTBjLWIwYzEtMjZmMWRkOWFhZGI3",
        "@type": "odrl:Offer",
        "odrl:permission": {
            "odrl:action": {
                "@id": "odrl:use"
            },
            "odrl:constraint": {
                "odrl:leftOperand": {
                    "@id": "edc:location"
                },
                "odrl:operator": {
                    "@id": "odrl:eq"
                },
                "odrl:rightOperand": "eu"
            }
        },
        "odrl:prohibition": [],
        "odrl:obligation": []
    },
    "dcat:distribution": [
        {
            "@type": "dcat:Distribution",
            "dct:format": {
                "@id": "HttpData-PULL"
            },
            "dcat:accessService": {
                "@id": "bd93a48d-7c5c-4f21-8f91-9c5fe41c2fed",
                "@type": "dcat:DataService",
                "dcat:endpointDescription": "dspace:connector",
                "dcat:endpointUrl": "http://localhost:17194/protocol",
                "dcat:endpointURL": "http://localhost:17194/protocol"
            }
        },
        {
            "@type": "dcat:Distribution",
            "dct:format": {
                "@id": "HttpData-PUSH"
            },
            "dcat:accessService": {
                "@id": "bd93a48d-7c5c-4f21-8f91-9c5fe41c2fed",
                "@type": "dcat:DataService",
                "dcat:endpointDescription": "dspace:connector",
                "dcat:endpointUrl": "http://localhost:17194/protocol",
                "dcat:endpointURL": "http://localhost:17194/protocol"
            }
        }
    ],
    "dct:issued": "2025-10-22T13:04:33+00:00",
    "dct:modified": "2025-10-22T13:04:33+00:00",
    "dct:title": "test name",
    "dct:accessRights": "https://opendatacommons.org/licenses/odbl/1.0/",
    "dct:description": "test description",
    "id": "assetId",
    "contenttype": "application/json"
}
```

You must extract and save the following values:
- `ASSET_ID`: the value of `@id`.
- `CONTRACT_OFFER_ID`: the value of `odrl:hasPolicy["@id"]`.
- Any constraints required by the policy, found under `odrl:permission.constraint`, these constraints must be included in the contract negotiation request.

Once the dataset is selected and these values are identified, proceed to the contract negotiation step.

---

**3 Contract Negotiation**
To initiate a contract negotiation for the selected dataset, use the following Postman request:
```GET Provider - Ebird / Transfer / Negociate Contract Advanced Docker```

Before sending the request, you must replace all placeholders in the body with the actual values extracted from the catalog:
- `{{CONTRACT_OFFER_ID}}`: replace this with the value found in `odrl:hasPolicy["@id"]`.
- `{{ASSET_ID}}`: replace this with the dataset identifier (`@id`).
- If the dataset contains constraints under `odrl:permission.constraint`, you must include those constraints exactly as they appear.

The body you must send is the following, **after removing the placeholders and inserting the real values**:
```json 
{
    "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
    "@type": "ContractRequest",
    "counterPartyAddress": "http://provider-ebird:{{PROVIDER_PROTOCOL_PORT}}/protocol",
    "protocol": "dataspace-protocol-http",
    "policy": {
        "@context": "http://www.w3.org/ns/odrl.jsonld",
        "@id":"{{CONTRACT_OFFER_ID}}",
        "@type": "Offer",
        "permission": [
            {
                "action": "use",
                "target": "{{ASSET_ID}}",
                "constraint": {
                    "@type": "AtomicConstraint",
                    "leftOperand": "location",
                    "operator": "eq",
                    "rightOperand": "eu"
                }
            }
        ],
        "prohibition": [],
        "obligation": [],
        "assigner": "provider-ebird",
        "target": "{{ASSET_ID}}"
    }
}
```
> **[ ⚠️ Replace all placeholders in this block before executing the request ]**

If the request is valid, the provider returns an identifier representing the negotiation process. Example:
``` json
{
    "@type": "IdResponse",
    "@id": "96ac9569-98ef-400d-8b68-03d1f64ac84d",
    "createdAt": 1763558664992,
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    }
}
```

Record the `NEGOTATION_ID` (`@id`) from the provider response, as it is required for the next step.

---

**4 Check Contract Status**
To verify the status of the ongoing negotiation, use the following Postman request:
```GET Provider - Ebird / Transfer / Check Contract```

You must provide the negotiation ID (`NEGOTATION_ID`) returned in **step 3**. This value must be placed inside the request body, as shown below (replace `{{NEGOTATION_ID}}` with the actual value returned previously (e.g., `96ac9569-98ef-400d-8b68-03d1f64ac84d`)):
```json
{
    "@id": "{{NEGOTATION_ID}}"
}
```

If the negotiation has been successfully completed, the response will include the `state` field set to `FINALIZED` and the corresponding `contractAgreementId`. Example:
```json
{
    "@type": "ContractNegotiation",
    "@id": "b581bb51-bea5-4e65-98e4-f94ae1646572",
    "type": "CONSUMER",
    "protocol": "dataspace-protocol-http",
    "state": "FINALIZED",
    "counterPartyId": "provider-ebird",
    "counterPartyAddress": "http://localhost:17194/protocol",
    "callbackAddresses": [],
    "createdAt": 1763564446787,
    "contractAgreementId": "5ef6c8f6-98fe-4fdd-9242-3d188f015335",
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    }
}
```

---

**5. Transfer the Dataset**

Once the contract negotiation has been successfully finalized, you can initiate the data transfer using the following Postman request:
```GET Provider - Ebird / Transfer / Transfer```

The request requires inserting the following values, you must replace all placeholders in the body with the actual values:
- `{{ASSET_ID}}`: the dataset identifier (`@id` obtained in **step 2**).
- `{{CONTRACT_AGREEMENT_ID}}`: the contractAgreementId (obtained in **step 4**).

In this example request, **dataDestination / baseurl** is the destination endpoint where the provider will deliver the data (e.g., logger container at `http://request-logger:4000`).

```json
{
    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
    "@type": "TransferRequestDto",
    "connectorId": "provider-ebird",
    "counterPartyAddress": "http://provider-ebird:{{PROVIDER_PROTOCOL_PORT}}/protocol",
    "contractId": "{{CONTRACT_AGREEMENT_ID}}",
    "assetId": "{{ASSET_ID}}",
    "protocol": "dataspace-protocol-http",
    "transferType": "HttpData-PUSH",
    "dataDestination": {
        "type": "HttpData",
        "baseUrl": "http://request-logger:4000/api/consumer/store"
    }
}
```
> **[ ⚠️ Replace all placeholders in this block before executing the request ]**

If the request is valid, the provider returns a transfer process identifier. Example:
``` json
{
    "@type": "IdResponse",
    "@id": "9db3d714-c517-468e-b097-cc82ba19427f",
    "createdAt": 1763565130998,
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    }
}
```

---

**6. Verify Received Data**

After submitting the transfer request, the e-bird provider sends the dataset to the destination endpoint specified in the request body. To confirm that the transfer has been completed successfully, you must check the logs of the logger container ( http://request-logger:4000)

```docker logs -f <container_id>```

If the transfer was executed correctly, the dataset delivered by the provider will appear in the logger output. Example:
``` json
[
  {
    "id": 1,
    "name": "Leanne Graham",
    "username": "Bret",
    "email": "Sincere@april.biz",
    "address": {
      "street": "Kulas Light",
      "suite": "Apt. 556",
      "city": "Gwenborough",
      "zipcode": "92998-3874",
      "geo": {
        "lat": "-37.3159",
        "lng": "81.1496"
      }
    },
    "phone": "1-770-736-8031 x56442",
    "website": "hildegard.org",
    "company": {
      "name": "Romaguera-Crona",
      "catchPhrase": "Multi-layered client-server neural-net",
      "bs": "harness real-time e-markets"
    }
  }
]
```