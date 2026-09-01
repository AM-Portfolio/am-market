import yfinance as yf
from pymongo import MongoClient
import time
import sys
import datetime
import math
import os

# Connect to local/dev MongoDB by default (production URI is injected via environment variables)
DEFAULT_URI = "mongodb://localhost:27017/market_data?authSource=admin"
uri = os.getenv("MONGODB_URI", DEFAULT_URI)
client = MongoClient(uri)
db = client.get_database("market_data")
collection = db.get_collection("fundamental_analysis")

def clean_nan(val):
    if val is None or (isinstance(val, float) and math.isnan(val)):
        return None
    return val

def backfill():
    # Print start message to console
    print(f"Starting Yahoo Finance backfill at {datetime.datetime.now()}...", flush=True)
    
    # We count documents first without loading them into memory
    total_docs = collection.count_documents({})
    print(f"Total documents found in database: {total_docs}", flush=True)
    
    if total_docs == 0:
        print("MongoDB collection is empty. Nothing to backfill.", flush=True)
        return

    # Iterate using cursor directly to avoid RAM spikes
    cursor = collection.find({}, projection={"symbol": 1, "_id": 1})
    
    success_count = 0
    fail_count = 0
    processed_count = 0
    
    for doc in cursor:
        processed_count += 1
        symbol = doc.get('symbol')
        if not symbol:
            continue
            
        # Log progress every 5 stocks to keep console clean and avoid RAM spike on logs
        if processed_count % 5 == 0 or processed_count == total_docs:
            print(f"[Progress] Processed {processed_count}/{total_docs} | Success: {success_count} | Fails: {fail_count}", flush=True)
            
        yf_symbol = f"{symbol}.NS"
        ticker = yf.Ticker(yf_symbol)
        
        try:
            # Fetch financials (only what we need: income statement and balance sheet)
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
                collection.update_one({"_id": doc["_id"]}, {"$set": update_data})
                success_count += 1
            else:
                # Log only as a minor warning if no data is found
                print(f"[Warning] No financials found for {symbol}", flush=True)
                fail_count += 1
            
        except Exception as e:
            # Log exact problem faced
            print(f"[Error] Failed to fetch {symbol}: {str(e)}", flush=True)
            fail_count += 1
            
        # Safe delay to avoid yfinance rate limits
        time.sleep(2)

    print(f"Finished backfilling. Total Processed: {processed_count} | Success: {success_count} | Failures: {fail_count}", flush=True)

if __name__ == "__main__":
    backfill()
