"""ETF domain models"""
from typing import Optional, List, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field


class ETFHolding(BaseModel):
    """Individual holding within an ETF"""
    stock_name: Optional[str] = None
    isin_code: Optional[str] = None
    percentage: Optional[float] = None
    market_value: Optional[float] = None
    quantity: Optional[int] = None
    # Store raw data for any additional fields
    raw_data: Optional[Dict[str, Any]] = None


class ETFInstrument(BaseModel):
    symbol: str = Field(..., description="Ticker / trading symbol")
    name: str = Field(..., description="ETF name")
    asset_class: Optional[str] = None
    market_cap_category: Optional[str] = None
    isin: Optional[str] = Field(None, description="ISIN code if available")
    holdings: Optional[List[ETFHolding]] = Field(default=None, description="ETF holdings data")
    holdings_fetched_at: Optional[datetime] = Field(None, description="When holdings were last fetched")
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

    def to_mongo_document(self):
        doc = self.dict()
        return doc
