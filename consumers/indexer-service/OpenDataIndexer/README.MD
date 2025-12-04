## 🌟 Highlights 
Los portales de datos abiertos proporcionan conjuntos de datos muy valiosos, pero con frecuencia presentan inconsistencias, faltan datos o están mal estructurados.  
El **indexer-service** automatiza la obtención, limpieza y transformación de los datos provenientes del Catálogo Federado, generando embeddings y almacenándolos en **Qdrant** para que el buscador pueda ofrecer una experiencia unificada y de mayor calidad.

## ℹ️ Overview 
Proyecto enfocado en la extracción e indexación de datos abiertos procedentes del **Federated Catalog**.  
El servicio se encarga de:

- Obtener metadatos (título, descripción).  
- Descargar y procesar *samples*.  
- Convertir samples en datos tabulares cuando es necesario.  
- Generar embeddings.  
- Crear y poblar colecciones en **Qdrant**.

La aplicación está implementada en **Python** y se ejecuta dentro de Docker.

## 📄 Adaptación de Dominio 
Actualmente no se plantea adaptar este servicio a otros dominios o tipos de datos.

## 🔧 Setup 
El servicio forma parte del entorno Docker del proyecto. Para ponerlo en marcha:

```bash
docker compose build
docker compose up -d
```

Antes de ejecutar el indexador, es necesario:

1. Que el **Federated Catalog** haya cargado los datasets en su MongoDB.

2. Crear manualmente las colecciones en Qdrant desde el dashboard:

```
http://localhost:6333/dashboard#/console
```

3. Acceder al contenedor del indexador:

```bash
docker exec -it indexer-service bash
```

4. Ejecutar el proceso de indexación:

```
python run_indexer.py
```

## 🧩 Ejemplo de Ejecución

Ejecutar el indexador:

```
python run_indexer.py
```

Este proceso:

- Descarga datos del catálogo federado  
- Limpia y genera metadatos  
- Descarga y transforma samples  
- Genera embeddings  
- Inserta la información en Qdrant  

Tras completar la indexación, el buscador podrá consultar Qdrant. Ejemplos:

### Búsqueda normal:
```
http://localhost:8002/search?q=salamanca
```

### Búsqueda normal (paginada):
```
http://localhost:8002/search?q=salamanca&page=2
```

### Búsqueda por filtros:
```
http://localhost:8002/search?theme=Economía
```

### Búsqueda combinada:
```
http://localhost:8002/search?q=salamanca&theme=Economía
```

## 🎯 Objetivo Actual
Completar la integración de samples provenientes de la negociación federada y automatizar el flujo de indexación mediante un scheduler.
