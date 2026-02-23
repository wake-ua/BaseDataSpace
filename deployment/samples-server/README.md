Sample server.
---
The sample server uses **Nginx** to serve static sample files over HTTP.

- The server is available inside the Docker network as **`sample-server:80`**.
- The server is exposed on **port 9990** of the local machine.
- When creating a sample, the **base-url** format is:

`http://<base-url>/samples/<provider-id>/<asset-id>`



### How to enable a provider to use the sample server

1. Dataspace authority creates a new provider using the sample server API.

```bash
curl --location 'http://<base-url>/api/providers/' \
--header 'Authorization: <admin-token>' \
--header 'Content-Type: application/json' \
--data '{
  "provider_id": "<provider-id>"
}'
```

This returns the provider credentials. Example:

```json
{
    "provider_id": "provider_ebird",
    "password": "*qgK%KP1mq3gbGig",
    "share_token": "=F-w-B2NflpFFwqs"
}
```

2. Provider login

The provider logs in using the sample server API and the credentials obtained from dataspace authority.

```bash
curl --location 'http://<base-url>/auth/login' \
--header 'Content-Type: application/json' \
--data '{
    "password": "*qgK%KP1mq3gbGig",
    "provider_id": "provider_ebird"
}'
```

This returns a token that must be stored by the provider.

Example:

```json
{
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwcm92aWRlcl9lIiwiZXhwIjoxNzc0MjYwODY5fQ.zOfSyZ7_8vMXzLvIv36f6yYxVHaS60LR3zoQbkGuBSI",
    "token_type": "bearer"
}
```

3. Sample creation

To host a file, create a sample.

```bash
curl --location 'http://<base-url>/samples/' \
--header 'Authorization: Bearer <provider-token>' \
--form 'file=@"/route/to/file.txt"' \
--form 'asset_id="2d184692-b683-4e3d-92ab-55b0781d6c99"'
```

If the asset is created successfully:

```json
{
    "detail": "Sample created successfully",
    "sample": {
        "asset_id": "2d184692-b683-4e3d-92ab-55b0781d6c99",
        "provider_id": "provider_ebird",
        "path": "provider_ebird/2d184692-b683-4e3d-92ab-55b0781d6c99"
    }
}
```

### Web / search

##### Get sample content

Web and search services are given an admin token.
Use this token to get sample content.

```bash
curl --location '<base-url>/samples/provider-ebird/asset-hotspots-in-badajoz' \
--header 'Authorization:  <admin-token>'
```

Example response:

```txt
L5152982,ES,ES-EX,ES-EX-BD,39.061826,-5.613062,Acedera--Arrozales al oeste,2026-02-21 10:31,51,6
L8033687,ES,ES-EX,ES-EX-BD,39.0750911,-5.5712951,Acedera--Pueblo,2025-08-02 08:00,37,22
L5811357,ES,ES-EX,ES-EX-BD,39.0980279,-5.5645323,Acedera--Río Gargáligas,2025-12-25 12:27,92,40
L8935123,ES,ES-EX,ES-EX-BD,38.677686,-6.468465,Aceuchal--Antiguos Estiles,2026-01-18 14:33,67,22
L20169761,ES,ES-EX,ES-EX-BD,38.6472913,-6.4872987,Aceuchal--Pueblo,2026-02-18 12:43,35,19
L9817955,ES,ES-EX,ES-EX-BD,38.
```

##### Get preview

Web / seach can can request a preview of a sample.
Currently only `.csv`, `.txt`, and `.json` files are supported.

```bash
curl --location 'http://<base-url>/samples/preview/provider-ebird/asset-hotspots-in-badajoz' \
--header 'Authorization: <admin-token>'
```

For more info, check the [Postman collection](https://github.com/wake-ua/SampleServer/blob/main/Samples%20-%20Server.postman_collection.json).
