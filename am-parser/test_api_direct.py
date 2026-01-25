import asyncio
import httpx

async def test_api():
    url = "http://localhost:8094/v1/securities/batch-search"
    queries = [
        "Reliance Industries Ltd.", 
        "Reliance Industries Ltd", 
        "Reliance Industries",
        "HDFC Bank Ltd.",
        "HDFC Bank"
    ]
    
    payload = {
        "queries": queries,
        "limit": 3,
        "minMatchScore": 0.5 # Low score to see if ANYTHING comes back
    }
    
    async with httpx.AsyncClient(timeout=10.0) as client:
        print(f"POST {url}")
        resp = await client.post(url, json=payload)
        print(f"Status: {resp.status_code}")
        data = resp.json()
        
        for r in data.get('results', []):
            q = r['query']
            matches = r['matches']
            print(f"Query: '{q}' -> {len(matches)} matches")
            if matches:
                print(f"   Top: {matches[0]['companyName']} (Score: {matches[0]['matchScore']})")
                
if __name__ == "__main__":
    asyncio.run(test_api())
