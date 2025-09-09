#

These are some scripts to seed provider-ebird.

It creates assets, and samples from Ebird API using the new Dcat endpoint. 

It also creates some policies and contracts for the created assets and samples. 

#### 1. Ebird Token

Generating the assets requires making API calls to the eBird API. Make sure you have a valid access token to authenticate your requests.

You can obtain an access token by visiting this link. You'll need to create an account on the eBird platform if you don't already have one. After signing up, you’ll be able to generate your personal API token.

After you have your token, create a .env file in the root of the project and add the following line:

```
API_KEY={Token}
```

#### 2. .ENV File Configuration

Add the following environment variables to your .env file:

BASE_URL: The URL and management port of the provider. You can find this in the provider's configuration.properties file under the key web.http.management.port.

SAMPLE_BASE_URL: The base URL where sample data will be served (used for simulation purposes).


#### 3.  Script

main.py creates assets and samples using the new dcat endpoint. This allows to create a sample asset and relate it to an asset. 

Also for each sample data, it creates a file, so it is posible to simulate that this sample asset is actually being served somewhere and the base url is valid, so when making the transfer process, it can actually work. 

For this, in /create_assets/samples there is a docker file that serve the static files. 




