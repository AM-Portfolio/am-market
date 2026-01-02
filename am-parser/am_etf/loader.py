"""CLI utility to load ETF JSON data into MongoDB"""
import json
import asyncio
from pathlib import Path
import argparse
from typing import List

from am_etf.service import create_etf_service
from am_etf.models import ETFInstrument


def load_json_file(path: Path) -> List[dict]:
    with path.open('r', encoding='utf-8') as f:
        data = json.load(f)
    if not isinstance(data, list):
        raise ValueError("Expected a list of ETF records in JSON")
    return data


async def ingest(path: Path, mongo_uri: str, db_name: str, dry_run: bool):
    records = load_json_file(path)
    instruments: List[ETFInstrument] = []
    for rec in records:
        try:
            instruments.append(ETFInstrument(**rec))
        except Exception as e:
            print(f"Skipping invalid record {rec}: {e}")
    print(f"Parsed {len(instruments)} ETF instruments from file (input total {len(records)})")
    if dry_run:
        print("Dry run: not persisting to database.")
        return
    service = create_etf_service(mongo_uri=mongo_uri, db_name=db_name)
    inserted = await service.bulk_upsert(instruments)
    print(f"Upserted {inserted} ETF instruments (duplicates merged via symbol/isin).")
    await service.close()


def main():
    import os
    parser = argparse.ArgumentParser(description="Load ETF details JSON into MongoDB")
    parser.add_argument("--file", required=True, help="Path to etf_details.json")
    parser.add_argument("--mongo-uri", default=os.getenv("MONGO_URI", "mongodb://admin:password123@localhost:27017"), help="Mongo connection URI")
    parser.add_argument("--db-name", default=os.getenv("MONGO_DB", "etf_data"), help="Mongo database name for ETF data")
    parser.add_argument("--dry-run", action="store_true", help="Parse and validate only; do not persist")
    args = parser.parse_args()

    path = Path(args.file)
    if not path.exists():
        raise SystemExit(f"File not found: {path}")

    asyncio.run(ingest(path, args.mongo_uri, args.db_name, args.dry_run))


if __name__ == "__main__":
    main()
