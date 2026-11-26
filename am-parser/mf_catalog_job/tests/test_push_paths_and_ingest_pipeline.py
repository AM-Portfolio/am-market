"""Regression: post-move paths + SCH/NAV ingest + daily_job alert gating (no NSE/JDBC)."""
from __future__ import annotations

import json
import sqlite3
import subprocess
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))


def test_migration_sql_resolves_for_docker_and_checkout() -> None:
    import importlib

    if "_push_core" in sys.modules:
        del sys.modules["_push_core"]
    push = importlib.import_module("_push_core")
    assert push.MIGRATION.is_file(), push.MIGRATION
    assert push.MIGRATION.name == "V1__mf_catalog.sql"
    text = push.MIGRATION.read_text(encoding="utf-8")
    assert "CREATE TABLE IF NOT EXISTS mf_scheme" in text
    assert "CREATE TABLE IF NOT EXISTS mf_nav_eod" in text
    resolved = str(push.MIGRATION).replace("\\", "/")
    assert "/am-market/am-market/" not in resolved


def test_dockerfile_and_vendored_files_present() -> None:
    df = (ROOT / "Dockerfile").read_text(encoding="utf-8")
    assert "COPY mf_catalog_job/" in df
    assert 'CMD ["python", "mf_catalog_job/daily_job.py", "--fail-on-stale"]' in df
    for name in (
        "V1__mf_catalog.sql",
        "daily_job.py",
        "download_masters.py",
        "ingest_masters.py",
        "_push_core.py",
        "compute_scheme_returns.py",
        "schema.sql",
        "requirements-job.txt",
    ):
        assert (ROOT / name).is_file(), name


def test_sch_nav_ingest_sets_latest_and_prev(tmp_path: Path) -> None:
    masters = tmp_path / "masters"
    masters.mkdir()
    # Header skipped by ingest_sch; data row needs scheme_code at index 1
    (masters / "SCH_test.txt").write_text(
        "hdr\n"
        "1|SC99|RTA99|AMC99|INF99XXXXX|TESTAMC|EQUITY|NORMAL|Test Fund GROWTH|"
        "Y|DP|100|100||1|14:00|Y|DP|1|1|||1|1|14:00|CAMS|Y|N|"
        "Y|Y|Y|Y|T1|Y|10|||||||||\n",
        encoding="utf-8",
    )
    (masters / "NAV_test.txt").write_text(
        "2026-11-24|SC99|Test Fund|RTA|N|INF99XXXXX|100.0|CAMS\n"
        "2026-11-25|SC99|Test Fund|RTA|N|INF99XXXXX|110.0|CAMS\n",
        encoding="utf-8",
    )
    db = tmp_path / "mf_catalog.db"
    rc = subprocess.call(
        [
            sys.executable,
            str(ROOT / "ingest_masters.py"),
            "--db",
            str(db),
            "--masters-dir",
            str(masters),
            "--types",
            "SCH",
            "NAV",
        ],
        cwd=str(ROOT),
    )
    assert rc == 0
    assert db.is_file()
    conn = sqlite3.connect(db)
    schemes = conn.execute("SELECT COUNT(*) FROM mf_scheme").fetchone()[0]
    navs = conn.execute("SELECT COUNT(*) FROM mf_nav_eod").fetchone()[0]
    row = conn.execute(
        "SELECT latest_nav, latest_nav_date, prev_nav, prev_nav_date, change_1d_pct "
        "FROM mf_scheme WHERE scheme_code='SC99'"
    ).fetchone()
    conn.close()
    assert schemes == 1
    assert navs == 2
    assert row is not None
    latest, latest_d, prev, prev_d, chg = row
    assert latest == 110.0
    assert latest_d == "2026-11-25"
    assert prev == 100.0
    assert prev_d == "2026-11-24"
    assert chg == pytest.approx(10.0)


