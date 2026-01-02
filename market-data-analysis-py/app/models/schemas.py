from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class IndicatorConfig(BaseModel):
    kind: str  # e.g., 'sma', 'rsi'
    length: int = 14
    extra_params: Dict[str, Any] = {}

class AnalysisRequest(BaseModel):
    symbol: str
    timeframe: str = "1D"
    exchange: str = "NSE"
    indicators: List[IndicatorConfig]

class AnalysisResponse(BaseModel):
    symbol: str
    results: Dict[str, List[Optional[float]]]
