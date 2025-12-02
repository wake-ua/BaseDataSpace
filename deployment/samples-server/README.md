
 ## Server Setup with Nginx

- **Provider folders**: Each provider must have a dedicated folder named after the provider (e.g., `provider1`, `provider2`).
- **Sample files**: Place all related sample files inside the corresponding provider folder.

---
The samples server uses **Nginx** to serve the static sample files via HTTP.

- The server is available inside the Docker network as **`samples-server:80`**.
- The server is exposed on **port 9099** of the local machine.  
- When creating the sample, the **base-url** will be something like: http://samples-server/provider-ebird/sample-asset-recent-notable-observations-in-albacete.json

