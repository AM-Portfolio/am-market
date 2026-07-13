"""
Backward-compatible re-export of ETF routes.

Prefer: from am_api.routers.etf import router
"""
from am_api.routers.etf import router
from am_api.schemas.etf import EtfHoldingsLookupRequest, EtfHoldingsLookupResponse

__all__ = [
    "router",
    "EtfHoldingsLookupRequest",
    "EtfHoldingsLookupResponse",
]
