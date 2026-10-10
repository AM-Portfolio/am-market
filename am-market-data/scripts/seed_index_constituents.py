#!/usr/bin/env python3
"""
seed_index_constituents.py
---------------------------
Populates and synchronizes constituent stocks for all NSE indices in MongoDB collection:
`market_data.stock_indices_market_data`

Uses authoritative official NSE Archive CSV feeds (archives.nseindia.com) which do NOT
require session cookies or Akamai bypasses. Enforces minimum constituent count validation
so no index constituent is ever truncated or missed.
"""

import os
import io
import csv
import logging
import datetime
import urllib.request
from pymongo import MongoClient, UpdateOne

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("IndexConstituentSeeder")

MONGO_URI = os.getenv("MONGODB_URI")
if not MONGO_URI:
    raise SystemExit("MONGODB_URI environment variable is required")
DB_NAME = "market_data"
COLLECTION_NAME = "stock_indices_market_data"

# Mapping of Index symbol to official NSE archive CSV URL and minimum required constituent count
INDEX_CSV_REGISTRY = {
    "NIFTY 50": {
        "url": "https://archives.nseindia.com/content/indices/ind_nifty50list.csv",
        "min_count": 50
    },
    "NIFTY NEXT 50": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftynext50list.csv",
        "min_count": 50
    },
    "NIFTY 100": {
        "url": "https://archives.nseindia.com/content/indices/ind_nifty100list.csv",
        "min_count": 100
    },
    "NIFTY 200": {
        "url": "https://archives.nseindia.com/content/indices/ind_nifty200list.csv",
        "min_count": 200
    },
    "NIFTY 500": {
        "url": "https://archives.nseindia.com/content/indices/ind_nifty500list.csv",
        "min_count": 500
    },
    "NIFTY MIDCAP 50": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftymidcap50list.csv",
        "min_count": 50
    },
    "NIFTY MIDCAP 100": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftymidcap100list.csv",
        "min_count": 100
    },
    "NIFTY MIDCAP 150": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftymidcap150list.csv",
        "min_count": 150
    },
    "NIFTY SMLCAP 50": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftysmallcap50list.csv",
        "min_count": 50
    },
    "NIFTY SMLCAP 100": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftysmallcap100list.csv",
        "min_count": 100
    },
    "NIFTY SMLCAP 250": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftysmallcap250list.csv",
        "min_count": 250
    },
    "NIFTY BANK": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftybanklist.csv",
        "min_count": 12
    },
    "NIFTY AUTO": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftyautolist.csv",
        "min_count": 15
    },
    "NIFTY FMCG": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftyfmcglist.csv",
        "min_count": 15
    },
    "NIFTY IT": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftyitlist.csv",
        "min_count": 10
    },
    "NIFTY MEDIA": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftymedialist.csv",
        "min_count": 10
    },
    "NIFTY METAL": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftymetallist.csv",
        "min_count": 15
    },
    "NIFTY PHARMA": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftypharmalist.csv",
        "min_count": 20
    },
    "NIFTY PSU BANK": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftypsubanklist.csv",
        "min_count": 12
    },
    "NIFTY REALTY": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftyrealtylist.csv",
        "min_count": 10
    },
    "NIFTY ENERGY": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftyenergylist.csv",
        "min_count": 10
    },
    "NIFTY HEALTHCARE": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftyhealthcarelist.csv",
        "min_count": 20
    },
    "NIFTY CONSR DURABLE": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftyconsumerdurableslist.csv",
        "min_count": 14
    },
    "NIFTY OIL AND GAS": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftyoilgaslist.csv",
        "min_count": 15
    },
    "NIFTY FIN SERVICES": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftyfinancelist.csv",
        "min_count": 20
    },
    "NIFTY COMMODITIES": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftycommoditieslist.csv",
        "min_count": 30
    },
    "NIFTY CPSE": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftycpselist.csv",
        "min_count": 10
    },
    "NIFTY INFRA": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftyinfralist.csv",
        "min_count": 30
    },
    "NIFTY PSE": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftypselist.csv",
        "min_count": 20
    },
    "NIFTY SERVICES": {
        "url": "https://archives.nseindia.com/content/indices/ind_niftyservicelist.csv",
        "min_count": 30
    }
}

