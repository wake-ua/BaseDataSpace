
Execute e-bird provider:

```
java -Dedc.fs.config=providers/provider-ebird/resources/configuration/provider-ebird-configuration.properties -jar providers/provider-ebird/build/libs/provider-ebird.jar --log-level=DEBUG
```

The provider stores its catalog in a postgresql database. It is necessary to create the tables beforehand

```
psql -f providers/provider-ebird/src/main/resources/META-INF.services/database.sql
```