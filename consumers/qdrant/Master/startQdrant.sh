QDRANT__CLUSTER__ENABLED=true
docker run -d -p 6333:6333 \
  -p 6334:6334 \
  -p 6335:6335 \
  --name "qdrant" \
  -v $(pwd)/qstorage:/qdrant/storage \
  -v $(pwd)/custom_config.yaml:/qdrant/production.yaml \
  -v $(pwd)/tls:/qdrant/tls \
  qdrant/qdrant \
qdrant/qdrant ./qdrant --config-path /qdrant/production.yaml --uri 'http://10.58.167.110:6335'
