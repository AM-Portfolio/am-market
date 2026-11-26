"""Parse NSE MASTER_DOWNLOAD pipe files into SQLite mf_catalog.db (idempotent upserts)."""
from __future__ import annotations

import argparse
import sqlite3
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SCHEMA = ROOT / "schema.sql"
DEFAULT_DB = ROOT / "data" / "mf_catalog.db"
# Local-only (gitignored). Produced by mf_catalog_job/download_masters.py / daily_job.
MASTERS_DIR = ROOT / "data" / "masters"

SCH_COLS = [
    "unique_sr_no",
    "scheme_code",
    "rta_scheme_code",
    "amc_scheme_code",
    "isin",
    "amc_code",
    "scheme_type",
    "plan_type",
    "scheme_name",
    "purchase_allowed",
    "purchase_transaction_mode",
    "new_purchase_min_amount",
    "additional_purchase_min_amount",
    "additional_purchase_max_amount",
    "purchase_amount_multiplier",
    "purchase_cutoff_time",
    "redemption_allowed",
    "redemption_transaction_mode",
    "redemption_min_qty",
    "redemption_qty_multiplier",
    "redemption_max_qty",
    "redemption_min_amount",
    "redemption_max_amount",
    "redemption_amount_multiplier",
    "redemption_cutoff_time",
    "rta_agent_code",
    "amc_active_flag",
    "div_reinvest_flag",
    "sip_allowed",
    "stp_enabled",
    "swp_enabled",
    "switch_allowed",
    "settlement_type",
    "amc_ind",
    "face_value",
    "scheme_start_date",
    "maturity_date",
    "exit_load_flag",
    "exit_load",
    "lock_in_period_flag",
    "lock_in_period",
    "channel_partner_code",
    "reopening_date",
    "open_close_ended",
]


def _ensure_scheme_prev_nav_columns(conn: sqlite3.Connection) -> None:
    """Existing DBs keep CREATE TABLE IF NOT EXISTS; add prev_* columns if missing."""
    cols = {r[1] for r in conn.execute("PRAGMA table_info(mf_scheme)").fetchall()}
    for name, decl in (
        ("prev_nav", "REAL"),
        ("prev_nav_date", "TEXT"),
        ("change_1d_pct", "REAL"),
    ):
        if name not in cols:
            conn.execute(f"ALTER TABLE mf_scheme ADD COLUMN {name} {decl}")
    conn.commit()


def connect(db_path: Path) -> sqlite3.Connection:
    db_path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(db_path))
    conn.execute("PRAGMA journal_mode=WAL")
    conn.executescript(SCHEMA.read_text(encoding="utf-8"))
    _ensure_scheme_prev_nav_columns(conn)
    return conn


def split_row(line: str) -> list[str]:
    return [c.strip() for c in line.rstrip("\n").split("|")]


def start_run(conn: sqlite3.Connection, file_type: str, source: Path) -> int:
    cur = conn.execute(
        "INSERT INTO mf_ingest_run(started_at, status, file_type, source_path, rows_read, rows_upserted, parse_errors) VALUES (?,?,?,?,0,0,0)",
        (datetime.now(timezone.utc).isoformat(), "RUNNING", file_type, str(source)),
    )
    conn.commit()
    return int(cur.lastrowid)


def finish_run(conn: sqlite3.Connection, run_id: int, **kwargs) -> None:
    cols = ", ".join(f"{k}=?" for k in kwargs)
    conn.execute(
        f"UPDATE mf_ingest_run SET finished_at=?, {cols} WHERE id=?",
        (datetime.now(timezone.utc).isoformat(), *kwargs.values(), run_id),
    )
    conn.commit()


