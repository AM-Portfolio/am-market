"""v2 Funds HTTP routes — ETF + MF-ready; performance SoT in parser."""
from __future__ import annotations

from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status

from am_api.deps import (
    get_etf_lookup_service,
    get_etf_service,
    get_fund_performance_service,
    get_holdings_service,
)
from am_api.schemas.fund_v2 import (
    FundDocument,
    FundHoldingsLookupRequest,
    FundHoldingsLookupResponse,
    FundPerformanceBatchRequest,
    FundPerformanceBatchResponse,
    FundSearchResponse,
    FundSummary,
    ProductType,
)
from am_common.logging.request_logging import get_logger
from am_etf.holdings_service import ETFHoldingsService
from am_etf.lookup_service import EtfLookupService
from am_etf.performance_service import FundPerformanceService
from am_etf.service import ETFService

log = get_logger("funds_v2_api")
router = APIRouter(tags=["Funds v2"])


def _default_product_types(
    product_types: Optional[List[ProductType]],
) -> List[ProductType]:
    if not product_types:
        return [ProductType.ETF]
    return list(product_types)


def _wants_etf(types: List[ProductType]) -> bool:
    return ProductType.ETF in types


def _wants_mf(types: List[ProductType]) -> bool:
    return ProductType.MUTUAL_FUND in types


def _category_label(etf: dict) -> Optional[str]:
    parts = []
    name = (etf.get("name") or "").strip()
    asset = (etf.get("asset_class") or "").strip()
    mcap = (etf.get("market_cap_category") or "").strip()
    if mcap:
        parts.append(mcap)
    if asset:
        parts.append(asset)
    if parts:
        return " · ".join(parts)
    return name or None


def _etf_dict_to_fund(etf: dict, perf: Optional[dict] = None) -> FundDocument:
    perf = perf or {}
    holdings = etf.get("holdings")
    holdings_count = etf.get("holdings_count")
    if holdings_count is None and isinstance(holdings, list):
        holdings_count = len(holdings)
    return FundDocument(
        productType=ProductType.ETF,
        symbol=etf.get("symbol"),
        name=etf.get("name"),
        isin=etf.get("isin"),
        assetClass=etf.get("asset_class"),
        categoryLabel=_category_label(etf),
        holdings=holdings,
        holdingsCount=holdings_count,
        message=etf.get("message"),
        return1Y=perf.get("return1Y"),
        return3Y=perf.get("return3Y"),
        return5Y=perf.get("return5Y"),
        returnsAsOf=perf.get("returnsAsOf"),
        sparklineCloses=perf.get("sparklineCloses"),
    )


@router.post("/holdings", response_model=FundHoldingsLookupResponse)
async def post_fund_holdings_lookup(
    body: FundHoldingsLookupRequest,
    lookup: EtfLookupService = Depends(get_etf_lookup_service),
    perf_svc: FundPerformanceService = Depends(get_fund_performance_service),
):
    types = _default_product_types(body.product_types)
    log.info("POST /v2/funds/holdings items=%s productTypes=%s", body.items, types)

    funds: List[FundDocument] = []
    not_found: List[str] = []

    if _wants_etf(types):
        try:
            v1 = await lookup.lookup_holdings(body.items)
        except Exception as e:
            log.exception("v2 holdings ETF lookup failed: %s", e)
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Failed to lookup fund holdings: {str(e)}",
            )
        for etf in v1.etfs:
            funds.append(_etf_dict_to_fund(etf, {}))
        not_found.extend(v1.not_found)
        symbols = [
            (etf.get("symbol") or "").strip().upper()
            for etf in v1.etfs
            if (etf.get("symbol") or "").strip()
        ]
        if symbols:
            try:
                batch = await perf_svc.get_performance_batch(symbols, force_refresh=False)
                by_sym = {
                    (r.symbol or "").upper(): r
                    for r in batch
                    if r and r.symbol
                }
                for fund in funds:
                    sym = (fund.symbol or "").upper()
                    perf = by_sym.get(sym)
                    if not perf:
                        continue
                    fund.return_1y = perf.return_1y
                    fund.return_3y = perf.return_3y
                    fund.return_5y = perf.return_5y
                    fund.returns_as_of = perf.returns_as_of
                    fund.sparkline_closes = perf.sparkline_closes
            except Exception:
                log.exception("v2 holdings performance batch failed — fail-open without returns")
    elif _wants_mf(types):
        # MF stub: accept productType, return empty with notFound
        not_found = list(
            dict.fromkeys([s.strip() for s in body.items if s and s.strip()])
        )

    return FundHoldingsLookupResponse(
        items=list(dict.fromkeys([s.strip() for s in body.items if s and s.strip()])),
        totalFound=len(funds),
        funds=funds,
        notFound=not_found,
    )


