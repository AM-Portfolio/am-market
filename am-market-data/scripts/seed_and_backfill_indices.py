import yfinance as yf
from pymongo import MongoClient
import time
import math
import os
import datetime

DEFAULT_URI = "mongodb://localhost:27017/market_data?authSource=admin"
uri = os.getenv("MONGODB_URI", DEFAULT_URI)
client = MongoClient(uri)
db = client.get_database("market_data")

indices_collection = db.get_collection("stock_indices_market_data")
securities_collection = db.get_collection("securities")
fundamentals_collection = db.get_collection("fundamental_analysis")

def clean_nan(val):
    if val is None or (isinstance(val, float) and math.isnan(val)):
        return None
    return val

def run_seeding_and_backfill():
    print("--- Phase 1: Gathering index constituents ---")
    symbols = set()
    
    # 1. Fetch Nifty 50 and Nifty 500 constituents
    for idx_name in ["NIFTY 50", "NIFTY 500"]:
        doc = indices_collection.find_one({"_id": idx_name})
        if doc and doc.get("data"):
            for item in doc["data"]:
                sym = item.get("symbol")
                if sym:
                    symbols.add(sym.strip().upper())
                    
    # 2. Add standard Nifty IT symbols manually to ensure coverage
    nifty_it = ["TCS", "INFY", "WIPRO", "HCLTECH", "TECHM", "LTIM", "COFORGE", "PERSISTENT", "KPITTECH", "MPHASIS"]
    for sym in nifty_it:
        symbols.add(sym.strip().upper())
        
    print(f"Total unique symbols found in indexes: {len(symbols)}")
    
    print("\n--- Phase 2: Seeding missing symbols into fundamental_analysis ---")
    seeded_count = 0
    for symbol in sorted(symbols):
        # Check if already exists
        existing = fundamentals_collection.find_one({"symbol": symbol})
        if existing:
            continue
            
        # Lookup in securities collection for ISIN & metadata
        sec_doc = securities_collection.find_one({"key.symbol": symbol})
        if not sec_doc:
            # Fallback regex search
            sec_doc = securities_collection.find_one({"key.symbol": {"$regex": f"^{symbol}$", "$options": "i"}})
            
        if sec_doc:
            isin = sec_doc.get("key", {}).get("isin")
            company_name = sec_doc.get("metadata", {}).get("company_name") or symbol
            if isin:
                # Seed the document
                seed_doc = {
                    "symbol": symbol,
                    "isin": isin.upper(),
                    "providerSource": "UPSTOX",
                    "instrumentKey": f"NSE_EQ|{isin.upper()}",
                    "companyName": company_name,
                    "createdAt": datetime.datetime.utcnow(),
                    "updatedAt": datetime.datetime.utcnow()
                }
                fundamentals_collection.insert_one(seed_doc)
                print(f"  [Seeded] {symbol} | ISIN: {isin}")
                seeded_count += 1
        else:
            print(f"  [Skip] {symbol} (Not found in securities master)")
            
    print(f"Total new symbols seeded: {seeded_count}")
    
    print("\n--- Phase 3: Backfilling missing financials from Yahoo Finance ---")
    # Query all documents in fundamental_analysis that are missing financials
    docs_to_backfill = list(fundamentals_collection.find({
        "$or": [
            {"incomeStatements": {"$exists": False}},
            {"incomeStatements": {"$size": 0}},
            {"balanceSheets": {"$exists": False}},
            {"balanceSheets": {"$size": 0}}
        ]
    }))
    
    print(f"Found {len(docs_to_backfill)} stocks missing financials. Starting yfinance backfill...")
    
    success_count = 0
    fail_count = 0
    processed_count = 0
    
    for doc in docs_to_backfill:
        processed_count += 1
        symbol = doc.get("symbol")
        if not symbol:
            continue
            
        print(f"[{processed_count}/{len(docs_to_backfill)}] Fetching financials for {symbol}...", flush=True)
        
        yf_symbol = f"{symbol}.NS"
        ticker = yf.Ticker(yf_symbol)
        
        try:
            income = ticker.financials
            balance = ticker.balance_sheet
            
            update_data = {}
            
            # Map Income Statement
            if income is not None and not income.empty:
                income_list = []
                for date_col in income.columns:
                    year_data = income[date_col]
                    income_list.append({
                        "periodEndDate": date_col.strftime("%Y-%m-%d") if hasattr(date_col, 'strftime') else str(date_col),
                        "totalRevenue": clean_nan(year_data.get("Total Revenue")),
                        "ebitda": clean_nan(year_data.get("EBITDA")),
                        "netIncome": clean_nan(year_data.get("Net Income")),
                        "basicEps": clean_nan(year_data.get("Basic EPS"))
                    })
                update_data["incomeStatements"] = income_list
                
            # Map Balance Sheet
            if balance is not None and not balance.empty:
                balance_list = []
                for date_col in balance.columns:
                    year_data = balance[date_col]
                    balance_list.append({
                        "periodEndDate": date_col.strftime("%Y-%m-%d") if hasattr(date_col, 'strftime') else str(date_col),
                        "totalAssets": clean_nan(year_data.get("Total Assets")),
                        "totalLiabilities": clean_nan(year_data.get("Total Liabilities Net Minority Interest")),
                        "totalEquity": clean_nan(year_data.get("Stockholders Equity"))
                    })
                update_data["balanceSheets"] = balance_list
            
            if update_data:
                fundamentals_collection.update_one({"_id": doc["_id"]}, {"$set": update_data})
                print(f"  -> Successfully updated {symbol}")
                success_count += 1
            else:
                print(f"  -> [Warning] No data returned for {symbol}")
                fail_count += 1
                
        except Exception as e:
            print(f"  -> [Error] Failed to fetch {symbol}: {e}")
            fail_count += 1
            
        time.sleep(2.5) # Gentle delay to prevent rate limits
        
    print(f"\n--- Seeding & Backfill complete ---")
    print(f"Successfully backfilled: {success_count} | Failures/Warnings: {fail_count}")

if __name__ == "__main__":
    run_seeding_and_backfill()
