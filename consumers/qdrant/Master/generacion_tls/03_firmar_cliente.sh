#!/bin/sh
# Uso: sh 03_firmar_cliente.sh csr_10_58_167_120.csr 10.58.167.120

CSR_FILENAME="$1"
IP_ESCLAVO="$2"

if [ -z "$CSR_FILENAME" ] || [ -z "$IP_ESCLAVO" ]; then
  echo "Uso: $0 <nombre_fichero_csr> <ip_esclavo>"
  exit 1
fi

BASE_DIR="$(dirname "$0")/.."
TLS_DIR="$BASE_DIR/tls"
TMP_DIR="$BASE_DIR/tmp"
CSR_PATH="$TMP_DIR/$CSR_FILENAME"
IP_FILENAME=$(echo "$IP_ESCLAVO" | tr '.' '_')
CONFIG_TMP="$TMP_DIR/openssl_${IP_FILENAME}.cnf"
CERT_TMP="$TMP_DIR/cert_${IP_FILENAME}.pem"
ENVIO_DIR="$TMP_DIR/envio_${IP_FILENAME}"

# Validar existencia del CSR
if [ ! -f "$CSR_PATH" ]; then
  echo "❌ No se encontró $CSR_PATH"
  exit 1
fi

# Crear config personalizada
cp "$TLS_DIR/openssl.cnf" "$CONFIG_TMP"
sed -i "s/^CN = .*/CN = $IP_ESCLAVO/" "$CONFIG_TMP"
sed -i "s/^IP.1 = .*/IP.1 = $IP_ESCLAVO/" "$CONFIG_TMP"
grep -q "IP.2 = 10.58.167.110" "$CONFIG_TMP" || echo "IP.2 = 10.58.167.110" >> "$CONFIG_TMP"

# Firmar el CSR
openssl x509 -req -in "$CSR_PATH" -CA "$TLS_DIR/cacert.pem" -CAkey "$TLS_DIR/ca-key.pem" \
  -CAcreateserial -out "$CERT_TMP" -days 365 -sha256 \
  -extfile "$CONFIG_TMP" -extensions req_ext

# Preparar carpeta de salida
mkdir -p "$ENVIO_DIR"
cp "$CERT_TMP" "$ENVIO_DIR/cert.pem"
cp "$TLS_DIR/cacert.pem" "$ENVIO_DIR/cacert.pem"

# Limpiar temporales
rm -f "$CERT_TMP" "$CONFIG_TMP" "$TLS_DIR/cacert.srl"

# Resultado
echo "✅ Certificado firmado correctamente"
echo ""
echo "📤 Debes copiar los siguientes ficheros al esclavo:"
echo "  $ENVIO_DIR/cert.pem   → qdrant/tls/cert.pem"
echo "  $ENVIO_DIR/cacert.pem → qdrant/tls/cacert.pem"
