"""ETF API request/response contracts (OpenAPI models)."""
from typing import List, Optional

from pydantic import BaseModel, Field


class EtfSummary(BaseModel):
    symbol: Optional[str] = None
    name: Optional[str] = None
    isin: Optional[str] = None
    asset_class: Optional[str] = None
    market_cap_category: Optional[str] = None


class EtfSearchResponse(BaseModel):
    query: str
    total_found: int
    etfs: List[EtfSummary] = Field(default_factory=list)


class EtfHoldingsLookupRequest(BaseModel):
    """POST /v1/etf/holdings — symbols, ISINs, and/or name queries; ETFs are merged and deduped."""

    items: List[str] = Field(
        ...,
        min_length=1,
        description=(
            "Symbols, ISINs, or name queries. Overlapping entries are merged "
            "(e.g. IT + Nifty IT returns each Nifty IT ETF once)."
        ),
        examples=[["Nifty IT"], ["IT", "NIFITETF", "ITBEES"]],
    )


class EtfHoldingsLookupResponse(BaseModel):
    items: List[str]
    total_found: int
    etfs: List[dict] = Field(default_factory=list)
    not_found: List[str] = Field(default_factory=list)
