"""
ETF Holdings API Endpoints
Handles ETF holdings fetching and management
"""

from fastapi import APIRouter, HTTPException, status, BackgroundTasks, Query, File, UploadFile
from fastapi.responses import JSONResponse
from typing import Optional, List
import asyncio
import json
from datetime import datetime, timedelta
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from am_common.job_models import (
    JobResponse, JobStatusResponse, BackgroundJob, JobStatus, JobType
)
from am_services.job_queue_service import get_job_queue
from am_etf.service import ETFService
from am_etf.holdings_service import ETFHoldingsService
from am_etf.smart_holdings_service import SmartETFHoldingsService



# Removing /etf prefix to align with global /v1 structure (will be included with /v1 in api.py)
router = APIRouter(tags=["ETF Holdings"])

# Simple in-memory cache for ETF list
_etf_list_cache = {
    "data": [],
    "last_updated": None
}
CACHE_DURATION_MINUTES = 60

@router.post("/fetch-all-holdings", response_model=JobResponse)
async def fetch_all_etf_holdings(
    callback_url: Optional[str] = None,
    user_id: Optional[str] = None,
    limit: Optional[int] = Query(default=None, description="Limit number of ETFs to process"),
    force_refresh: bool = Query(default=False, description="Force refresh even if data exists for today")
):
    """
    Fetch holdings for all ETFs with ISINs from moneycontrol API
    Returns immediately with job ID, processes in background
    Smart caching: Only fetches if data is missing or stale
    """
    try:
        # Initialize services
        etf_service = ETFService()
        job_queue = await get_job_queue()
        
        # Get count of ETFs with ISINs
        all_etfs = await etf_service.list(limit=1000)
        etfs_with_isin = [etf for etf in all_etfs if etf.isin]
        
        if limit:
            etfs_with_isin = etfs_with_isin[:limit]
        
        total_count = len(etfs_with_isin)
        
        if total_count == 0:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="No ETFs with ISIN found in database"
            )
        
        print(f"🚀 Starting async ETF holdings fetch for {total_count} ETFs (force_refresh={force_refresh})")
        
        # Validate webhook URL if provided
        normalized_callback = None
        callback_note = None
        if callback_url:
            cb = callback_url.strip()
            if cb.startswith("http://") or cb.startswith("https://"):
                normalized_callback = cb
            else:
                callback_note = "Ignoring invalid callback_url (missing http/https)."
        
        # Create background job for holdings fetching
        job_input = {
            "etf_count": total_count,
            "limit": limit,
            "force_refresh": force_refresh,
            "operation": "fetch_all_holdings"
        }
        
        job_id = await job_queue.create_job(
            job_type=JobType.ETF_HOLDINGS_FETCH,
            input_data=job_input,
            callback_url=normalized_callback,
            user_id=user_id
        )
        
        # Estimate completion time (smart: less time if cache hits expected)
        estimated_api_calls = total_count if force_refresh else int(total_count * 0.3)  # Assume 70% cache hit rate
        estimated_minutes = (estimated_api_calls * 2) / 60  # Convert seconds to minutes
        estimated_completion = datetime.now() + timedelta(minutes=estimated_minutes)
        
        await etf_service.close()
        
        message = f"Started smart fetching holdings for {total_count} ETFs in background."
        if not force_refresh:
            message += " Using cache for recently fetched data."
        
        resp = JobResponse(
            job_id=job_id,
            status=JobStatus.PENDING,
            message=message,
            estimated_completion_time=estimated_completion.strftime("%Y-%m-%d %H:%M:%S"),
            status_url=f"/jobs/{job_id}/status",
            webhook_url=normalized_callback
        )
        
        # If invalid callback provided, include an extra hint field in response
        if callback_note:
            return JSONResponse(status_code=200, content={
                **resp.dict(),
                "note": callback_note
            })
        return resp
        
    except Exception as e:
        print(f"❌ ETF holdings fetch error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to start ETF holdings fetch: {str(e)}"
        )