def ingest_sch(conn: sqlite3.Connection, path: Path) -> dict:
    run_id = start_run(conn, "SCH", path)
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    if not lines:
        finish_run(conn, run_id, status="EMPTY", message="no lines")
        return {"file_type": "SCH", "rows": 0}
    # Skip header; skip blanks. NSE/UAT occasionally returns a duplicated body —
    # UNIQUE scheme_code upsert keeps a single row per code.
    data = [ln for ln in lines[1:] if ln.strip()]
    now = datetime.now(timezone.utc).isoformat()
    upserted = 0
    errors = 0
    seen: set[str] = set()
    sql = (
        "INSERT INTO mf_scheme("
        + ",".join(SCH_COLS)
        + ",updated_at) VALUES ("
        + ",".join("?" * len(SCH_COLS))
        + ",?) "
        "ON CONFLICT(scheme_code) DO UPDATE SET "
        + ",".join(f"{c}=excluded.{c}" for c in SCH_COLS if c != "scheme_code")
        + ",updated_at=excluded.updated_at"
    )
    for line in data:
        if not line.strip():
            continue
        parts = split_row(line)
        if len(parts) < 9:
            errors += 1
            continue
        while len(parts) < len(SCH_COLS):
            parts.append("")
        parts = parts[: len(SCH_COLS)]
        code = parts[1]
        if code in seen:
            continue
        seen.add(code)
        try:
            conn.execute(sql, (*parts, now))
            upserted += 1
        except sqlite3.Error:
            errors += 1
    conn.commit()
    scheme_count = conn.execute("SELECT COUNT(*) FROM mf_scheme").fetchone()[0]
    finish_run(
        conn,
        run_id,
        status="OK" if errors == 0 else "PARTIAL",
        rows_read=len(data),
        rows_upserted=upserted,
        parse_errors=errors,
        scheme_count=scheme_count,
        message=None,
    )
    return {
        "file_type": "SCH",
        "rows_read": len(data),
        "unique_codes": len(seen),
        "upserted": upserted,
        "errors": errors,
        "scheme_count": scheme_count,
    }


def ingest_nav(conn: sqlite3.Connection, path: Path) -> dict:
    run_id = start_run(conn, "NAV", path)
    lines = [ln for ln in path.read_text(encoding="utf-8", errors="replace").splitlines() if ln.strip()]
    upserted = 0
    errors = 0
    max_date = ""
    sql = (
        "INSERT INTO mf_nav_eod(scheme_code, nav_date, scheme_name, rta_scheme_code, div_flag, isin, nav, rta_agent) "
        "VALUES (?,?,?,?,?,?,?,?) "
        "ON CONFLICT(scheme_code, nav_date) DO UPDATE SET "
        "scheme_name=excluded.scheme_name, rta_scheme_code=excluded.rta_scheme_code, "
        "div_flag=excluded.div_flag, isin=excluded.isin, nav=excluded.nav, rta_agent=excluded.rta_agent"
    )
    for line in lines:
        parts = split_row(line)
        if len(parts) < 8:
            errors += 1
            continue
        nav_date, scheme_code, name, rta_code, div_flag, isin, nav_s, rta = parts[:8]
        try:
            nav = float(nav_s)
        except ValueError:
            errors += 1
            continue
        try:
            conn.execute(sql, (scheme_code, nav_date, name, rta_code, div_flag, isin, nav, rta))
            upserted += 1
            if nav_date > max_date:
                max_date = nav_date
        except sqlite3.Error:
            errors += 1
    # latest = newest EOD point; prev = second-newest (null if only one point).
    # Trust NSE nav_date — never stamp calendar today onto NAV.
    conn.execute(
        """
        UPDATE mf_scheme SET
          latest_nav = (
            SELECT n.nav FROM mf_nav_eod n
            WHERE n.scheme_code = mf_scheme.scheme_code
            ORDER BY n.nav_date DESC LIMIT 1
          ),
          latest_nav_date = (
            SELECT n.nav_date FROM mf_nav_eod n
            WHERE n.scheme_code = mf_scheme.scheme_code
            ORDER BY n.nav_date DESC LIMIT 1
          ),
          prev_nav = (
            SELECT n.nav FROM mf_nav_eod n
            WHERE n.scheme_code = mf_scheme.scheme_code
            ORDER BY n.nav_date DESC LIMIT 1 OFFSET 1
          ),
          prev_nav_date = (
            SELECT n.nav_date FROM mf_nav_eod n
            WHERE n.scheme_code = mf_scheme.scheme_code
            ORDER BY n.nav_date DESC LIMIT 1 OFFSET 1
          )
        WHERE scheme_code IN (SELECT DISTINCT scheme_code FROM mf_nav_eod)
        """
    )
    conn.execute(
        """
        UPDATE mf_scheme SET
          change_1d_pct = CASE
            WHEN prev_nav IS NULL OR prev_nav = 0 OR latest_nav IS NULL THEN NULL
            ELSE ((latest_nav - prev_nav) / prev_nav) * 100.0
          END
        WHERE scheme_code IN (SELECT DISTINCT scheme_code FROM mf_nav_eod)
        """
    )
    conn.commit()
    finish_run(
        conn,
        run_id,
        status="OK" if errors == 0 else "PARTIAL",
        rows_read=len(lines),
        rows_upserted=upserted,
        parse_errors=errors,
        max_nav_date=max_date or None,
        scheme_count=conn.execute("SELECT COUNT(*) FROM mf_scheme").fetchone()[0],
        message=None,
    )
    return {"file_type": "NAV", "rows_read": len(lines), "upserted": upserted, "errors": errors, "max_nav_date": max_date}


