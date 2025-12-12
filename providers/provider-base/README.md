# Base Provider
Provider inheriting from the main provider for dev purposes


## Publish datasets
To publish all datasets in a folder use the following shell script:
````shell
for f in *.json ; do  
  ls $f
  curl --location 'http://localhost:19193/management/v3/assets-cbm' \
--header 'Content-Type: application/json' \
--header 'x-api-key: *****' \
--data "$(cat $f)"
done

````