"""Market-data service client for holdings ISIN enrichment."""
from typing import List

import httpx

from am_configs.settings import settings
from am_etf.holdings_models import ETFHoldingRecord
from am_common.logging.request_logging import get_logger

_log = get_logger("market_data_client")


def _process_enrichment_results(
    holdings: List[ETFHoldingRecord], api_response: dict
) -> None:
    if not api_response or "results" not in api_response:
        return

    results_map = {
        r.get("query"): r.get("matches", []) for r in api_response.get("results", [])
    }

    for holding in holdings:
        matches = results_map.get(holding.stock_name)
        if not matches:
            holding.enrichment_status = "NO_MATCH"
            continue

        sorted_matches = sorted(
            matches, key=lambda x: x.get("matchScore", 0), reverse=True
        )
        best_match = None
        for match in sorted_matches:
            if match.get("matchScore", 0) < 0.6:
                break
            candidate_isin = match.get("isin")
            if not candidate_isin or candidate_isin == "-" or len(candidate_isin) < 10:
                continue
            best_match = match
            break

        if best_match:
            enriched_isin = best_match.get("isin")
            holding.isin_code = enriched_isin
            holding.matched_isin = enriched_isin
            holding.matched_symbol = best_match.get("symbol")
            holding.match_score = best_match.get("matchScore")
            holding.enrichment_status = "MATCHED"
        else:
            holding.enrichment_status = "NO_MATCH"


async def enrich_holdings_with_isins(holdings: List[ETFHoldingRecord]) -> None:
    """Enrich holdings via market-data batch-search."""
    stock_names = [h.stock_name for h in holdings if h.stock_name]
    if not stock_names:
        return

    api_url = f"{settings.market_data_url.rstrip('/')}/v1/securities/batch-search"
    chunk_size = 500

    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            for i in range(0, len(stock_names), chunk_size):
                chunk_names = stock_names[i : i + chunk_size]
                payload = {
                    "queries": chunk_names,
                    "limit": 3,
                    "minMatchScore": 0.7,
                }
                try:
                    response = await client.post(api_url, json=payload)
                    response.raise_for_status()
                    _process_enrichment_results(holdings, response.json())
                except Exception as req_err:
                    _log.warning("Batch search chunk failed: %s", req_err)
    except Exception as e:
        _log.warning("Holdings enrichment failed: %s", e)
