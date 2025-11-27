import os
import re
import sys
import json
import time
import csv
import requests
import numpy as np
from uuid import uuid5, NAMESPACE_DNS
from tqdm import tqdm
import torch
from sentence_transformers import SentenceTransformer
#from medir_disco_qdrant import get_cluster_gib

# ============================================================================================================================== #
DEBUG = True
DATA_PATH  = 'C:\\Users\\Selles\\git\\BaseDataSpace\\consumers\\search-service\\OpenDataSearch\\datos'
SOLO_UN_RECURSO = True # Indexar solo un recurso por distribución de metadatos
MODEL_TO_16 = True

# Nombres de las colecciones en Qdrant
COL_TIT            = 'titulos'
COL_DESC           = 'descripciones'
COL_CAB            = 'cabeceras'
COL_CON            = 'contenidos'

# Batches. Limitadores de filas y metadatos para pruebas
BATCH_SIZE         = 4
LINES_BATCH_SIZE   = 4
MAX_CONTENT_LINES  = 25
MAX_META_FILES     = None  

# Recortes por fila
MAX_CELL_CHARS     = 50   # Tope por celda
CHARS_PER_TOKEN    = 4      # Heurística chars/token
MAX_CONTENT_TEXT_CHARS = 100_000

# Tomamos el maxsize dependiendo de si trabajamos con Linux o Windows
if sys.platform.startswith("win"): 
    CSV_MAX_FIELD = 2**31 - 1 # En Windows el C long es 32-bit → máx = 2GB aprox
else:
    CSV_MAX_FIELD = sys.maxsize # En Linux/Mac el C long suele ser 64-bit → sys.maxsize cabe
csv.field_size_limit(CSV_MAX_FIELD)
# ============================================================================================================================== #
MODEL_NAME         = 'sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2'
MODEL_MAX_TOKENS   = 512 # VER QUE PONGO PARA INDEXAR SIN QUE REVIENTE

def get_model_max_tokens(model):
    try:
        if hasattr(model, 'get_max_seq_length'):
            return int(model.get_max_seq_length())
    except Exception:
        pass
    try:
        return int(model.tokenizer.model_max_length)
    except Exception:
        return 512
# ============================================================================================================================== #
# Acceso a la bbdd
API_URL            = 'http://127.0.0.1:6333'
API_KEY            = '123'
CACERT_PATH        = ''

# Insertar en qdrant
def qdrant_upsert(name, ids, vectors, texts, extras=None):
    points = []
    for uid, vec, txt in zip(ids, vectors, texts):
        payload = {'text': txt}
        if extras and uid in extras:
            payload.update(extras[uid])
        points.append({
            'id': str(uuid5(NAMESPACE_DNS, uid)),
            'vector': vec,
            'payload': payload
        })

    request = requests.put(f'{API_URL}/collections/{name}/points?wait=true',
                            headers={'Content-Type': 'application/json', 'api-key': API_KEY}, 
                            json={'points': points}, 
                            verify=CACERT_PATH)

    try:
        if request.status_code not in (200, 202):
            raise RuntimeError(f"Error insertando en '{name}': {request.status_code} {request.text}")
    finally:
        request.close() 
# ============================================================================================================================== #
 # Funciones de extracción y limpieza
