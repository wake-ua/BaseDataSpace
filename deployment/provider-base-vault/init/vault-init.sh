#! /bin/sh

set -e

export VAULT_ADDR=http://provider-base-vault:8200

# give some time for Vault to start and be ready
sleep 3

# login
 vault login dev-only-token

# enable the secret store
vault secrets enable -path=secret/ kv && echo continue || true

# save the key-value pairs
vault kv put secret/accessKeyId content=provider
vault kv put secret/secretAccessKey content=password
vault kv put secret/provider-key content=password

vault kv put secret/edc.datasource.default.user content=postgres
vault kv put secret/edc.datasource.default.password content=postgres
vault kv put secret/edc.datasource.default.url content=jdbc:postgresql://provider-base-postgresql:5432/edc

# vault kv get secret/edc.datasource.default.url