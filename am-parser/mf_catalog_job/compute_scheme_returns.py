"""Compute mf_scheme_returns from Postgres mf_nav_eod (never invent thin history).

Run after catalog push so dig SoT is Postgres. Same lookbacks as am-parser _pct_return:
  1M=30d, 3M=90d, 1Y=365d, 3Y=3*365d, 5Y=5*365d.

Env: AM_MARKET_MF_JDBC_URL, AM_MARKET_MF_JDBC_USER, AM_MARKET_MF_JDBC_PASSWORD
"""
from __future__ import annotations

import os
import sys
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Optional
ROOT = Path(__file__).resolve().parent
# Optional local dotenv (CronJob uses Vault env; no mf_service under am-parser).
ENV = ROOT / "data" / ".env"
CHUNK = int(os.environ.get("MF_RETURNS_CHUNK", "200"))

# Same env-safe DSN parse as push (never rewrite DB path to dig).
from _push_core import dig_dsn, load_dotenv  # noqa: E402


def require_jdbc() -> tuple[str, str, str, str]:
    url = (os.environ.get("AM_MARKET_MF_JDBC_URL") or "").strip()
    user = (os.environ.get("AM_MARKET_MF_JDBC_USER") or "").strip()
    password = (os.environ.get("AM_MARKET_MF_JDBC_PASSWORD") or "").strip()
    if not url:
        raise SystemExit("FAIL: AM_MARKET_MF_JDBC_URL unset")
    if not user or not password:
        raise SystemExit("FAIL: AM_MARKET_MF_JDBC_USER/PASSWORD unset")
    dsn, schema = dig_dsn(url)
    return dsn, schema, user, password

def _to_date(v: object) -> Optional[date]:
    if v is None:
        return None
    if isinstance(v, datetime):
        return v.date()
    if isinstance(v, date):
        return v
    try:
        return date.fromisoformat(str(v)[:10])
    except ValueError:
        return None


def pct_return(dates: list[date], navs: list[float], lookback_days: int) -> Optional[float]:
    """Simple % change over lookback; null when history is too thin (never invent)."""
    if not dates or not navs:
        return None
    last = len(dates) - 1
    end = dates[last]
    target = end - timedelta(days=lookback_days)
    end_nav = navs[last]
    start_nav: Optional[float] = None
    for i in range(last, -1, -1):
        if dates[i] <= target:
            start_nav = navs[i]
            break
    if start_nav is None or start_nav == 0:
        # Only fall back to first point when span covers the lookback window.
        if dates[0] <= target:
            start_nav = navs[0]
        else:
            return None
    return ((end_nav - start_nav) / start_nav) * 100.0


def ensure_returns_table(cur) -> None:
    cur.execute(
        """
        CREATE TABLE IF NOT EXISTS mf_scheme_returns (
          isin VARCHAR(16) PRIMARY KEY,
          scheme_code VARCHAR(64),
          ret_1m DOUBLE PRECISION,
          ret_3m DOUBLE PRECISION,
          ret_1y DOUBLE PRECISION,
          ret_3y DOUBLE PRECISION,
          ret_5y DOUBLE PRECISION,
          as_of VARCHAR(16),
          point_count INTEGER,
          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
        """
    )
    cur.execute(
        "CREATE INDEX IF NOT EXISTS idx_mf_scheme_returns_scheme "
        "ON mf_scheme_returns(scheme_code)"
    )


def main() -> int:
    import psycopg2
    from psycopg2.extras import execute_batch

    sys.stdout.reconfigure(line_buffering=True)
    load_dotenv(ENV)
    dsn, schema, user, password = require_jdbc()
    print(f"returns connect host={urlparse(dsn).hostname} schema={schema}…", flush=True)
    pg = psycopg2.connect(dsn, user=user, password=password, connect_timeout=30)
    pg.autocommit = False
    cur = pg.cursor()
    cur.execute(f"SET search_path TO {schema}, public")
    ensure_returns_table(cur)
    pg.commit()

    # Prefer ISIN key; fall back to scheme_code when ISIN blank.
    cur.execute(
        """
        SELECT COALESCE(NULLIF(TRIM(isin), ''), scheme_code) AS ret_key,
               MAX(scheme_code) AS scheme_code,
               array_agg(nav_date ORDER BY nav_date) AS dates,
               array_agg(nav ORDER BY nav_date) AS navs
        FROM mf_nav_eod
        GROUP BY COALESCE(NULLIF(TRIM(isin), ''), scheme_code)
        """
    )
    groups = cur.fetchall()
    print(f"schemes with NAV history: {len(groups)}", flush=True)

    rows: list[tuple] = []
    for ret_key, scheme_code, dates_raw, navs_raw in groups:
        dates: list[date] = []
        navs: list[float] = []
        for d, n in zip(dates_raw or [], navs_raw or []):
            dd = _to_date(d)
            if dd is None or n is None:
                continue
            dates.append(dd)
            navs.append(float(n))
        if not dates:
            continue
        isin = ret_key if len(str(ret_key)) <= 16 else str(ret_key)[:16]
        as_of = dates[-1].isoformat()
        rows.append(
            (
                isin,
                scheme_code,
                pct_return(dates, navs, 30),
                pct_return(dates, navs, 90),
                pct_return(dates, navs, 365),
                pct_return(dates, navs, 365 * 3),
                pct_return(dates, navs, 365 * 5),
                as_of,
                len(dates),
            )
        )

    sql = """
        INSERT INTO mf_scheme_returns (
          isin, scheme_code, ret_1m, ret_3m, ret_1y, ret_3y, ret_5y, as_of, point_count, updated_at
        ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s, NOW())
        ON CONFLICT (isin) DO UPDATE SET
          scheme_code = EXCLUDED.scheme_code,
          ret_1m = EXCLUDED.ret_1m,
          ret_3m = EXCLUDED.ret_3m,
          ret_1y = EXCLUDED.ret_1y,
          ret_3y = EXCLUDED.ret_3y,
          ret_5y = EXCLUDED.ret_5y,
          as_of = EXCLUDED.as_of,
          point_count = EXCLUDED.point_count,
          updated_at = NOW()
    """
    for i in range(0, len(rows), CHUNK):
        batch = rows[i : i + CHUNK]
        execute_batch(cur, sql, batch, page_size=min(CHUNK, 50))
        pg.commit()
        print(f"  mf_scheme_returns: {min(i + len(batch), len(rows))}/{len(rows)}", flush=True)

    cur.execute("SELECT COUNT(*), COUNT(ret_1y), COUNT(ret_3y), COUNT(ret_5y) FROM mf_scheme_returns")
    total, c1y, c3y, c5y = cur.fetchone()
    print(
        f"OK returns total={total} with_1y={c1y} with_3y={c3y} with_5y={c5y} "
        f"(nulls expected until multi-year mf_nav_eod)",
        flush=True,
    )
    cur.close()
    pg.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
