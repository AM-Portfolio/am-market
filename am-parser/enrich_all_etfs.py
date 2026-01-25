import asyncio
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from am_etf.smart_holdings_service import create_smart_etf_holdings_service
from am_etf.service import create_etf_service

async def enrich_all_etfs():
    print("🚀 Starting Bulk ETF Enrichment...")
    from am_configs.settings import settings
    
    # Set env var for market-data URL
    os.environ["MARKET_DATA_URL"] = "http://localhost:8093"
    
    # 1. Get list of all ETFs from main 'etfs' collection (the directory of ETFs)
    # We need to know WHICH ETFs to fetch.
    base_service = create_etf_service(mongo_uri=settings.mongo_uri, db_name=settings.effective_etf_db)
    col = base_service._get_collection()
    
    # Fetch all ETFs that have an ISIN
    cursor = col.find({"isin": {"$exists": True, "$ne": None}}, {"isin": 1, "symbol": 1, "etf_name": 1})
    etfs_to_process = await cursor.to_list(length=1000)
    
    print(f"📋 Found {len(etfs_to_process)} ETFs to process.")
    await base_service.close()
    
    # 2. Process using Smart Service
    smart_service = create_smart_etf_holdings_service(mongo_uri=settings.mongo_uri, db_name=settings.effective_etf_db)
    smart_service.set_cache_policy(expiry_days=0, force_refresh=True) # FORCE UPDATE
    
    success_count = 0
    fail_count = 0
    
    for i, etf in enumerate(etfs_to_process):
        isin = etf.get('isin')
        symbol = etf.get('symbol', 'UNKNOWN')
        name = etf.get('etf_name', 'Unknown ETF')
        
        print(f"[{i+1}/{len(etfs_to_process)}] Processing {symbol} ({isin})...")
        
        try:
            result = await smart_service.smart_fetch_and_store_holdings(
                isin=isin, 
                symbol=symbol, 
                etf_name=name
            )
            
            if result["success"]:
                # Check enrichment stats
                holdings_count = result.get('holdings_count', 0)
                # Quick peek at how many have ISINs
                doc = await smart_service.holdings_collection.find_one({"isin": isin})
                enriched = 0
                if doc and 'holdings' in doc:
                    enriched = sum(1 for h in doc['holdings'] if h.get('isin_code'))
                
                print(f"   ✅ Done. Holdings: {holdings_count}, Enriched with ISIN: {enriched}")
                success_count += 1
            else:
                print(f"   ⚠️ Failed: {result['reason']}")
                fail_count += 1
                
        except Exception as e:
            print(f"   ❌ Error: {e}")
            fail_count += 1
            
        # Small delay to be nice to API
        await asyncio.sleep(1.0)
            
    print("="*40)
    print(f"🎉 Bulk Enrichment Complete!")
    print(f"Success: {success_count}")
    print(f"Failed: {fail_count}")
    
    await smart_service.close()

if __name__ == "__main__":
    asyncio.run(enrich_all_etfs())
