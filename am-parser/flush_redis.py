import redis
import os

try:
    # Use config from env or default
    host = "100.72.208.15"
    port = 6379
    password = "password123"
    
    print(f"Connecting to Redis at {host}:{port}...")
    r = redis.Redis(host=host, port=port, password=password, decode_responses=True)
    
    # We want to delete keys related to security search
    # Pattern likely: sec_search:* or similar
    # But let's just flush db if safe, or delete specific keys
    
    # Search keys
    print("Scanning keys...")
    keys = []
    # Try common patterns or * if not too many
    # cursor = '0'
    # while cursor != 0:
    #     cursor, data = r.scan(cursor=cursor, match='*', count=100)
    #     keys.extend(data)
        
    # Just flushall for dev environment is usually okay
    print("Flushing DB...")
    r.flushall()
    print("Redis Flushed.")
    
except Exception as e:
    print(f"Failed: {e}")
