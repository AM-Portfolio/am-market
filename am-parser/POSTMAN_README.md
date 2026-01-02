# Postman Collection - AM Parser API

## Overview

This Postman collection provides all endpoints for testing the AM Parser Mutual Fund Portfolio API running on **http://localhost:9000**.

## Files

- **`AM_Parser_API.postman_collection.json`** - Complete API collection (pre-configured for localhost:9000)

## Quick Start

### 1. Import into Postman

1. Open Postman
2. Click **Import** button (top left)
3. Select `AM_Parser_API.postman_collection.json`
4. Click **Import**

✅ That's it! The collection is ready to use with http://localhost:9000

### 2. Ensure Docker is Running

Make sure your Docker containers are running:

```bash
cd c:\Users\drabh\Documents\am-parser
docker-compose ps
```

Expected output:
```
am_parser_api          Up (healthy)    0.0.0.0:9000->8000/tcp
am_parser_mongodb      Up (healthy)    0.0.0.0:27017->27017/tcp
am_parser_mongo_express Up             0.0.0.0:8081->8081/tcp
```

If not running:
```bash
docker-compose up -d
```

## Collection Structure

### 📂 Health & Info
- **Root - API Info** - Get API information and available endpoints
- **Health Check** - Check API and database health status

### 📂 Portfolio Management
- **Save Portfolio** - Save a new mutual fund portfolio
- **List All Portfolios** - Get all portfolios with optional filtering
- **Get Portfolio by ID** - Retrieve specific portfolio using saved ID
- **Search Portfolios** - Search by fund name

### 📂 Holdings & Funds
- **Get Holdings by ISIN** - Find all holdings with specific ISIN code
- **Get Fund Statistics** - Get statistics for a specific fund

### 📂 File Upload & Processing
- **Upload Excel File (Complete Workflow)** - Full automation:
  1. Upload Excel file
  2. Split into sheets
  3. Parse each sheet
  4. Save all portfolios
- **Upload File (Simple)** - Alternative upload endpoint
- **Process Uploaded File** - Process already uploaded file
- **Parse Sheet File** - Parse individual sheet

### 📂 ETF Holdings
- **Load ETFs from JSON** - Upload etf_details.json to load ETFs into database
- **ETF Stats** - Get database statistics (total ETFs, ISIN coverage, holdings coverage)
- **Search ETFs** - Search ETFs by symbol, name, or ISIN
- **Fetch All Holdings (Background)** - Fetch holdings for all ETFs from MoneyControl API
- **Fetch Holdings for One ETF** - Fetch holdings for a specific ETF symbol
- **Get ETF Holdings** - View cached holdings for an ETF (shows all stocks)
- **Cache Statistics** - Check cache efficiency (fresh vs stale data)

### 📂 Background Jobs
- **Upload Excel (Async)** - Upload file for background processing (returns job_id immediately)
- **List All Jobs** - View all background jobs and their status
- **Get Job Status** - Check progress of a running job
- **Get Job Result** - Retrieve result of a completed job
- **Cancel Job** - Cancel a pending or running job
- **Admin - Fix Stuck Job** - Manually fix a stuck job
- **Admin - Recover All Stuck** - Trigger recovery for all stuck jobs

## Testing Workflow

### Scenario 1: Save and Retrieve Portfolio

1. **Save Portfolio**
   - Open: `Portfolio Management → Save Portfolio`
   - Click **Send**
   - ✅ Response will save `portfolio_id` automatically

2. **Get Portfolio by ID**
   - Open: `Portfolio Management → Get Portfolio by ID`
   - Click **Send** (uses saved `portfolio_id`)
   - ✅ Returns the portfolio you just saved

3. **List All Portfolios**
   - Open: `Portfolio Management → List All Portfolios`
   - Click **Send**
   - ✅ See all portfolios in the database

### Scenario 2: Search and Filter

1. **Search by Fund Name**
   - Open: `Portfolio Management → Search Portfolios`
   - Modify query parameter: `fund_name=Motilal`
   - Click **Send**
   - ✅ Returns matching portfolios

2. **Get Holdings by ISIN**
   - Open: `Holdings & Funds → Get Holdings by ISIN`
   - Change ISIN in URL: `INE745G01035`
   - Click **Send**
   - ✅ Returns all holdings with that ISIN

### Scenario 3: File Upload (Complete Workflow)