@router.get("/search")
async def search_etfs(
    query: str = Query(..., description="Search by symbol, name, or ISIN"),
    limit: int = Query(default=10, description="Maximum results to return")
):
    """
    Search ETFs by symbol, name, or ISIN
    """
    try:
        global _etf_list_cache
        
        # Check cache validity
        cache_valid = False
        if _etf_list_cache["data"] and _etf_list_cache["last_updated"]:
            age = datetime.now() - _etf_list_cache["last_updated"]
            if age < timedelta(minutes=CACHE_DURATION_MINUTES):
                cache_valid = True
        
        all_etfs = []
        if cache_valid:
            all_etfs = _etf_list_cache["data"]
            # print("✅ Using cached ETF list")
        else:
            # print("🔄 Refreshing ETF list cache")
            etf_service = ETFService()
            all_etfs = await etf_service.list(limit=2000) # Increased limit for full coverage
            await etf_service.close()
            
            # Update cache
            _etf_list_cache["data"] = all_etfs
            _etf_list_cache["last_updated"] = datetime.now()
        
        query_lower = query.lower()
        matching_etfs = []
        
        # If query is empty or "*", return all (up to limit)
        if not query or query == "*":
             for etf in all_etfs:
                matching_etfs.append({
                    "symbol": etf.symbol,
                    "name": etf.name,
                    "isin": etf.isin,
                    "asset_class": etf.asset_class,
                    "market_cap_category": etf.market_cap_category
                })
                if len(matching_etfs) >= limit:
                    break
        else:
            for etf in all_etfs:
                # Check if query matches symbol, name, or ISIN
                if (
                    (etf.symbol and query_lower in etf.symbol.lower()) or
                    (etf.name and query_lower in etf.name.lower()) or
                    (etf.isin and query_lower in etf.isin.lower())
                ):
                    matching_etfs.append({
                        "symbol": etf.symbol,
                        "name": etf.name,
                        "isin": etf.isin,
                        "asset_class": etf.asset_class,
                        "market_cap_category": etf.market_cap_category
                    })
                    
                    if len(matching_etfs) >= limit:
                        break
        
        return {
            "query": query,
            "total_found": len(matching_etfs),
            "etfs": matching_etfs
        }

        
    except Exception as e:
        print(f"❌ ETF search error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to search ETFs: {str(e)}"
        )


@router.post("/fetch-holdings/{symbol}")
async def fetch_holdings_for_etf(
    symbol: str,
    callback_url: Optional[str] = None,
    user_id: Optional[str] = None
):
    """
    Fetch holdings for a specific ETF by symbol
    Returns immediately with job ID, processes in background
    """
    try:
        # Initialize services
        etf_service = ETFService()
        job_queue = await get_job_queue()
        
        # Find ETF by symbol
        etf = await etf_service.get_by_symbol(symbol)
        
        if not etf:
            await etf_service.close()
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"ETF not found: {symbol}"
            )
        
        if not etf.isin:
            await etf_service.close()
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"ETF {symbol} does not have an ISIN"
            )
        
        print(f"🚀 Starting async holdings fetch for ETF {symbol} (ISIN: {etf.isin})")
        
        # Validate webhook URL if provided
        normalized_callback = None
        callback_note = None
        if callback_url:
            cb = callback_url.strip()
            if cb.startswith("http://") or cb.startswith("https://"):
                normalized_callback = cb
            else:
                callback_note = "Ignoring invalid callback_url (missing http/https)."
        
        # Create background job for single ETF holdings fetching
        job_input = {
            "symbol": symbol,
            "isin": etf.isin,
            "etf_name": etf.name,
            "operation": "fetch_single_holdings"
        }
        
        job_id = await job_queue.create_job(
            job_type=JobType.ETF_HOLDINGS_FETCH,
            input_data=job_input,
            callback_url=normalized_callback,
            user_id=user_id
        )
        
        # Estimate completion time (2 seconds for single ETF)
        estimated_completion = datetime.now() + timedelta(seconds=5)
        
        await etf_service.close()
        
        resp = JobResponse(
            job_id=job_id,
            status=JobStatus.PENDING,
            message=f"Started fetching holdings for ETF {symbol} in background.",
            estimated_completion_time=estimated_completion.strftime("%Y-%m-%d %H:%M:%S"),
            status_url=f"/jobs/{job_id}/status",
            webhook_url=normalized_callback
        )
        
        # If invalid callback provided, include an extra hint field in response
        if callback_note:
            return JSONResponse(status_code=200, content={
                **resp.dict(),
                "note": callback_note
            })
        return resp
        
    except Exception as e:
        print(f"❌ ETF holdings fetch error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to start ETF holdings fetch: {str(e)}"
        )


