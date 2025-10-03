# Providers

In this module the different provider implementations are included.

* provider: Library to extend from
* provider-base: Basic provider executable without any additions
* provider-base-prod: Provider with enhanced security for production deployment

The provider stores its catalog in a postgresql database. It is necessary to create the tables beforehand.
Alternatively, you can use the configuration option edc.sql.schema.autocreate=true
You can modify and execute the associated sql script:
```
psql -f providers/provider/src/main/resources/META-INF/services/database.sql
```
Replace the name of the database with your desired database name that will be in the configuration.


