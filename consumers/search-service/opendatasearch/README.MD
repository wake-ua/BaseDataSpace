## 🌟 Highlights 
Los portales de datos abiertos aportan conjuntos de datos de gran interés, sin embargo, a menudo no se presentan como cabría esperar, no recuperan los datos deseados, falta información o están vacios. Mediante el desarrollo de este buscador propio, buscamos mejorar la experiencia de usuario adaptando el proceso de recuperación de la información, además de integrar diferentes portales en uno mismo para centralizar los datos. En paralelo, a través de otros proyectos del grupo, trabajamos activamente en la limpieza y mejora de la calidad de los datos.

## ℹ️ Overview 
Proyecto enfocado al desarrollo de un buscador de datos abiertos indexados en la base de datos **Qdrant**. Diseñado para almacenar datos compuestos por metadatos (título y descripción) y datos tabulares (cabeceras y contenido). La aplicación está implementada con **FastAPI** y se sirve mediante **Uvicorn**.

## 📄 Adaptación de Dominio 
Actualmente no se plantea adaptar el buscador a otro tipo de datos.

## 🔧 Setup 
Recuerda crear un nuevo entorno para evitar crear conflictos con tus otros proyectos, instala los requisitos mediante:

```{bash}
pip install -r requirements.txt
```
Requiere que la base de datos de Qdrant (https://github.com/Josemelgahez/qdrant) esté activa. Antes de hacer ninguna búsqueda, es necesario:
1. Crear las colecciones que almacenan los datos. (Hecho en el repositorio de Qdrant)
2. Indexar datos mediante el fichero "index_limpio.py".

## 🧩 Ejemplo de Ejecución
Ejecutar el buscador
```
uvicorn main:app --port 8002
```
Búsquedas por query:
```
http://127.0.0.1:8002/search?q=Turismo
```
Búsquedas por filtros:
```
http://127.0.0.1:8002/search?theme=Economía
```
Búsquedas por query + filtros:
```
http://127.0.0.1:8002/search?q=Turismo&theme=Economía
```
También soporta filtros por ubicación (geo), fecha de inició (temporal_start) y fecha de finalización (temporal_end).

## 🎯 Objetivo Actual
Trabajo de interfaz y clusterización de resultados.
