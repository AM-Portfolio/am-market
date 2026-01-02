"""
Mutual Fund Models - Data models for mutual fund portfolio data
"""
from typing import List, Optional
from pydantic import BaseModel, Field
from datetime import datetime


class Holding(BaseModel):
    """Individual holding in a mutual fund portfolio"""
    name_of_instrument: str = Field(..., description="Name of the security/instrument")
    isin_code: str = Field(..., description="ISIN code of the security")
    percentage_to_nav: str = Field(..., description="Percentage allocation to NAV")
    
    class Config:
        json_schema_extra = {
            "example": {
                "name_of_instrument": "Multi Commodity Exchange of India Limited",
                "isin_code": "INE745G01035",
                "percentage_to_nav": "0.0159%"
            }
        }


class MutualFundPortfolio(BaseModel):
    """Complete mutual fund portfolio data"""
    mutual_fund_name: str = Field(..., description="Name of the mutual fund")
    portfolio_date: str = Field(..., description="Portfolio date (e.g., 'March 2025')")
    total_holdings: int = Field(..., description="Total number of holdings")
    portfolio_holdings: List[Holding] = Field(..., description="List of all holdings")
    
    # Metadata fields
    created_at: Optional[datetime] = Field(default_factory=datetime.now)
    updated_at: Optional[datetime] = Field(default_factory=datetime.now)
    
    class Config:
        json_schema_extra = {
            "example": {
                "mutual_fund_name": "Motilal Oswal Nifty Smallcap 250 Index Fund",
                "portfolio_date": "March 2025",
                "total_holdings": 250,
                "portfolio_holdings": []
            }
        }
    
    def to_mongo_document(self) -> dict:
        """Convert to MongoDB document format"""
        doc = self.model_dump()
        
        # Convert datetime objects to ISO format for MongoDB
        if doc.get("created_at"):
            doc["created_at"] = doc["created_at"].isoformat() if isinstance(doc["created_at"], datetime) else doc["created_at"]
        if doc.get("updated_at"):
            doc["updated_at"] = doc["updated_at"].isoformat() if isinstance(doc["updated_at"], datetime) else doc["updated_at"]
            
        return doc


class PortfolioSummary(BaseModel):
    """Summary statistics for a mutual fund portfolio"""
    fund_name: str
    total_holdings: int
    portfolio_date: str
    top_holdings: List[Holding] = Field(default_factory=list, max_items=10)
    total_percentage: float = Field(default=0.0, description="Sum of all holding percentages")
    
    @classmethod
    def from_portfolio(cls, portfolio: MutualFundPortfolio, top_n: int = 10) -> "PortfolioSummary":
        """Create summary from full portfolio"""
        # Calculate total percentage (convert percentage strings to floats)
        total_pct = 0.0
        for holding in portfolio.portfolio_holdings:
            try:
                pct_value = float(holding.percentage_to_nav.rstrip('%'))
                total_pct += pct_value
            except (ValueError, AttributeError):
                continue
        
        return cls(
            fund_name=portfolio.mutual_fund_name,
            total_holdings=portfolio.total_holdings,
            portfolio_date=portfolio.portfolio_date,
            top_holdings=portfolio.portfolio_holdings[:top_n],
            total_percentage=total_pct
        )
