## Policy  evaluation.


In this module, we implement and register  function-policies
for policy-enforcement. 

Currently, we have:

- Location policy
- Entity Type policy
### Steps to implement a policy function:

1. Rule bindings:

We need to define which rules and constraints should be evaluated in which scopes.
This is done by creating rule bindings at the RuleBindingRegistry.

EDC use the concept of policy scopes to define which rules and
constraints should be evaluated in certain contexts.

Example: 

``` java
ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, ALL_SCOPES);
ruleBindingRegistry.bind(LOCATION_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
```

When creating a rule binding, we can bind an action type or constraint to either
all scopes or just a specific one. Here, we bind the action type use to all scopes,
so that rules with this action type are always evaluated.
For the location constraint we choose the negotiation scope, meaning it will only be evaluated during the contract negotiation.

2. Registering the function with the policy engine


``` java
policyEngine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, LOCATION_CONSTRAINT_KEY, new LocationConstraintFunction(monitor));
```

3. Implementing the function for evaluation

With the rule bindings in place, the provider will now try to evaluate
our policy including the constraint during a contract negotiation,
but it does not yet know how to evaluate this constraint.

```java
public class LocationConstraintFunction implements AtomicConstraintRuleFunction<Permission, ContractNegotiationPolicyContext> {
    
    //...
    
    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, ContractNegotiationPolicyContext context) {

        var region = context.participantAgent().getClaims().get("region");
        monitor.info(context.participantAgent().getClaims().toString());

        if (operator != Operator.EQ) {
            context.reportProblem("Invalid operator, only EQ is allowed!");
            return false;
        }

        monitor.info(format("Evaluating constraint: location %s %s", operator, rightValue.toString()));

        return region != null && Objects.equals(region, rightValue);
    }
}
```

Since we want to check the requesting participant's location,
we need to access information about the participant.
This is supplied through the context.
We get the participant's claim with key region to obtain information
about the participant's location.
We can then compare the location to the expected value depending on the operator used.
The function should return true, if the constraint is fulfilled, and false otherwise.

One thing to keep in mind is that currently we are using Iam-claims extension
which generates and verifies the tokens. Claims are found  inside the token.



### TEST IT

#### Using docker

### 1. Run conectors:

``` sh
docker compose build provider-ebird-postgresql provider-ebird consumer-base
```

```sh
docker compose up -d provider-ebird-postgresql provider-ebird consumer-base
```

### 2. Create the provider's offer

In order for the provider to offer any data,
we need to create 3 things: an Asset (= what data should be offered), a PolicyDefinition
(= under which conditions should data be offered), and a ContractDefinition,
that links the Asset and PolicyDefinition.


#### 2.1 Create de asset
``` curl
curl --location 'http://localhost:17193/management/v3/assets' \
--header 'Content-Type: application/json' \
--data-raw '{
"@context": {
"@vocab": "https://w3id.org/edc/v0.0.1/ns/"
},
"@id": "assetId1",
"properties": {
"name": "Example dataset",
"contenttype": "application/json",
"dcterms:name": "example example",
"special": "test"
},
"dataAddress": [{
"type": "HttpData",
"name": "Test asset distribution",
"baseUrl": "https://www.opendata.nhs.scot/dataset/0d57311a-db66-4eaa-bd6d-cc622b6cbdfa/resource/a5f7ca94-c810-41b5-a7c9-25c18d43e5a4/download/weekly_ae_activity_20250511.csv",
"proxyPath": "true"
}]
}
'
```

2.2 Create the policy definition

Next. we'll create the PolicyDefinition,
which contains a Policy and an ID. Each Policy needs to contain at least one rule describing which actions are allowed, disallowed or required to perform.

```
curl --location 'http://localhost:17193/management/v3/policydefinitions' \
--header 'Content-Type: application/json' \
--data-raw '{
"@context": {
"@vocab": "https://w3id.org/edc/v0.0.1/ns/"
},
"@id": "location-policy",
"policy": {
"@context": "http://www.w3.org/ns/odrl.jsonld",
"@type": "Set",
"permission": [
{
"action": "use",
"constraint": {
"@type": "AtomicConstraint",
"leftOperand": "location",
"operator": {
"@id": "odrl:eq"
},
"rightOperand": "eu"
}
}
],
"prohibition": [],
"obligation": []
}
}
'
```

2.3 Create the contract definition

The last thing we create is a ContractDefinition,
that references the previously created policy definition and asset.
We will set the policy both as the access and the contract policy in the contract definition.


```
curl --location 'http://localhost:17193/management/v3/contractdefinitions' \
--header 'Content-Type: application/json' \
--data-raw '{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "location-contract",
  "accessPolicyId": "location-policy",
  "contractPolicyId": "location-policy",
  "assetsSelector": [{
    "@type": "CriterionDto",
    "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
    "operator": "=",
    "operandRight": "assetId1"
  }]
}
'
```

With this, the provider now offers the asset under the condition that the requesting participant is located in the EU.

### 3. Make a catalog request