ALIASES = {
    "NIFTY FIN SERVICE": "NIFTY FIN SERVICES",
    "NIFTY PHARMACEUTICALS": "NIFTY PHARMA",
    "NSE_INDEX:NIFTY 50": "NIFTY 50",
    "NSE_INDEX:NIFTY BANK": "NIFTY BANK",
}


USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

def fetch_constituents_from_csv(index_name: str, config: dict):
    url = config["url"]
    min_count = config["min_count"]
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            content = response.read().decode("utf-8")
    except Exception as e:
        logger.error("Failed to download CSV for %s from %s: %s", index_name, url, e)
        return None

    reader = csv.DictReader(io.StringIO(content))
    constituents = []
    priority = 1

    for row in reader:
        symbol = (row.get("Symbol") or row.get("symbol") or "").strip().upper()
        company_name = (row.get("Company Name") or row.get("companyName") or "").strip()
        industry = (row.get("Industry") or row.get("industry") or "").strip()
        series = (row.get("Series") or row.get("series") or "EQ").strip()
        isin = (row.get("ISIN Code") or row.get("isin") or "").strip()

        if not symbol:
            continue
        # Filter out parent index row if present in CSV
        if symbol == index_name.replace(" ", ""):
            continue

        stock_doc = {
            "symbol": symbol,
            "identifier": f"{symbol}{series}",
            "series": series,
            "name": symbol,
            "ffmc": None,
            "companyName": company_name,
            "isin": isin,
            "industry": industry,
            "priority": priority,
            "open": 0.0,
            "dayHigh": 0.0,
            "dayLow": 0.0,
            "lastPrice": 0.0,
            "previousClose": 0.0,
            "change": 0.0,
            "pChange": 0.0,
            "totalTradedVolume": 0
        }
        constituents.append(stock_doc)
        priority += 1

    if len(constituents) < min_count:
        logger.warning(
            "Validation failed for %s: expected at least %d stocks, found %d. Rejecting update.",
            index_name, min_count, len(constituents)
        )
        return None

    logger.info("Successfully parsed %d constituents for %s", len(constituents), index_name)
    return constituents

