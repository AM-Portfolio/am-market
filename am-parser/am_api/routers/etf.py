"""ETF HTTP routes — thin layer over domain services."""
import json
from datetime import datetime, timedelta
from typing import Optional

from fastapi import APIRouter, Depends, File, HTTPException, Query, UploadFile, status
from fastapi.responses import JSONResponse

from am_api.deps import (
    get_etf_lookup_service,
    get_etf_service,
    get_holdings_service,
    get_smart_holdings_service,
)
from am_api.schemas.etf import EtfHoldingsLookupRequest, EtfHoldingsLookupResponse
from am_common.job_models import JobResponse, JobStatus, JobType
from am_common.logging.request_logging import get_logger
from am_common.webhooks import normalize_callback_url
from am_configs.settings import get_mongo_target_label
from am_etf.holdings_service import ETFHoldingsService
from am_etf.lookup_service import EtfLookupService
from am_etf.models import ETFInstrument
from am_etf.service import ETFService
from am_etf.smart_holdings_service import SmartETFHoldingsService
from am_services.job_queue_service import get_job_queue

log = get_logger("etf_api")
router = APIRouter(tags=["ETF Holdings"])

DEPRECATED_HOLDINGS_HEADERS = {
    "Deprecation": "true",
    "Link": '</v1/etf/holdings>; rel="successor-version"',
}


@router.post("/holdings", response_model=EtfHoldingsLookupResponse)
async def post_etf_holdings_lookup(
    body: EtfHoldingsLookupRequest,
    lookup: EtfLookupService = Depends(get_etf_lookup_service),
):
    log.info("POST holdings lookup: items=%s", body.items)
    try:
        return await lookup.lookup_holdings(body.items)
    except Exception as e:
        log.exception("POST holdings lookup failed: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to lookup ETF holdings: {str(e)}",
        )


@router.get("/search")
async def search_etfs(
    query: str = Query(..., description="Search by symbol, name, or ISIN"),
    limit: int = Query(default=10, description="Maximum results to return"),
    lookup: EtfLookupService = Depends(get_etf_lookup_service),
):
    try:
        log.info("search: query=%r limit=%s", query, limit)
        return await lookup.search(query, limit)
    except Exception as e:
        log.exception("search failed: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to search ETFs: {str(e)}",
        )


@router.post("/fetch-all-holdings", response_model=JobResponse)
async def fetch_all_etf_holdings(
    callback_url: Optional[str] = None,
    user_id: Optional[str] = None,
    limit: Optional[int] = Query(default=None),
    force_refresh: bool = Query(default=False),
    etf_service: ETFService = Depends(get_etf_service),
):
    try:
        job_queue = await get_job_queue()
        all_etfs = await etf_service.list(limit=1000)
        etfs_with_isin = [etf for etf in all_etfs if etf.isin]
        if limit:
            etfs_with_isin = etfs_with_isin[:limit]
        total_count = len(etfs_with_isin)
        if total_count == 0:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="No ETFs with ISIN found in database",
            )

        normalized_callback, callback_note = normalize_callback_url(callback_url)
        job_id = await job_queue.create_job(
            job_type=JobType.ETF_HOLDINGS_FETCH,
            input_data={
                "etf_count": total_count,
                "limit": limit,
                "force_refresh": force_refresh,
                "operation": "fetch_all_holdings",
            },
            callback_url=normalized_callback,
            user_id=user_id,
        )
        estimated_api_calls = (
            total_count if force_refresh else int(total_count * 0.3)
        )
        estimated_completion = datetime.now() + timedelta(
            minutes=(estimated_api_calls * 2) / 60
        )
        message = f"Started smart fetching holdings for {total_count} ETFs in background."
        if not force_refresh:
            message += " Using cache for recently fetched data."
        resp = JobResponse(
            job_id=job_id,
            status=JobStatus.PENDING,
            message=message,
            estimated_completion_time=estimated_completion.strftime(
                "%Y-%m-%d %H:%M:%S"
            ),
            status_url=f"/jobs/{job_id}/status",
            webhook_url=normalized_callback,
        )
        if callback_note:
            return JSONResponse(status_code=200, content={**resp.dict(), "note": callback_note})
        return resp
    except HTTPException:
        raise
    except Exception as e:
        log.exception("fetch-all-holdings failed: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to start ETF holdings fetch: {str(e)}",
        )


@router.get("/holdings/bulk")
async def bulk_fetch_etf_holdings(
    limit: Optional[int] = Query(default=None),
    etf_service: ETFService = Depends(get_etf_service),
    holdings_service: ETFHoldingsService = Depends(get_holdings_service),
):
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
                "symbol": etf.symbol,
                "name": etf.name,
                "holdings": compressed_holdings,
            }
            count += 1
        return {"total_etfs": len(result_map), "data": result_map}
    except Exception as e:
        log.exception("bulk fetch failed: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to bulk fetch ETF holdings: {str(e)}",
        )


