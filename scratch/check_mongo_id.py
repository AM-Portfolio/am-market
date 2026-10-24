from pymongo import MongoClient

uri = "mongodb://dig_vault_seed:09e4kZph5el4qPUBNV7FFgaT@203.174.22.129:27017/?authSource=admin&directConnection=true"
client = MongoClient(uri, serverSelectionTimeoutMS=5000)

for db_name in ["market_data", "market_data_dev"]:
    db = client[db_name]
    coll = db["stock_indices_market_data"]
    doc = coll.find_one({"_id": "GLOBAL_INDEX|HSI"})
    if doc:
        print(f"[{db_name}] FOUND GLOBAL_INDEX|HSI:")
        print(doc)
    doc_raw = coll.find_one({"_id": "HSI"})
    if doc_raw:
        print(f"[{db_name}] FOUND HSI:")
        print(doc_raw)
