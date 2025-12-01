# Heleade Base Data Space 

Base Data Space for the Heleade project

## Components

* PROVIDERS:
  * **Base Provider**: Ports 1919x / 1929x
  * **Service Provider **: Ports 1619x / 1629x
  * **eBird Provider**: Ports 1719x / 1729x
  * **Mastral Provider**: Ports 1819x / 1829x
* CONSUMERS:
  * **Base Consumer**: Ports 2919x / 2929x
  * **Search Service**: Ports 2719x / 2729x
  * **Climate Service**: Ports 2819x / 2829x
* **Federated Catalog**: Ports 3919x / 3929x

### Docker Setup
Docker compose deployment instructions: [README](./deployment/README.md)

## Local Setup 

The providers need a postgresql database.
The federated catalog requires a MongoDB instance.

#### PostgreSQL
To run a PostgreSQL database, you can install it locally or start a container using this command:
```bash
docker run -d --name data_space_wake_ua -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=edc -p 5432:5432 postgres:17
```
The database name, user and password are detailed in the configuration file, for example: [provider base config](./providers/provider-base/resources/configuration/provider-base-configuration.properties)

For development/testing you can reuse a single PostgreSQL server for multiple providers.
In a productive environment, and according to the Data Space concept, each connector should use its own servers.

### Build
Build the whole project:
```
./gradlew clean build 
```
Execute the components:
* Provider Base
```
java -Dedc.fs.config=providers/provider-base/resources/configuration/provider-base-configuration.properties -jar providers/provider-base/build/libs/provider-base.jar --log-level=DEBUG
```
* Provider Ebird
```
java -Dedc.fs.config=providers/provider-ebird/resources/configuration/provider-ebird-configuration.properties -jar providers/provider-ebird/build/libs/provider-ebird.jar --log-level=DEBUG
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

