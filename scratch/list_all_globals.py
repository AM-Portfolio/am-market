from pymongo import MongoClient

uri = "mongodb://dig_vault_seed:09e4kZph5el4qPUBNV7FFgaT@203.174.22.129:27017/?authSource=admin&directConnection=true"
client = MongoClient(uri, serverSelectionTimeoutMS=5000)

db = client["market_data"]
coll = db["stock_indices_market_data"]

for doc in coll.find({"_id": {"$regex": "^GLOBAL_INDEX"}}):
    meta = doc.get("metadata", {})
    audit = doc.get("audit", {})
    print(f"ID: {doc['_id']}, Name: {meta.get('indexName')}, Last: {meta.get('last')}, Open: {meta.get('open')}, High: {meta.get('high')}, Low: {meta.get('low')}, PrevClose: {meta.get('previousClose')}, PctChange: {meta.get('percChange')}, Created: {audit.get('createdAt')}")
