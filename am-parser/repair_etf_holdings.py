import asyncio
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from am_etf.smart_holdings_service import create_smart_etf_holdings_service

async def repair_etf_holdings():
    print("Connecting to DB for Smart Holdings repair...")
    from am_configs.settings import settings
    
    # Set env var for market-data URL
    os.environ["MARKET_DATA_URL"] = "http://localhost:8093"
    
    print(f"Using MongoDB: {settings.mongo_uri.split('@')[-1] if '@' in settings.mongo_uri else 'localhost'}")
    
    # Use the SMART service for the correct collection (etf_holdings)
    service = create_smart_etf_holdings_service(mongo_uri=settings.mongo_uri, db_name=settings.effective_etf_db)
    
    # Force refresh
    service.set_cache_policy(expiry_days=0, force_refresh=True)
    
    isin = "INF204KB14I2"
    symbol = "NIFTYBEES"
    name = "Nippon India ETF Nifty 50 BeES"
    
    print(f"Force refreshing holdings for {symbol} ({isin})...")
    result = await service.smart_fetch_and_store_holdings(isin=isin, symbol=symbol, etf_name=name)
    
    if result["success"]:
        print(f"Success! Fetched {result['holdings_count']} holdings.")
        
        # Verify enrichment in DB
        col = service.holdings_collection
        doc = await col.find_one({"isin": isin})
        if doc and "holdings" in doc:
            enriched = [h for h in doc["holdings"] if h.get("isin_code")]
            print(f"✅ Verified in DB (etf_holdings): {len(enriched)}/{len(doc['holdings'])} have ISINs")
            
            # Print sample
            for h in doc["holdings"][:5]:
                if h.get("stock_name") in ["HDFC Bank Ltd.", "Reliance Industries Ltd."]:
                     print(f"   - {h.get('stock_name')}: {h.get('isin_code')} (Status: {h.get('enrichment_status')})")
        else:
            print("❌ Document not found in DB execution?")
            
    else:
        print(f"❌ Failed: {result['reason']}")
    
    await service.close()

if __name__ == "__main__":
    asyncio.run(repair_etf_holdings())