def sync_all_indices(mongo_uri=MONGO_URI):
    logger.info("Connecting to MongoDB: %s", mongo_uri.split("@")[-1] if "@" in mongo_uri else mongo_uri)
    client = MongoClient(mongo_uri, serverSelectionTimeoutMS=10000)
    db = client[DB_NAME]
    col = db[COLLECTION_NAME]

    success_count = 0
    fail_count = 0

    for index_name, config in INDEX_CSV_REGISTRY.items():
        constituents = fetch_constituents_from_csv(index_name, config)
        if not constituents:
            fail_count += 1
            continue

        now = datetime.datetime.now(datetime.timezone.utc)
        # Update or upsert document, preserving existing metadata if present
        existing = col.find_one({"_id": index_name})
        if existing:
            # Preserve existing live prices so mid-session reseed does not blank heatmap/movers
            existing_by_symbol = {}
            for item in existing.get("data") or []:
                if isinstance(item, dict) and item.get("symbol"):
                    existing_by_symbol[item["symbol"]] = item

            price_fields = [
                "open", "dayHigh", "dayLow", "lastPrice", "previousClose",
                "change", "pChange", "totalTradedVolume", "yearHigh", "yearLow"
            ]
            merged_constituents = []
            for item in constituents:
                sym = item.get("symbol")
                prev_stock = existing_by_symbol.get(sym)
                if prev_stock:
                    merged = dict(item)
                    for pf in price_fields:
                        if pf in prev_stock and prev_stock[pf] is not None:
                            merged[pf] = prev_stock[pf]
                    merged_constituents.append(merged)
                else:
                    merged_constituents.append(item)

            col.update_one(
                {"_id": index_name},
                {
                    "$set": {
                        "data": merged_constituents,
                        "timestamp": now.strftime("%d-%b-%Y %H:%M:%S"),
                        "audit.updatedAt": now
                    }
                }
            )
            logger.info("Updated existing index %s with %d stocks", index_name, len(constituents))
        else:
            new_doc = {
                "_id": index_name,
                "_class": "com.am.common.investment.model.stockindice.StockIndicesMarketData",
                "indexSymbol": index_name,
                "docVersion": "1.0",
                "data": constituents,
                "metadata": {
                    "indexName": index_name,
                    "open": 0.0,
                    "high": 0.0,
                    "low": 0.0,
                    "previousClose": 0.0,
                    "last": 0.0,
                    "change": 0.0,
                    "percChange": 0.0
                },
                "advance": {
                    "advances": 0,
                    "declines": 0,
                    "unchanged": 0
                },
                "timestamp": now.strftime("%d-%b-%Y %H:%M:%S"),
                "audit": {
                    "createdAt": now,
                    "updatedAt": now
                }
            }
            col.insert_one(new_doc)
            logger.info("Created new index document %s with %d stocks", index_name, len(constituents))

        success_count += 1

    # --- Sync NIFTY PVT BANK (derived from NIFTY BANK excluding PSU banks) ---
    bank_doc = col.find_one({"_id": "NIFTY BANK"})
    if bank_doc and bank_doc.get("data"):
        psu_banks = {"SBIN", "BANKBARODA", "PNB", "CANBK", "UNIONBANK", "INDIANB", "IOB", "UCOBANK", "CENTRALBK", "MAHABANK", "PSB"}
        pvt_bank_stocks = [s for s in bank_doc["data"] if s.get("symbol") not in psu_banks]
        if pvt_bank_stocks:
            now = datetime.datetime.now(datetime.timezone.utc)
            col.update_one(
                {"_id": "NIFTY PVT BANK"},
                {
                    "$set": {
                        "data": pvt_bank_stocks,
                        "timestamp": now.strftime("%d-%b-%Y %H:%M:%S"),
                        "audit.updatedAt": now
                    }
                },
                upsert=True
            )
            logger.info("Updated NIFTY PVT BANK with %d private banks", len(pvt_bank_stocks))

    # --- Sync NIFTY MIDSML HLTH (derived from NIFTY HEALTHCARE excluding top large caps) ---
    health_doc = col.find_one({"_id": "NIFTY HEALTHCARE"})
    if health_doc and health_doc.get("data"):
        large_caps = {"SUNPHARMA", "DRREDDY", "CIPLA", "APOLLOHOSP", "DIVISLAB"}
        midsml_health = [s for s in health_doc["data"] if s.get("symbol") not in large_caps]
        if midsml_health:
            now = datetime.datetime.now(datetime.timezone.utc)
            col.update_one(
                {"_id": "NIFTY MIDSML HLTH"},
                {
                    "$set": {
                        "data": midsml_health,
                        "timestamp": now.strftime("%d-%b-%Y %H:%M:%S"),
                        "audit.updatedAt": now
                    }
                },
                upsert=True
            )
            logger.info("Updated NIFTY MIDSML HLTH with %d stocks", len(midsml_health))

    # --- Replicate to Aliases ---
    for alias_name, target_name in ALIASES.items():
        target_doc = col.find_one({"_id": target_name})
        if target_doc and target_doc.get("data"):
            now = datetime.datetime.now(datetime.timezone.utc)
            col.update_one(
                {"_id": alias_name},
                {
                    "$set": {
                        "data": target_doc["data"],
                        "timestamp": now.strftime("%d-%b-%Y %H:%M:%S"),
                        "audit.updatedAt": now
                    }
                },
                upsert=True
            )
            logger.info("Replicated %d stocks from %s to alias %s", len(target_doc["data"]), target_name, alias_name)

    logger.info("--- Sync Completed: %d succeeded, %d failed ---", success_count, fail_count)

if __name__ == "__main__":
    sync_all_indices()