"""
Devuelve el texto en el idioma preferido de un campo multilingüe.
Prioriza español ('es') y luego inglés ('en') y, si no hay, el primer valor no vacío.
Soporta:
    - str -> se devuelve tal cual
    - dict con claves de idioma, p.ej. {'es': ['...'], 'en': ['...']}
    - list de objetos {'language': 'es'|'en', 'value': '...'} (tu formato original)
    - dict {'language': 'es', 'value': '...'}
"""
def extraer_texto_multilingue(campo, preferidas=('es', 'en')):

    def _primero_no_vacio_de_lista(lst):
        # Busca el primer string/valor no vacío dentro de una lista heterogénea
        for v in lst:
            if isinstance(v, str) and v.strip():
                return v.strip()
            if isinstance(v, dict) and v.get('value'):
                val = str(v.get('value', '')).strip()
                if val:
                    return val
        return ''

    if campo is None:
        return ''

    # Caso 1: ya es string
    if isinstance(campo, str):
        return campo.strip()

    # Caso 2: dict con posibles claves de idioma o formato {'language','value'}
    if isinstance(campo, dict):
        # a) Formato {'language': 'es', 'value': '...'}
        if 'language' in campo and 'value' in campo:
            val = str(campo.get('value', '')).strip()
            if val:
                return val

        # b) Formato {'es': [...|str], 'en': [...|str], ...}
        for lang in preferidas:
            if lang in campo and campo[lang]:
                val = campo[lang]
                if isinstance(val, list):
                    res = _primero_no_vacio_de_lista(val)
                    if res:
                        return res
                elif isinstance(val, str) and val.strip():
                    return val.strip()

        # c) Fallback: primer valor no vacío en cualquier clave
        for val in campo.values():
            if isinstance(val, list):
                res = _primero_no_vacio_de_lista(val)
                if res:
                    return res
            elif isinstance(val, str) and val.strip():
                return val.strip()
        return ''

    # Caso 3: lista del tipo [{'language':'es','value':'...'}, ...] o mezcla
    if isinstance(campo, list):
        # Prioriza idiomas preferidos
        for lang in preferidas:
            for it in campo:
                if isinstance(it, dict) and it.get('language') == lang and it.get('value'):
                    val = str(it.get('value', '')).strip()
                    if val:
                        return val
        # Fallback: primer valor no vacío (string o dict con 'value')
        res = _primero_no_vacio_de_lista(campo)
        if res:
            return res
        return ''

    # Tipos no contemplados
    return ''

'''
Normaliza texto para indexación/búsqueda.
'''
def normalizar_texto(texto, minusculas=True, normalizar_puntuacion=True, normalizar_espacios=True):
    if texto is None:
        return ''
    else:
        texto = str(texto)

    if minusculas:
        texto = texto.lower()

    if normalizar_puntuacion:
        texto = re.sub(r'\.{3,}', '...', texto)
        texto = re.sub(r'([!?])[!?]+', lambda m: ''.join(sorted(set(m.group(0)), key=m.group(0).index)), texto)
        texto = re.sub(r'#{2,}', '#', texto)
        texto = re.sub(r'\s+([.,:;!?)])', r'\1', texto)
        texto = re.sub(r'(?<=\d)(°)(?=\w)', r'\1 ', texto) 
        texto = re.sub(r'(?<!\d)([.,:;!?)])(?=[^\s.,:;!?)])', r'\1 ', texto) 
        texto = re.sub(r'(\w)([¿¡#(])', r'\1 \2', texto)
        texto = re.sub(r'([¿¡#(])\s+', r'\1', texto)

    if normalizar_espacios:
        texto = re.sub(r'\s+', ' ', str(texto))
        texto = texto.strip()

    return texto

'''
Normaliza un vector, necesario ya que al hacer la media del contenido se desnormaliza
'''
def l2_normalize(vec):
    norm = np.linalg.norm(vec)
    return vec if norm == 0 else vec / norm

'''
Recorta un texto a ~max_tokens*CHARS_PER_TOKEN para limitarlo al tamaño de entrada del modelo
'''
def clip_for_model(text: str) -> str:
    max_tokens = MODEL_MAX_TOKENS
    max_chars  = max_tokens * CHARS_PER_TOKEN
    if len(text) > max_chars:
        return text[:max_chars]
    return text

# ============================================================================================================================== #
'''
Carga de todos los ficheros de metadatos
'''
def listar_meta_archivos(dir_path):
    archivos = sorted(f for f in os.listdir(dir_path) if f.startswith('meta_'))
    if MAX_META_FILES is not None:
        return archivos[:MAX_META_FILES]
    return archivos

'''
Extrae de todos los metadatos sus ids, titulos, descripciones, extras
'''
def cargar_metadatos(dir_path):
    ids, titulos, descripciones = [], [], []
    extras_titles = {}
    extras_desc   = {}
    archivos = listar_meta_archivos(dir_path)
    for fichero_json in tqdm(archivos, desc='Leyendo metadatos'):
        ruta_json = os.path.join(dir_path, fichero_json)
        try:
            with open(ruta_json, 'r', encoding='utf-8') as fh:
                meta = json.load(fh)
        except Exception as e:
            if DEBUG == True:
                print(f"[!] No se pudo leer '{fichero_json}': {e}")
            continue

        uid = meta.get('identifier') or fichero_json
        title = clip_for_model(normalizar_texto(extraer_texto_multilingue(meta.get('title'))))
        desc  = clip_for_model(normalizar_texto(extraer_texto_multilingue(meta.get('description'))))
        if not title and not desc:
            continue

        recursos = meta.get('resources') or []
        # Soporta dict o list
        if isinstance(recursos, dict):
            recursos = recursos.values()
        res_files = [r.get('path') for r in recursos if r.get('path')]

        ids.append(uid)
        titulos.append(title)
        descripciones.append(desc)

        extras_titles[uid] = {
            'theme': meta.get('theme'),
            'temporal': meta.get('temporal'),
            'geo': meta.get('geo'),
            'source': meta.get('source'),
            'dataset_uid': uid,
            'license': meta.get('license'),
            'img': meta.get('img'),
        }
        if DEBUG == True:
            extras_titles[uid].update({
                'meta_fileName': meta.get('path') or fichero_json,
                'meta_json': fichero_json,
                'resource_fileNames': res_files,
            })
        
        extras_desc[uid] = {
            "dataset_uid": uid,
            "source": meta.get("source"),
        }
        if DEBUG:
            extras_desc[uid].update({
                "meta_fileName": meta.get('path') or fichero_json,
                "meta_json": fichero_json,
            })

    return ids, titulos, descripciones, extras_titles, extras_desc

