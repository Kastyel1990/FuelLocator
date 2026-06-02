import requests

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

response = requests.post("https://overpass-api.de/api/interpreter", data=query)
print(response.status_code)
if response.status_code == 200:
    print(f"Found {len(response.json().get('elements', []))} stations.")
else:
    print(response.text)
