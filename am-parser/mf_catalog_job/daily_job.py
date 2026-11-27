"""Daily NSE master download + ingest + Postgres push + returns denorm + lag alerts.

Schedule (ops / dig CronJob): after NSE NAV publish (evening IST + morning catch-up).
  python mf_catalog_job/daily_job.py
  python mf_catalog_job/daily_job.py --skip-download --skip-push
  python mf_catalog_job/daily_job.py --skip-returns

Env:
  NSE_URL, NSE_LOGIN, NSE_API_KEY, NSE_API_SECRET, NSE_MEMBER — download
  AM_MARKET_MF_JDBC_URL, AM_MARKET_MF_JDBC_USER, AM_MARKET_MF_JDBC_PASSWORD — push/returns

Ownership: am-parser/docs/mf-catalog-nav-ownership.md
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import date, datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent
DOWNLOAD = ROOT / "download_masters.py"
INGEST = ROOT / "ingest_masters.py"
PUSH = ROOT / "_push_core.py"
RETURNS = ROOT / "compute_scheme_returns.py"
METRICS = ROOT / "data" / "latest_metrics.json"
ALERT_LOG = ROOT / "data" / "alerts.jsonl"
# Local-only masters (gitignored under mf_catalog_job/data/).
MASTERS_DIR = ROOT / "data" / "masters"


def nav_lag_days(max_nav_date: str | None) -> int | None:
    if not max_nav_date:
        return None
    try:
        d = date.fromisoformat(max_nav_date[:10])
    except ValueError:
        return None
    return (date.today() - d).days


def run_py(script: Path, extra_args: list[str] | None = None, *, cwd: Path) -> int:
    if not script.is_file():
        print(f"MISSING {script}", flush=True)
        return 2
    cmd = [sys.executable, str(script), *(extra_args or [])]
    print(f"RUN {' '.join(cmd)} cwd={cwd}", flush=True)
    return subprocess.call(cmd, cwd=str(cwd))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--skip-download", action="store_true")
    ap.add_argument("--skip-push", action="store_true")
    ap.add_argument(
        "--skip-returns",
        action="store_true",
        help="Skip Postgres mf_scheme_returns denorm after push",
    )
    ap.add_argument("--alert-if-nav-lag-days", type=int, default=1)
    ap.add_argument("--alert-if-schemes-below", type=int, default=1000)
    ap.add_argument(
        "--fail-on-stale",
        action="store_true",
        help="Exit non-zero on NAV_STALE WARN (default: only CRIT fails)",
    )
    args = ap.parse_args()

    if not args.skip_download:
        rc = run_py(DOWNLOAD, cwd=ROOT)
        if rc != 0:
            print("DOWNLOAD failed", rc, flush=True)
            return rc

    rc = run_py(
        INGEST,
        ["--masters-dir", str(MASTERS_DIR)],
        cwd=ROOT,
    )
    if rc != 0:
        print("INGEST failed", rc, flush=True)
        return rc

    if not args.skip_push:
        rc = run_py(PUSH, cwd=ROOT)
        if rc != 0:
            print("PUSH failed", rc, flush=True)
            return rc

    # Denorm 1M/3M/1Y/3Y/5Y on Postgres after push (null when history is thin).
    if not args.skip_push and not args.skip_returns:
        rc = run_py(RETURNS, cwd=ROOT)
        if rc != 0:
            print("RETURNS failed", rc, flush=True)
            return rc

    metrics = json.loads(METRICS.read_text(encoding="utf-8")) if METRICS.exists() else {}
    alerts: list[dict] = []
    lag = nav_lag_days(metrics.get("max_nav_date"))
    metrics["nav_lag_days"] = lag
    metrics["checked_at"] = datetime.now(timezone.utc).isoformat()
    metrics["masters_dir"] = str(MASTERS_DIR)

    if lag is not None and lag > args.alert_if_nav_lag_days:
        alerts.append(
            {
                "level": "WARN",
                "code": "NAV_STALE",
                "message": f"max_nav_date lag {lag}d > {args.alert_if_nav_lag_days}",
            }
        )
    schemes = int(metrics.get("schemes") or 0)
    if schemes < args.alert_if_schemes_below:
        alerts.append(
            {
                "level": "CRIT",
                "code": "SCH_SHRINK",
                "message": f"scheme_count {schemes} < {args.alert_if_schemes_below}",
            }
        )
    if int(metrics.get("nav_points") or 0) == 0:
        alerts.append(
            {"level": "CRIT", "code": "NAV_EMPTY", "message": "no NAV points ingested"}
        )

    METRICS.parent.mkdir(parents=True, exist_ok=True)
    METRICS.write_text(json.dumps(metrics, indent=2), encoding="utf-8")
    ALERT_LOG.parent.mkdir(parents=True, exist_ok=True)
    with ALERT_LOG.open("a", encoding="utf-8") as f:
        for a in alerts:
            row = {"ts": metrics["checked_at"], **a, "metrics": metrics}
            f.write(json.dumps(row) + "\n")
            print("ALERT", a, flush=True)

    print("OK metrics", {k: metrics.get(k) for k in ("schemes", "nav_points", "max_nav_date", "nav_lag_days")}, flush=True)
    if any(a["level"] == "CRIT" for a in alerts):
        return 1
    if args.fail_on_stale and any(a["code"] == "NAV_STALE" for a in alerts):
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
