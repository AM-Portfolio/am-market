#!/usr/bin/env python3
"""Live probe: dig Postgres + optional HTTP bases. Never prints secrets."""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, unquote, urlparse
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[3]  # Asrax/
MF_ENV = ROOT / "am-mutual-fund" / "mf_service" / ".env"


def load_env(path: Path) -> None:
    if not path.is_file():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, _, v = line.partition("=")
        k, v = k.strip(), v.strip().strip('"').strip("'")
        if k and k not in os.environ:
            os.environ[k] = v


def parse_jdbc(jdbc: str) -> dict:
    raw = jdbc[5:] if jdbc.lower().startswith("jdbc:") else jdbc
    p = urlparse(raw)
    qs = parse_qs(p.query or "")
    schema = (qs.get("currentSchema") or qs.get("currentschema") or ["public"])[0]
    dbname = unquote((p.path or "/").lstrip("/").split("/")[0])
    return {
        "host": p.hostname,
        "port": p.port or 5432,
        "dbname": dbname,
        "schema": schema,
        "options": f"-c search_path={schema}",
    }


def probe_pg() -> int:
    import psycopg

    user = os.environ.get("AM_MARKET_MF_JDBC_USER", "").strip()
    password = os.environ.get("AM_MARKET_MF_JDBC_PASSWORD", "")
    candidates = []
    if os.environ.get("AM_MARKET_MF_JDBC_URL", "").strip():
        candidates.append(os.environ["AM_MARKET_MF_JDBC_URL"].strip())
    candidates.append(
        "jdbc:postgresql://postgres.asrax.in:5432/user_platform_dev?currentSchema=mf_catalog"
    )
    seen = set()
    for jdbc in candidates:
        if jdbc in seen:
            continue
        seen.add(jdbc)
        p = parse_jdbc(jdbc)
        label = f"{p['host']}:{p['port']}/{p['dbname']} schema={p['schema']}"
        print(f"PG try {label}")
        try:
            with psycopg.connect(
                host=p["host"],
                port=p["port"],
                dbname=p["dbname"],
                user=user,
                password=password,
                options=p["options"],
                connect_timeout=20,
            ) as conn:
                with conn.cursor() as cur:
                    cur.execute(
                        "SELECT COUNT(*), COUNT(DISTINCT isin) FROM mf_scheme "
                        "WHERE isin IS NOT NULL AND isin <> ''"
                    )
                    schemes, isins = cur.fetchone()
                    cur.execute("SELECT COUNT(*), MAX(nav_date)::text FROM mf_nav_eod")
                    navs, maxd = cur.fetchone()
                    cur.execute(
                        "SELECT scheme_code, LEFT(scheme_name, 48), isin, "
                        "latest_nav::float8, latest_nav_date::text "
                        "FROM mf_scheme WHERE LOWER(scheme_name) LIKE %s "
                        "ORDER BY scheme_name LIMIT 5",
                        ("%hdfc%",),
                    )
                    rows = cur.fetchall()
            print(f"PG OK schemes={schemes} isins={isins} nav_eod={navs} max_nav_date={maxd}")
            for r in rows:
                print(
                    f"  SAMPLE code={r[0]} name={r[1]!r} isin={r[2]} nav={r[3]} navDate={r[4]}"
                )
            if schemes < 100 or navs < 1 or not rows or rows[0][3] is None:
                return 2
            return 0
        except Exception as e:
            print(f"PG FAIL {type(e).__name__}: {e}")
    return 1


def probe_http(name: str, url: str, headers: dict | None = None) -> tuple[int, str]:
    hdrs = {"Accept": "application/json", **(headers or {})}
    try:
        req = Request(url, headers=hdrs)
        with urlopen(req, timeout=20) as resp:
            body = resp.read().decode("utf-8", errors="replace")
            code = resp.status
    except HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        code = e.code
    except URLError as e:
        print(f"HTTP {name} FAIL connect: {e.reason}")
        return 1, ""
    snip = body[:400].replace("\n", " ")
    print(f"HTTP {name} status={code} url={url}")
    print(f"  body={snip}")
    return code, body


def probe_service_via_catalog_module() -> int:
    """Exercise new parser MfCatalogService against dig JDBC (no HTTP needed)."""
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    from am_mf_catalog.service import MfCatalogService

    jdbc = (
        os.environ.get("AM_MARKET_MF_JDBC_URL")
        or "jdbc:postgresql://postgres.asrax.in:5432/user_platform_dev?currentSchema=mf_catalog"
    )
    # Prefer platform DB if .env points at wrong db name
    if "user_platform_dev" not in jdbc and "mf_catalog" in jdbc and "currentSchema" not in jdbc:
        jdbc = "jdbc:postgresql://postgres.asrax.in:5432/user_platform_dev?currentSchema=mf_catalog"
        print("SERVICE using platform_dev JDBC (env URL was db=mf_catalog without schema)")
    svc = MfCatalogService(
        jdbc,
        os.environ.get("AM_MARKET_MF_JDBC_USER", ""),
        os.environ.get("AM_MARKET_MF_JDBC_PASSWORD", ""),
    )
    out = svc.search_schemes("HDFC", None, None, 5)
    if out.get("error"):
        print(f"SERVICE search FAIL error={out.get('error')} message={out.get('message')}")
        return 1
    items = out.get("items") or []
    print(
        f"SERVICE search OK source={out.get('source')} priceType={out.get('priceType')} "
        f"count={out.get('count')}"
    )
    for it in items[:5]:
        print(
            f"  item code={it.get('schemeCode')} nav={it.get('nav')} "
            f"navDate={it.get('navDate')} name={(it.get('schemeName') or '')[:40]!r}"
        )
    if not items or items[0].get("nav") is None:
        return 2
    code = items[0]["schemeCode"]
    details = svc.get_scheme_details(code)
    print(
        f"SERVICE details OK code={details and details.get('schemeCode')} "
        f"nav={details and details.get('nav')} navDate={details and details.get('navDate')}"
    )
    return 0 if details and details.get("nav") is not None else 2


def main() -> int:
    load_env(MF_ENV)
    print("=== 1) Postgres dig mf_catalog ===")
    pg = probe_pg()
    print("\n=== 2) New parser MfCatalogService (local, dig JDBC) ===")
    svc = probe_service_via_catalog_module()
    print("\n=== 3) Dig ingress HTTP (may be SPA/auth blocked) ===")
    probe_http(
        "parser-search",
        "https://am-dev.asrax.in/parser/v1/mutual-fund/search?q=HDFC&limit=3",
    )
    probe_http(
        "market-search",
        "https://am-dev.asrax.in/market/v1/market-data/mutual-fund/search?q=HDFC&limit=3",
    )
    probe_http(
        "bff-schemes",
        "https://am-dev.asrax.in/router/am/mf/v1/mf/schemes?q=HDFC&limit=3",
        {"X-User-Id": "smoke-nav"},
    )
    print("\n=== SUMMARY ===")
    print(f"postgres_data={'OK' if pg == 0 else 'FAIL/SPARSE'}")
    print(f"parser_service_local={'OK' if svc == 0 else 'FAIL/SPARSE'}")
    print(
        "cluster_note: am-parser-dev was scaled to 0; new /v1/mutual-fund routes "
        "are not deployed yet — ingress /parser currently returns Flutter HTML."
    )
    return 0 if pg == 0 and svc == 0 else (pg or svc)


if __name__ == "__main__":
    sys.exit(main())
