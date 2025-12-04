# Script empleado principalmente para la extracción de keywords de una intent.
# Tiene extras como la generación de sugerencias para análisis de un dataset,
# la generación de mensajes de información adicional y cuando no hay resultados.
import os
from dotenv import load_dotenv
# Servicios externos
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import JsonOutputParser
#from langchain.prompts import PromptTemplate
from langchain_core.prompts import PromptTemplate
# ============================================================================================================================== #
class GenerativeEngine:
    def __init__(self, model_name, logger):
        print('Created Generative Engine')
        load_dotenv()
        os.getenv("OPENAI_API_KEY")

        self.model_name = model_name
        self.llm = ChatOpenAI(temperature=0, model_name=model_name)
        self.logger = logger

    # Dada una intención pasa una PromptTemplate a un LLM para extraer keywords
    def get_keywords(self, intent, adicional=False):
        if adicional:
            intent_template = """
            Actúa como un experto en analítica de datos.
            Dime hasta 6 consultas que harías a portales de datos que ayuden a resolver la tarea.
            Cada consulta tiene entre 3 y 5 palabras. Cada consulta debe ser única y no deben parecerse entre ellas, en este sentido, los resultados de cada consulta no deberían parecerse.
            
            Además para cada consulta aporta:
            Una Descripción por cada tarea que diga porqué la información que proporciona esa consulta es relevante para resolver la tarea. Tiene que tener entre 10 palabras y 30 palabras.
            Un titulo tiene 3 y 5 palabras.
            
            Además genera una intro generar una intro que indique al usuario porqué esas consultas son útiles para su búsqueda.
            Para la intro no uses más de 60 palabras y qué sea útil y directa. No hagas listas.
            
            Sé realista con las consultas, debe ser información que habitualmente esté disponible y en abierto.
        
            Adicionalmente, como extra, dime portales o APIs dónde se pueda encontrar datasets para la consulta que te voy a pasar.
            Debe estar centrado en España a menos que en la consulta se indique otro lugar.
            Para cada fuente de datos, proporciona un enlace válido para acceder, siendo clikcable el título.
            Por ejemplo si te consultan horarios de trenes. Podrías responder con la siguiente fuente https://data.renfe.com/
            Usa html para maquetar el resultado de lo extra.
            Ejemplo:
            <p><a class='font-semibold underline' href="..." target="_blank">Título de la fuente</a>: Descripción de la fuente...(máximo 20 palabras)<p><br>
            
            La respuesta la debes devolver en formato json. El json debe seguir la siguiente estructura {response_format}.
            La tarea es la siguiente: {task}.
            """
            response_format = """{intro:"...", "extra":"...", "keywords":[
                {"desc":"...", "titulo":"...", "consulta":"..."},
                {"desc":"...", "titulo":"...", "consulta":"..."},
                {"desc":"...", "titulo":"...", "consulta":"..."}]}"""
        else:
            intent_template = """
            Actúa como un experto en analítica de datos.
            Dime hasta 6 consultas que harías a portales de datos que ayuden a resolver la tarea.
            Cada consulta tiene entre 3 y 5 palabras. Cada consulta debe ser única y no deben parecerse entre ellas, en este sentido, los resultados de cada consulta no deberían parecerse.
            
            Además para cada consulta aporta:
            Una Descripción por cada tarea que diga porqué la información que proporciona esa consulta es relevante para resolver la tarea. Tiene que tener entre 10 palabras y 30 palabras.
            Un titulo tiene 3 y 5 palabras.
            
            Además genera una intro que indique al usuario porqué esas consultas son útiles para su búsqueda.
            Para la intro no uses más de 60 palabras y qué sea útil y directa. No hagas listas.
            
            Sé realista con las consultas, debe ser información que habitualmente esté disponible y en abierto.
            
            La respuesta la debes devolver en formato json. El json debe seguir la siguiente estructura {response_format}.
            La tarea es la siguiente: {task}.
            """
            response_format = """{intro:"...", "keywords":[
                {"desc":"...", "titulo":"...", "consulta":"..."},
                {"desc":"...", "titulo":"...", "consulta":"..."},
                {"desc":"...", "titulo":"...", "consulta":"..."}]}"""

        parser = JsonOutputParser()
        # Construcción de la plantilla del prompt y llamada al modelo
        prompt_template = PromptTemplate(
            input_variables=["task"],
            template=intent_template,
            partial_variables={"format_instructions": parser.get_format_instructions()}
        )

        chain = prompt_template | self.llm | parser
        try:
            res = chain.invoke({"task": intent, "parsed": parser, "format": response_format})
            return res
        except Exception as e:
            self.logger.exception(f"[get_keywords] Error: {e}")
            print(f"[get_keywords] Error: {e}")
            return []

    # Dado el ID de un dataset usa un LLM para generar sugerencias de análisis que se podría realizar con dicho dataset
    def get_suggestions(self, dataset_uid, db):
        try:
            # Recuerar el dataset
            meta = db.get_item(dataset_uid)
            if not meta["hits"]:
                return {"response": "No encontrado"}
            meta_src = meta["hits"][0]["_source"]
            title = meta_src.get("title", "")
            description = meta_src.get("description", "")

            # Recuperar todos los recursos asociados en contenidos y cabeceras
            recursos = db.get_all_resources_by_dataset_uid("contenidos", dataset_uid)
            cabeceras = db.get_all_resources_by_dataset_uid("cabeceras", dataset_uid)
            
            # Mapear fnames a sus cabeceras
            headers_map = {}
            for cab in cabeceras:
                fname = cab.get("resource_fileName") or cab.get("resource_path")
                cab_text = cab.get("text", "")
                if cab_text:
                    # Primera línea del 'text' es la cabecera
                    primera_linea = cab_text.split("\n")[0]
                    headers_map[fname] = primera_linea

            if not recursos:
                return {"response": "No hay recursos CSV asociados a este dataset"}

            analyses = []
            # Recorrer todos los recursos
            for recurso in recursos:
                csv_text = recurso.get("text", "")
                resource_name = recurso.get("resource_fileName") or recurso.get("resource_name")
                # Buscar la cabecera correspondiente a este recurso
                header_line = headers_map.get(resource_name, "")

                if not csv_text:
                    continue
                # Usa las primeras 10 filas como muestra
                sample_rows = csv_text.split("\n")[0:10]
                sample_str = "\n".join(sample_rows)

                parser = JsonOutputParser()
                response_format = """[
                    {{"titulo":"...", "desc":"..."}},
                    {{"titulo":"...", "desc":"..."}},
                    {{"titulo":"...", "desc":"..."}},
                    {{"titulo":"...", "desc":"..."}}
                ]"""
                template = f"""
                    Di 4 análisis que se pueda hacer a un dataset cuyo título es "{title}" y descripción "{description}".
                    El recurso concreto es "{resource_name}" y su cabecera es: "{header_line}".
                    El contenido es una muestra de filas (sin cabecera): {{sample}}.
                    Devuelve un json con las 4 ideas. Devuelve únicamente el json.
                    El json debe seguir la siguiente estructura: {response_format}
                """
                # Construcción de la plantilla del prompt y llamada al modelo
                prompt = PromptTemplate(
                    template=template,
                    input_variables=["sample"],
                    partial_variables={"format_instructions": parser.get_format_instructions()},
                )
                chain = prompt | self.llm | parser
                res = chain.invoke({"sample": sample_str, "format": response_format})

                analyses.append({
                    "resource_name": resource_name,
                    "analysis": res
                })

            return {
                "dataset_uid": dataset_uid,
                "title": title,
                "description": description,
                "analyses": analyses
            }
        except Exception as e:
            print(f"[full_dataset_analysis] Error: {e}")
            self.logger.exception(f"[full_dataset_analysis] Error: {e}")
            return {"response": "Error generando el análisis"}

    # Genera una respuesta con portales de datos relacionados, con intro variable según si proviene de una 
    def get_data_sources_response(self, query):
        prompt_instructions = """
        Dime portales, APIs o fuentes abiertas donde se pueda encontrar datasets para la consulta que te paso.
        Céntrate en España salvo que la consulta pida otro territorio.
        Para cada fuente de datos, proporciona un enlace válido y que el título sea clickable (usa HTML).
        Ejemplo de formato:
        <p><a class='font-semibold underline' href="..." target="_blank">Título de la fuente</a>: Descripción de la fuente...(máximo 20 palabras)</p><br>
        Consulta: {query}
        """

        prompt_template = PromptTemplate(
            input_variables=["query"],
            template=prompt_instructions
        )
        chain = prompt_template | self.llm
        res = chain.invoke({"query": query})
        return res.content

    # Genera un título representativo para un grupo de títulos mediante LLM usando LangChain.
    def generate_group_title(self, titles):
        prompt_template_str = """
        Dado el siguiente conjunto de títulos de datasets, genera un título breve y representativo que resuma su tema común.
        El título debe ser original, no repetir literalmente ninguno de los títulos dados, y debe captar la temática general.

        Títulos:
        {titulos}

        Título representativo:
        """

        prompt = PromptTemplate(
            input_variables=["titulos"],
            template=prompt_template_str
        )

        try:
            joined_titles = "\n".join(f"- {t}" for t in titles)
            chain = prompt | self.llm
            res = chain.invoke({"titulos": joined_titles})
            return res.content.strip()
        except Exception as e:
            print(f"[LLM TITLE] Error generando título representativo: {e}")
            self.logger.exception(f"[LLM TITLE] Error generando título representativo: {e}")
            return titles[0] if titles else "Grupo sin título"