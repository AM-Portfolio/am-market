
import asyncio
import os
import sys

# Mock environment variables to ensure we check the right headers
os.environ["MONGO_URI"] = "mongodb://admin:password123@100.72.208.15:27017"
os.environ["MONGO_DB"] = "mutual_funds" 
# Note: User's screenshot confirms 'mutual_funds' has the 'etfs' collection

from motor.motor_asyncio import AsyncIOMotorClient

async def check_data():
    uri = os.environ["MONGO_URI"]
    print(f"Connecting to: {uri}")
    client = AsyncIOMotorClient(uri)
    
    # 1. Check 'mutual_funds' database
    db = client["mutual_funds"]
    print(f"Checking Database: mutual_funds")
    
    collections = await db.list_collection_names()
    print(f"Collections found: {collections}")
    
    if "etfs" in collections:
        count = await db.etfs.count_documents({})
        print(f"Count in 'etfs': {count}")
    else:
        print("❌ 'etfs' collection NOT found in mutual_funds")

    if "etf_holdings" in collections:
        count = await db.etf_holdings.count_documents({})
        print(f"Count in 'etf_holdings': {count}")
    else:
        print("❌ 'etf_holdings' collection NOT found in mutual_funds")
        
    if "portfolios" in collections:
         count = await db.portfolios.count_documents({})
         print(f"Count in 'portfolios' (Mutual Funds): {count}")
         
    client.close()

if __name__ == "__main__":
    try:
        asyncio.run(check_data())
    except Exception as e:
        print(f"Error: {e}")