@router.get("/holdings/{symbol}")
async def get_etf_holdings(symbol: str):
    """
    Get stored holdings for a specific ETF
    """
    try:
        # First get ETF info
        etf_service = ETFService()
        etf = await etf_service.get_by_symbol(symbol)
        
        if not etf:
            await etf_service.close()
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"ETF not found: {symbol}"
            )
        
        # Then get holdings from dedicated collection
        holdings_service = ETFHoldingsService()
        holdings_data = await holdings_service.get_holdings_by_isin(etf.isin) if etf.isin else None
        
        await etf_service.close()
        await holdings_service.close()
        
        response = {
            "symbol": etf.symbol,
            "name": etf.name,
            "isin": etf.isin,
            "asset_class": etf.asset_class,
            "market_cap_category": etf.market_cap_category
        }
        
        if holdings_data:
            response.update({
                "holdings_count": holdings_data.total_holdings,
                "holdings_fetched_at": holdings_data.fetched_at.isoformat() if holdings_data.fetched_at else None,
                "holdings": [
                    {
                        "stock_name": holding.stock_name,
                        "isin_code": holding.isin_code,
                        "percentage": holding.percentage,
                        "market_value": holding.market_value,
                        "quantity": holding.quantity
                    } for holding in holdings_data.holdings
                ]
            })
        else:
            response["holdings"] = None
            response["message"] = "No holdings data available"
        
        return response
        
    except Exception as e:
        print(f"❌ Get ETF holdings error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get ETF holdings: {str(e)}"
        )


@router.get("/cache-stats")
async def get_cache_statistics():
    """
    Get ETF holdings cache statistics
    """
    try:
        holdings_service = SmartETFHoldingsService()
        cache_stats = await holdings_service.get_cache_statistics()
        
        await holdings_service.close()
        
        return {
            "cache_statistics": cache_stats,
            "description": {
                "total_cached_records": "Total ETFs with holdings data stored",
                "fresh_records_today": "ETFs with data fetched today",
                "stale_records": "ETFs with old data that need refresh",
                "cache_hit_potential": "Percentage of requests that could use cache"
            }
        }
        
    except Exception as e:
        print(f"❌ Get cache stats error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get cache statistics: {str(e)}"
        )


@router.get("/stats")
async def get_etf_stats():
    """
    Get ETF database statistics
    """
    try:
        etf_service = ETFService()
        holdings_service = ETFHoldingsService()
        
        # Get ETF stats
        all_etfs = await etf_service.list(limit=1000)
        etfs_with_isin = [etf for etf in all_etfs if etf.isin]
        etfs_with_holdings = await etf_service.get_etfs_with_holdings(limit=1000)
        
        # Get holdings stats
        holdings_stats = await holdings_service.get_holdings_stats()
        all_holdings = await holdings_service.list_all_holdings(limit=1000)
        
        await etf_service.close()
        await holdings_service.close()
        
        return {
            "etf_collection": {
                "total_etfs": len(all_etfs),
                "etfs_with_isin": len(etfs_with_isin),
                "etfs_with_embedded_holdings": len(etfs_with_holdings)
            },
            "holdings_collection": {
                "total_holdings_records": holdings_stats["total_etfs_with_holdings"],
                "collection_name": holdings_stats["collection_name"]
            },
            "coverage": {
                "isin_coverage": f"{(len(etfs_with_isin) / len(all_etfs) * 100):.1f}%" if all_etfs else "0%",
                "holdings_coverage": f"{(len(all_holdings) / len(etfs_with_isin) * 100):.1f}%" if etfs_with_isin else "0%"
            }
        }
        
    except Exception as e:
        print(f"❌ Get ETF stats error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get ETF stats: {str(e)}"
        )


@router.post("/load-from-json")
async def load_etfs_from_json(
    file: UploadFile = File(..., description="ETF details JSON file"),
    dry_run: bool = Query(default=False, description="Validate only, don't persist")
):
    """
    Load ETF data from JSON file
    Accepts etf_details.json and loads all ETFs into database
    """
    try:
        from am_etf.models import ETFInstrument
        
        # Read and parse JSON
        content = await file.read()
        data = json.loads(content)
        
        if not isinstance(data, list):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Expected a JSON array of ETF records"
            )
        
        # Parse ETF instruments
        instruments = []
        errors = []
        for i, rec in enumerate(data):
            try:
                instruments.append(ETFInstrument(**rec))
            except Exception as e:
                errors.append(f"Record {i}: {str(e)}")
        
        print(f"📊 Parsed {len(instruments)} ETF instruments from {len(data)} records")
        
        if dry_run:
            return {
                "status": "validated",
                "total_records": len(data),
                "valid_instruments": len(instruments),
                "errors": errors[:10] if errors else [],  # First 10 errors
                "message": "Dry run: not persisted to database"
            }
        
        # Save to database
        etf_service = ETFService()
        inserted_count = await etf_service.bulk_upsert(instruments)
        await etf_service.close()
        
        return {
            "status": "success",
            "total_records": len(data),
            "valid_instruments": len(instruments),
            "inserted_count": inserted_count,
            "errors": errors[:10] if errors else [],
            "message": f"Successfully loaded {inserted_count} ETFs into database"
        }
        
    except json.JSONDecodeError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid JSON: {str(e)}"
        )
    except Exception as e:
        print(f"❌ ETF load error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to load ETFs: {str(e)}"
        )