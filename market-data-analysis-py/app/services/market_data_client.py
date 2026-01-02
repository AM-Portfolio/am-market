import requests
import pandas as pd
from datetime import datetime, timedelta
from typing import Dict, List, Optional
from app.core.config import settings
from app.core.auth_client import AuthClient

class MarketDataClient:
    def __init__(self):
        self.base_url = settings.MARKET_DATA_SERVICE_URL
        self.auth_client = AuthClient() # Initialize auth client

    def get_historical_data(self, symbol: str, timeframe: str = "1D", days_back: int = 365) -> pd.DataFrame:
        """
        Fetch historical data from Market Data Service and return as DataFrame.
        """
        # Calculate date range
        end_date = datetime.now()
        start_date = end_date - timedelta(days=days_back)
        
        payload = {
            "symbols": [symbol], 
            "fromDate": start_date.strftime("%Y-%m-%dT%H:%M:%SZ"), 
            "toDate": end_date.strftime("%Y-%m-%dT%H:%M:%SZ"),
            "interval": timeframe,
            "exchange": "NSE" 
        }
        
        try:
            url = f"{self.base_url}/historical-data"
            # Use auth client to get headers
            headers = self.auth_client.get_headers()
            
            print(f"[MarketDataClient] Fetching historical data:")
            print(f"  URL: {url}")
            print(f"  Symbol: {symbol}, Timeframe: {timeframe}, Days: {days_back}")
            print(f"  Payload: {payload}")
            
            response = requests.post(url, json=payload, headers=headers, timeout=10)
            response.raise_for_status()
            
            data = response.json()
            print(f"[MarketDataClient] Response status: {response.status_code}")
            print(f"[MarketDataClient] Response keys: {list(data.keys())}")
            
            # Navigate response structure:
            # { "data": { "SYMBOL": { "dataPoints": [ ... ] } }, "metadata": ..., "error": ... }
            
            if data.get("error"):
                print(f"[MarketDataClient] Error in response: {data.get('message')}")
                raise ValueError(f"Market Data Service Error: {data.get('message')}")
                
            symbol_data = data.get("data", {}).get(symbol)
            if not symbol_data:
                # Try fallback for NSE:SYMBOL format if simple symbol fails
                symbol_data = data.get("data", {}).get(f"NSE:{symbol}")
                
            if not symbol_data or "dataPoints" not in symbol_data:
                print(f"[MarketDataClient] No data found for symbol: {symbol}")
                return pd.DataFrame() # Return empty if no data
                
            points = symbol_data["dataPoints"]
            print(f"[MarketDataClient] Retrieved {len(points)} data points")
            
            # Convert to DataFrame
            df = pd.DataFrame(points)
            
            # Ensure columns match what pandas-ta expects (lowercase: open, high, low, close, volume)
            # The Java model returns: time, open, high, low, close, volume
            # We might need to rename or ensure types
            df = df.rename(columns={
                "time": "date"
            })
            
            # Set index to date
            if not df.empty:
                df["date"] = pd.to_datetime(df["date"])
                df.set_index("date", inplace=True)
                
            return df
            
        except requests.RequestException as e:
            print(f"Error fetching market data: {str(e)}")
            raise e