def ingest_sip(conn: sqlite3.Connection, path: Path) -> dict:
    run_id = start_run(conn, "SIP", path)
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    data = lines[1:] if lines else []
    upserted = errors = 0
    sql = (
        "INSERT INTO mf_sip_rule(amc_code,amc_name,scheme_code,scheme_name,sip_transaction_mode,sip_frequency,sip_dates,"
        "sip_minimum_gap,sip_maximum_gap,sip_installment_gap,sip_status,sip_min_installment_amount,sip_max_installment_amount,"
        "sip_multiplier_amount,sip_min_installment_numbers,sip_max_installment_numbers,scheme_isin,scheme_type,pause_flag,"
        "pause_min_installments,pause_max_installments,pause_modification_count) "
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
        "ON CONFLICT(scheme_code, sip_frequency, sip_dates, sip_transaction_mode) DO UPDATE SET "
        "amc_code=excluded.amc_code, amc_name=excluded.amc_name, scheme_name=excluded.scheme_name, "
        "sip_status=excluded.sip_status, sip_min_installment_amount=excluded.sip_min_installment_amount, "
        "scheme_isin=excluded.scheme_isin"
    )
    for line in data:
        if not line.strip():
            continue
        p = split_row(line)
        if len(p) < 22:
            errors += 1
            continue
        try:
            conn.execute(sql, tuple(p[:22]))
            upserted += 1
        except sqlite3.Error:
            errors += 1
    conn.commit()
    finish_run(conn, run_id, status="OK" if errors == 0 else "PARTIAL", rows_read=len(data), rows_upserted=upserted, parse_errors=errors, message=None)
    return {"file_type": "SIP", "upserted": upserted, "errors": errors}


def ingest_stp(conn: sqlite3.Connection, path: Path) -> dict:
    run_id = start_run(conn, "STP", path)
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    data = lines[1:] if lines else []
    upserted = errors = 0
    sql = (
        "INSERT INTO mf_stp_rule(amc_code,amc_name,nse_scheme_code,scheme_name,scheme_isin,scheme_type,astp_transaction_mode,"
        "astp_in_min_amount,astp_in_max_amount,astp_in_multiplier,astp_out_min_amount,astp_out_max_amount,astp_out_multiplier,"
        "astp_min_units,astp_max_units,astp_multiplier_units,astp_min_numbers,astp_max_numbers,astp_reg_in,astp_reg_out,"
        "astp_frequency,astp_dates,astp_min_gap,astp_max_gap,astp_installment_gap,astp_status) "
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
        "ON CONFLICT(nse_scheme_code, astp_frequency, astp_dates, astp_transaction_mode, astp_reg_in, astp_reg_out) DO UPDATE SET "
        "astp_status=excluded.astp_status, scheme_name=excluded.scheme_name"
    )
    for line in data:
        if not line.strip():
            continue
        p = split_row(line)
        if len(p) < 26:
            errors += 1
            continue
        try:
            conn.execute(sql, tuple(p[:26]))
            upserted += 1
        except sqlite3.Error:
            errors += 1
    conn.commit()
    finish_run(conn, run_id, status="OK" if errors == 0 else "PARTIAL", rows_read=len(data), rows_upserted=upserted, parse_errors=errors, message=None)
    return {"file_type": "STP", "upserted": upserted, "errors": errors}