'''
Devuelve string con cabeceras normalizadas para el recurso.
Prioriza schema.fields; si no, intenta leer la primera fila del CSV.
'''
def extraer_cabeceras_de_resource(resource):
    # 1) Schema de los metadatos obtenido mediante Friction Less Data
    schema = resource.get('schema') or {}
    fields = schema.get('fields') or []
    names_schema = [str(f.get('name', '')).strip() for f in fields if isinstance(f, dict) and f.get('name')]
    if names_schema:
        return clip_for_model(normalizar_texto(' '.join(names_schema)))

    # 2) Si el Schema no esta bien, se busca directamente en el fichero
    filename = resource.get('path')
    ruta_csv = os.path.join(DATA_PATH, filename)

    enc = resource.get('encoding')
    delim = resource.get('delimiter')

    try:
        with open(ruta_csv, 'r', encoding=enc, errors='ignore') as fh:
            reader = csv.reader(fh, delimiter=delim)
            header = next(reader, None)
            if header:
                header_clean = [str(h).strip() for h in header if h is not None]
                return clip_for_model(normalizar_texto(' '.join(header_clean)))
    except Exception as e:
        if DEBUG == True:
            print(f"[cabeceras] No se pudieron leer cabeceras de {ruta_csv}: {e}")
    return ''

'''
Procesa el CSV del recurso y devuelve:
    - mean_vec: media de embeddings por fila
    - total: nº de filas embebidas
    - payload_text: Texto de las filas recortado a MAX_CONTENT_TEXT_CHARS
    - truncated: True si el payload_text fue recortado
'''
def media_embeddings_contenido_csv(resource, model, batch_size=256, max_rows=None):
    filename = resource.get('path')
    ruta_csv = os.path.join(DATA_PATH, filename)

    enc = resource.get('encoding')
    total = 0
    sum_vec = None

    # Acumulador de texto para payload (solo filas)
    content_parts = []
    content_len = 0
    truncated = False

    def _clip_cell(s):
        return (str(s) if s is not None else '')[:MAX_CELL_CHARS]

    def _append_text(line):
        nonlocal content_len, truncated
        if truncated:
            return
        to_add = line + '\n'
        remaining = MAX_CONTENT_TEXT_CHARS - content_len
        if remaining <= 0:
            truncated = True
            return
        if len(to_add) <= remaining:
            content_parts.append(to_add)
            content_len += len(to_add)
        else:
            content_parts.append(to_add[:remaining])
            content_len += remaining
            truncated = True

    try:
        with open(ruta_csv, 'r', encoding=enc or 'utf-8', errors='ignore') as fh:
            # Detectar dialecto y si hay cabecera
            sample = fh.read(8192)
            fh.seek(0)
            try:
                dialect = csv.Sniffer().sniff(sample)
            except Exception:
                dialect = csv.get_dialect('excel')

            # Si el schema trae fields, asumimos que hay cabecera
            schema = resource.get('schema') or {}
            fields = schema.get('fields') or []
            assume_header = bool(fields)
            try:
                has_header = True if assume_header else csv.Sniffer().has_header(sample)
            except Exception:
                has_header = assume_header

            reader = csv.reader(fh, dialect)

            # Saltar cabecera solo si realmente existe
            if has_header:
                _ = next(reader, None)

            buffer = []
            for row in reader:
                # Texto 'real' para payload
                raw_row = ','.join(_clip_cell(x) for x in row)
                _append_text(raw_row)

                # Texto para embedding (clip por modelo + normalizar)
                raw_for_model = clip_for_model(raw_row)
                line_txt = normalizar_texto(raw_for_model)
                if not line_txt:
                    continue
                buffer.append(line_txt)

                # encode por lotes
                if len(buffer) >= batch_size:
                    embs = model.encode(buffer, batch_size=batch_size, normalize_embeddings=True,
                                        show_progress_bar=False)
                    embs = np.asarray(embs)
                    sum_vec = embs.sum(axis=0) if sum_vec is None else (sum_vec + embs.sum(axis=0))
                    total += embs.shape[0]
                    buffer = []

                    del embs
                    torch.cuda.empty_cache()
                if max_rows and total >= max_rows:
                    buffer = []
                    break

            # flush final
            if buffer:
                embs = model.encode(buffer, batch_size=batch_size, normalize_embeddings=True,
                                    show_progress_bar=False)
                embs = np.asarray(embs)
                sum_vec = embs.sum(axis=0) if sum_vec is None else (sum_vec + embs.sum(axis=0))
                total += embs.shape[0]
                del embs
                torch.cuda.empty_cache()
                
        if total == 0 or sum_vec is None:
            return None, 0, ''.join(content_parts), truncated

        mean_vec = l2_normalize(sum_vec / float(total)).tolist()
        return mean_vec, total, ''.join(content_parts), truncated

    except Exception as e:
        print(f"[contenido] No se pudo procesar {ruta_csv}: {e}")
        return None, 0, '', False

