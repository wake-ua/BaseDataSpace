## Despliegue de aplicativos, 
Debido a que todavía no tenemos contenerizada la aplicación, debemos realizar la construcción de las imágenes en local, por lo que primero debemos construir la imagen, y posteriormente, en el docker-stack, indicar la imagen.

Primero construimos la imagen, para el catalogo por ejemplo, vamos a BaseDataSpace/federated-catalog y:
```sh
docker build -t federated-catalog-local:latest .
```

Y posteriormente, en el docker-stack del catálogo federado debemos de modificar (en caso de que se requiera) la imagen.

Habrá que tener en cuenta si esta tiene secrets o no



