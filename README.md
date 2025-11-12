# Heleade Base Data Space 

Base Data Space for the Heleade project

## Components

* PROVIDERS:
  * **Base Provider**: Ports 1919x / 1929x
  * **Base Provider Prod**: (under development) Ports 1919x (same as base provider) / 1929x (same as base provider)
  * **eBird Provider**: Ports 1719x / 1729x
  * **Mastral Provider**: Ports 1819x / 1829x
* CONSUMERS:
  * **Base Consumer**: Ports 2919x / 2929x
  * **Search Service**: Ports 2719x / 2729x
  * **Climate Service**: Ports 2819x / 2829x
* **Federated Catalog**: Ports 3919x / 3929x

## Setup
### Database
#### Setting up the database
To run a PostgreSQL database, you can start a container using this command:
```bash
docker run -d --name data_space_wake_ua -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=edc -p 5432:5432 postgres:17
```
#### Connecting the Database
Whenever your container is up and running, you should connect your database by SQL Client such as DBeaver.
Once installed do the following:
1. Open DBeaver and click on New Database Connection.
2. Select PostgreSQL from the list of database types.
3. Fill in the connection details:
   - Host: localhost 
   - Port: 5432
   - Database: edc
   - Username: postgres (or the one that you specify in the docker run command)
   - Password: postgres (or the password you set)
4. Test the connection and click Finish if successful.

After connecting, open a new SQL Editor in this connection and follow these instructions for security purposes:
```sql
-- 0. Change the password for the user defined as POSTGRES_USER
ALTER USER postgres WITH PASSWORD 'XXXXX';


-- 1. Create a new user with a password and limited permissions.
-- This user will not have 'root' (superuser) privileges.
CREATE USER wake WITH PASSWORD 'YYYYYY';


-- 2. Create a new database and assign ownership to the limited user.
-- Syntax: CREATE DATABASE database_name OWNER user_name;
CREATE DATABASE edc_ebird OWNER wake;

-- 3. Grant all privileges on the database and public schema to the new user.
-- For testing, you can grant all privileges, but in production it's better to limit permissions.
GRANT ALL PRIVILEGES ON DATABASE edc TO wake;
GRANT ALL PRIVILEGES ON SCHEMA public TO wake;
```

After this setup, you can reuse this single PostgreSQL container for multiple providers, consumers, or any other services that need database access for testing purposes at least.
By creating a new database and a dedicated user for each service, you effectively "jail" them to their own isolated database environment.
This approach keeps all your tests or services organized within the same PostgreSQL instance, without mixing permissions or data.

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

### Docker Deployment
Docker compose deployment instructions: [README](./deployment/README.md)