"""
Recorre meta_*.json y, para CADA metadato:
    - calcula embeddings de cabeceras de sus recursos
    - calcula embedding medio de contenidos (si procede)
    - hace upsert inmediato a Qdrant (cabeceras y contenidos)
Así evitamos acumular todo en memoria.
"""
def indexar_recursos(dir_path, model, solo_un_recurso = SOLO_UN_RECURSO):
    archivos = listar_meta_archivos(dir_path)

    for fichero_json in tqdm(archivos, desc='Procesando metadatos/recursos (streaming)'):
        ruta_json = os.path.join(dir_path, fichero_json)
        try:
            with open(ruta_json, 'r', encoding='utf-8') as fh:
                meta = json.load(fh)
        except Exception as e:
            print(f"[!] No se pudo leer '{fichero_json}': {e}")
            continue

        dataset_uid = meta.get('identifier') or fichero_json
        

        # Acumuladores de este metadato
        cabeceras_ids, cabeceras_texts, cabeceras_extras = [], [], []
        contenido_ids, contenido_vectors, contenido_texts, contenido_extras = [], [], [], []
    
        procesados = 0
        resources = meta.get('resources') or []
        if isinstance(resources, dict):
            resources = resources.values()
        for res in resources:
            if solo_un_recurso and procesados >= 1:
                break
            # Solo considerar recursos descargados (con path)
            if res.get('path'):
                res_uid = f"{dataset_uid}:::{res.get('path')}"

                # ----- Cabeceras -----
                cabeceras_text = extraer_cabeceras_de_resource(res) or ''
                cabeceras_text = clip_for_model(cabeceras_text)

                cabeceras_ids.append(res_uid)
                cabeceras_texts.append(cabeceras_text)

                # Generación de extras de las cabeceras, "dataset_uid" imprescindible, el resto ya veremos
                res_name = normalizar_texto(extraer_texto_multilingue(res.get('name')))
                
                buffer_extra = {
                    'dataset_uid': dataset_uid,
                }
                if DEBUG == True:
                    buffer_extra.update({
                        'resource_name': res_name,
                        'resource_path': res.get('path'),
                        'resource_fileName': res.get('fileName'),
                        'resource_mediaType': res.get('mediaType'),
                        'resource_encoding': res.get('encoding'),
                    })
                cabeceras_extras.append((res_uid, buffer_extra))

                # ----- Contenidos -----
                media_contenido, n_filas_index, text_payload, was_truncated = media_embeddings_contenido_csv(res, model, batch_size=LINES_BATCH_SIZE, max_rows=MAX_CONTENT_LINES)
                if media_contenido is not None:
                    procesados = 1
                    contenido_ids.append(res_uid)
                    contenido_vectors.append(media_contenido)
                    contenido_texts.append(text_payload)
                    buffer_extra_contenido = dict(buffer_extra)
                    if DEBUG == True:
                        buffer_extra_contenido['num_rows_used'] = n_filas_index # Poco útil (número de filas usasdas para el embedding)
                        buffer_extra_contenido['payload_truncated'] = was_truncated
                    contenido_extras.append((res_uid, buffer_extra_contenido))

        # ===== Upserts INMEDIATOS para este metadato =====
        if cabeceras_texts:
            cabeceras_vectors = model.encode(cabeceras_texts, batch_size=BATCH_SIZE, normalize_embeddings=True).tolist()
            qdrant_upsert(COL_CAB, cabeceras_ids, cabeceras_vectors, cabeceras_texts, extras={k: v for k, v in cabeceras_extras})

        if contenido_vectors:
            qdrant_upsert(COL_CON, contenido_ids, contenido_vectors, contenido_texts, extras={k: v for k, v in contenido_extras})

        # Limpieza explícita de acumuladores
        del cabeceras_ids, cabeceras_texts, cabeceras_extras, contenido_ids, contenido_vectors, contenido_texts, contenido_extras


