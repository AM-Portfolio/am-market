"""One-shot: import Prod holdings bulk JSON into Dev Mongo via ETFHoldingsService.

Expects /tmp/prod-etf-holdings-bulk.json inside the pod:
  { "total_etfs": N, "data": { "<isin>": { "symbol", "name", "holdings": [
       { "isin", "symbol", "sector", "weight" }, ... ] } } }
"""
import asyncio
import json
import sys
from datetime import datetime
from pathlib import Path

from am_etf.holdings_models import ETFHoldingRecord, ETFHoldingsData
from am_etf.holdings_service import create_etf_holdings_service


async def main(path: str) -> None:
    raw = json.loads(Path(path).read_text(encoding="utf-8"))
    data = raw.get("data") or {}
    svc = create_etf_holdings_service()
    ok = 0
    fail = 0
    try:
        for isin, doc in data.items():
            try:
                holdings = []
                for h in doc.get("holdings") or []:
                    holdings.append(
                        ETFHoldingRecord(
                            stock_name=h.get("symbol") or h.get("stock_name"),
                            isin_code=h.get("isin") or h.get("isin_code"),
                            percentage=h.get("weight") if h.get("weight") is not None else h.get("percentage"),
                        )
                    )
                payload = ETFHoldingsData(
                    isin=isin,
                    symbol=doc.get("symbol"),
                    etf_name=doc.get("name"),
                    holdings=holdings,
                    total_holdings=len(holdings),
                    fetched_at=datetime.utcnow(),
                    api_source="prod_copy_seed",
                )
                await svc.store_holdings(payload)
                ok += 1
            except Exception as e:
                fail += 1
                print(f"FAIL {isin}: {e}", file=sys.stderr)
        print(json.dumps({"stored": ok, "failed": fail, "input": len(data)}))
    finally:
        await svc.close()


if __name__ == "__main__":
    asyncio.run(main(sys.argv[1] if len(sys.argv) > 1 else "/tmp/prod-etf-holdings-bulk.json"))
