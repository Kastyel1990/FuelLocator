import requests
from bs4 import BeautifulSoup
import json
import uuid
import time
from datetime import datetime

# Fuel types mapping
FUEL_TYPES = {
    'ai_92': 'AI_92',
    'ai_95': 'AI_95',
    'np_95': 'AI_95_PLUS',
    'ai_100': 'AI_100',
    'dt': 'DT',
    'np_dt': 'DT_PLUS',
    'lpg': 'PROPANE'
}

def parse_tes():
    url = "https://td-tes.com/fuel/status"
    headers = {'User-Agent': 'Mozilla/5.0'}
    print("Fetching TES...")
    resp = requests.get(url, headers=headers)
    if resp.status_code != 200:
        print("Failed to fetch TES")
        return []
        
    soup = BeautifulSoup(resp.text, 'html.parser')
    table = soup.find('table', {'id': 'azs-table'})
    if not table:
        print("TES Table not found")
        return []
        
    stations = []
    tbody = table.find('tbody')
    for tr in tbody.find_all('tr'):
        station_id = tr.get('data-azs-id', '')
        region = tr.get('data-region', '')
        
        tds = tr.find_all('td')
        if len(tds) < 3: continue
        
        num = tds[1].text.strip()
        address = tds[2].text.strip()
        
        fuel_statuses = []
        for slug, enum_name in FUEL_TYPES.items():
            td = tr.find('td', {'data-fuel-slug': slug})
            if td:
                orig_val = td.get('data-original-value', '').lower()
                availability = 'UNKNOWN'
                if orig_val == 'в свободной продаже':
                    availability = 'FREE_SALE'
                elif orig_val == 'талоны и топливные карты':
                    availability = 'CARDS_ONLY'
                elif orig_val == '-' or orig_val == 'n/a':
                    availability = 'NOT_AVAILABLE'
                
                fuel_statuses.append({
                    'fuelType': enum_name,
                    'availability': availability
                })
        
        stations.append({
            'id': f"tes_{station_id}",
            'number': num,
            'network': 'ТЭС',
            'address': address,
            'region': region,
            'latitude': 0.0, # Will update later if geocoding needed, or leave 0 for now
            'longitude': 0.0,
            'fuelStatuses': fuel_statuses,
            'paymentMethods': ['CARDS', 'CASHLESS', 'CASH'], # Default
            'isVerified': True,
            'lastUpdated': datetime.utcnow().isoformat() + 'Z'
        })
        
    print(f"Parsed {len(stations)} TES stations.")
    return stations

def fetch_osm():
    print("Fetching OSM...")
    import urllib.parse
    query = """[out:json][timeout:90];
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
out center;"""
    url = "https://lz4.overpass-api.de/api/interpreter?data=" + urllib.parse.quote(query)
    headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'}
    resp = requests.get(url, headers=headers)
    if resp.status_code != 200:
        print("Failed OSM, trying alternative endpoint...")
        url2 = "https://overpass.kumi.systems/api/interpreter?data=" + urllib.parse.quote(query)
        resp = requests.get(url2, headers=headers)
        if resp.status_code != 200:
            print(f"Failed OSM completely. Code: {resp.status_code}")
            return []
    
    data = resp.json()
    elements = data.get('elements', [])
    stations = []
    
    for el in elements:
        tags = el.get('tags', {})
        name = tags.get('name', tags.get('brand', 'Неизвестная АЗС'))
        brand = tags.get('brand', '')
        
        if el['type'] == 'node':
            lat = el.get('lat')
            lon = el.get('lon')
        else:
            center = el.get('center', {})
            lat = center.get('lat')
            lon = center.get('lon')
            
        address = tags.get('addr:street', '')
        if tags.get('addr:housenumber'):
            address += f", {tags.get('addr:housenumber')}"
        if tags.get('addr:city'):
            address += f" ({tags.get('addr:city')})"
            
        # Default all fuel types to UNKNOWN
        fuel_statuses = []
        for enum_name in FUEL_TYPES.values():
            fuel_statuses.append({
                'fuelType': enum_name,
                'availability': 'UNKNOWN'
            })
            
        stations.append({
            'id': f"osm_{el['id']}",
            'number': '',
            'network': brand if brand else name,
            'address': address.strip(', '),
            'region': 'UNKNOWN',
            'latitude': lat,
            'longitude': lon,
            'fuelStatuses': fuel_statuses,
            'paymentMethods': ['CARDS', 'CASHLESS', 'CASH'],
            'isVerified': False,
            'lastUpdated': datetime.utcnow().isoformat() + 'Z'
        })
        
    print(f"Parsed {len(stations)} OSM stations.")
    return stations

if __name__ == "__main__":
    tes_stations = parse_tes()
    osm_stations = fetch_osm()
    
    all_stations = tes_stations + osm_stations
    
    with open('app/src/main/assets/seed_stations.json', 'w', encoding='utf-8') as f:
        json.dump(all_stations, f, ensure_ascii=False, indent=2)
        
    print("Saved to app/src/main/assets/seed_stations.json")
