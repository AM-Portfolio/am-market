"""ETF Holdings models - separate from main ETF data"""
from typing import List, Dict, Any, Optional
from datetime import datetime
from pydantic import BaseModel, Field


class ETFHoldingRecord(BaseModel):
    """Individual stock holding within an ETF"""
    stock_name: str = Field(..., description="Name of the stock/instrument")
    isin_code: Optional[str] = None
    percentage: Optional[float] = None
    market_value: Optional[float] = None
    quantity: Optional[int] = None
    raw_data: Optional[Dict[str, Any]] = None


class ETFHoldingsData(BaseModel):
    """Complete holdings data for one ETF"""
    isin: str = Field(..., description="ETF ISIN code")
    symbol: Optional[str] = None
    etf_name: Optional[str] = None
    holdings: List[ETFHoldingRecord] = Field(default_factory=list)
    total_holdings: int = 0
    fetched_at: datetime = Field(default_factory=datetime.utcnow)
    api_source: str = Field(default="moneycontrol", description="Source API")
    
    def to_mongo_document(self):
        doc = self.dict()
        return doc