## Deployment with Docker-Compose

### Development environment
It is composed of the following containers:

* Providers
  * *provider-base*: Provider Base with this [configuration](../providers/provider-base/resources/configuration/provider-base-docker-configuration.properties).  
  * eBird Provider:
    * *provider-ebird*: E-Bird Provider.
    * *provider-ebird-init*: Initialization script
    * *samples-server*: Server for the dataset samples
  * *provider-mastral*: Provider for the Mastral project
* Consumers:
  * *consumer-base*: Consumer base with the [configuration](../consumers/consumer-base/resources/configuration/consumer-base-docker-configuration.properties).
  * *search-service*: Search services connector for the catalog.
  * *climate service*: Services for climate shelters.
* *federated-catalog*: Federated catalog and participant registry.
* Other:
    * *bds-postgresql*: Shared PostgreSQL server for all the providers (only for dev purposes). 
                        Databases are created through entrypoint: [init script](./bds-postgresql/initdb/init-bds-postgresql-dev.sh)
                        Any changes on entrypoint require deleting the data directory to be excuted again. 
    * *fc-mongodb*: MongoDB database for the federated catalog
    * *request-logger*: Echo web server to check the data transfer (testing purposes).

The data space can be started with docker-compose from the file [docker-compose.yaml](../docker-compose.yaml) from the project root.

Please execute the [initialization script](init.sh) to add the _provider-base_ to the node directory to the federated catalog.

```shell
  docker compose up
```
Then run the script [init-docker-dev.sh](init-docker-dev.sh) to add the provider-base and the provider-ebird to the node directory.
You can find some calls to test the setup in the [test-docker-dev.sh](test-docker-dev.sh) script and in the [Postman collection](../BaseDS-EDC.postman_collection.json).


### Production environment

(under development)
t of the dataspace with the file [docker-compose-prod.yaml](../docker-compose-prod.yaml)