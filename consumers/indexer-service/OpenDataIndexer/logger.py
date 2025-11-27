import csv
import os
import threading
from datetime import datetime
import logging
from logging.handlers import RotatingFileHandler
# ============================================================================================================================== #
# Logger para consultas y métricas
class LoggerOpenSearch:
    def __init__(self, logfile="query_log.csv"):
        self.logfile = logfile
        self.lock = threading.Lock()
        # Crea el archivo y cabecera si no existe
        if not os.path.exists(self.logfile):
            with open(self.logfile, mode="w", newline="") as f:
                writer = csv.writer(f)
                writer.writerow(["query", "response_time", "date"])

    def save_log(self, query, response_time, client_ip):
        try:
            with self.lock:
                with open(self.logfile, mode="a", newline="") as f:
                    writer = csv.writer(f)
                    writer.writerow([client_ip, query, response_time, datetime.now().strftime("%Y-%m-%d %H:%M:%S")])
        except Exception as e:
            print(f"[LOGGER] Error escribiendo en log de consultas: {e}")

# Logger para errores del backend con tamaño limitado
class LoggerBuscador:
    def __init__(self, logfile="buscador_server.log"):
        self.logger = logging.getLogger(f"buscador_{logfile}")
        self.logger.setLevel(logging.ERROR)
        formatter = logging.Formatter("%(asctime)s [%(levelname)s] %(message)s")
        file_handler = RotatingFileHandler(logfile, maxBytes=5*1024*1024, backupCount=3)
        file_handler.setFormatter(formatter)
        if not self.logger.handlers:
            self.logger.addHandler(file_handler)
            self.logger.addHandler(logging.StreamHandler())

    def error(self, msg, *args, **kwargs):
        self.logger.error(msg, *args, **kwargs)
    def exception(self, msg, *args, **kwargs):
        self.logger.exception(msg, *args, **kwargs)