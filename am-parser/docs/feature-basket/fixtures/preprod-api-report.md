# Preprod API / Postman-style report — 2026-09-06T20:12:45.5464298+05:30

Env: `https://am-preprod.asrax.in` | No gitops in this roll (kubectl set image)

## [PASS] A1 parser health — 369ms status=200
- Asserts: status=healthy
```json
{"status":"healthy","database":"connected","total_portfolios":6,"environment":"preprod","mongo_target":"mongodb.infra.svc.cluster.local:27017/?authSource=admin\u0026directConnection=true","mongo_db":"mutual_funds","market_data_url":"http://am-market-data-preprod.am-apps-preprod.svc.cluster.local:8080","os_env_mongo_uri_set":true,"os_env_mongo_tail":"mongodb.infra.svc.cluster.local:27017/?authSource=admin\u0026directConnection=true","file_uri_used":"mongodb.infra.svc.cluster.local:27017/?authSource=admin\u0026directConnection=true","local_dotenv_override":false}
```

## [PASS] A2 v1 holdings — 612ms status=200
- Asserts: etfs=1 holdings=51
```json
{"not_found":[],"etfs":[{"holdingsCount":51,"symbol":"NIFTYBEES"}]}
```

## [PASS] A3 v2 holdings — 510ms status=200
- Asserts: symbol=NIFTYBEES return1Y=-1.0106 spark=24 category=Large cap Â· Equity
```json
{"sparklineCloses":[189.82,186.0,180.85,171.4,186.54,188.02,195.72,185.68,204.03,212.76,218.02,241.3,245.53,270.53,291.97,276.08,248.45,280.21,275.14,291.04,290.59,258.89,272.95,273.27],"symbol":"NIFTYBEES","return5Y":43.9627,"return3Y":27.5354,"categoryLabel":"Large cap Â· Equity","holdingsCount":51,"return1Y":-1.0106,"productType":"ETF","returnsAsOf":"2026-08-31"}
```

## [PASS] A4 MF stub — 474ms status=200
- Asserts: funds=0 notFound=1
```json
{"items":["ANY"],"totalFound":0,"funds":[],"notFound":["ANY"]}
```

## [PASS] A5 search — 385ms status=200
- Asserts: totalFound=5
```json
{"funds":[{"symbol":"UTINIFTETF","name":"UTI Nifty 50 ETF","productType":"ETF"},{"symbol":"BANKNIFTY1","name":"Kotak Nifty Bank ETF","productType":"ETF"},{"symbol":"UTIBANKETF","name":"UTI Nifty Bank ETF","productType":"ETF"}],"totalFound":5}
```

## [PASS] A6 bulk — 2747ms status=200
- Asserts: totalFunds=2 fundsCount=1
```json
{"totalFunds":2,"first":null}
```

## [PASS] A7 perf batch — 572ms status=200
- Asserts: NIFTYBEES 1Y=-1.0106 spark=24
```json
{"items":["NIFTYBEES","BANKBEES"],"results":[{"productType":"ETF","symbol":"NIFTYBEES","return1Y":-1.0106,"return3Y":27.5354,"return5Y":43.9627,"returnsAsOf":"2026-08-31","sparklineCloses":[189.82,186.0,180.85,171.4,186.54,188.02,195.72,185.68,204.03,212.76,218.02,241.3,245.53,270.53,291.97,276.08,248.45,280.21,275.14,291.04,290.59,258.89,272.95,273.27]},{"productType":"ETF","symbol":"BANKBEES","return1Y":7.4254,"return3Y":31.6696,"return5Y":57.0725,"returnsAsOf":"2026-08-31","sparklineCloses":[379.57,365.16,367.65,335.01,378.04,397.86,427.41,397.73,447.75,450.63,444.92,469.24,484.79,539.6,553.23,548.93,496.72,567.6,567.59,594.8,611.83,540.27,597.32,596.2]}]}
```

## [PASS] B1 portfolio health — 303ms status=200
- Asserts: UP
```json
{"status":"UP","components":{"diskSpace":{"status":"UP","details":{"total":414921494528,"free":258113335296,"threshold":10485760,"path":"/app/.","exists":true}},"livenessState":{"status":"UP"},"mongo":{"status":"UP","details":{"maxWireVersion":21}},"ping":{"status":"UP"},"readinessState":{"status":"UP"}},"groups":["liveness","readiness"]}
```

## [PASS] B2 catalog — 299ms status=200
- Asserts: themes=9
```json
{"themes":[{"id":"nifty-50","label":"Nifty 50","query":"NIFTYBEES"},{"id":"bank","label":"Bank","query":"BANKBEES"},{"id":"it","label":"IT","query":"ITBEES"}],"defaultQuery":"NIFTYBEES,BANKBEES,ITBEES"}
```

## [FAIL] B3 opportunities — 1299ms status=200
- Asserts: INCOMPLETE etf=NIFTYBEES cat= 1Y= spark=0
```json
[{"etfSymbol":"NIFTYBEES","categoryLabel":null,"matchScore":0.0,"return1Y":null,"return3Y":null,"return5Y":null,"returnsAsOf":null,"sparklineCloses":{},"requiredInvestment":50000.0},{"etfSymbol":"BANKBEES","categoryLabel":null,"matchScore":0.0,"return1Y":null,"return3Y":null,"return5Y":null,"returnsAsOf":null,"sparklineCloses":{},"requiredInvestment":50000.0},{"etfSymbol":"ITBEES","categoryLabel":null,"matchScore":0.0,"return1Y":null,"return3Y":null,"return5Y":null,"returnsAsOf":null,"sparklineCloses":{},"requiredInvestment":50000.0}]
```

Summary: PASS=9 FAIL=1 TOTAL=10
