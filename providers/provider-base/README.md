# Base Provider
Provider inheriting from the main provider with some addtions:
- Hashicorp vault

## Hashicorp Vault
See instructions at: https://developer.hashicorp.com/vault/docs/get-started/developer-qs

Further tutorials at: https://developer.hashicorp.com/vault/docs/get-started/operations-qs
### Install Hashicorp Vault
```shell
# Install vault in your os following https://developer.hashicorp.com/vault/install
# For mac 
brew tap hashicorp/tap
brew install hashicorp/tap/vault
vault -help
````

### Development mode for Hashicorp Vault
```shell
# Start vault in dev mode
vault server -dev -dev-root-token-id="dev-only-token"

# OPEN NEW TERMINAL
# Export options for local self-signed certificate
export VAULT_ADDR=http://127.0.0.1:8200
```

### Production mode for Hashicorp Vault (WIP)
```shell
# go to the root project resources folder
cd resources/vault

# Start vault with provided config
server -config=config.hcl    
```

Then go to http://localhost:8200 and finish the setup. Alternatively, use the commandline:
```shell
vault operator init
```
You can select just 1 for the number of keys.
Write down or save the token (`root_token`) and the keys (`keys_base64`) for future usage.

Then edit the provider configuration to include the new hashicorp token.

For further restarts of the vault, you will need to unseal it via the web UI or with the command:
```shell
vault operator unseal <KEYS_BASE64>
```

Please check beforehand the vault contents via API, client or curl (see bottom of this page).

## Vault setup

```shell
# OPEN NEW TERMINAL
# Export environment variables
export VAULT_ADDR=http://127.0.0.1:8200

# login
 vault login <TOKEN>

# enable the secret store
vault secrets enable -path=secret/ kv

# save the key-value pairs
vault kv put secret/accessKeyId content=provider
vault kv put secret/secretAccessKey content=password
vault kv put secret/provider-key content=password

vault kv put secret/edc.datasource.default.user content=postgres
vault kv put secret/edc.datasource.default.password content=postgres
vault kv put secret/edc.datasource.default.url content=jdbc:postgresql://localhost:5432/edc

vault kv put secret/web.http.management.auth.key content=managementApiKeyProviderBase

```
### TROUBLESHOOTING
If the above creates problems when running the provider, try creating the secrets using json as follows:
```shell
# go to the root project resources folder
cd resources/vault

# run the following commands
vault kv put secret/data/edc.datasource.default.password @password.json     
vault kv put secret/data/edc.datasource.default.user @user.json
vault kv put secret/data/edc.datasource.default.url @url.json
vault kv put secret/data/web.http.management.auth.key @management-apikey.json
```
Afterward check that the output of calling `curl --header "X-Vault-Token: <TOKEN>" --request GET http://127.0.0.1:8200/v1/secret/data/edc.datasource.default.url`    
is similar to this (note the data sections in JSON):
```json
{"request_id":"1e5032f5-c8e3-6f7c-5feb-c2667e23e43c",
  "lease_id":"","renewable":false,"lease_duration":2764800,
  "data":{"data":{"content":"jdbc:postgresql://localhost:5432/edc"}},
  "wrap_info":null,"warnings":null,"auth":null,"mount_type":"kv"}
```
Alternatively, you can create all this in the web interface at http://127.0.0.1:8200 using the JSON switch in the secrets editor. 