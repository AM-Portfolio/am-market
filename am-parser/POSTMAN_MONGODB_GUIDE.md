# 🎯 COMPLETE POSTMAN TO MONGODB WORKFLOW GUIDE

## 🚀 STEP 1: API Server Status
✅ **API Server Running**: http://127.0.0.1:8001
✅ **MongoDB Running**: Port 27017 (confirmed)
✅ **Together AI Service**: Loaded and ready

---

## 📬 STEP 2: POSTMAN CONFIGURATION

### **Upload Excel File (Together AI Processing)**

#### **Request Setup:**
```
Method: POST
URL: http://127.0.0.1:8001/upload/excel
```

#### **Headers:**
```
Content-Type: multipart/form-data
```

#### **Body (form-data):**
```
Key: file
Type: File
Value: [Browse and select: data/samples/motilal-hy-portfolio-march-2025.xlsx]

Key: parse_method
Type: Text
Value: together

Key: together_api_key  
Type: Text
Value: <your-together-api-key-from-.env-or-vault>
```

#### **Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Complete Excel processing finished! 2/2 sheets parsed successfully",
  "summary": {
    "parse_method": "together",
    "total_sheets": 2,
    "successfully_parsed": 2
  },
  "parsed_portfolios": [
    {
      "portfolio_id": "sheet-id-1",
      "mutual_fund_name": "Motilal Oswal Nifty 50 ETF",
      "total_holdings": 50,
      "parse_method": "together"
    },
    {
      "portfolio_id": "sheet-id-2", 
      "mutual_fund_name": "Motilal Oswal Nasdaq 100 ETF",
      "total_holdings": 101,
      "parse_method": "together"
    }
  ]
}
```

---

## 📋 STEP 3: VERIFY IN POSTMAN

### **List All Portfolios:**
```
Method: GET
URL: http://127.0.0.1:8001/portfolios/
```

### **Get Specific Portfolio:**
```
Method: GET  
URL: http://127.0.0.1:8001/portfolios/{portfolio_id}
```
*(Replace {portfolio_id} with actual ID from upload response)*

---

## 🗄️ STEP 4: VIEW IN MONGODB

### **Option A: MongoDB Web Interface (mongo-express)**
```
URL: http://127.0.0.1:8081
Username: admin
Password: <REDACTED_PASSWORD>

Navigate to:
Database: mutual_funds → Collection: portfolios
```

### **Option B: Command Line Query**
```powershell
docker exec -it am_parser_mongodb mongosh --authenticationDatabase admin -u admin -p <REDACTED_PASSWORD> mutual_funds --eval "db.portfolios.find().pretty()"
```

### **Option C: Quick Portfolio Summary**
```powershell
docker exec -it am_parser_mongodb mongosh --authenticationDatabase admin -u admin -p <REDACTED_PASSWORD> mutual_funds --eval "db.portfolios.find({}, {mutual_fund_name: 1, portfolio_date: 1, total_holdings: 1, _id: 1}).pretty()"
```

---

## 🎯 STEP 5: COMPLETE WORKFLOW TEST

### **1. Send Postman Request:**
- Use the upload configuration above
- Hit **Send** button
- Should get 200 OK response with portfolio IDs

### **2. Verify API Response:**
- Check `parsed_portfolios` array
- Look for real fund names (not "Portfolio YO01")
- Confirm `parse_method: "together"`

### **3. Check MongoDB:**
- Use mongo-express web interface
- Or run command line query
- Should see portfolios with:
  - Real fund names: "Motilal Oswal..."
  - Correct dates: "March 2025"  
  - Proper holdings count: 50, 101

---

## ✅ SUCCESS INDICATORS:

**📤 Upload Success:**
- Status: 200 OK
- Message: "Excel processing finished!"
- Portfolio IDs returned

**🤖 Together AI Success:**
- Fund names: "Motilal Oswal Nifty 50 ETF", "Motilal Oswal Nasdaq 100 ETF"
- Holdings: 50 and 101 respectively
- Date: "March 2025"
- Parse method: "together"

**💾 MongoDB Success:**
- Portfolios visible in database
- Complete holding details with percentages
- Sheet ID matches Portfolio ID

---

## 🚨 TROUBLESHOOTING:

**If you get generic names like "Portfolio YO01":**
- Together AI failed, fell back to manual parsing
- Check API key is correct
- Verify `parse_method=together` in form data

**If API not responding:**
- Check server is running on port 8001
- Visit http://127.0.0.1:8001 to test

**If MongoDB empty:**
- Check docker containers: `docker ps`
- Restart MongoDB if needed

---

## 🎉 READY TO TEST!

Your API server is running on **http://127.0.0.1:8001**

Open Postman and start testing the complete workflow! 🚀