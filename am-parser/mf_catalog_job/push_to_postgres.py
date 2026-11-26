#!/usr/bin/env python3
"""Push local SQLite mf_catalog.db into Postgres (Phase 0 productionize).

Env:
  AM_MARKET_MF_JDBC_URL   e.g. jdbc:postgresql://host:5432/mf_catalog
                          or postgresql://user:pass@host:5432/mf_catalog
  AM_MARKET_MF_JDBC_USER / AM_MARKET_MF_JDBC_PASSWORD  (if not in URL)
  MF_CATALOG_SQLITE       optional path to SQLite (default: mf_catalog_job/data/mf_catalog.db)

Applies V1__mf_catalog.sql then copies mf_scheme + mf_nav_eod (+ rules if present).
"""
from __future__ import annotations

import os
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SQLITE = Path(os.environ.get("MF_CATALOG_SQLITE", ROOT / "data" / "mf_catalog.db"))
ENV_FILE = ROOT.parent / "mf_service" / ".env"
MIGRATION = (
    ROOT.parents[1]
    / "am-market"
    / "am-market-data"
    / "market-data-api"
    / "src"
    / "main"
    / "resources"
    / "db"
    / "migration"
    / "V1__mf_catalog.sql"
)


def load_dotenv(path: Path) -> None:
    if not path.is_file():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, val = line.partition("=")
        key = key.strip()
        if key and key not in os.environ:
            os.environ[key] = val


DIG_FALLBACK_DSN = "postgresql://postgres.asrax.in:5432/user_platform_dev"
DIG_FALLBACK_SCHEMA = "mf_catalog"


def strip_query(dsn: str) -> str:
    return dsn.split("?", 1)[0]


def connect_kwargs(dsn: str, user: str | None, password: str | None) -> dict:
    base = strip_query(dsn)
    kwargs: dict = {"dsn": base, "connect_timeout": 30}
    if "://" in base and "@" in base:
        return {"dsn": base, "connect_timeout": 30}
    if user:
        kwargs["user"] = user
    if password:
        kwargs["password"] = password
    return kwargs


def ensure_target(dsn: str, user: str | None, password: str | None) -> tuple[str, str | None]:
    """Return (effective_dsn, schema). Prefer dedicated mf_catalog DB; dig falls back to schema."""
    import psycopg2
    from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT

    # Try CREATE DATABASE mf_catalog when URL targets that name
    if "/mf_catalog" in strip_query(dsn):
        maint = re.sub(r"/[^/?]+(\?.*)?$", r"/postgres\1", strip_query(dsn))
        try:
            conn = psycopg2.connect(**connect_kwargs(maint, user, password))
            conn.set_isolation_level(ISOLATION_LEVEL_AUTOCOMMIT)
            cur = conn.cursor()
            cur.execute("SELECT 1 FROM pg_database WHERE datname = 'mf_catalog'")
            if cur.fetchone() is None:
                cur.execute("CREATE DATABASE mf_catalog")
                print("Created database mf_catalog")
            else:
                print("Database mf_catalog already exists")
            cur.close()
            conn.close()
            return strip_query(dsn), None
        except Exception as e:
            print(f"CREATE DATABASE mf_catalog blocked ({type(e).__name__}); using dig fallback")
            dsn = DIG_FALLBACK_DSN

    # Schema-hosted catalog (dig_vault_seed pattern)
    schema = DIG_FALLBACK_SCHEMA
    conn = psycopg2.connect(**connect_kwargs(dsn, user, password))
    conn.autocommit = True
    cur = conn.cursor()
    cur.execute(f"CREATE SCHEMA IF NOT EXISTS {schema}")
    print(f"Using database {strip_query(dsn).rsplit('/', 1)[-1]} schema {schema}")
    cur.close()
    conn.close()
    return strip_query(dsn), schema

# Dig browse needs scheme+NAV only; set MF_PUSH_FULL=1 to include SIP/STP/SWP rules.
_CORE_TABLES = ["mf_scheme", "mf_nav_eod"]
_FULL_TABLES = _CORE_TABLES + [
    "mf_sip_rule",
    "mf_stp_rule",
    "mf_swp_rule",
    "mf_settlement",
    "mf_scheme_enrichment",
]
TABLES = _FULL_TABLES if os.environ.get("MF_PUSH_FULL", "").strip() in ("1", "true", "yes") else _CORE_TABLES