def ingest_swp(conn: sqlite3.Connection, path: Path) -> dict:
    run_id = start_run(conn, "SWP", path)
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    data = lines[1:] if lines else []
    upserted = errors = 0
    sql = (
        "INSERT INTO mf_swp_rule(amc_code,amc_name,nse_scheme_code,scheme_name,scheme_isin,scheme_type,aswp_transaction_mode,"
        "aswp_min_amount,aswp_max_amount,aswp_multiplier_amount,aswp_min_units,aswp_max_units,aswp_multiplier_units,"
        "aswp_min_numbers,aswp_max_numbers,aswp_frequency,aswp_dates,aswp_min_gap,aswp_max_gap,aswp_installment_gap,aswp_status) "
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
        "ON CONFLICT(nse_scheme_code, aswp_frequency, aswp_dates, aswp_transaction_mode) DO UPDATE SET "
        "aswp_status=excluded.aswp_status, scheme_name=excluded.scheme_name"
    )
    for line in data:
        if not line.strip():
            continue
        p = split_row(line)
        if len(p) < 21:
            errors += 1
            continue
        try:
            conn.execute(sql, tuple(p[:21]))
            upserted += 1
        except sqlite3.Error:
            errors += 1
    conn.commit()
    finish_run(conn, run_id, status="OK" if errors == 0 else "PARTIAL", rows_read=len(data), rows_upserted=upserted, parse_errors=errors, message=None)
    return {"file_type": "SWP", "upserted": upserted, "errors": errors}


def ingest_set(conn: sqlite3.Connection, path: Path) -> dict:
    run_id = start_run(conn, "SET", path)
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    data = lines[1:] if lines else []
    upserted = errors = 0
    sql = (
        "INSERT INTO mf_settlement(settlement_type,settlement_no,trading_date,funds_pay_in_date,sec_pay_in_date,"
        "funds_pay_out_date,sec_pay_out_date) VALUES (?,?,?,?,?,?,?) "
        "ON CONFLICT(settlement_type, settlement_no, trading_date) DO UPDATE SET "
        "funds_pay_in_date=excluded.funds_pay_in_date, sec_pay_in_date=excluded.sec_pay_in_date, "
        "funds_pay_out_date=excluded.funds_pay_out_date, sec_pay_out_date=excluded.sec_pay_out_date"
    )
    for line in data:
        if not line.strip():
            continue
        p = split_row(line)
        if len(p) < 7:
            errors += 1
            continue
        try:
            conn.execute(sql, tuple(p[:7]))
            upserted += 1
        except sqlite3.Error:
            errors += 1
    conn.commit()
    finish_run(conn, run_id, status="OK" if errors == 0 else "PARTIAL", rows_read=len(data), rows_upserted=upserted, parse_errors=errors, message=None)
    return {"file_type": "SET", "upserted": upserted, "errors": errors}


INGESTORS = {
    "SCH": ingest_sch,
    "NAV": ingest_nav,
    "SIP": ingest_sip,
    "STP": ingest_stp,
    "SWP": ingest_swp,
    "SET": ingest_set,
}


def latest_file(masters_dir: Path, file_type: str) -> Path | None:
    files = sorted(masters_dir.glob(f"{file_type}_*.txt"))
    return files[-1] if files else None


def main() -> None:
    ap = argparse.ArgumentParser(description="Ingest NSE master pipe files into SQLite")
    ap.add_argument("--db", type=Path, default=DEFAULT_DB)
    ap.add_argument("--masters-dir", type=Path, default=MASTERS_DIR)
    ap.add_argument("--types", nargs="+", default=list(INGESTORS.keys()))
    args = ap.parse_args()
    conn = connect(args.db)
    results = []
    for ft in args.types:
        path = latest_file(args.masters_dir, ft)
        if not path:
            print(f"SKIP {ft}: no file in {args.masters_dir}")
            continue
        print(f"INGEST {ft} from {path.name} …")
        results.append(INGESTORS[ft](conn, path))
        print(results[-1])
    # metrics snapshot
    metrics = {
        "schemes": conn.execute("SELECT COUNT(*) FROM mf_scheme").fetchone()[0],
        "nav_points": conn.execute("SELECT COUNT(*) FROM mf_nav_eod").fetchone()[0],
        "max_nav_date": conn.execute("SELECT MAX(nav_date) FROM mf_nav_eod").fetchone()[0],
        "sip_rules": conn.execute("SELECT COUNT(*) FROM mf_sip_rule").fetchone()[0],
        "stp_rules": conn.execute("SELECT COUNT(*) FROM mf_stp_rule").fetchone()[0],
        "swp_rules": conn.execute("SELECT COUNT(*) FROM mf_swp_rule").fetchone()[0],
        "settlements": conn.execute("SELECT COUNT(*) FROM mf_settlement").fetchone()[0],
        "db": str(args.db),
    }
    print("METRICS", metrics)
    (ROOT / "data" / "latest_metrics.json").write_text(
        __import__("json").dumps(metrics, indent=2), encoding="utf-8"
    )
    conn.close()


if __name__ == "__main__":
    main()
