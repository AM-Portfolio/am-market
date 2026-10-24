from pymongo import MongoClient

uri = "mongodb://dig_vault_seed:09e4kZph5el4qPUBNV7FFgaT@203.174.22.129:27017/?authSource=admin&directConnection=true"
client = MongoClient(uri, serverSelectionTimeoutMS=5000)

print("Databases:", client.list_database_names())
for db_name in client.list_database_names():
    db = client[db_name]
    for coll_name in db.list_collection_names():
        coll = db[coll_name]
        doc = coll.find_one({"$or": [{"indexSymbol": "HSI"}, {"indexSymbol": "GLOBAL_INDEX|HSI"}, {"symbol": "HSI"}, {"_id": "HSI"}]})
        if doc:
            print(f"Found HSI in db: {db_name}, collection: {coll_name} -> {doc.keys()}")
            print(doc)
