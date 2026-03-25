To use HashiCorp vault:

### - Use edc library

```
   implementation(libs.edc.vault.hashicorp)
```

### - Add config properties to config file

```
edc.vault.hashicorp.url=http://127.0.0.1:8300
edc.vault.hashicorp.token=
edc.vault.hashicorp.api.secret.path=/v1/secret
edc.vault.hashicorp.health.check.enabled=true
```

Note:

- The hashicorp token SHOULDN'T BE the root token,
it should be created with a renewal date, otherwise an exception will be thrown. 

- Tokens starts with hvs.


To create a token:

-A policy should be created first:

Example:

``` 
vault policy write edc-policy - <<EOF
path "secret/*" {
  capabilities = ["create", "read", "update", "delete", "list", "patch"]
}
EOF
```

```
vault token create -policy=edc-policy -ttl=600h
```

The output will be the token used by edc IH.


----------------

EDC extension to use hashicorp uses **KV v2** not **KV v1**

To use KV v2 we need to execute this command:

```
 vault secrets enable -path=secret kv-v2
```

If this command is not executed, edc HashiCorp extension will CRASH.


Store secret:


```
vault kv put secret/edc.datasource.default.url content="jdbc:postgresql://localhost:5436/edc_consumer_base"
vault kv put secret/edc.datasource.default.user content="postgres"
vault kv put secret/edc.datasource.default.name content="default"
vault kv put secret/edc.datasource.default.password content="postgres"
```

```
vault kv put secret/edc.datasource.default.url content="jdbc:postgresql://localhost:5436/edc_ebird"
vault kv put secret/edc.datasource.default.user content="postgres"
vault kv put secret/edc.datasource.default.name content="default"
vault kv put secret/edc.datasource.default.password content="postgres"
```

For connector secrets, we will need to init them somehow. 


Test if token and permissions are fine:


```
curl -H "X-Vault-Token:  <token>" http://localhost:8300/v1/secret/data/edc.datasource.default.url
```

A response like this will be returned:

```json
{"request_id":"5424bec0-1a13-55d0-c61b-2c2e8caf934c","lease_id":"","renewable":false,"lease_duration":0,"data":{"data":{"content":"postgres"},"metadata":{"created_time":"2026-03-25T11:39:40.154764689Z","custom_metadata":null,"deletion_time":"","destroyed":false,"version":1}},"wrap_info":null,"warnings":null,"auth":null,"mount_type":"kv"}
```
