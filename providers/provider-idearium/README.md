# Idearium Provider

## Installation

This provider requires docker and python.

## Execution
The execution assumes there is a common postgresql BD container running
First start the containers
```shell
docker compose up
```

Then start the python virtual environment, install the dependencies and run the script to load the datasets:
```shell
python3 -m venv ./venv 
source venv/bin/activate 
pip install -r scripts/requirements.txt 
python3 scripts/main.py 
```
