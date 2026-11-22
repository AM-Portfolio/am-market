"""Normalize mf_scheme_enrichment.holdings_json into fund-holdings wire shape."""
from __future__ import annotations

import json
from typing import Any, List, Optional


def _pct(raw: Any) -> Optional[float]:
    if raw is None:
        return None
    if isinstance(raw, (int, float)):
        return float(raw)
    s = str(raw).strip().rstrip("%")
    if not s:
        return None
    try:
        return float(s)
    except ValueError:
        return None


def _row_to_holding(row: dict[str, Any]) -> Optional[dict[str, Any]]:
    if not isinstance(row, dict):
        return None
    name = (
        row.get("stock_name")
        or row.get("stockName")
        or row.get("name_of_instrument")
        or row.get("name")
        or row.get("symbol")
        or ""
    )
    name = str(name).strip()
    isin = (
        row.get("isin_code")
        or row.get("isinCode")
        or row.get("isin")
        or ""
    )
    isin = str(isin).strip().upper()
    pct = _pct(
        row.get("percentage")
        if row.get("percentage") is not None
        else row.get("percentage_to_nav")
        if row.get("percentage_to_nav") is not None
        else row.get("weight")
        if row.get("weight") is not None
        else row.get("weightPct")
    )
    if not name and not isin:
        return None
    out: dict[str, Any] = {
        "stock_name": name or isin,
        "isin_code": isin or None,
        "percentage": pct,
    }
    if row.get("market_value") is not None:
        out["market_value"] = row.get("market_value")
    if row.get("quantity") is not None:
        out["quantity"] = row.get("quantity")
    if row.get("sector"):
        out["sector"] = row.get("sector")
    return out


def parse_holdings_json(raw: Any) -> List[dict[str, Any]]:
    """Accept list, JSON string, or {holdings:[...]} wrappers."""
    if raw is None:
        return []
    data = raw
    if isinstance(raw, (bytes, bytearray)):
        raw = raw.decode("utf-8", errors="ignore")
    if isinstance(raw, str):
        s = raw.strip()
        if not s:
            return []
        try:
            data = json.loads(s)
        except json.JSONDecodeError:
            return []
    rows: list = []
    if isinstance(data, list):
        rows = data
    elif isinstance(data, dict):
        for key in ("holdings", "portfolio_holdings", "items", "data"):
            if isinstance(data.get(key), list):
                rows = data[key]
                break
    out: List[dict[str, Any]] = []
    for row in rows:
        h = _row_to_holding(row) if isinstance(row, dict) else None
        if h:
            out.append(h)
    return out


def scheme_details_to_fund_document(details: dict[str, Any]) -> Optional[dict[str, Any]]:
    """Map MfCatalogService.get_scheme_details → FundDocument-compatible dict."""
    if not details:
        return None
    holdings = parse_holdings_json(details.get("holdingsJson"))
    if not holdings:
        return None
    return {
        "productType": "MUTUAL_FUND",
        "symbol": details.get("schemeCode") or details.get("isin"),
        "name": details.get("schemeName"),
        "isin": details.get("isin"),
        "assetClass": details.get("sebiCategory") or details.get("schemeType"),
        "categoryLabel": details.get("sebiSubCategory") or details.get("sebiCategory"),
        "holdings": holdings,
        "holdingsCount": len(holdings),
        "message": None,
    }
