"""Step 0: download NSE MASTER_DOWNLOAD files (SCH/NAV/SIP/STP/SWP/SET). Secrets via env."""
from __future__ import annotations

import base64
import json
import os
import random
import sys
from datetime import datetime, timezone
from pathlib import Path

import requests
from Crypto.Cipher import AES
from Crypto.Hash import SHA1
from Crypto.Protocol.KDF import PBKDF2
from Crypto.Util.Padding import pad

# Local-only (mf_catalog_job/data/* is gitignored). Do not commit master dumps.
OUT = Path(__file__).resolve().parent / "data" / "masters"
FILE_TYPES = ["SCH", "NAV", "SIP", "STP", "SWP", "SET"]


def _require_nse_env() -> tuple[str, str, str, str, str]:
    missing = [
        k
        for k in ("NSE_URL", "NSE_LOGIN", "NSE_API_KEY", "NSE_API_SECRET", "NSE_MEMBER")
        if not (os.environ.get(k) or "").strip()
    ]
    if missing:
        raise SystemExit(f"FAIL: missing env {', '.join(missing)}")
    return (
        os.environ["NSE_URL"].rstrip("/"),
        os.environ["NSE_LOGIN"],
        os.environ["NSE_API_KEY"],
        os.environ["NSE_API_SECRET"],
        os.environ["NSE_MEMBER"],
    )


def headers(login: str, api_key: str, api_secret: str, member: str) -> dict:
    plain = f"{api_secret}|{random.randint(1, 10_000_000_000)}"
    iv, salt = os.urandom(16), os.urandom(16)
    key = PBKDF2(api_key, salt, dkLen=16, count=1000, hmac_hash_module=SHA1)
    ct = AES.new(key, AES.MODE_CBC, iv=iv).encrypt(pad(plain.encode(), AES.block_size))
    packed = iv.hex() + "::" + salt.hex() + "::" + base64.b64encode(ct).decode()
    enc = base64.b64encode(packed.encode()).decode()
    basic = base64.b64encode(f"{login}:{enc}".encode()).decode()
    return {
        "Content-Type": "application/json",
        "memberId": member,
        "Authorization": f"Basic {basic}",
        "Accept-Language": "en-US",
        "Referer": "www.google.com",
        "User-Agent": "PostmanRuntime/7.43.0",
        "Accept": "",
        "Connection": "keep-alive",
    }


def main() -> int:
    url, login, api_key, api_secret, member = _require_nse_env()
    OUT.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    summary: dict = {"stamp": stamp, "files": {}}

    for ft in FILE_TYPES:
        endpoint = f"{url}/nsemfdesk/api/v2/reports/MASTER_DOWNLOAD"
        resp = requests.post(
            endpoint,
            headers=headers(login, api_key, api_secret, member),
            json={"file_type": ft},
            timeout=180,
        )
        path = OUT / f"{ft}_{stamp}.txt"
        text = resp.text
        # Guard: UAT occasionally returns a duplicated body (exact 2x). Keep first half.
        lines_all = text.splitlines()
        if len(lines_all) >= 4 and len(lines_all) % 2 == 0:
            mid = len(lines_all) // 2
            if lines_all[:mid] == lines_all[mid:]:
                text = "\n".join(lines_all[:mid]) + ("\n" if text.endswith("\n") else "")
                print(f"{ft}: de-duplicated doubled body -> {mid} lines")
        path.write_text(text, encoding="utf-8", errors="replace")
        lines = [ln for ln in text.splitlines() if ln.strip()]
        first = lines[0] if lines else ""
        has_header = (
            "|" in first
            and any(c.isalpha() for c in first.split("|")[0])
            and not first.split("|")[0].replace(".", "").isdigit()
        )
        data_rows = len(lines) - (1 if has_header and ft != "NAV" else 0)
        if ft == "NAV":
            has_header = False
            data_rows = len(lines)
        elif ft in ("SCH", "SIP", "STP", "SWP", "SET") and lines:
            upper = first.upper()
            has_header = (
                "SCHEME" in upper
                or "AMC" in upper
                or "DATE" in upper
                or "SETTLEMENT" in upper
                or "UNIQUE" in upper
            )
            data_rows = len(lines) - (1 if has_header else 0)

        cols = first.split("|") if lines else []
        summary["files"][ft] = {
            "path": str(path),
            "http": resp.status_code,
            "bytes": len(resp.text.encode("utf-8", errors="replace")),
            "line_count": len(lines),
            "data_rows": data_rows,
            "has_header": has_header,
            "column_count": len(cols),
            "header_or_first_row": first[:500],
            "columns": cols if has_header else [],
            "nav_columns_inferred": (
                [
                    "nav_date",
                    "scheme_code",
                    "scheme_name",
                    "rta_scheme_code",
                    "div_flag",
                    "isin",
                    "nav",
                    "rta_agent",
                ]
                if ft == "NAV"
                else None
            ),
        }
        print(
            f"{ft}: HTTP {resp.status_code} lines={len(lines)} "
            f"data_rows={data_rows} cols={len(cols)} -> {path.name}"
        )

    sum_path = OUT / f"summary_{stamp}.json"
    sum_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")
    (OUT / "latest_summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print("SUMMARY", sum_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
