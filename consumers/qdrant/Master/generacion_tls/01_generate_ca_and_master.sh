#!/bin/sh
# Uso: sh 01_generate_ca_and_master.sh
IP_MAESTRO="10.58.167.110"
TLS_DIR="qdrant/tls"
mkdir -p "$TLS_DIR"
# Crear clave privada de la CA
openssl genrsa -out "$TLS_DIR/ca-key.pem" 4096
# Crear certificado autofirmado de la CA
openssl req -x509 -new -nodes -key "$TLS_DIR/ca-key.pem" \
  -sha256 -days 3650 -out "$TLS_DIR/cacert.pem" \
  -subj "/CN=QdrantRootCA"
# Crear clave privada del maestro
openssl genrsa -out "$TLS_DIR/key.pem" 4096
# Crear archivo de configuración
cat > "$TLS_DIR/openssl.cnf" <<EOF
[ req ]
default_bits       = 4096
prompt             = no
default_md         = sha256
req_extensions     = req_ext
distinguished_name = dn
[ dn ]
CN = $IP_MAESTRO
[ req_ext ]
subjectAltName = @alt_names
[ alt_names ]
IP.1 = $IP_MAESTRO
EOF
# Crear y firmar el CSR del maestro
openssl req -new -key "$TLS_DIR/key.pem" -out "$TLS_DIR/cert.csr" -config "$TLS_DIR/openssl.cnf"
openssl x509 -req -in "$TLS_DIR/cert.csr" -CA "$TLS_DIR/cacert.pem" -CAkey "$TLS_DIR/ca-key.pem" \
  -CAcreateserial -out "$TLS_DIR/cert.pem" -days 365 -sha256 \
  -extfile "$TLS_DIR/openssl.cnf" -extensions req_ext
# Limpiar fichero temporal
rm -f "$TLS_DIR/cert.csr" "$TLS_DIR/cacert.srl"
echo "✅ CA y certificado del maestro creados en $TLS_DIR"