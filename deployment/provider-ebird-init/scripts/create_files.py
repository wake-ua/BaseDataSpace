import os
import json
import csv
from io import StringIO


def save_parsed_data(file_name, data_type, data):
    directory = os.path.join(os.path.dirname(__file__), 'samples', 'data')
    os.makedirs(directory, exist_ok=True)
    if data_type == 'json':
        file_path = os.path.join(directory, f'{file_name}.json')
        with open(file_path, 'w', encoding='utf-8') as f:
            # If you want pretty output, uncomment the next 2 lines:
            json.dump(data, f, indent=2, ensure_ascii=False)
            

    elif data_type == 'csv':
        if not data: 
            return
        
        fieldsnames = data[0].keys()
    
        file_path = os.path.join(directory, f'{file_name}.csv')
        with open(file_path, 'w', newline='', encoding='utf-8') as f_out:
            writer = csv.DictWriter(f_out, fieldnames=fieldsnames)
            writer.writeheader()
            writer.writerows(data)
            print(f"Saved {len(data)} rows to {file_path}")

    else:
        raise ValueError("Invalid data_type. Must be 'csv' or 'json'.")

   
