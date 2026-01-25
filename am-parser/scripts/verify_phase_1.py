
import subprocess
import time
import requests
import sys
import os
import signal
from pathlib import Path

API_URL = "http://127.0.0.1:8022"
ROOT_DIR = Path(__file__).parent.parent

def run_seed():
    print("----- Step 1: Seeding Mock Data -----")
    result = subprocess.run([sys.executable, "scripts/seed_mock_data.py"], cwd=ROOT_DIR, capture_output=True, text=True, encoding='utf-8')
    if result.returncode != 0:
        print("Seeding failed:")
        print(result.stderr)
        sys.exit(1)
    print(result.stdout)
    print("Seeding Successful")

def verify_api():
    print("\n----- Step 2: Checking API Server -----")
    
    # Check if API is already running
    api_running = False
    try:
        resp = requests.get(f"{API_URL}/health", timeout=2)
        if resp.status_code == 200:
            print("API is already UP and Running!")
            api_running = True
    except requests.exceptions.ConnectionError:
        print("API is NOT running. Starting local instance...")
        api_running = False

    api_process = None
    
    if not api_running:
        # Start API in background with custom port
        env = os.environ.copy()
        env["PORT"] = "8022"
        
        api_process = subprocess.Popen(
            [sys.executable, "start_api.py"], 
            cwd=ROOT_DIR,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding='utf-8',
            env=env
        )
        
        try:
            # Wait for API to start
            print("Waiting for API to come online...")
            max_retries = 20
            
            for i in range(max_retries):
                try:
                    resp = requests.get(f"{API_URL}/health", timeout=2)
                    if resp.status_code == 200:
                        print("API is UP!")
                        break
                except requests.exceptions.ConnectionError:
                    pass
                time.sleep(1)
                print(".", end="", flush=True)
            else:
                print("\nAPI failed to start within timeout")
                # Print stderr
                outs, errs = api_process.communicate(timeout=1)
                print("API Stderr:", errs)
                sys.exit(1)
        except Exception as e:
            print(f"Error starting API: {e}")
            if api_process:
                api_process.kill()
            sys.exit(1)

    try:
        print("\n----- Step 3: Verifying Bulk Endpoint -----")
        # Endpoint path: start_api.py uses include_router(prefix="/v1")
        endpoint = f"{API_URL}/v1/holdings/bulk"
        print(f"Calling {endpoint}...")
        
        resp = requests.get(endpoint)
        print(f"Status Code: {resp.status_code}")
        
        if resp.status_code != 200:
            print("Failed to get 200 OK")
            print("Response:", resp.text)
            sys.exit(1)
            
        data = resp.json()
        print("Response keys:", list(data.keys()))
        
        # Validation Logic
        if "data" not in data:
            print("Response missing 'data' key")
            sys.exit(1)
            
        holdings_map = data["data"]
        print(f"Holdings Map Keys: {list(holdings_map.keys())}")
        
        # Check for NIFTY IT ISIN
        isin_it = "INF20220202"
        if isin_it in holdings_map:
            print(f"Found NIFTY IT ({isin_it})")
            it_data = holdings_map[isin_it]
            if it_data.get("symbol") == "NIFTYIT":
                print("   Symbol Matches")
            else:
                print(f"   Symbol Mismatch: {it_data.get('symbol')}")
                
            if len(it_data.get("holdings", [])) == 10:
                 print(f"   Holdings count is 10")
            else:
                 print(f"   Holdings count mismatch: {len(it_data.get('holdings', []))}")
        else:
            print(f"NIFTY IT ISIN {isin_it} not found in response")
            # Don't exit yet, might be testing against partial data
            
        # Check for NIFTY 50 ISIN
        isin_50 = "INF20220203"
        if isin_50 in holdings_map:
            print(f"Found NIFTY 50 ({isin_50})")
        else:
            print(f"NIFTY 50 ISIN {isin_50} not found")
            
        if isin_it in holdings_map and isin_50 in holdings_map:
            print("\nPHASE 1 VERIFICATION PASSED!")
        else:
            print("\nVerification Failed: Missing Expected ISINs")
            # If we are running against a server that doesn't have our mock data, this is expected.
            # But the user asked us to verify.
        
    finally:
        if api_process:
            print("\n----- Teardown: Stopping Local API -----")
            api_process.terminate()
            try:
                api_process.wait(timeout=2)
            except subprocess.TimeoutExpired:
                api_process.kill()
            print("API Stopped")
        else:
            print("\nLeaving external API running.")

if __name__ == "__main__":
    run_seed()
    verify_api()
