#!/usr/bin/env bash
set -euo pipefail

echo "=== generate-keys.sh starting ==="


echo "Initial working directory: $(pwd)"

KEY_DIR="$1"
JSON_FILE="${2:-}"

echo "Arguments:"
echo "  KEY_DIR   = $KEY_DIR"
echo "  JSON_FILE = $JSON_FILE"


KEY_DIR_ABS="$(mkdir -p "$KEY_DIR" && cd "$KEY_DIR" && pwd)"
JSON_FILE_ABS="$(cd "$(dirname "$JSON_FILE")" && pwd)/$(basename "$JSON_FILE")"

echo "Resolved paths:"
echo "  KEY_DIR_ABS   = $KEY_DIR_ABS"
echo "  JSON_FILE_ABS = $JSON_FILE_ABS"


PRIVATE_KEY="$KEY_DIR_ABS/ed25519_private.pem"
PUBLIC_KEY="$KEY_DIR_ABS/ed25519_public.pem"

# Generate keys if missing
if [[ -f "$PRIVATE_KEY" && -f "$PUBLIC_KEY" ]]; then
    echo "Key pair already exists, skipping generation."
else
    echo "Generating Ed25519 key pair..."
    cd "$KEY_DIR_ABS"
    openssl genpkey -algorithm Ed25519 -out ed25519_private.pem
    openssl pkey -in ed25519_private.pem -pubout -out ed25519_public.pem
    echo "Key pair generated successfully."
fi


[[ -z "$JSON_FILE" ]] && echo "No JSON file provided, stopping script." && exit 0

# Extract PEM (base64 only)
echo "Extracting PEM from public key..."
PEM=$(grep -v "BEGIN\|END" "$PUBLIC_KEY" | tr -d '\n')

if [[ -z "$PEM" ]]; then
    echo "ERROR: Failed to extract PEM from public key"
    exit 1
fi

echo "Extracted PEM:"
echo "  $PEM"

# Ensure JSON exists
if [[ ! -f "$JSON_FILE_ABS" ]]; then
    echo "ERROR: JSON file not found: $JSON_FILE_ABS"
    exit 1
fi



DIR="$(dirname "$JSON_FILE_ABS")"
BASE="$(basename "$JSON_FILE_ABS")"
NAME="${BASE%.json}"

COPY_FILE="$DIR/${NAME}-local.json"

if [ ! -f "$COPY_FILE" ]; then
  cp "$JSON_FILE_ABS" "$COPY_FILE"
  echo "Original json file copied correctly."
fi



# Ensure jq exists
if ! command -v jq >/dev/null 2>&1; then
    echo "ERROR: jq is required but not installed"
    exit 1
fi

# Log previous value
OLD_PEM=$(jq -r '.security.pem // empty' "$COPY_FILE")

echo "Previous security.pem:"
if [[ -z "$OLD_PEM" ]]; then
    echo "  <empty>"
else
    echo "  $OLD_PEM"
fi


echo "Updating security.pem in JSON..."
jq --arg pem "$PEM" '
  .security.pem = $pem
' "$COPY_FILE" > "$COPY_FILE.tmp"

mv "$COPY_FILE.tmp" "$COPY_FILE"


NEW_PEM=$(jq -r '.security.pem' "$COPY_FILE")

echo "Updated security.pem:"
echo "  $NEW_PEM"

echo "=== generate-keys.sh completed successfully ==="
rm -f "$COPY_FILE.bak"