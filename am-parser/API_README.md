# Mutual Fund Portfolio REST API

## 🚀 Quick Start


### 1. Start the API Server
```bash
# Method 1: Using the start script
python start_api.py

# Method 2: Using uvicorn directly
python -m uvicorn am_api.api:app --host 127.0.0.1 --port 8000 --reload
```

### 2. Test the API
```bash
# Health check
curl http://127.0.0.1:8000/health

# API documentation
# Open in browser: http://127.0.0.1:8000/docs
```

## 📋 API Endpoints

### **Save Portfolio** (POST)
```bash
POST /portfolios
```

**Example Request:**
```bash
curl -X POST "http://127.0.0.1:8000/portfolios" \
     -H "Content-Type: application/json" \
     -d @data/mfextractedholdings/motilaloswalmf.json
```

**Example Response:**
```json
{
  "status": "success",
  "message": "Portfolio saved successfully",
  "data": {
    "id": "68c59ff3cedd9763e196a1d6",
    "mutual_fund_name": "Motilal Oswal Nifty Smallcap 250 Index Fund",
    "portfolio_date": "March 2025",
    "total_holdings": 250,
    "created_at": "2025-09-13T22:17:31.863723",
    "updated_at": "2025-09-13T22:17:31.929294"
  },
  "portfolio": { /* Full portfolio data */ }
}
```

### **Get Portfolio by ID** (GET)
```bash
GET /portfolios/{portfolio_id}
```

**Example Request:**
```bash
curl "http://127.0.0.1:8000/portfolios/68c59ff3cedd9763e196a1d6"
```

### **List All Portfolios** (GET)
```bash
GET /portfolios?limit=50&fund_name=optional
```

**Example Request:**
```bash
curl "http://127.0.0.1:8000/portfolios"
```

### **Search Portfolios** (GET)
```bash
GET /portfolios/search?fund_name={fund_name}
```

**Example Request:**
```bash
curl "http://127.0.0.1:8000/portfolios/search?fund_name=Motilal%20Oswal%20Nifty%20Smallcap%20250%20Index%20Fund"
```

### **Get Holdings by ISIN** (GET)
```bash
GET /holdings/{isin_code}
```

**Example Request:**
```bash
curl "http://127.0.0.1:8000/holdings/INE745G01035"
```

### **Get Fund Statistics** (GET)
```bash
GET /funds/{fund_name}/statistics
```

**Example Request:**
```bash
curl "http://127.0.0.1:8000/funds/Motilal%20Oswal%20Nifty%20Smallcap%20250%20Index%20Fund/statistics"
```

## 🐍 Python Usage Examples

### Save Portfolio
```python
import requests
import json

# Load JSON data
with open('data/mfextractedholdings/motilaloswalmf.json', 'r') as f:
    portfolio_data = json.load(f)

# Save via API
response = requests.post(
    'http://127.0.0.1:8000/portfolios',
    json=portfolio_data
)

if response.status_code == 201:
    result = response.json()
    portfolio_id = result['data']['id']
    print(f"Saved portfolio: {portfolio_id}")
```

### Get Portfolio
```python
import requests

# Get portfolio by ID
response = requests.get(f'http://127.0.0.1:8000/portfolios/{portfolio_id}')

if response.status_code == 200:
    portfolio = response.json()
    print(f"Fund: {portfolio['data']['mutual_fund_name']}")
    print(f"Holdings: {portfolio['data']['total_holdings']}")
```

### List Portfolios
```python
import requests

# List all portfolios
response = requests.get('http://127.0.0.1:8000/portfolios')

if response.status_code == 200:
    data = response.json()
    print(f"Total portfolios: {data['count']}")
    for portfolio in data['data']:
        print(f"- {portfolio['fund_name']} ({portfolio['total_holdings']} holdings)")
```

## 📊 Response Format

All API responses follow this structure:

```json
{
  "status": "success|error",
  "message": "Description of the result",
  "data": { /* Main response data */ },
  "count": 123,  // For list endpoints
  "error": "Error details"  // For error responses
}
```

## 🔧 Error Handling

### HTTP Status Codes
- `200` - Success
- `201` - Created (for POST requests)
- `404` - Not Found
- `422` - Validation Error
- `500` - Internal Server Error
- `503` - Service Unavailable

### Error Response Example
```json
{
  "status": "error",
  "detail": "Portfolio with ID 123 not found"
}
```

## 🌐 Interactive Documentation

Once the server is running, visit:

- **Swagger UI**: http://127.0.0.1:8000/docs
- **ReDoc**: http://127.0.0.1:8000/redoc

These provide interactive API documentation where you can test endpoints directly.

## 🧪 Testing

```bash
# Run the comprehensive test suite
python test_api.py

# Quick test
python quick_api_test.py
```

## 📦 Dependencies

The API requires these packages (already in requirements.txt):
- `fastapi>=0.104.0`
- `uvicorn[standard]>=0.24.0`
- `pydantic>=2.0`
- `motor>=3.3.0`
- `requests` (for testing)

## 🗄️ Database Integration

The API automatically connects to your MongoDB instance:
- **Connection**: `mongodb://admin:password123@localhost:27017`
- **Database**: `mutual_funds`
- **Collection**: `portfolios`

Make sure Docker MongoDB is running:
```bash
docker-compose up -d
```