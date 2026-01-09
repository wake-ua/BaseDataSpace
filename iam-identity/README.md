## I AM - IDENTITY


This extension generates and verifies the participant token and includes the participant claims && participant 
signed claims, these claims are signed with the participant private key. 
----------------
## In participant onboarding, participant needs:

- One private key → used only to sign
- One public key → shared with participant registry to verify signatures

This works in the following way:

Participant(Consumer) 1:

- Signs data with their private key
Sends (claims + signature) to provider

Participant (Provider) 2:
- Looks up participant 1’s public key in participant registry.
- Verifies the signature
- Verifies the claims were created by participant 1
- Verifies that claims were not modified
-----

How to generate key pair




``
openssl genpkey -algorithm Ed25519 -out ed25519_private.pem
``

``
openssl pkey -in ed25519_private.pem -pubout -out ed25519_public.pem
``

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
}
```

To specify the path containing the private key.

``
edc.participant.private.key=<path-to-pem-file>
``
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
    "location": "eu",
    "entityType": "public"
    },
     "security": {
        "pem": "MCowBQYDK2VwAyEA/UfM1mYj4c3y7P7AXigfjl08PXISX/0ixait4gflOQU="
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