@router.post("/fetch-holdings/{symbol}")
async def fetch_holdings_for_etf(
    symbol: str,
    callback_url: Optional[str] = None,
    user_id: Optional[str] = None,
    etf_service: ETFService = Depends(get_etf_service),
):
    try:
        job_queue = await get_job_queue()
        etf = await etf_service.get_by_symbol(symbol)
        if not etf:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"ETF not found: {symbol}",
            )
        if not etf.isin:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"ETF {symbol} does not have an ISIN",
            )
        normalized_callback, callback_note = normalize_callback_url(callback_url)
        job_id = await job_queue.create_job(
            job_type=JobType.ETF_HOLDINGS_FETCH,
            input_data={
                "symbol": symbol,
                "isin": etf.isin,
                "etf_name": etf.name,
                "operation": "fetch_single_holdings",
            },
            callback_url=normalized_callback,
            user_id=user_id,
        )
        resp = JobResponse(
            job_id=job_id,
            status=JobStatus.PENDING,
            message=f"Started fetching holdings for ETF {symbol} in background.",
            estimated_completion_time=(
                datetime.now() + timedelta(seconds=5)
            ).strftime("%Y-%m-%d %H:%M:%S"),
            status_url=f"/jobs/{job_id}/status",
            webhook_url=normalized_callback,
        )
        if callback_note:
            return JSONResponse(status_code=200, content={**resp.dict(), "note": callback_note})
        return resp
    except HTTPException:
        raise
    except Exception as e:
        log.exception("fetch-holdings failed: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to start ETF holdings fetch: {str(e)}",
        )


@router.get("/holdings/by-query", deprecated=True)
async def get_etf_holdings_by_query(
    query: str = Query(...),
    lookup: EtfLookupService = Depends(get_etf_lookup_service),
):
    response = await lookup.lookup_holdings([query])
    if response.total_found == 0:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"No ETF found for query: {query}",
        )
    body = (
        response.etfs[0]
        if response.total_found == 1
        else response.model_dump()
    )
    return JSONResponse(content=body, headers=DEPRECATED_HOLDINGS_HEADERS)


@router.get("/holdings/{symbol_or_isin}", deprecated=True)
async def get_etf_holdings(
    symbol_or_isin: str,
    lookup: EtfLookupService = Depends(get_etf_lookup_service),
):
    items = (
        [s.strip() for s in symbol_or_isin.split(",") if s.strip()]
        if "," in symbol_or_isin
        else [symbol_or_isin]
    )
    response = await lookup.lookup_holdings(items)
    if response.total_found == 0:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"ETF not found: {symbol_or_isin}",
        )
    body = (
        response.etfs[0]
        if len(items) == 1 and response.total_found == 1
        else response.model_dump()
    )
    return JSONResponse(content=body, headers=DEPRECATED_HOLDINGS_HEADERS)


@router.get("/cache-stats")
async def get_cache_statistics(
    smart: SmartETFHoldingsService = Depends(get_smart_holdings_service),
):
    try:
        return {
            "cache_statistics": await smart.get_cache_statistics(),
            "description": {
                "total_cached_records": "Total ETFs with holdings data stored",
                "fresh_records_today": "ETFs with data fetched today",
                "stale_records": "ETFs with old data that need refresh",
                "cache_hit_potential": "Percentage of requests that could use cache",
            },
        }
    except Exception as e:
        log.exception("cache-stats failed: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get cache statistics: {str(e)}",
        )


@router.get("/stats")
async def get_etf_stats(
    etf_service: ETFService = Depends(get_etf_service),
    holdings_service: ETFHoldingsService = Depends(get_holdings_service),
):
    try:
        all_etfs = await etf_service.list(limit=1000)
        etfs_with_isin = [etf for etf in all_etfs if etf.isin]
        etfs_with_holdings = await etf_service.get_etfs_with_holdings(limit=1000)
        holdings_stats = await holdings_service.get_holdings_stats()
        all_holdings = await holdings_service.list_all_holdings(limit=1000)
        return {
            "etf_collection": {
                "total_etfs": len(all_etfs),
                "etfs_with_isin": len(etfs_with_isin),
                "etfs_with_embedded_holdings": len(etfs_with_holdings),
            },
            "holdings_collection": {
                "total_holdings_records": holdings_stats["total_etfs_with_holdings"],
                "collection_name": holdings_stats["collection_name"],
            },
            "coverage": {
                "isin_coverage": (
                    f"{(len(etfs_with_isin) / len(all_etfs) * 100):.1f}%"
                    if all_etfs
                    else "0%"
                ),
                "holdings_coverage": (
                    f"{(len(all_holdings) / len(etfs_with_isin) * 100):.1f}%"
                    if etfs_with_isin
                    else "0%"
                ),
            },
        }
    except Exception as e:
        log.exception("stats failed: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get ETF stats: {str(e)}",
        )


@router.post("/load-from-json")
async def load_etfs_from_json(
    file: UploadFile = File(...),
    dry_run: bool = Query(default=False),
    etf_service: ETFService = Depends(get_etf_service),
):
    try:
        log.info("load-from-json: filename=%s dry_run=%s", file.filename, dry_run)
        content = await file.read()
        data = json.loads(content)
        if not isinstance(data, list):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Expected a JSON array of ETF records",
            )
        instruments = []
        errors = []
        for i, rec in enumerate(data):
            try:
                instruments.append(ETFInstrument(**rec))
            except Exception as e:
                errors.append(f"Record {i}: {str(e)}")
        if dry_run:
            return {
                "status": "validated",
                "total_records": len(data),
                "valid_instruments": len(instruments),
                "errors": errors[:10] if errors else [],
                "message": "Dry run: not persisted to database",
            }
        log.info("load-from-json: mongo_target=%s", get_mongo_target_label())
        inserted_count = await etf_service.bulk_upsert(instruments)
        return {
            "status": "success",
            "total_records": len(data),
            "valid_instruments": len(instruments),
            "inserted_count": inserted_count,
            "errors": errors[:10] if errors else [],
            "message": f"Successfully loaded {inserted_count} ETFs into database",
        }
    except json.JSONDecodeError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid JSON: {str(e)}",
        )
    except HTTPException:
        raise
    except Exception as e:
        log.exception("load-from-json failed: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to load ETFs: {str(e)}",
        )
