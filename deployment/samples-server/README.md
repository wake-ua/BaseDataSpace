
 ## Samples server with Nginx
---
The samples server uses **Nginx** to serve the static sample files via HTTP.

- The server is available inside the Docker network as **`samples-server:80`**.
- The server is exposed on **port 9099** of the local machine.  
- When creating the sample, the **base-url** will be something like: http://samples-server/provider-ebird/sample-asset-recent-notable-observations-in-albacete.json


### How to enable a provider to use the sample server


1. **Create a folder for the provider**: 
   Inside [samples](./samples)
   directory, create a folder named after the provider.
   Example: ```/samples/provider-ebird```
2. Mount this folder as a volume in the docker container of the provider. 
Example:
```
provider-ebird-init:
  container_name: provider-ebird-init
  volumes:
    - ./deployment/samples-server/samples/provider-ebird:/app/samples/data
```
Inside the container, samples will be written to:

```/app/samples/data```

3. Ensure generated samples end up in the mounted folder.
All sample files created by the container must be saved inside the volume path (/app/samples/data).
This ensures the files are persisted on the host and available for the sample server.

4. Provide the correct base URL when creating the sample-asset
   
The URL should be constructed as:

```
http://samples-server/<provider-folder>/<file-name>
```

