#!/usr/bin/env bash
set -e

arr_variable=("idearium" "climate" "ebird" "mastral" "provider-base")

## now loop through above array
for i in "${arr_variable[@]}"
do
   echo "Create DB: ${POSTGRES_DB}_$i"

psql -v ON_ERROR_STOP=0 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
   CREATE DATABASE "${POSTGRES_DB}_$i";
   GRANT ALL PRIVILEGES ON DATABASE "${POSTGRES_DB}_$i" TO "$POSTGRES_USER";
EOSQL

done