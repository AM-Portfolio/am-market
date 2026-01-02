import pandas as pd

from fastapi import APIRouter, HTTPException
from app.models.schemas import AnalysisRequest, AnalysisResponse
from app.services.market_data_client import MarketDataClient

router = APIRouter()
market_data_client = MarketDataClient()

@router.post("/analyze", response_model=AnalysisResponse)
async def analyze_data(request: AnalysisRequest):
    try:
        print(f"\n[AnalyzeEndpoint] ===== New Analysis Request =====")
        print(f"[AnalyzeEndpoint] Symbol: {request.symbol}")
        print(f"[AnalyzeEndpoint] Timeframe: {request.timeframe}")
        print(f"[AnalyzeEndpoint] Indicators: {[ind.kind for ind in request.indicators]}")
        
        # Mock implementation to verify connectivity
        # pandas_ta installation is failing in this environment
        
        # 1. Fetch Historical Data (still verifying this integration works!)
        try:
             df = market_data_client.get_historical_data(request.symbol, request.timeframe, days_back=365)
             print(f"[AnalyzeEndpoint] Historical data shape: {df.shape if not df.empty else 'empty'}")
        except Exception as e:
             print(f"Fetch failed: {e}")
             # Proceed with mock data even if fetch fails, to prove endpoint reachability
             pass

        results = {}
        # Mock results
        import random
        for ind in request.indicators:
            kind = ind.kind.lower()
            # Generate 100 dummy points
            results[kind] = [random.uniform(100, 200) for _ in range(100)]
        
        print(f"[AnalyzeEndpoint] Generated results for {len(results)} indicators")
        print(f"[AnalyzeEndpoint] ===== Request Complete =====\n")
            
        return AnalysisResponse(symbol=request.symbol, results=results)

    except Exception as e:
        print(f"[AnalyzeEndpoint] ERROR: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Analysis failed: {str(e)}")
