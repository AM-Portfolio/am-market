"""CLI utility to fetch ETF holdings data from moneycontrol API"""
import asyncio
import argparse
from am_etf.service import create_etf_service


async def fetch_holdings(mongo_uri: str, db_name: str, limit: int):
    """Fetch holdings for ETFs from the moneycontrol API"""
    service = create_etf_service(mongo_uri=mongo_uri, db_name=db_name)
    
    print(f"🔄 Fetching holdings data for ETFs (limit: {limit if limit else 'all'})")
    
    try:
        updated_count = await service.fetch_and_update_holdings(limit=limit)
        print(f"✅ Successfully updated holdings for {updated_count} ETFs")
        
        # Show sample of ETFs with holdings
        etfs_with_holdings = await service.get_etfs_with_holdings(limit=5)
        if etfs_with_holdings:
            print("\n📊 Sample ETFs with holdings:")
            for etf in etfs_with_holdings:
                holdings_count = len(etf.holdings) if etf.holdings else 0
                print(f"  - {etf.symbol}: {etf.name} ({holdings_count} holdings)")
        
    finally:
        await service.close()


def main():
    import os
    parser = argparse.ArgumentParser(description="Fetch ETF holdings data from moneycontrol API")
    parser.add_argument("--mongo-uri", default=os.getenv("MONGO_URI", "mongodb://admin:password123@localhost:27017"), help="Mongo connection URI")
    parser.add_argument("--db-name", default=os.getenv("MONGO_DB", "etf_data"), help="Mongo database name for ETF data")
    parser.add_argument("--limit", type=int, help="Limit number of ETFs to process (default: all)")
    args = parser.parse_args()

    asyncio.run(fetch_holdings(args.mongo_uri, args.db_name, args.limit))


if __name__ == "__main__":
    main()