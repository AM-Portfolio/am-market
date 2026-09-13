import os
import sys
import gzip
import shutil
import urllib.request
import urllib.error
import argparse
import logging
import json
import time

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger("seed_bse")

UPSTOX_BSE_CDN_URL = "https://assets.upstox.com/market-quote/instruments/exchange/BSE.json.gz"
DEFAULT_OUTPUT_DIR = os.path.join("data", "upstock", "instrument")
DEFAULT_OUTPUT_FILE = os.path.join(DEFAULT_OUTPUT_DIR, "BSE.json")
BACKEND_UPDATE_ENDPOINT = "http://localhost:8080/v1/instruments/update"

def download_and_extract_bse_master(output_path: str) -> bool:
    """
    Downloads the daily BSE instruments master gz from Upstox CDN,
    decompresses it, and saves it to output_path.
    """
    gz_temp_path = output_path + ".gz"
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    logger.info("Downloading BSE instrument master from %s ...", UPSTOX_BSE_CDN_URL)
    req = urllib.request.Request(
        UPSTOX_BSE_CDN_URL,
        headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}
    )

    try:
        with urllib.request.urlopen(req, timeout=60) as response, open(gz_temp_path, "wb") as out_file:
            total_size = int(response.info().get("Content-Length", 0))
            downloaded = 0
            block_size = 65536

            while True:
                buffer = response.read(block_size)
                if not buffer:
                    break
                downloaded += len(buffer)
                out_file.write(buffer)

        logger.info("Download completed (%d bytes). Decompressing to %s ...", downloaded, output_path)

        with gzip.open(gz_temp_path, "rb") as f_in, open(output_path, "wb") as f_out:
            shutil.copyfileobj(f_in, f_out)

        if os.path.exists(gz_temp_path):
            os.remove(gz_temp_path)

        file_size_mb = os.path.getsize(output_path) / (1024 * 1024)
        logger.info("Successfully created %s (%.2f MB)", output_path, file_size_mb)
        return True

    except Exception as e:
        logger.error("Failed to download or decompress BSE master: %s", e)
        if os.path.exists(gz_temp_path):
            os.remove(gz_temp_path)
        return False

def trigger_backend_ingestion(file_path: str, backend_url: str):
    """
    Calls the Market Data Service streaming ingestion endpoint
    to upsert BSE records into MongoDB without blocking application traffic.
    """
    normalized_path = file_path.replace("\\", "/")
    endpoint = f"{backend_url}?filePath={normalized_path}&provider=UPSTOX"
    logger.info("Triggering backend streaming ingestion at %s ...", endpoint)

    req = urllib.request.Request(endpoint, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            body = resp.read().decode("utf-8")
            logger.info("Backend response: %s", body)
            return True
    except urllib.error.URLError as e:
        logger.warning(
            "Backend ingestion call failed (%s). "
            "If the server is not currently running, the file %s is downloaded and ready "
            "to be ingested when the server starts or via Postman POST /v1/instruments/update.",
            e, file_path
        )
        return False

def main():
    parser = argparse.ArgumentParser(description="Seed BSE Instrument Master Data into MongoDB")
    parser.add_argument("--output", default=DEFAULT_OUTPUT_FILE, help="Target JSON file path")
    parser.add_argument("--endpoint", default=BACKEND_UPDATE_ENDPOINT, help="Backend update endpoint URL")
    parser.add_argument("--no-trigger", action="store_true", help="Download only without triggering backend API")
    args = parser.parse_args()

    start_time = time.time()
    logger.info("=== Starting BSE Master Data Seeding ===")

    success = download_and_extract_bse_master(args.output)
    if not success:
        sys.exit(1)

    if not args.no_trigger:
        trigger_backend_ingestion(args.output, args.endpoint)

    elapsed = time.time() - start_time
    logger.info("=== Finished in %.2f seconds ===", elapsed)

if __name__ == "__main__":
    main()