def test_daily_job_fail_on_stale_and_empty_metrics(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    import daily_job

    data = tmp_path / "data"
    data.mkdir()
    metrics_path = data / "latest_metrics.json"
    metrics_path.write_text(
        json.dumps({"schemes": 15000, "nav_points": 30000, "max_nav_date": "2020-01-01"}),
        encoding="utf-8",
    )
    monkeypatch.setattr(daily_job, "ROOT", tmp_path)
    monkeypatch.setattr(daily_job, "MASTERS_DIR", tmp_path / "masters")
    monkeypatch.setattr(daily_job, "METRICS", metrics_path)
    monkeypatch.setattr(daily_job, "ALERT_LOG", data / "alerts.jsonl")
    monkeypatch.setattr(
        daily_job,
        "run_py",
        lambda *a, **k: 0,
    )
    monkeypatch.setattr(
        sys,
        "argv",
        ["daily_job.py", "--skip-download", "--skip-push", "--fail-on-stale"],
    )
    # ingest is still invoked unless we also skip it — daily_job always ingests.
    # Force ingest path to no-op via run_py returning 0 for all scripts.
    rc = daily_job.main()
    assert rc == 1  # NAV_STALE with --fail-on-stale


def test_daily_job_ok_when_fresh_metrics(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    import daily_job
    from datetime import date

    data = tmp_path / "data"
    data.mkdir()
    today = date.today().isoformat()
    metrics_path = data / "latest_metrics.json"
    metrics_path.write_text(
        json.dumps({"schemes": 15000, "nav_points": 30000, "max_nav_date": today}),
        encoding="utf-8",
    )
    monkeypatch.setattr(daily_job, "ROOT", tmp_path)
    monkeypatch.setattr(daily_job, "MASTERS_DIR", tmp_path / "masters")
    monkeypatch.setattr(daily_job, "METRICS", metrics_path)
    monkeypatch.setattr(daily_job, "ALERT_LOG", data / "alerts.jsonl")
    monkeypatch.setattr(daily_job, "run_py", lambda *a, **k: 0)
    monkeypatch.setattr(
        sys,
        "argv",
        ["daily_job.py", "--skip-download", "--skip-push", "--fail-on-stale"],
    )
    assert daily_job.main() == 0


def test_require_jdbc_fails_closed_without_secrets(monkeypatch: pytest.MonkeyPatch) -> None:
    import importlib

    if "_push_core" in sys.modules:
        del sys.modules["_push_core"]
    push = importlib.import_module("_push_core")
    monkeypatch.delenv("AM_MARKET_MF_JDBC_URL", raising=False)
    monkeypatch.delenv("AM_MARKET_MF_JDBC_USER", raising=False)
    monkeypatch.delenv("AM_MARKET_MF_JDBC_PASSWORD", raising=False)
    with pytest.raises(SystemExit) as ei:
        push.require_jdbc()
    assert "AM_MARKET_MF_JDBC_URL" in str(ei.value)


def test_download_masters_import_safe_without_nse(monkeypatch: pytest.MonkeyPatch) -> None:
    """Must not read NSE_* or hit network on import (CronJob runs as __main__)."""
    import importlib

    for k in ("NSE_URL", "NSE_LOGIN", "NSE_API_KEY", "NSE_API_SECRET", "NSE_MEMBER"):
        monkeypatch.delenv(k, raising=False)
    if "download_masters" in sys.modules:
        del sys.modules["download_masters"]
    mod = importlib.import_module("download_masters")
    with pytest.raises(SystemExit) as ei:
        mod._require_nse_env()
    assert "missing env" in str(ei.value)


def test_dig_dsn_prefers_platform_db_and_schema() -> None:
    import importlib

    if "_push_core" in sys.modules:
        del sys.modules["_push_core"]
    push = importlib.import_module("_push_core")
    dsn, schema = push.dig_dsn(
        "jdbc:postgresql://postgres.asrax.in:5432/user_platform_dev?currentSchema=mf_catalog"
    )
    assert schema == "mf_catalog"
    assert "user_platform_dev" in dsn
    assert "postgres.asrax.in" in dsn
    assert "password" not in dsn.lower()
