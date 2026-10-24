import urllib.request
import json

token = "R9LT2CZpDZXxTkNDpdmfhgUCEnP1qGnV"
url = "http://203.174.22.129:8086/api/v2/query?org=am-portfolio"

flux_query = '''
from(bucket: "market-data")
  |> range(start: -365d)
  |> filter(fn: (r) => r["_measurement"] == "global_index" or r["symbol"] =~ /HSI|SSEC/ or r["instrument_key"] =~ /HSI|SSEC/)
  |> last()
'''

req = urllib.request.Request(url, data=flux_query.encode("utf-8"), headers={
    "Authorization": f"Token {token}",
    "Content-Type": "application/vnd.flux",
    "Accept": "application/csv"
})

try:
    with urllib.request.urlopen(req, timeout=10) as r:
        lines = r.read().decode("utf-8").splitlines()
        print(f"Status: {r.status}, Lines: {len(lines)}")
        for l in lines[:20]:
            print("  ", l)
except Exception as e:
    print("FAILED:", e)
