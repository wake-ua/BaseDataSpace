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