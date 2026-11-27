"""Dig-safe push: mf_scheme + mf_nav_eod (+ enrichment) into Postgres mf_catalog schema.

Chunked commits with progress. Skips Flyway replay when tables already exist (dig is slow/locked).

Secrets: AM_MARKET_MF_JDBC_URL (+ USER/PASSWORD). No hardcoded host. Never logs credentials.
"""
from __future__ import annotations

import os
import sqlite3
import sys
from pathlib import Path
from urllib.parse import parse_qs, urlencode, urlparse, urlunparse

ROOT = Path(__file__).resolve().parent
SQLITE = ROOT / "data" / "mf_catalog.db"
# Optional local dotenv (CronJob uses Vault env; no mf_service under am-parser).
ENV = ROOT / "data" / ".env"


def _resolve_migration() -> Path:
    """Postgres DDL for empty schemas. Prefer vendored copy (Docker-safe)."""
    override = (os.environ.get("MF_CATALOG_MIGRATION_SQL") or "").strip()
    if override:
        p = Path(override)
        if p.is_file():
            return p
        raise SystemExit(f"FAIL: MF_CATALOG_MIGRATION_SQL not a file: {override}")
    vendored = ROOT / "V1__mf_catalog.sql"
    if vendored.is_file():
        return vendored
    # Dev checkout fallback: am-market/am-market-data/.../V1__mf_catalog.sql
    sibling = (
        ROOT.parents[1]
        / "am-market-data"
        / "market-data-api"
        / "src"
        / "main"
        / "resources"
        / "db"
        / "migration"
        / "V1__mf_catalog.sql"
    )
    if sibling.is_file():
        return sibling
    raise SystemExit(
        "FAIL: V1__mf_catalog.sql missing next to _push_core.py "
        "(required for empty Postgres; dig with existing tables skips migrate)"
    )


MIGRATION = _resolve_migration()
CORE = ("mf_scheme", "mf_nav_eod")
CHUNK = int(os.environ.get("MF_CATALOG_PUSH_CHUNK", "100"))


def load_dotenv(path: Path) -> None:
    if not path.is_file():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, val = line.partition("=")
        key = key.strip()
        val = val.strip().strip('"').strip("'")
        if key and key not in os.environ:
            os.environ[key] = val


def dig_dsn(jdbc_or_url: str) -> tuple[str, str]:
    """Return (psycopg2 dsn without password, schema).

    Never rewrite the database path. CronJobs must set the exact DB
    (user_platform / user_platform_preprod / user_platform_dev) via
    AM_MARKET_MF_JDBC_URL; rewriting /mf_catalog → user_platform_dev
    would dump prod/preprod into dig.
    """
    raw = jdbc_or_url.replace("jdbc:", "").strip()
    parsed = urlparse(raw)
    qs = parse_qs(parsed.query)
    schema = (qs.get("currentSchema") or ["mf_catalog"])[0]
    path = (parsed.path or "").rstrip("/") or "/"
    if path in ("/", ""):
        raise ValueError("AM_MARKET_MF_JDBC_URL missing database path")
    qs.pop("currentSchema", None)
    query = urlencode({k: v[0] for k, v in qs.items()}) if qs else ""
    # Strip any embedded userinfo from URL — credentials come from env only.
    host = parsed.hostname or ""
    if not host:
        raise ValueError("AM_MARKET_MF_JDBC_URL missing host")
    netloc = host
    if parsed.port:
        netloc = f"{host}:{parsed.port}"
    dsn = urlunparse((parsed.scheme or "postgresql", netloc, path, "", query, ""))
    return dsn, schema


def require_jdbc() -> tuple[str, str, str | None, str | None]:
    url = (os.environ.get("AM_MARKET_MF_JDBC_URL") or "").strip()
    user = (os.environ.get("AM_MARKET_MF_JDBC_USER") or "").strip() or None
    password = (os.environ.get("AM_MARKET_MF_JDBC_PASSWORD") or "").strip() or None
    if not url:
        raise SystemExit("FAIL: AM_MARKET_MF_JDBC_URL unset (env-only; no hardcoded DSN)")
    if not user or not password:
        raise SystemExit("FAIL: AM_MARKET_MF_JDBC_USER/PASSWORD unset")
    dsn, schema = dig_dsn(url)
    return dsn, schema, user, password