1. **Upload Excel File**
   - Open: `File Upload & Processing → Upload Excel File (Complete Workflow)`
   - In **Body** tab → **form-data**:
     - Select **file** → Click **Select Files** → Choose your Excel file
     - Set **parse_method** to `together` or `manual`
   - Click **Send**
   - ✅ File uploads, splits into sheets, parses, and saves all portfolios
   - ✅ Response shows parsed portfolios and any errors

2. **View Results**
   - Open: `Portfolio Management → List All Portfolios`
   - Click **Send**
   - ✅ See newly imported portfolios

### Scenario 4: ETF Data Management

1. **Load ETF Data**
   - Open: `ETF Holdings → Load ETFs from JSON`
   - In **Body** tab → **form-data**:
     - Select **file** → Choose `data/etf/etf_details.json`
     - Set **dry_run** to `false`
   - Click **Send**
   - ✅ Loads all ETFs into database
   - Response shows: total_records, valid_instruments, inserted_count

2. **Verify ETF Stats**
   - Open: `ETF Holdings → ETF Stats`
   - Click **Send**
   - ✅ Shows: total ETFs, ISIN coverage, holdings coverage

3. **Search for ETFs**
   - Open: `ETF Holdings → Search ETFs`
   - Modify query: `query=NIFTY&limit=5`
   - Click **Send**
   - ✅ Returns Nifty-related ETFs (NIFTYBEES, BANKNIFTY, etc.)

4. **View ETF Holdings**
   - Open: `ETF Holdings → Get ETF Holdings`
   - Change symbol in URL: `NIFTYBEES`
   - Click **Send**
   - ✅ Shows all stocks held by the ETF (if holdings fetched)

## Sample Data

### Save Portfolio - Sample Request Body

```json
{
  "mutual_fund_name": "Motilal Oswal Nifty Smallcap 250 Index Fund",
  "portfolio_date": "March 2025",
  "total_holdings": 3,
  "portfolio_holdings": [
    {
      "name_of_instrument": "Multi Commodity Exchange of India Limited",
      "isin_code": "INE745G01035",
      "percentage_to_nav": "0.0159%"
    },
    {
      "name_of_instrument": "Laurus Labs Limited",
      "isin_code": "INE947Q01028",
      "percentage_to_nav": "0.0141%"
    },
    {
      "name_of_instrument": "Crompton Greaves Consumer Electricals Limited",
      "isin_code": "INE299U01018",
      "percentage_to_nav": "0.0134%"
    }
  ]
}
```

You can also use the complete sample file at:
`data/mfextractedholdings/motilaloswalmf.json`

## Troubleshooting

### API Not Responding

**Error:** `Could not get response` or `Connection refused`

**Solution:**
```bash
# Check if Docker containers are running
docker-compose ps

# If not running, start them
docker-compose up -d

# Check API logs
docker-compose logs -f am-api
```

### Port Already in Use

**Error:** API accessible at different port

**Solution:**
- If your API runs on a different port, you'll need to edit the collection file
- Open the JSON file in a text editor and replace `http://localhost:9000` with your port
- Re-import the collection in Postman

### File Upload Failing

**Error:** `Only Excel files (.xlsx, .xls) are supported`

**Solution:**
- Ensure you're uploading `.xlsx` or `.xls` files
- Check file is not corrupted
- Try `parse_method`: `manual` if `together` fails

### Database Empty

**Problem:** No portfolios returned

**Solution:**
```bash
# Check if MongoDB has data
# Open Mongo Express: http://localhost:8081
# Login: webadmin / webpass123
# Navigate to: mutual_funds → portfolios

# Or use the API
curl http://localhost:9000/portfolios
```

## API Documentation

For interactive API documentation, visit:
- **Swagger UI**: http://localhost:9000/docs
- **ReDoc**: http://localhost:9000/redoc

## Tips

1. **Pre-configured for localhost:9000**: No need to set environment variables
2. **Check Response Status**: Green status (200, 201) means success
3. **View Logs**: Use `docker-compose logs -f am-api` to see real-time API logs
4. **Auto-save IDs**: Some requests have test scripts that auto-save important values for reuse
5. **Copy IDs Manually**: For requests needing IDs, copy them from previous responses

## Additional Resources

- **API Source Code**: `am_api/api.py`
- **Sample Data**: `data/mfextractedholdings/`
- **Database UI**: http://localhost:8081 (Mongo Express)

## Support

If you encounter issues:
1. Check Docker containers are running: `docker-compose ps`
2. View API logs: `docker-compose logs am-api`
3. Test health endpoint: `GET http://localhost:9000/health`
4. Open Swagger docs: http://localhost:9000/docs

---

✅ **Ready to test!** Start with the **Health Check** endpoint to verify everything is working.
