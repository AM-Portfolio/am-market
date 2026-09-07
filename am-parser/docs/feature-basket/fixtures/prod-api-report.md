# PROD API report — 2026-09-06T20:23:34.0853451+05:30
Base: https://am.asrax.in
Images: parser=funds-v2-mdurl-20260906 portfolio=basket-discover-cachefix-20260906

## [PASS] A1 parser health — 295ms
- db=connected market=https://am.asrax.in/market
```json
{"status":"healthy","database":"connected","total_portfolios":6,"environment":"prod","mongo_target":"mongodb-prod.asrax.in:8894/?authSource=admin\u0026directConnection=true","mongo_db":"mutual_funds","market_data_url":"https://am.asrax.in/market","os_env_mongo_uri_set":true,"os_env_mongo_tail":"mongodb-prod.asrax.in:8894/?authSource=admin\u0026directConnection=true","file_uri_used":"mongodb-prod.asrax.in:8894/?authSource=admin\u0026directConnection=true","local_dotenv_override":false}
```

## [PASS] A2 v1 holdings — 479ms
- holdings=51
```json
{"holdingsCount":51,"symbol":"NIFTYBEES"}
```

## [PASS] A3 v2 holdings — 463ms
- 1Y=-1.0106 3Y=27.5354 5Y=43.9627 spark=24 cat=Large cap Â· Equity
```json
{"returnsAsOf":"2026-08-31","return5Y":43.9627,"holdingsCount":51,"return3Y":27.5354,"symbol":"NIFTYBEES","return1Y":-1.0106,"categoryLabel":"Large cap Â· Equity","sparklineCloses":[189.82,186.0,180.85,171.4,186.54,188.02,195.72,185.68,204.03,212.76,218.02,241.3,245.53,270.53,291.97,276.08,248.45,280.21,275.14,291.04,290.59,258.89,272.95,273.27]}
```

## [PASS] A4 MF stub — 452ms
- notFound=1
```json
{"items":["ANY"],"totalFound":0,"funds":[],"notFound":["ANY"]}
```

## [PASS] A5 search — 320ms
- total=5
```json
{"totalFound":5}
```

## [PASS] A6 perf batch — 475ms
- NIFTY 1Y=-1.0106 spark=24 | BANK 1Y=7.4254
```json
{"items":["NIFTYBEES","BANKBEES"],"results":[{"productType":"ETF","symbol":"NIFTYBEES","return1Y":-1.0106,"return3Y":27.5354,"return5Y":43.9627,"returnsAsOf":"2026-08-31","sparklineCloses":[189.82,186.0,180.85,171.4,186.54,188.02,195.72,185.68,204.03,212.76,218.02,241.3,245.53,270.53,291.97,276.08,248.45,280.21,275.14,291.04,290.59,258.89,272.95,273.27]},{"productType":"ETF","symbol":"BANKBEES","return1Y":7.4254,"return3Y":31.6696,"return5Y":57.0725,"returnsAsOf":"2026-08-31","sparklineCloses":[379.57,365.16,367.65,335.01,378.04,397.86,427.41,397.73,447.75,450.63,444.92,469.24,484.79,539.6,553.23,548.93,496.72,567.6,567.59,594.8,611.83,540.27,597.32,596.2]}]}
```

## [PASS] B1 portfolio health — 326ms
- UP
```json
{"status":"UP"}
```

## [PASS] B2 catalog — 314ms
- themes=9
```json
{"themes":[{"id":"nifty-50","label":"Nifty 50","query":"NIFTYBEES"},{"id":"bank","label":"Bank","query":"BANKBEES"},{"id":"it","label":"IT","query":"ITBEES"}],"defaultQuery":"NIFTYBEES,BANKBEES,ITBEES"}
```

## [PASS] B3 opportunities — 13482ms
- etf=NIFTYBEES cat=Large cap Â· Equity 1Y=-1.0106 5Y=43.9627 spark=24
```json
[{"etfSymbol":"NIFTYBEES","categoryLabel":"Large cap Â· Equity","matchScore":0.0,"return1Y":-1.0106,"return3Y":27.5354,"return5Y":43.9627,"returnsAsOf":"2026-08-31","sparkCount":24,"minInvest":50000.0},{"etfSymbol":"BANKBEES","categoryLabel":"Financials Â· Equity","matchScore":0.0,"return1Y":7.4254,"return3Y":31.6696,"return5Y":57.0725,"returnsAsOf":"2026-08-31","sparkCount":24,"minInvest":50000.0},{"etfSymbol":"ITBEES","categoryLabel":"Information technology Â· Equity","matchScore":0.0,"return1Y":-11.2702,"return3Y":3.0844,"return5Y":-6.2689,"returnsAsOf":"2026-08-31","sparkCount":24,"minInvest":50000.0}]
```

Summary PASS=9 FAIL=0 TOTAL=9
