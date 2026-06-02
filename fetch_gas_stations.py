# -*- coding: utf-8 -*-
import urllib.request
import urllib.parse
import json
import csv
import sys

def fetch_gas_stations():
    area_name = "Крым"
    print("Fetching gas stations from OpenStreetMap (Overpass API)...")
    
    # Overpass QL query
    query = """
    [out:json][timeout:90];
    (
      area["name"="Республика Крым"]->.a1;
      area["name"="Севастополь"]->.a2;
    );
    (
      node["amenity"="fuel"](area.a1);
      way["amenity"="fuel"](area.a1);
      relation["amenity"="fuel"](area.a1);
      node["amenity"="fuel"](area.a2);
      way["amenity"="fuel"](area.a2);
      relation["amenity"="fuel"](area.a2);
    );
    out center;
    """
    
    url = "https://overpass-api.de/api/interpreter?data=" + urllib.parse.quote(query)
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    
    try:
        with urllib.request.urlopen(req) as response:
            result = json.loads(response.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        print(f"HTTP Error {e.code}: {e.reason}")
        error_body = e.read().decode('utf-8', errors='ignore')
        print(f"Error body: {error_body}")
        sys.exit(1)
    except Exception as e:
        print(f"Error fetching data: {e}")
        sys.exit(1)
        
    elements = result.get('elements', [])
    print(f"Found {len(elements)} gas stations.")
    
    output_file = 'gas_stations.csv'
    with open(output_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['Name', 'Brand', 'Latitude', 'Longitude', 'Street', 'House Number', 'City'])
        
        for el in elements:
            tags = el.get('tags', {})
            name = tags.get('name', 'Unknown')
            brand = tags.get('brand', '')
            
            if el['type'] == 'node':
                lat = el.get('lat')
                lon = el.get('lon')
            else:
                center = el.get('center', {})
                lat = center.get('lat')
                lon = center.get('lon')
                
            street = tags.get('addr:street', '')
            housenumber = tags.get('addr:housenumber', '')
            city = tags.get('addr:city', '')
            
            writer.writerow([name, brand, lat, lon, street, housenumber, city])
            
    print(f"Data saved to {output_file}")

if __name__ == "__main__":
    fetch_gas_stations()