def parse_jdbc(url: str) -> tuple[str, str | None, str | None]:
    """Convert jdbc:postgresql://... to psycopg DSN pieces."""
    u = url.strip()
    if u.startswith("jdbc:"):
        u = u[len("jdbc:") :]
    if u.startswith("postgresql://") or u.startswith("postgres://"):
        return u, None, None
    # jdbc style host:port/db already stripped of jdbc:
    if u.startswith("postgresql:"):
        # postgresql://host:5432/db
        return u.replace("postgresql:", "postgresql://", 1) if "://" not in u else u, None, None
    raise SystemExit(f"Unsupported JDBC URL: {url}")


def main() -> int:
    try:
        import psycopg2
        from psycopg2.extras import execute_batch
    except ImportError:
        print("Install psycopg2-binary: pip install psycopg2-binary", file=sys.stderr)
        return 1

    # Unbuffered progress when piped (Cursor terminal)
    try:
        sys.stdout.reconfigure(line_buffering=True)
    except Exception:
        pass

    load_dotenv(ENV_FILE)
    print(f"SQLite={SQLITE} exists={SQLITE.is_file()}")
    jdbc = os.environ.get("AM_MARKET_MF_JDBC_URL", "").strip()
    if not jdbc:
        print("Set AM_MARKET_MF_JDBC_URL", file=sys.stderr)
        return 1
    if not SQLITE.is_file():
        print(f"SQLite missing: {SQLITE}", file=sys.stderr)
        return 1

    dsn, _, _ = parse_jdbc(jdbc)
    user = os.environ.get("AM_MARKET_MF_JDBC_USER")
    password = os.environ.get("AM_MARKET_MF_JDBC_PASSWORD")
    dsn, schema = ensure_target(dsn, user, password)

    pg = psycopg2.connect(**connect_kwargs(dsn, user, password))
    pg.autocommit = False
    cur = pg.cursor()
    if schema:
        cur.execute(f"SET search_path TO {schema}, public")

    if MIGRATION.is_file():
        sql = MIGRATION.read_text(encoding="utf-8")
        cur.execute(sql)
        print(f"Applied migration {MIGRATION.name}")
    else:
        print(f"WARN: migration not found at {MIGRATION}", file=sys.stderr)

    sq = sqlite3.connect(SQLITE)
    sq.row_factory = sqlite3.Row

    for table in TABLES:
        try:
            rows = sq.execute(f"SELECT * FROM {table}").fetchall()
        except sqlite3.OperationalError:
            print(f"skip {table} (not in SQLite)")
            continue
        if not rows:
            print(f"{table}: 0 rows")
            continue
        cols = rows[0].keys()
        col_list = ", ".join(cols)
        placeholders = ", ".join(["%s"] * len(cols))
        # Upsert: use first column as conflict for enrichment/scheme; nav uses composite
        if table == "mf_scheme":
            updates = ", ".join(f"{c}=EXCLUDED.{c}" for c in cols if c != "scheme_code")
            sql = (
                f"INSERT INTO {table} ({col_list}) VALUES ({placeholders}) "
                f"ON CONFLICT (scheme_code) DO UPDATE SET {updates}"
            )
        elif table == "mf_nav_eod":
            updates = ", ".join(f"{c}=EXCLUDED.{c}" for c in cols if c not in ("scheme_code", "nav_date"))
            sql = (
                f"INSERT INTO {table} ({col_list}) VALUES ({placeholders}) "
                f"ON CONFLICT (scheme_code, nav_date) DO UPDATE SET {updates}"
            )
        elif table == "mf_scheme_enrichment":
            updates = ", ".join(f"{c}=EXCLUDED.{c}" for c in cols if c != "isin")
            sql = (
                f"INSERT INTO {table} ({col_list}) VALUES ({placeholders}) "
                f"ON CONFLICT (isin) DO UPDATE SET {updates}"
            )
        else:
            # rules/settlement — delete+insert by table for simplicity on full refresh
            cur.execute(f"DELETE FROM {table}")
            sql = f"INSERT INTO {table} ({col_list}) VALUES ({placeholders})"

        data = [tuple(r[c] for c in cols) for r in rows]
        # Drop SQLite autoincrement id on rules if present and PG uses BIGSERIAL — keep if column exists
        execute_batch(cur, sql, data, page_size=500)
        print(f"{table}: upserted {len(data)}")

    pg.commit()
    cur.close()
    pg.close()
    sq.close()
    print("OK — Postgres catalog synced")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
