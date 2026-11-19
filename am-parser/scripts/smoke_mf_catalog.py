#!/usr/bin/env python3
"""Smoke MF catalog on am-parser (any env).

Usage:
  MF_CATALOG_API_URL=http://am-parser-dev:8080 python scripts/smoke_mf_catalog.py
  # or port-forward / ingress:
  MF_CATALOG_API_URL=https://am-dev.asrax.in/parser python scripts/smoke_mf_catalog.py

Exit 0 = search returns items with nav/navDate and source=nse-master-download.
Exit 2 = empty / missing NAV (catalog DB may be unpopulated for that env).
Exit 1 = HTTP/connect failure.

Preprod/prod: run only after mf_catalog is populated for that env's Postgres.
"""
from __future__ import annotations

import json
import os
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def main() -> int:
    base = (os.environ.get("MF_CATALOG_API_URL") or "").rstrip("/")
    if not base:
        print("FAIL: set MF_CATALOG_API_URL (e.g. http://am-parser-dev:8080)")
        return 1
    url = f"{base}/v1/mutual-fund/search?q=HDFC&limit=5"
    try:
        req = Request(url, headers={"Accept": "application/json"})
        with urlopen(req, timeout=20) as resp:
            body = json.loads(resp.read().decode("utf-8"))
    except HTTPError as e:
        print(f"FAIL: HTTP {e.code} {e.reason}")
        return 1
    except URLError as e:
        print(f"FAIL: connect {e.reason}")
        return 1

    if not isinstance(body, dict):
        print("FAIL: non-object response")
        return 1
    if body.get("error"):
        print(f"FAIL: error={body.get('error')} message={body.get('message')}")
        return 1
    items = body.get("items") or []
    if not items:
        print("SPARSE: empty search — populate mf_catalog for this env then retry")
        return 2
    first = items[0]
    nav = first.get("nav")
    nav_date = first.get("navDate")
    source = body.get("source")
    print(
        f"OK count={body.get('count')} source={source} "
        f"first.nav={nav} first.navDate={nav_date} priceType={body.get('priceType')}"
    )
    if source != "nse-master-download":
        print("FAIL: unexpected source")
        return 2
    if nav is None or not nav_date:
        print("FAIL: missing nav/navDate on first item")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
