"""latest/prev NAV refresh after ingest — EOD clarity."""
from __future__ import annotations

import sqlite3
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from ingest_masters import connect, ingest_nav  # noqa: E402


def _write_nav(path: Path, rows: list[tuple[str, str, float]]) -> None:
    # nav_date|scheme_code|name|rta|div|isin|nav|rta_agent
    lines = [
        f"{d}|{code}|Fund {code}|RTA|N|INF{code}|{nav}|CAMS"
        for d, code, nav in rows
    ]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def _seed_scheme(conn: sqlite3.Connection, code: str = "SC1") -> None:
    conn.execute(
        "INSERT INTO mf_scheme(scheme_code, scheme_name, isin, updated_at) VALUES (?,?,?,datetime('now'))",
        (code, f"Fund {code}", f"INF{code}"),
    )
    conn.commit()


def test_single_nav_point_leaves_prev_null(tmp_path: Path) -> None:
    db = tmp_path / "mf.db"
    conn = connect(db)
    _seed_scheme(conn)
    nav = tmp_path / "nav.txt"
    _write_nav(nav, [("2026-11-24", "SC1", 100.0)])
    ingest_nav(conn, nav)
    row = conn.execute(
        "SELECT latest_nav, latest_nav_date, prev_nav, prev_nav_date, change_1d_pct "
        "FROM mf_scheme WHERE scheme_code='SC1'"
    ).fetchone()
    assert row[0] == 100.0
    assert row[1] == "2026-11-24"
    assert row[2] is None
    assert row[3] is None
    assert row[4] is None


def test_two_nav_points_set_latest_prev_and_change(tmp_path: Path) -> None:
    db = tmp_path / "mf.db"
    conn = connect(db)
    _seed_scheme(conn)
    nav = tmp_path / "nav.txt"
    _write_nav(
        nav,
        [
            ("2026-11-24", "SC1", 100.0),
            ("2026-11-25", "SC1", 110.0),
        ],
    )
    ingest_nav(conn, nav)
    row = conn.execute(
        "SELECT latest_nav, latest_nav_date, prev_nav, prev_nav_date, change_1d_pct "
        "FROM mf_scheme WHERE scheme_code='SC1'"
    ).fetchone()
    assert row[0] == 110.0
    assert row[1] == "2026-11-25"
    assert row[2] == 100.0
    assert row[3] == "2026-11-24"
    assert row[4] == pytest.approx(10.0)


def test_newer_file_shifts_old_latest_to_prev(tmp_path: Path) -> None:
    db = tmp_path / "mf.db"
    conn = connect(db)
    _seed_scheme(conn)
    d1 = tmp_path / "nav1.txt"
    _write_nav(d1, [("2026-11-24", "SC1", 100.0)])
    ingest_nav(conn, d1)
    d2 = tmp_path / "nav2.txt"
    _write_nav(d2, [("2026-11-25", "SC1", 105.0)])
    ingest_nav(conn, d2)
    row = conn.execute(
        "SELECT latest_nav, latest_nav_date, prev_nav, prev_nav_date, change_1d_pct "
        "FROM mf_scheme WHERE scheme_code='SC1'"
    ).fetchone()
    assert row[0] == 105.0
    assert row[1] == "2026-11-25"
    assert row[2] == 100.0
    assert row[3] == "2026-11-24"
    assert row[4] == pytest.approx(5.0)
    # latest is never overwritten with prev; nav_date comes from NSE file
    assert row[1] > row[3]