After starting both connectors,
we'll first make a catalog request from the consumer
to the provider to see the provider's offers.


```
curl --location 'http://localhost:29193/management/v3/catalog/request' \
--header 'Content-Type: application/json' \
--data-raw '  {"@context": {
"@vocab": "https://w3id.org/edc/v0.0.1/ns/"
},
"counterPartyAddress": "http://provider-ebird:17194/protocol",
"protocol": "dataspace-protocol-http"
}'
```

We'll receive the following catalog in the response

```json
{
"@id": "9ebca5cc-57e5-48df-9326-f5498029d080",
"@type": "dcat:Catalog",
"dcat:dataset": {
    "@id": "assetId1",
    "@type": "dcat:Dataset",
    "odrl:hasPolicy": {
        "@id": "bG9jYXRpb24tY29udHJhY3Q=:YXNzZXRJZDE=:ZTlhMTg0OWQtNjQwOS00NTIyLTk2YmQtZGJjNDg5NTU5NjJk",
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
    "@id": "3782fc79-4347-48e8-b04b-fa17c505501e",
    "@type": "dcat:DataService",
    "dcat:endpointDescription": "dspace:connector",
    "dcat:endpointUrl": "http://provider-ebird:17194/protocol",
    "dcat:endpointURL": "http://provider-ebird:17194/protocol"
    }
    },
    {
    "@type": "dcat:Distribution",
    "dct:format": {
    "@id": "HttpData-PUSH"
    },
    "dcat:accessService": {
    "@id": "3782fc79-4347-48e8-b04b-fa17c505501e",
    "@type": "dcat:DataService",
    "dcat:endpointDescription": "dspace:connector",
    "dcat:endpointUrl": "http://provider-ebird:17194/protocol",
    "dcat:endpointURL": "http://provider-ebird:17194/protocol"
    }
    }
    ],
    "dcterms:name": "example example",
    "special": "test",
    "name": "Example dataset",
    "id": "assetId1",
    "contenttype": "application/json"
    },
"dcat:catalog": [],
"dcat:distribution": [],
"dcat:service": [
    {
    "@id": "fc5b7b70-6095-4edb-a151-c28192d8c5c7",
    "@type": "dcat:DataService",
    "dcat:endpointDescription": "dspace:connector",
    "dcat:endpointUrl": "http://localhost:17194/protocol",
    "dcat:endpointURL": "http://localhost:17194/protocol"
    },
    {
    "@id": "3782fc79-4347-48e8-b04b-fa17c505501e",
    "@type": "dcat:DataService",
    "dcat:endpointDescription": "dspace:connector",
    "dcat:endpointUrl": "http://provider-ebird:17194/protocol",
    "dcat:endpointURL": "http://provider-ebird:17194/protocol"
    }
    ],
"dspace:participantId": "provider-ebird",
"@context": {
"schema": "https://schema.org/",
"cbm": "https://w3id.org/cbm/v0.0.1/ns/",
"dcat": "http://www.w3.org/ns/dcat#",
"dct": "http://purl.org/dc/terms/",
"odrl": "http://www.w3.org/ns/odrl/2/",
"dspace": "https://w3id.org/dspace/v0.8/",
"@vocab": "https://w3id.org/edc/v0.0.1/ns/",
"edc": "https://w3id.org/edc/v0.0.1/ns/"
}
}
````

### 4. Start a contract negotiation

``` curl
curl --location 'http://localhost:29193/management/v3/contractnegotiations' \
--header 'Content-Type: application/json' \
--data-raw '{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "ContractRequest",
  "counterPartyAddress": "http://provider-ebird:17194/protocol",
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
'
```

Here we must provide the ASSET_ID and the CONTRACT_OFFER_ID

In this case the contract offer id is:

```json
  {
  "@id": "bG9jYXRpb24tY29udHJhY3Q=:YXNzZXRJZDE=:ZTlhMTg0OWQtNjQwOS00NTIyLTk2YmQtZGJjNDg5NTU5NjJk"
  }

```

We get this response:

``` json
{
"@type": "IdResponse",
"@id": "86278770-ebc4-4752-a171-590915aa8837",
"createdAt": 1757496764230,
"@context": {
"@vocab": "https://w3id.org/edc/v0.0.1/ns/",
"edc": "https://w3id.org/edc/v0.0.1/ns/",
"odrl": "http://www.w3.org/ns/odrl/2/"
}
}
```

### 5. Get the contract negotiation state

Using the ID received in the previous step, we can now view the state of the negotiation by calling another endpoint of the consumer's management API:
```curl
curl --location --globoff 'http://localhost:29193/management/v3/contractnegotiations/{{CONTRACT_ID}}'
````


