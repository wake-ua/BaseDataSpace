import re 

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
        texto = re.sub(r'\s{1,10}([.,:;!?\)\]])', r'\1', texto)
        texto = re.sub(r'(?<=\d)(°)(?=\w)', r'\1 ', texto) 
        texto = re.sub(r'(?<!\d)([.,:;!?)])(?=[^\s.,:;!?)])', r'\1 ', texto) 
        texto = re.sub(r'(\w)([¿¡#(])', r'\1 \2', texto)
        texto = re.sub(r'([¿¡#(])\s+', r'\1', texto)

    if normalizar_espacios:
        texto = re.sub(r'\s+', ' ', str(texto))
        texto = texto.strip()

    return texto

# Conversión de días y meses a ingles para poder parsearlo
def traducir_fecha_es(fecha_str):
    if not fecha_str:
        return None
    meses = {
        "ene": "Jan", "feb": "Feb", "mar": "Mar", "abr": "Apr",
        "may": "May", "jun": "Jun", "jul": "Jul", "ago": "Aug",
        "sep": "Sep", "oct": "Oct", "nov": "Nov", "dic": "Dec"
    }
    dias = {
        "lun": "Mon", "mar": "Tue", "mié": "Wed", "jue": "Thu",
        "vie": "Fri", "sáb": "Sat", "dom": "Sun"
    }

    for esp, eng in {**meses, **dias}.items():
        fecha_str = fecha_str.replace(esp, eng)

    return fecha_str