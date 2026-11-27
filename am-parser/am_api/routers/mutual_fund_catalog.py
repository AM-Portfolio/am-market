"""Public MF catalog routes — EOD NAV from Postgres mf_catalog (mesh-public like /v1/etf)."""

from __future__ import annotations

from datetime import date
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel, Field

from am_api.deps import get_mf_catalog_service
from am_common.logging.request_logging import get_logger
from am_mf_catalog.service import MfCatalogService

log = get_logger("mf_catalog_api")
router = APIRouter(tags=["Mutual Fund Catalog"])


class NavBatchRequest(BaseModel):
    isins: list[str] = Field(default_factory=list, max_length=200)


@router.get("/search")
def search_schemes(
    q: Optional[str] = Query(None, description="Name / scheme code / ISIN substring"),
    amc: Optional[str] = Query(None),
    purchaseAllowed: Optional[str] = Query(None, alias="purchaseAllowed"),
    limit: int = Query(50, ge=1, le=200),
    catalog: MfCatalogService = Depends(get_mf_catalog_service),
):
    result = catalog.search_schemes(q, amc, purchaseAllowed, limit)
    if result.get("error") == "Mutual fund catalog not configured":
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=result,
        )
    if result.get("error"):
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=result,
        )
    return result


@router.post("/nav/batch")
def nav_batch(
    body: NavBatchRequest,
    catalog: MfCatalogService = Depends(get_mf_catalog_service),
):
    """Bulk latest + previous EOD NAV for portfolio MF books (one round trip, one mark per ISIN)."""
    result = catalog.get_nav_batch(body.isins)
    if result.get("error") == "Mutual fund catalog not configured":
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=result,
        )
    if result.get("error"):
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=result,
        )
    return result


@router.get("/{scheme_id}/history")
def scheme_history(
    scheme_id: str,
    from_: date = Query(..., alias="from"),
    to: date = Query(...),
    catalog: MfCatalogService = Depends(get_mf_catalog_service),
):
    if not catalog.is_configured():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={"error": "Mutual fund catalog not configured"},
        )
    if from_ > to:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": "from must be <= to"},
        )
    result = catalog.get_nav_history(scheme_id, from_, to)
    if result is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": "History unavailable", "schemeId": scheme_id},
        )
    return result


@router.get("/{scheme_id}/performance")
def scheme_performance(
    scheme_id: str,
    catalog: MfCatalogService = Depends(get_mf_catalog_service),
):
    if not catalog.is_configured():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={"error": "Mutual fund catalog not configured"},
        )
    result = catalog.get_performance(scheme_id)
    if result is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": "Scheme not found", "schemeId": scheme_id},
        )
    return result


@router.get("/{scheme_id}")
def scheme_details(
    scheme_id: str,
    catalog: MfCatalogService = Depends(get_mf_catalog_service),
):
    if not catalog.is_configured():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={"error": "Mutual fund catalog not configured"},
        )
    result = catalog.get_scheme_details(scheme_id)
    if result is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": "Scheme not found", "schemeId": scheme_id},
        )
    return result
