## Deployment with Docker-Compose

### Development environment
It is composed of the following containers:

* provider-base-vault: Hashicorp vault for the provider base
* provider-base-vault-init: Script to initialize the vault (temporary, looking for a better solution) 
* provider-base-postgresql: PostgreSQL database for the provider-base container. Please delete the file [.gitignore](provider-base-postgresql/data/.gitignore) when starting the database and add it back later.
* provider-base: Provider Base with this [configuration](../providers/provider-base/resources/configuration/provider-base-docker-configuration.properties).
This container may FAIL initially, wait for _provider-base-vault-init_ to finish and restart it. 
* consumer-base: Consumer base with the [configuration](../consumers/consumer-base/resources/configuration/consumer-base-docker-configuration.properties).
* request-logger: Echo web server to check the data transfer.
* fc-mongodb: MongoDB database for the federated catalog 
* federated-catalog: Federated catalog. Please execute the [initialization script](init.sh) to add the _provider-base_ to the node directory.

The data space can be started with docker-compose from the file [docker-compose.yaml](../docker-compose.yaml) from the project root.
```shell
  docker compose up
```
Then run the script [init.sh](init.sh) to add the provider-base and the provider-ebird to the node directory.

Ypou can find some calls to test the setup in the [test.sh](test.sh) script.


### Production environment

(under development)

First launch the provider-base-vault from its subdirectory using the file: [docker-compose-provider-base-vault](./provider-base-vault/docker-compose-provider-base-vault.yaml).
The vault has to be initialized and unsealed. Once unsealed, you have to add the secrets by hand or run the [initialization script](./provider-base-vault/init/vault-init.sh)

Then launch the rest of the dataspace with the file [docker-compose-prod.yaml](../docker-compose-prod.yaml)