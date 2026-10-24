import requests, json

# Let's get Upstox token from dev pod or vault
token = "c3f870231cfb77626960d705c93540bf6c36746cfdf0a1966a3d9ff4fcda00a2" # from dev pod env? Let's check!
headers = {
    "Accept": "application/json",
    "User-Agent": "Mozilla/5.0"
}

# 1. Historical candle day
url = "https://api.upstox.com/v2/historical-candle/GLOBAL_INDEX%7CHSI/day/2026-10-24/2026-10-01"
r = requests.get(url, headers=headers)
print("Historical candle status:", r.status_code)
print("Historical candle body:", r.text[:500])

# 2. Intraday candle 30minute
url2 = "https://api.upstox.com/v2/historical-candle/intraday/GLOBAL_INDEX%7CHSI/30minute"
r2 = requests.get(url2, headers=headers)
print("Intraday status:", r2.status_code)
print("Intraday body:", r2.text[:500])
