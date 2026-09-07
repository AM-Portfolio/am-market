"""v2 Funds API request/response contracts (OpenAPI models)."""
from __future__ import annotations

from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, ConfigDict, Field


class ProductType(str, Enum):
    ETF = "ETF"
    MUTUAL_FUND = "MUTUAL_FUND"


class FundHoldingsLookupRequest(BaseModel):
    """POST /v2/funds/holdings"""

    items: List[str] = Field(
        ...,
        min_length=1,
        description="Symbols, ISINs, or name queries",
        examples=[["NIFTYBEES", "BANKBEES"]],
    )
    product_types: Optional[List[ProductType]] = Field(
        default=None,
        alias="productTypes",
        description="Defaults to [ETF] when omitted",
    )

    model_config = ConfigDict(populate_by_name=True)


class FundPerformanceBatchRequest(BaseModel):
    """POST /v2/funds/performance/batch and /refresh"""

    items: List[str] = Field(..., min_length=1, examples=[["NIFTYBEES"]])
    product_types: Optional[List[ProductType]] = Field(
        default=None,
        alias="productTypes",
    )

    model_config = ConfigDict(populate_by_name=True)


class FundSummary(BaseModel):
    product_type: ProductType = Field(alias="productType")
    symbol: Optional[str] = None
    name: Optional[str] = None
    isin: Optional[str] = None
    asset_class: Optional[str] = Field(default=None, alias="assetClass")
    category_label: Optional[str] = Field(default=None, alias="categoryLabel")
    return_1y: Optional[float] = Field(default=None, alias="return1Y")
    return_3y: Optional[float] = Field(default=None, alias="return3Y")
    return_5y: Optional[float] = Field(default=None, alias="return5Y")
    returns_as_of: Optional[str] = Field(default=None, alias="returnsAsOf")
    sparkline_closes: Optional[List[float]] = Field(
        default=None, alias="sparklineCloses"
    )

    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)


class FundDocument(FundSummary):
    holdings: Optional[List[Dict[str, Any]]] = None
    holdings_count: Optional[int] = Field(default=None, alias="holdingsCount")
    message: Optional[str] = None

    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)


class FundHoldingsLookupResponse(BaseModel):
    items: List[str]
    total_found: int = Field(alias="totalFound")
    funds: List[FundDocument] = Field(default_factory=list)
    not_found: List[str] = Field(default_factory=list, alias="notFound")

    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)


class FundSearchResponse(BaseModel):
    query: str
    total_found: int = Field(alias="totalFound")
    funds: List[FundSummary] = Field(default_factory=list)

    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)


class FundPerformanceResult(BaseModel):
    product_type: ProductType = Field(alias="productType")
    symbol: Optional[str] = None
    return_1y: Optional[float] = Field(default=None, alias="return1Y")
    return_3y: Optional[float] = Field(default=None, alias="return3Y")
    return_5y: Optional[float] = Field(default=None, alias="return5Y")
    returns_as_of: Optional[str] = Field(default=None, alias="returnsAsOf")
    sparkline_closes: Optional[List[float]] = Field(
        default=None, alias="sparklineCloses"
    )

    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)


class FundPerformanceBatchResponse(BaseModel):
    items: List[str]
    results: List[FundPerformanceResult] = Field(default_factory=list)

    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)