``` json
{
    "@type":"ContractNegotiation",
    "@id":"86278770-ebc4-4752-a171-590915aa8837",
    "type":"CONSUMER",
    "protocol":"dataspace-protocol-http",
    "state":"FINALIZED",
    "counterPartyId":"provider-ebird",
    "counterPartyAddress":"http://provider-ebird:17194/protocol",
    "callbackAddresses":[],
    "createdAt":1757496764230,
    "contractAgreementId":"fed3b6bb-2921-4417-89c5-7d300989f4a7",
    "@context":{"@vocab":"https://w3id.org/edc/v0.0.1/ns/",
    "edc":"https://w3id.org/edc/v0.0.1/ns/","odrl":"http://www.w3.org/ns/odrl/2/"
}
```

Since we received a contractAgreementId,
it means the contract definition was approved.

If we check the containers logs

```
docker logs {{CONTAINER-ID}}
```

We get this logs

````
INFO 2025-09-10T09:32:45.019740525 {region=eu, client_id=consumer-base}

INFO 2025-09-10T09:32:45.019785192 Evaluating constraint: location EQ eu
````


As we can see the policy was evaluated and the region of the consumer was eu


### Test a declined offer

1. Stop the consumer-base container

``
docker compose down consumer-base 
``

2. Add the following line to ./consumer-base/resources/configuration/consumer-base-docker-configuration.properties

```
edc.mock.region=us
```

3. Rebuild and start the consumer-base container

``
docker compose build --no-cache consumer-base
``

``
docker compose up -d consumer-base
``

4. Make a catalog request
5. Start a contract negotiation
6. Get the contract negotiation state

In this case we would get this response.
````
{
    "@type": "ContractNegotiation",
    "@id": "342e23ca-b392-46ea-908e-87b3fcb7bb73",
    "type": "CONSUMER",
    "protocol": "dataspace-protocol-http",
    "state": "TERMINATED",
    "counterPartyId": "provider-ebird",
    "counterPartyAddress": "http://localhost:17194/protocol",
    "callbackAddresses": [],
    "createdAt": 1757513546848,
    "errorDetail": "Failed to send termination to counter party: Value in JsonObjects name/value pair cannot be null",
    "@context": {
        "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
        "edc": "https://w3id.org/edc/v0.0.1/ns/",
        "odrl": "http://www.w3.org/ns/odrl/2/"
    }
}
````

Provider logs


```
INFO 2025-09-10T18:38:56.332370238 {region=us, client_id=consumer-base}
INFO 2025-09-10T18:38:56.33250927 Evaluating constraint: location EQ eu
DEBUG 2025-09-10T18:38:56.33453093 [Provider] Contract offer rejected as invalid: Policy in scope contract.negotiation not fulfilled: [Permission constraints: [Constraint 'https://w3id.org/edc/v0.0.1/ns/location' EQ 'eu']]
DEBUG 2025-09-10T18:38:56.335353211 DSP: Service call failed: Contract offer is not valid: Policy in scope contract.negotiation not fulfilled: [Permission constraints: [Constraint 'https://w3id.org/edc/v0.0.1/ns/location' EQ 'eu']]
```


In this case the policy was evaluated but was rejected since the region of the consumer
was us instead of eu

Note: The errorDetail is an EDC bug which has been reported.

[Check the official docs](https://github.com/eclipse-edc/Samples/tree/main/policy/policy-01-policy-enforcement)


### About access policy and contract policy in Contract definition


🔒 **Access policy**: determines whether a particular consumer
is offered an asset when making a catalog request.
For example, we may want to restrict certain assets such that only
consumers within a particular geography can see them.
Consumers outside that geography wouldn’t even have them in their catalog.


🔒 **Contract policy**: determines the conditions for
initiating a contract negotiation for a particular asset.
Note that this only guarantees the successful initiation of a
contract negotiation,
it does not automatically guarantee the successful conclusion of it!


Things to keep in mind:

### - How to set  an access policy:

1. Create policy
2. Add policy as policy access in contract definition
3. Create policy function inside the policy functions extension
4. Register policy function using catalog scope

Each time a consumer requests the catalog, the access policy is evaluated.
If the consumer meets the policy requirements,
the corresponding offer will be included in the catalog; otherwise,
it will be excluded.

⚠️ Every time that the Federated Catalog (FC) makes a request to get the assets of a provider,
this policy will be evaluated, if the FC meets the policies then will be able to scrap the asset otherwise will not be able to access it.

⚠️  If only access policy is added on contract definition,
but this policy has not been registered in the catalog scope,  
then this policy will not be evaluated.

⚠️  If a policy funtion is bound to catalag scope
and contractNegociation scope,
the accessPolicy and contract policy of contract definition will be respected.


⚠️  A policy function cannot be bound to ALL SCOPES,  this works only for odrl action (use)

Currently, we plan to bind policy functions to both scopes so contract definition can choose.


------------------------------------
### Reminder:

1. To enable the provider to verify participant claims against the validated claims stored in the Participant Registry (FC), the following properties must be added in the provider config file.
    
    ``` edc.participant.registry.url={{url}} ```

2. There is an already created policy with no restrictions, this policy has a fixed id : open-policy, this policy can be used when we would like to apply a NO RESTRICTION policy.
   (To be able to use this, the proper extension must be included)