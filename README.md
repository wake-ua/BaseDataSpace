# *HELEADE/Base Data Space*

> Base Data Space for the Heleade project

---

## Objectives

- Develop a base Data Space.  
- Scalabe and secure.

---

## Components

* PROVIDERS:
  * **Base Provider**: Ports 1919x / 1929x
  * **Climate Provider**: Ports 1619x / 1629x
  * **eBird Provider**: Ports 1719x / 1729x
  * **Mastral Provider**: Ports 1819x / 1829x
  * **Idearium Provider**: Ports 1519x / 1529x
* CONSUMERS:
  * **Base Consumer**: Ports 2919x / 2929x
  * **Search Service**: Ports 2719x / 2729x
* **Federated Catalog**: Ports 3919x / 3929x

---

## Funding Information

This research project is supported by:

- **Funding organization/institution:** MINISTERIO PARA LA TRANSFORMACION DIGITAL Y DE LA FUNCION PUBLICA
- **Program or grant:** CONVOCATORIA DE AYUDAS PROGRAMA DE ESPACIOS DE DATOS SECTORIALES PARA LA TRANSFORMACIÓN DIGITAL DE LOS SECTORES PRODUCTIVOS ESTRATÉGICOS MEDIANTE LA CREACIÓN DE DEMOSTRADORES Y CASOS DE USO DE ESPACIOS DE COMPARTICIÓN DE DATOS
- **Project code/reference:** TSI-100121-2024-24
- **Duration:** [01/11/2024 – 31/12/2025] 

---

## Technology

Provide a brief description of the main technologies used.
- Java
- EDC Connector

---

## Installation and Usage

### Docker Setup
Docker compose deployment instructions: [README](./deployment/README.md)

### Local Setup 

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

To properly generate the key pair jq must be installed.

``sudo apt install jq``

``brew install jq``

[generate-keys.sh](system-tests/src/test/resources/keys/generate-keys.sh) should have execution permissions.

For that run:

``chmod +x generate-keys.sh``





### Postman Collection
The postman collection is under development.
Import into Postman the file BaseDS-EDC.postman_collection.json

Start endpoint echo for transfers:
```
docker build -t http-request-logger util/http-request-logger
docker run -p 4000:4000 http-request-logger
```

---

## Authors / Contributors

- **Collaborators:**
- Lucía De Espona Pernas – [@espona](https://github.com/espona)
- María de los Ángeles Ortega Rivas – [@ortegi](https://github.com/ortegi)
- David Bernabeu Ferrer – [@ortegi](https://github.com/dvddepennde)

---

## License

This project is distributed under the [MIT License](LICENSE).

---

## References

### Articles
**Morejón, A., Berenguer, A., de Espona, L., Tomás, D., & Mazón, J.-N. (2025).**  
*Exploring Content-Based Catalogs for Enhanced Discovery Services in Data Spaces.*  
CEUR.

---

### Conferences

**Morejón, A., Berenguer, A., de Espona, L., Tomás, D., & Mazón, J.-N. (2025).**  
*Improving Data Discovery Effectiveness: Experimental Evaluation of Content-Based Catalogs in Data Spaces.*  
In *European Conference on Advances in Databases and Information Systems* (pp. 280–295).  
Springer Nature Switzerland Cham.

---

**Morejón, A., de Espona, L., & Berenguer, A. (2025).**  
*Extension of Data Catalog Vocabulary for Federating Open Datasets in Data.*  
In *New Trends in Database and Information Systems: ADBIS 2025 Short Papers, Workshops, Doctoral Consortium and Tutorials, Tampere, Finland, September 23–26, 2025, Proceedings* (p. 108).  
Springer Nature.

---

**Morejón, A., de Espona, L., Berenguer, A., Tomás, D., & Mazón, J.-N. (2025).**  
*Extension of Data Catalog Vocabulary for Federating Open Datasets in Data Spaces.*  
In *European Conference on Advances in Databases and Information Systems* (pp. 108–117).  
Springer Nature Switzerland Cham.

---

## 💬 Contact

For questions, collaborations, or further information:

📧 [wake@dlsi.ua.es](mailto:wake@dlsi.ua.es)  
🌐 [Wake Research group](https://wake.dlsi.ua.es/)
