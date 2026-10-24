import urllib.request

token = "R9LT2CZpDZXxTkNDpdmfhgUCEnP1qGnV"
url = "http://203.174.22.129:8086/api/v2/query?org=am-portfolio"

flux_query = '''
import "influxdata/influxdb/schema"
schema.measurements(bucket: "market-data")
'''

req = urllib.request.Request(url, data=flux_query.encode("utf-8"), headers={
    "Authorization": f"Token {token}",
    "Content-Type": "application/vnd.flux",
    "Accept": "application/csv"
})

try:
    with urllib.request.urlopen(req, timeout=10) as r:
        lines = r.read().decode("utf-8").splitlines()
        for l in lines:
            print(l)
except Exception as e:
    print("FAILED:", e)
