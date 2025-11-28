## IAM CLAIMS


This extension generates and verifies the participant token and includes the participant claims inside it.

When the extension is initialized, it loads participant claims.

By default, the claims are read from a local file, but this behavior can be replaced with other sources in the future.

#### Configuration

To specify the path to the JSON file containing the participant claims, add the following property to your configuration file:

```
edc.participant.claims=<path-to-json-file>
```

```
edc.participant.claims=resources/claims.json
```

Example Claims File

The JSON file should define the participant claims that will be included in the generated token.
Here’s an example structure:


```json

{
  "location": "eu",
  "entityType": "public",
  "membership": {
    "level": "SILVER",
    "branch": "operator"
  }
}
```

### Reminder
When creating a participant—especially a consumer:

Be sure to specify the path to the JSON file that contains the participant’s credentials:
edc.participant.claims = {path to JSON credentials file}
Without this, the participant will not have any claims in the token.

Also remember that for contract negotiation to work, the consumer participant node must be located in the node directory with the necessary claims for verification.

In a production environment, this process will be handled by the dataspace authority.

Example:
``` json
{
    "name": "Default Base Consumer",
    "id": "search-service",
    "supportedProtocols": [],
    "claims": {
    "membership": {
    "level": "SILVER",
    "branch": "operator"
    },
    "location": "eu"
    },
    "attributes": {
    "role": "consumer with claims",
    "description": "consumer node"
    },
    "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
    }
}
```


#### Important:

Since the Identity Hub was overly complex and not functioning correctly, 
we decided not to use it. Instead, 
this extension implements the IdentityService interface, 
with a small modification to include claims in the generated token so 
they are available to the participant agent.
Because the Identity Service component in EDC is not well documented,
much of this understanding 
comes from community issues and discussions on Discord. 
In the future, we can—and should—expand this implementation to 
further improve identity handling.