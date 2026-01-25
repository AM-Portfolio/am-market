
import asyncio
import json
import sys
import os
from pathlib import Path
from datetime import datetime

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_etf.service import ETFService
from am_etf.smart_holdings_service import SmartETFHoldingsService
from am_etf.models import ETFInstrument, ETFHolding
from am_etf.holdings_models import ETFHoldingsData, ETFHoldingRecord

async def seed_data():
    print("Starting mock data seeding...")
    
    # Load JSON
    json_path = Path(__file__).parent / "mock_etf_data.json"
    if not json_path.exists():
        print(f"Mock data file not found: {json_path}")
        return
        
    with open(json_path, "r") as f:
        data = json.load(f)
        
    print(f"Loaded {len(data)} records from JSON")
    
    # Initialize services
    etf_service = ETFService()
    holdings_service = SmartETFHoldingsService()
    
    for item in data:
        try:
            # 1. Prepare ETF Instrument
            # Note: The JSON 'holdings' field matches ETFHolding model structure
            # but we also need to convert it to ETFHolding objects for Pydantic validation if strictly typed
            # Pydantic is smart enough to handle dicts if passed to constructor
            
            # Create ETFInstrument
            etf_inst = ETFInstrument(**item)
            
            # Upsert ETF Instrument
            await etf_service.upsert_etf(etf_inst)
            print(f"Upserted ETF: {etf_inst.symbol}")
            
            # 2. Prepare Detailed Holdings (Smart Service)
            if item.get("holdings"):
                holdings_records = []
                for h in item["holdings"]:
                    holdings_records.append(ETFHoldingRecord(**h))
                
                holdings_data = ETFHoldingsData(
                    isin=etf_inst.isin,
                    symbol=etf_inst.symbol,
                    etf_name=etf_inst.name,
                    holdings=holdings_records,
                    total_holdings=len(holdings_records),
                    fetched_at=datetime.utcnow(),
                    api_source="mock_seed"
                )
                
                await holdings_service.store_holdings(holdings_data)
                print(f"   -- Stored {len(holdings_records)} holdings records")
                
        except Exception as e:
            print(f"Failed to process item {item.get('symbol', 'unknown')}: {e}")
            import traceback
            traceback.print_exc()

    await etf_service.close()
    await holdings_service.close()
    print("Seeding completed!")

if __name__ == "__main__":
    asyncio.run(seed_data())
