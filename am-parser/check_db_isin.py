import asyncio
from am_etf.service import create_etf_service
from am_configs.settings import settings

async def check():
    service = create_etf_service()
    
    # 1. Search by Symbol
    doc = await service.collection.find_one({"symbol": "NIFTYBEES"})
    print(f"By Symbol 'NIFTYBEES': {doc}")
    if doc:
        print(f"  ISIN in DB: '{doc.get('isin')}'")
        
    # 2. Search by ISIN
    isin = "INF204KB14I2"
    doc2 = await service.collection.find_one({"isin": isin})
    print(f"By ISIN '{isin}':Found={doc2 is not None}")

    # 3. Check regex ISIN (maybe case or small diff)
    cursor = service.collection.find({"isin": {"$regex": "INF204.*"}})
    docs = await cursor.to_list(length=10)
    print(f"Regex Matches: {len(docs)}")
    for d in docs:
        print(f"  - {d.get('symbol')}: {d.get('isin')}")
        
    await service.close()

if __name__ == "__main__":
    asyncio.run(check())
