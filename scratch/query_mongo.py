from pymongo import MongoClient

uri = "mongodb://dig_vault_seed:09e4kZph5el4qPUBNV7FFgaT@203.174.22.129:27017/?authSource=admin&directConnection=true"
client = MongoClient(uri, serverSelectionTimeoutMS=5000)

for db_name in ["market_data", "market_data_dev"]:
    db = client[db_name]
    coll = db["stock_indices_market_data"]
    cursor = coll.find({"indexSymbol": {"$in": ["GLOBAL_INDEX|HSI", "GLOBAL_INDEX|SSEC", "GLOBAL_INDEX|DJI", "HSI", "SSEC"]}})
    for doc in cursor:
        meta = doc.get("metadata", {})
        audit = doc.get("audit", {})
        print(f"[{db_name}] Symbol: {doc.get('indexSymbol')}, Last: {meta.get('last')}, Open: {meta.get('open')}, High: {meta.get('high')}, Low: {meta.get('low')}, UpdatedAt: {audit.get('updatedAt')}")
