# Providers

In this module the different provider implementations are included.

* provider: Library to extend from
* provider-base: Basic provider executable without any additions
* provider-base-prod: Provider with enhanced security for production deployment

The provider stores its catalog in a postgresql database that needs to exist beforehand. 
It will create the tables automatically with the configuration option `edc.sql.schema.autocreate=true`


