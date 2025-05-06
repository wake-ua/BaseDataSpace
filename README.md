# Heleade Base Data Space 

Base Data Space for the Heleade project

## Components

* **Federated Catalog**: (link)
* **Base Provider**: (link)
* **Base Consumer**: (link)

## Setup
IntelliJ IDEA based
You need a PostgreSQL connection for the provider and create the store with the sql code in the resources.
Also, temporarily the tests run on a PostgreSQL database too, it is planned to replace them with a container.

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
vault kv put secret/accessKeyId content=consumer
vault kv put secret/secretAccessKey content=password
vault kv put secret/provider-key content=password

vault kv put secret/edc.datasource.default.user content=postgres
vault kv put secret/edc.datasource.default.password content=postgres
vault kv put secret/edc.datasource.default.url content=jdbc:postgresql://localhost:5432/edc

```

## Build
Build the whole project:
```
./gradlew clean build 
```
Execute the components:
* Provider Base
```
java -Dedc.fs.config=providers/provider-base/resources/configuration/provider-base-configuration.properties -jar providers/provider-base/build/libs/provider-base.jar --log-level=DEBUG
```
* Provider Template
```
java -Dedc.fs.config=providers/provider-template/resources/configuration/provider-template-configuration.properties -jar providers/provider-template/build/libs/provider-template.jar --log-level=DEBUG
```
* Consumer Base
```
java -Dedc.fs.config=consumers/consumer-base/resources/configuration/consumer-base-configuration.properties -jar consumers/consumer-base/build/libs/consumer-base.jar --log-level=DEBUG
```
* Federated Catalog
```
java -Dedc.fs.config=federated-catalog/resources/configuration/fc-configuration.properties -jar federated-catalog/build/libs/federated-catalog.jar --log-level=DEBUG
```


### System Tests
To execute the systems tests, just right-click on the tests folder and select the option 'Run "All Tests"'.

### Postman Collection
The postman collection is under development.
Import into Postman the file BaseDS-EDC.postman_collection.json

Start endpoint echo for transfers:
```
docker build -t http-request-logger util/http-request-logger
docker run -p 4000:4000 http-request-logger
```
