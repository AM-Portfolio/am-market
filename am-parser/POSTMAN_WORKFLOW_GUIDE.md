# 📬 Postman Complete Workflow Guide

## 🚀 API Server Running
- **URL**: `http://127.0.0.1:8000`
- **Status**: ✅ Running with MongoDB connected
- **API Docs**: http://127.0.0.1:8000/docs

---

## 📤 **STEP 1: Upload Excel File via Postman**

### **Request Configuration:**
```
Method: POST
URL: http://127.0.0.1:8000/upload/excel
```

### **Headers:**
```
Content-Type: multipart/form-data
```

### **Body (form-data):**
```
Key: file
Type: File
Value: Select "motilal-hy-portfolio-march-2025.xlsx" from data/samples/

Key: parse_method
Type: Text
Value: together

Key: together_api_key
Type: Text
Value: bff39f38ee07df9a08ff8d2e7279b9d7223ab3f283a30bc39590d36f77dbd2fd
```

### **Expected Response (200 OK):**
```json
{
  "message": "Excel file processed successfully",
  "file_id": "uuid-generated-id",
  "portfolio_ids": [
    "sheet-id-1",
    "sheet-id-2"
  ]
}
```

---

## 🔄 **What Happens During Upload:**

1. **📁 File Upload**: Excel file is uploaded and stored
2. **📊 Sheet Breaking**: Excel file is split into individual sheet files
3. **🤖 AI Parsing**: Each sheet is parsed using Together AI LLM
4. **🔗 ID Matching**: Portfolio ID is set to match Sheet ID
5. **💾 Database Persistence**: Portfolios are saved to MongoDB

---

## 📋 **STEP 2: List All Portfolios**

### **Request Configuration:**
```
Method: GET
URL: http://127.0.0.1:8000/portfolios/
```

### **Expected Response:**
```json
[
  {
    "_id": "sheet-id-1",
    "mutual_fund_name": "Motilal Oswal Nifty 50 ETF",
    "portfolio_date": "2025-03-31",
    "total_holdings": 50,
    "holdings": [
      {
        "company_name": "RELIANCE INDUSTRIES LTD",
        "percentage": 8.23,
        "market_value": 1234567.89
      }
    ]
  }
]
```

---

## 🔍 **STEP 3: Get Specific Portfolio**

### **Request Configuration:**
```
Method: GET
URL: http://127.0.0.1:8000/portfolios/{portfolio_id}
```

Replace `{portfolio_id}` with one of the IDs from the upload response.

---

## 🗄️ **STEP 4: Verify in MongoDB**

### **Option A: MongoDB Web Interface**
- **URL**: http://127.0.0.1:8081
- **Username**: `admin`
- **Password**: `password123`
- **Database**: `mutual_funds`
- **Collection**: `portfolios`

### **Option B: Direct MongoDB Query**
```powershell
docker exec -it am_parser_mongodb mongosh --authenticationDatabase admin -u admin -p password123 mutual_funds --eval "db.portfolios.find().pretty()"
```

---

## 🎯 **Complete Workflow Summary:**

1. **Upload Excel** → File stored & processed
2. **Sheet Breaking** → Individual sheets extracted
3. **AI Parsing** → Together AI extracts portfolio data
4. **ID Matching** → Portfolio ID = Sheet ID
5. **Database Storage** → All data persisted in MongoDB
6. **API Access** → Query portfolios via REST API

---

## 🚨 **Troubleshooting:**

### **If Upload Fails:**
- Ensure file is selected in Postman
- Check `parse_method` is set to "together"
- Verify API key is correct
- Confirm file is Excel format (.xlsx)

### **If No Portfolios Found:**
- Check upload response for `portfolio_ids`
- Verify MongoDB is running: `docker ps`
- Check API server logs for errors

### **API Not Responding:**
- Restart API server: `uvicorn am_api.api:app --host 127.0.0.1 --port 8000`
- Check if port 8000 is available

---

## 📊 **Sample Test Data:**
- **File**: `data/samples/motilal-hy-portfolio-march-2025.xlsx`
- **Expected Sheets**: YO01, YO03 (2 portfolios)
- **Fund**: Motilal Oswal Nifty 50 ETF
- **Holdings**: ~50 companies each

---

## 🎉 **Success Indicators:**

✅ **Upload Success**: Status 200 with `portfolio_ids`  
✅ **Sheet Breaking**: Multiple sheet files created  
✅ **AI Parsing**: Fund name and holdings extracted  
✅ **ID Matching**: Portfolio ID matches Sheet ID  
✅ **Database Storage**: Portfolios visible in MongoDB  
✅ **API Access**: GET requests return portfolio data  

Ready for Postman testing! 🚀