@router.get("/search", response_model=FundSearchResponse)
async def search_funds(
    query: str = Query(..., description="Search by symbol, name, or ISIN"),
    limit: int = Query(default=10, description="Maximum results"),
    product_types: Optional[List[ProductType]] = Query(
        default=None, alias="productTypes"
    ),
    lookup: EtfLookupService = Depends(get_etf_lookup_service),
):
    types = _default_product_types(product_types)
    funds: List[FundSummary] = []
    if _wants_etf(types):
        try:
            v1 = await lookup.search(query, limit)
        except Exception as e:
            log.exception("v2 search failed: %s", e)
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Failed to search funds: {str(e)}",
            )
        for s in v1.etfs:
            funds.append(
                FundSummary(
                    productType=ProductType.ETF,
                    symbol=s.symbol,
                    name=s.name,
                    isin=s.isin,
                    assetClass=s.asset_class,
                    categoryLabel=s.market_cap_category or s.asset_class,
                )
            )
    # MF stub: no search hits yet
    return FundSearchResponse(query=query, totalFound=len(funds), funds=funds)


@router.get("/holdings/bulk")
async def bulk_fund_holdings(
    limit: Optional[int] = Query(default=None),
    product_types: Optional[List[ProductType]] = Query(
        default=None, alias="productTypes"
    ),
    etf_service: ETFService = Depends(get_etf_service),
    holdings_service: ETFHoldingsService = Depends(get_holdings_service),
):
    types = _default_product_types(product_types)
    if not _wants_etf(types):
        return {"totalFunds": 0, "data": {}}
    try:
        all_etfs = await etf_service.list(limit=5000)
        etfs_with_isin = [etf for etf in all_etfs if etf.isin]
        all_holdings_docs = await holdings_service.list_all_holdings(limit=5000)
        holdings_map = {doc.isin: doc for doc in all_holdings_docs}

        result_map = {}
        count = 0
        for etf in etfs_with_isin:
            if limit and count >= limit:
                break
            if etf.isin not in holdings_map:
                continue
            doc = holdings_map[etf.isin]
            compressed_holdings = []
            if doc.holdings:
                for h in doc.holdings:
                    compressed_holdings.append(
                        {
                            "isin": h.isin_code,
                            "symbol": h.stock_name,
                            "sector": getattr(h, "sector", None) or "Unknown",
                            "weight": h.percentage,
                        }
                    )
            result_map[etf.isin] = {
                "productType": ProductType.ETF.value,
                "symbol": etf.symbol,
                "name": etf.name,
                "holdings": compressed_holdings,
            }
            count += 1
        return {"totalFunds": len(result_map), "data": result_map}
    except Exception as e:
        log.exception("v2 bulk fetch failed: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to bulk fetch fund holdings: {str(e)}",
        )


@router.post("/performance/batch", response_model=FundPerformanceBatchResponse)
async def performance_batch(
    body: FundPerformanceBatchRequest,
    lookup: EtfLookupService = Depends(get_etf_lookup_service),
    perf_svc: FundPerformanceService = Depends(get_fund_performance_service),
):
    types = _default_product_types(body.product_types)
    items = list(dict.fromkeys([s.strip() for s in body.items if s and s.strip()]))
    if not _wants_etf(types):
        return FundPerformanceBatchResponse(items=items, results=[])

    # Resolve names/ISINs to symbols when possible (fail-open on resolve miss)
    symbols: List[str] = []
    for item in items:
        try:
            resolved = await lookup.resolve_holdings_by_query(item)
        except Exception:
            resolved = None
        if resolved is None:
            symbols.append(item.upper())
            continue
        candidates = resolved if isinstance(resolved, list) else [resolved]
        for etf in candidates:
            sym = (etf.get("symbol") or item).strip().upper()
            if sym:
                symbols.append(sym)

    results = await perf_svc.get_performance_batch(symbols, force_refresh=False)
    return FundPerformanceBatchResponse(items=items, results=results)


@router.post("/performance/refresh", response_model=FundPerformanceBatchResponse)
async def performance_refresh(
    body: FundPerformanceBatchRequest,
    lookup: EtfLookupService = Depends(get_etf_lookup_service),
    perf_svc: FundPerformanceService = Depends(get_fund_performance_service),
):
    """Ops: force recompute from charts. Hard-capped to 20 items to limit abuse."""
    types = _default_product_types(body.product_types)
    items = list(dict.fromkeys([s.strip() for s in body.items if s and s.strip()]))[:20]
    if not _wants_etf(types):
        return FundPerformanceBatchResponse(items=items, results=[])

    symbols: List[str] = []
    for item in items:
        try:
            resolved = await lookup.resolve_holdings_by_query(item)
        except Exception:
            resolved = None
        if resolved is None:
            symbols.append(item.upper())
            continue
        candidates = resolved if isinstance(resolved, list) else [resolved]
        for etf in candidates:
            sym = (etf.get("symbol") or item).strip().upper()
            if sym:
                symbols.append(sym)

    results = await perf_svc.get_performance_batch(symbols[:20], force_refresh=True)
    return FundPerformanceBatchResponse(items=items, results=results)
