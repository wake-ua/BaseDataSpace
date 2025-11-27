#!/bin/sh
set -e

for i in ebird mastral provider-base
do
    echo "Creating database: ${POSTGRES_DB}_${i}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<EOSQL
CREATE DATABASE "${POSTGRES_DB}_${i}";
GRANT ALL PRIVILEGES ON DATABASE "${POSTGRES_DB}_${i}" TO "$POSTGRES_USER";
EOSQL

done
