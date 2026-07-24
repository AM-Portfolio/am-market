"""ETF API request/response contracts (OpenAPI models)."""
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field, ConfigDict


class EtfSummary(BaseModel):
    symbol: Optional[str] = Field(default=None, description="ETF trading symbol", examples=["NIFTYBEES"])
    name: Optional[str] = Field(default=None, description="Full ETF name", examples=["Nippon India ETF Nifty BeES"])
    isin: Optional[str] = Field(default=None, description="ISIN identifier", examples=["INF204KB14I2"])
    asset_class: Optional[str] = Field(default=None, description="Asset class category", examples=["Equity"])
    market_cap_category: Optional[str] = Field(default=None, description="Market cap classification", examples=["Large Cap"])


class EtfSearchResponse(BaseModel):
    query: str = Field(..., description="Search query string", examples=["Nifty IT"])
    total_found: int = Field(..., description="Total matching ETFs found", examples=[2])
    etfs: List[EtfSummary] = Field(default_factory=list, description="List of matching ETF summaries")


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

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "items": ["NIFTY IT", "ITBEES"]
            }
        }
    )


class EtfHoldingsLookupResponse(BaseModel):
    items: List[str] = Field(..., description="Requested lookup items", examples=[["NIFTY IT"]])
    total_found: int = Field(..., description="Total unique ETFs retrieved", examples=[1])
    etfs: List[Dict[str, Any]] = Field(default_factory=list, description="List of ETF holdings documents")
    not_found: List[str] = Field(default_factory=list, description="Items that failed to resolve", examples=[[]])

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "items": ["NIFTY IT"],
                "total_found": 1,
                "etfs": [
                    {
                        "symbol": "ITBEES",
                        "name": "Nippon India ETF Nifty IT",
                        "isin": "INF204KB14I2",
                        "holdings": [
                            {"company_name": "Tata Consultancy Services Ltd", "weight": 26.5},
                            {"company_name": "Infosys Ltd", "weight": 24.1}
                        ]
                    }
                ],
                "not_found": []
            }
        }
    )
