## 🌟 Highlights 
Multitud de los portales de datos abiertos con los que trabajamos a menudo tienen caídas de servicio. Este repositorio recoge los pasos y código necesario para desplegar una base de datos **Qdrant** en modo distribuido, aportando una mayor robustez ante fallos o caidas de nodos.

## 🔧 Setup
Previamente al despliegue de la base de datos, es necesario realizar un proceso de firma de certificados TLS para habilitar la comunicación segura entre nodos.
El código necesario para hacer este proceso se encuentra en las carpetas /Master/generacion_tls y /Slave/generacion_tls.

1. Máquina maestra: Generación de certificados con /Master/generacion_tls/01_generate_ca_and_master.sh 
2. Máquinas esclavas (Solo para despliegue distribuido): Generación de certificados con /Slave/generacion_tls/02_generar_csr_esclavo.sh. Requiere pasar como parámetro la IP de la máquina esclava. (Adaptar en el código la IP de la maestra)
   - Este script genera un fichero .csr que hay que mover a la máquina maestra para que lo autorice.
3. Máquina maestra (Solo para despliegue distribuido): Autorizar cliente con /Master/generacion_tls/sh 03_firmar_cliente.sh. Requiere pasar como parámetro el nombre del fichero csr a autorizar y la IP de la máquina esclava. 
   - Este script genera dos ficheros .pem que hay que mover a la máquina esclava para que pueda comunicarse con la maestra.
4. Opcional para Buscador: Si se quiere combinar con el buscador (https://github.com/wake-ua/OpenDataSearch), es necesario copiar el fichero 'cacert.pem' en la carpeta TLS del buscador.

## 🧩 Ejemplo de Ejecución
Ejecución del buscador mediante el fichero /Master/startQdrant.sh y /Slave/startQdrant.sh (primero master y después los slaves):
```
sh startQdrant.sh
```
💡En caso de ocurrir algún error, o querer empezar de cero: detener y eliminar los contenedores de docker y la carpeta de almacenamiento que se crea con Qdrant en todas las máquinas.

Para la creación de colecciones se aporta el fichero ColeccionesQdrant.txt, con ejemplos de creación de índices, basta con copiarlo en la interfaz de qdrant y ejecutar.
   - Para despliegue no distribuido comentar las lineas "shard_number": 3 y "replication_factor": 2.
Se accede a la interfaz desde:
```{r}
https://IP-MAQUINA:6333/dashboard (Al activar TLS es obligatorio usar https)
```
Se comprueba el estado del cluster desde:
```{r}
curl -k https://IP-MAQUINA:6333/cluster -H "api-key: 123"
```

Métricas:
```{r}
curl -k "https://IP-MAQUINA:6333/metrics"   -H "Authorization: Bearer 123"
```
Métricas de memoria y accesos a cada nodo.
```{r}
curl -k https://IP-MAQUINA:6333/cluster -H "api-key: 123"
```

## 🌟 Balanceo de Carga
Para que todas las consultas no vayan a una máquina concreta, se implementa un servicio NGINX como balanceador de carga.

0. Instalar NGINX:
```
sudo apt install nginx
```
2. Cambiar la configuración de NGINX:
  Cambiar el fichero /etc/nginx/nginx.conf por el fichero del repositorio NGINX/nginx.conf.
3. Configurar las IPs
  Añadir en la carpeta /etc/nginx/conf.d/ el fichero del repositorio NGINX/qdrant.conf.
4. Copiar los certificados TLS
  Copiar los ficheros generados durante el proceso de generación de certificados TLS (cacert.pem, cert.pem y key.pem) en /etc/nginx/qdrant_tls.
5. Reiniciar el servicio:
```
  sudo nginx -t && sudo systemctl reload nginx
```
5. Ahora en lugar de acceder al puerto 6333, se accede a 8443.
```
  curl -k https://10.58.167.110:8443/dashboard -H "api-key: 123"
```
6. Los logs se ven en el siguiente fichero, para ver que balancea la carga
```
  sudo cat /var/log/nginx/access.log 
```
