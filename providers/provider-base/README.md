Provider inheriting from the main provider with some addtions:
 - Hashicorp vault

### Hashicorp Vault
See instructions at: https://developer.hashicorp.com/vault/docs/get-started/developer-qs

Further tutorials at: https://developer.hashicorp.com/vault/docs/get-started/operations-qs

```shell
# Install vault in your os following https://developer.hashicorp.com/vault/install
# For mac 
brew tap hashicorp/tap
brew install hashicorp/tap/vault
vault -help

# Start vault in dev mode
vault server -dev -dev-root-token-id="dev-only-token"

# OPEN NEW TERMINAL
# Export options for local self-signed certificate
export VAULT_ADDR=http://127.0.0.1:8200

# login
 vault login dev-only-token

# enable the secret store
vault secrets enable -path=secret/ kv

# save the key-value pairs
vault kv put secret/accessKeyId content=provider
vault kv put secret/secretAccessKey content=password
vault kv put secret/provider-key content=password

vault kv put secret/edc.datasource.default.user content=postgres
vault kv put secret/edc.datasource.default.password content=postgres
vault kv put secret/edc.datasource.default.url content=jdbc:postgresql://localhost:5432/edc

```