def guardar_estadisticas_modelo(tiempo):
    
    url = "https://10.58.167.110:6333/metrics"
    headers = {
        "Authorization": f"Bearer {API_KEY}"
    }
    response = requests.get(url, headers=headers, verify=CACERT_PATH)
    text = response.text
    PATTERN = re.compile(r"^memory_resident_bytes\s+(\d+(?:\.\d+)?)$", re.M)
    match = PATTERN.search(text)

    memory_resident_bytes = float(match.group(1))
    espacio_ram = str(round(memory_resident_bytes / (1024**3), 4)) + 'GiB'

    estadisticas = {
        'tiempo_indexacion': tiempo,
        'espacio_ram': espacio_ram, 
        'espacio_disco': get_cluster_gib()
    }
    
    path = 'miscelanea_modelos/' + MODEL_NAME.split("/")[-1] + '.json'
    with open(path, 'w', encoding = 'utf-8') as file:
        json.dump(estadisticas, file, indent=2, ensure_ascii = False)


if __name__ == '__main__':
    torch.cuda.empty_cache()
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model = SentenceTransformer(MODEL_NAME, trust_remote_code=True, device=device)
    if MODEL_TO_16:
        model.to(torch.float16) # Reducci'on de modelo OPCIONAL
    
    MODEL_MAX_TOKENS = get_model_max_tokens(model)
    if DEBUG == True:
        print(f'Max tokens del modelo: {MODEL_MAX_TOKENS}')
        print('Cargando metadatos…')
    ids, titulos, descripciones, extras_titles, extras_desc  = cargar_metadatos(DATA_PATH)
    
    if DEBUG == True:
        print(f'Total metadatos: {len(ids)} (limitado por MAX_META_FILES={MAX_META_FILES})')

    if not ids:
        if DEBUG == True:
            print('No hay nada que indexar (títulos/descr vacíos o no se encontraron meta_*.json).')
        raise SystemExit(0)

    # Indexación de títulos y descripciones
    if DEBUG == True:
        print('Generando embeddings e insertando en Qdrant (títulos/descr)')
    t0 = time.time()

    titulos_clip = [clip_for_model(t) for t in titulos]
    descripciones_clip = [clip_for_model(d) for d in descripciones]

    # Indexación por batches
    for i in range(0, len(ids), BATCH_SIZE):
        batch_ids  = ids[i:i+BATCH_SIZE]
        batch_tit  = titulos_clip[i:i+BATCH_SIZE]
        batch_desc = descripciones_clip[i:i+BATCH_SIZE]

        emb_tit  = model.encode(batch_tit,  batch_size=BATCH_SIZE, normalize_embeddings=True).tolist()
        emb_desc = model.encode(batch_desc, batch_size=BATCH_SIZE, normalize_embeddings=True).tolist()

        # TITULOS -> extras completos (con theme/temporal/geo/...)
        qdrant_upsert(COL_TIT,  batch_ids, emb_tit,  batch_tit,  extras=extras_titles)
        # DESCRIPCIONES -> extras ligeros (sin theme/temporal/geo/license/img)
        qdrant_upsert(COL_DESC, batch_ids, emb_desc, batch_desc, extras=extras_desc)

    # Recursos: cabeceras + contenidos
    if DEBUG == True:
        print('Generando embeddings e insertando en Qdrant (cabeceras/contenidos por recurso)…')
    
    indexar_recursos(DATA_PATH, model)
    t1 = time.time() - t0
    print(f'Listo. Tiempo total: {t1}s')

    guardar_estadisticas_modelo(t1)