def split_sql(sql: str) -> list[str]:
    parts: list[str] = []
    buf: list[str] = []
    for line in sql.splitlines():
        if line.strip().startswith("--"):
            continue
        buf.append(line)
        if line.rstrip().endswith(";"):
            stmt = "\n".join(buf).strip().rstrip(";").strip()
            if stmt:
                parts.append(stmt)
            buf = []
    return parts


def upsert_chunks(cur, pg, table: str, cols: list[str], data: list[tuple], conflict_sql: str) -> None:
    from psycopg2.extras import execute_batch

    col_list = ", ".join(cols)
    placeholders = ", ".join(["%s"] * len(cols))
    sql = f"INSERT INTO {table} ({col_list}) VALUES ({placeholders}) {conflict_sql}"
    total = len(data)
    for i in range(0, total, CHUNK):
        batch = data[i : i + CHUNK]
        execute_batch(cur, sql, batch, page_size=min(CHUNK, 50))
        pg.commit()
        done = min(i + len(batch), total)
        print(f"  {table}: {done}/{total}", flush=True)


def main() -> int:
    import psycopg2

    sys.stdout.reconfigure(line_buffering=True)
    load_dotenv(ENV)
    dsn, schema, user, password = require_jdbc()
    # Never print DSN with credentials — host/db only.
    print(f"connect host={urlparse(dsn).hostname} schema={schema}…", flush=True)
    pg = psycopg2.connect(dsn, user=user, password=password, connect_timeout=30)
    pg.autocommit = False
    cur = pg.cursor()
    cur.execute("SET statement_timeout = '300s'")
    print("schema…", flush=True)
    cur.execute(f"CREATE SCHEMA IF NOT EXISTS {schema}")
    cur.execute(f"SET search_path TO {schema}, public")
    pg.commit()

    cur.execute(
        "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
        "WHERE table_schema=%s AND table_name='mf_scheme')",
        (schema,),
    )
    has_scheme = bool(cur.fetchone()[0])
    if has_scheme:
        print("migrate… skip (mf_scheme already present); ensure prev_nav cols…", flush=True)
        for stmt in (
            "ALTER TABLE mf_scheme ADD COLUMN IF NOT EXISTS prev_nav DOUBLE PRECISION",
            "ALTER TABLE mf_scheme ADD COLUMN IF NOT EXISTS prev_nav_date VARCHAR(16)",
            "ALTER TABLE mf_scheme ADD COLUMN IF NOT EXISTS change_1d_pct DOUBLE PRECISION",
        ):
            try:
                cur.execute(stmt)
                pg.commit()
            except Exception as e:
                pg.rollback()
                cur.execute(f"SET search_path TO {schema}, public")
                pg.commit()
                print(f"  prev_nav alter skip ({type(e).__name__}: {e})", flush=True)
    else:
        print("migrate…", flush=True)
        for i, stmt in enumerate(split_sql(MIGRATION.read_text(encoding="utf-8")), 1):
            # Indexes on dig can lock forever — create tables first, defer indexes.
            if stmt.lstrip().upper().startswith("CREATE INDEX"):
                print(f"  stmt {i} skip index (deferred)", flush=True)
                continue
            try:
                cur.execute("SET statement_timeout = '300s'")
                cur.execute(stmt)
                pg.commit()
                print(f"  stmt {i} ok", flush=True)
            except Exception as e:
                pg.rollback()
                cur.execute(f"SET search_path TO {schema}, public")
                pg.commit()
                name = type(e).__name__
                if "Duplicate" in name or "UniqueViolation" in name or "AlreadyExists" in str(e):
                    print(f"  stmt {i} skip ({name})", flush=True)
                    continue
                print(f"  stmt {i} ERR {name}: {e}", flush=True)
                raise
        # Apply deferred indexes after tables exist (empty greenfield preprod/prod).
        print("migrate… indexes…", flush=True)
        for i, stmt in enumerate(split_sql(MIGRATION.read_text(encoding="utf-8")), 1):
            if not stmt.lstrip().upper().startswith("CREATE INDEX"):
                continue
            try:
                cur.execute("SET statement_timeout = '120s'")
                cur.execute(stmt)
                pg.commit()
                print(f"  index {i} ok", flush=True)
            except Exception as e:
                pg.rollback()
                cur.execute(f"SET search_path TO {schema}, public")
                pg.commit()
                print(f"  index {i} skip ({type(e).__name__}: {e})", flush=True)
        cur.execute(f"SET search_path TO {schema}, public")

    cur.execute("SET statement_timeout = '180s'")
    sq = sqlite3.connect(SQLITE)
    sq.row_factory = sqlite3.Row
    for table in CORE:
        print(f"read {table}…", flush=True)
        rows = sq.execute(f"SELECT * FROM {table}").fetchall()
        if not rows:
            print(f"{table}: 0", flush=True)
            continue
        cols = list(rows[0].keys())
        if table == "mf_scheme":
            updates = ", ".join(f"{c}=EXCLUDED.{c}" for c in cols if c != "scheme_code")
            conflict = f"ON CONFLICT (scheme_code) DO UPDATE SET {updates}"
        else:
            updates = ", ".join(
                f"{c}=EXCLUDED.{c}" for c in cols if c not in ("scheme_code", "nav_date")
            )
            conflict = f"ON CONFLICT (scheme_code, nav_date) DO UPDATE SET {updates}"
        data = [tuple(r[c] for c in cols) for r in rows]
        print(f"upsert {table} n={len(data)} chunk={CHUNK}…", flush=True)
        upsert_chunks(cur, pg, table, cols, data, conflict)
        print(f"{table}: upserted {len(data)}", flush=True)

    try:
        erows = sq.execute("SELECT * FROM mf_scheme_enrichment").fetchall()
    except sqlite3.OperationalError:
        erows = []
    if erows:
        # Ensure enrichment table exists without full migrate
        cur.execute(
            """CREATE TABLE IF NOT EXISTS mf_scheme_enrichment (
                 isin VARCHAR(16) PRIMARY KEY,
                 aum DOUBLE PRECISION,
                 expense_ratio DOUBLE PRECISION,
                 riskometer VARCHAR(64),
                 sebi_category VARCHAR(128),
                 sebi_sub_category VARCHAR(128),
                 source VARCHAR(64),
                 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
               )"""
        )
        pg.commit()
        cols = list(erows[0].keys())
        updates = ", ".join(f"{c}=EXCLUDED.{c}" for c in cols if c != "isin")
        conflict = f"ON CONFLICT (isin) DO UPDATE SET {updates}"
        data = [tuple(r[c] for c in cols) for r in erows]
        print(f"upsert mf_scheme_enrichment n={len(data)}…", flush=True)
        upsert_chunks(cur, pg, "mf_scheme_enrichment", cols, data, conflict)

    cur.execute(f"SELECT COUNT(*), COUNT(DISTINCT isin) FROM {schema}.mf_scheme")
    print("mf_scheme counts =>", cur.fetchone(), flush=True)
    cur.execute(f"SELECT COUNT(*) FROM {schema}.mf_nav_eod")
    print("mf_nav_eod count =>", cur.fetchone()[0], flush=True)
    try:
        cur.execute(f"SELECT COUNT(*) FROM {schema}.mf_scheme_enrichment")
        print("mf_scheme_enrichment count =>", cur.fetchone()[0], flush=True)
    except Exception as e:
        print(f"mf_scheme_enrichment count => skip ({e})", flush=True)
    cur.close()
    pg.close()
    sq.close()
    print("OK", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
