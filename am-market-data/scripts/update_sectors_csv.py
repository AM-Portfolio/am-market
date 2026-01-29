
import csv
import os
from pymongo import MongoClient, UpdateOne

# Configuration
MONGO_URI = 'mongodb://admin:password123@100.72.208.15:27017/market_data?authSource=admin'
DB_NAME = 'market_data'
COLLECTION_NAME = 'securities'
CSV_PATH = os.path.join(os.path.dirname(__file__), '../data/Equity.csv')

def update_sectors():
    try:
        # Connect to MongoDB
        client = MongoClient(MONGO_URI)
        db = client[DB_NAME]
        collection = db[COLLECTION_NAME]
        
        print(f"Connected to MongoDB: {DB_NAME}.{COLLECTION_NAME}")
        
        operations = []
        updated_count = 0
        
        print(f"Reading CSV from: {CSV_PATH}")
        with open(CSV_PATH, mode='r', encoding='utf-8') as csvfile:
            reader = csv.DictReader(csvfile)
            
            for row in reader:
                isin = row.get('ISIN No')
                sector = row.get('Sector Name')
                
                if isin and sector and sector != '-':
                    # Map 'Industry' or 'Sector Name' based on preference? 
                    # User said: "Trading - Gas sector name is not correctly updated. wrtie script to update all peresent in db"
                    # CSV: Industry=Trading - Gas, Sector Name=Energy.
                    # User: "securti search return industry take this as sector" (This was for EtfApiClient)
                    # For this script, user said " sector name is not correctly updated". 
                    # And provided CSV snippet:
                    # ABB: Sector Name=Industrials, Industry=Heavy Electrical Equipment.
                    # User likely wants "Sector Name" column to be mapped to `metadata.sector`.
                    
                    # NOTE: Some rows might have '-' or empty sector. We skip those? 
                    # row 4: Torrent Power... Sector Name='-'
                    if sector and sector.strip() != '-':
                         operations.append(
                            UpdateOne(
                                {"key.isin": isin},
                                {"$set": {"metadata.sector": sector}}
                            )
                        )
                    
        print(f"Prepared {len(operations)} update operations.")
        
        if operations:
            result = collection.bulk_write(operations)
            print(f"Bulk write complete.")
            print(f"Matched: {result.matched_count}")
            print(f"Modified: {result.modified_count}")
        else:
            print("No operations to execute.")

    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    update_sectors()
