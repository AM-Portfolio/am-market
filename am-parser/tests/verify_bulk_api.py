
import sys
import os
import asyncio
from unittest.mock import MagicMock, AsyncMock

# Add parent directory to path to import app modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Mock Valid Services (Database/Queue layers)
sys.modules['am_services.job_queue_service'] = MagicMock()
sys.modules['am_etf.service'] = MagicMock()
sys.modules['am_etf.holdings_service'] = MagicMock()
sys.modules['am_etf.smart_holdings_service'] = MagicMock()
# Do NOT mock am_common.job_models - let it load the real one

# Now import the function to test
try:
    from am_api.etf_api import bulk_fetch_etf_holdings
except ImportError as e:
    # If am_api package is not found, try adding current dir
    print(f"Import Error: {e}")
    sys.path.append(os.path.dirname(os.path.abspath(__file__)))
    from am_api.etf_api import bulk_fetch_etf_holdings

async def test_bulk_fetch():
    print("🧪 Starting Bulk API Verification...")
    
    # Setup Mocks
    # Retrieve the mocks used by etf_api
    mock_etf_service_cls = sys.modules['am_etf.service'].ETFService
    mock_holdings_service_cls = sys.modules['am_etf.holdings_service'].ETFHoldingsService
    
    # Configure the instance returned by constructor
    mock_etf_service = mock_etf_service_cls.return_value
    mock_holdings_service = mock_holdings_service_cls.return_value
    
    # Mock Data
    mock_etf_1 = MagicMock()
    mock_etf_1.isin = "INF20220202"
    mock_etf_1.symbol = "NIFTY50"
    mock_etf_1.name = "Nifty 50 ETF"
    
    # Mock Holdings Doc
    mock_doc_1 = MagicMock()
    mock_doc_1.isin_code = "INF20220202"
    mock_holding_item = MagicMock()
    mock_holding_item.isin_code = "INE002A01018"
    mock_holding_item.stock_name = "RELIANCE"
    mock_holding_item.sector = "Energy"
    mock_holding_item.percentage = 10.5
    mock_doc_1.holdings = [mock_holding_item]
    
    # Configure Async Returns
    mock_etf_service.list = AsyncMock(return_value=[mock_etf_1])
    mock_holdings_service.list_all_holdings = AsyncMock(return_value=[mock_doc_1])
    mock_etf_service.close = AsyncMock()
    mock_holdings_service.close = AsyncMock()
    
    # Execute
    try:
        response = await bulk_fetch_etf_holdings(limit=10)
        
        # Verify
        print(f"✅ Response received: {response}")
        
        assert response['total_etfs'] == 1
        assert "INF20220202" in response['data']
        etf_data = response['data']["INF20220202"]
        assert etf_data['symbol'] == "NIFTY50"
        assert len(etf_data['holdings']) == 1
        assert etf_data['holdings'][0]['symbol'] == "RELIANCE"
        
        print("✅ SUCCESS: Bulk API logic verified locally with mocks.")
        
    except Exception as e:
        print(f"❌ FAILED: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(test_bulk_fetch())
