"""Moneycontrol ETF holdings API client."""
from typing import List, Optional

import httpx

from am_configs.settings import settings
from am_etf.holdings_models import ETFHoldingRecord
from am_common.logging.request_logging import get_logger

_log = get_logger("moneycontrol_client")


def _safe_float(value) -> Optional[float]:
    if value is None:
        return None
    try:
        if isinstance(value, str):
            value = value.replace("%", "").strip()
        return float(value)
    except (ValueError, TypeError):
        return None


def _safe_int(value) -> Optional[int]:
    if value is None:
        return None
    try:
        return int(value)
    except (ValueError, TypeError):
        return None


async def fetch_holdings_from_moneycontrol(isin: str) -> Optional[List[ETFHoldingRecord]]:
    """Fetch stock holdings for an ETF ISIN from Moneycontrol."""
    if not isin:
        return None

    url = settings.moneycontrol_holdings_url_template.format(isin=isin)
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(url)
            response.raise_for_status()
            data = response.json()

        if isinstance(data, dict) and "data" in data:
            holdings_data = data["data"]
        elif isinstance(data, list):
            holdings_data = data
        else:
            holdings_data = data

        holdings: List[ETFHoldingRecord] = []
        if not isinstance(holdings_data, list):
            return holdings

        for holding_data in holdings_data:
            holdings.append(
                ETFHoldingRecord(
                    stock_name=holding_data.get("name")
                    or holding_data.get("stock_name")
                    or "Unknown",
                    isin_code=holding_data.get("isin_code") or holding_data.get("isin"),
                    percentage=_safe_float(
                        holding_data.get("holdingPer")
                        or holding_data.get("percentage")
                        or holding_data.get("weight")
                    ),
                    market_value=_safe_float(
                        holding_data.get("investedAmount")
                        or holding_data.get("market_value")
                        or holding_data.get("value")
                    ),
                    quantity=_safe_int(holding_data.get("quantity")),
                    raw_data=holding_data,
                )
            )

        _log.debug("Fetched %s holdings for ISIN %s", len(holdings), isin)
        return holdings
    except Exception as e:
        _log.warning("Moneycontrol fetch failed for ISIN %s: %s", isin, e)
        return None
