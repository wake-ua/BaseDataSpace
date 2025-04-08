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
