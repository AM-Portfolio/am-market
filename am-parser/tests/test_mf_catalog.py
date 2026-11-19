"""Unit tests for MF catalog JDBC helper + service (mocked DB; env-independent)."""

from __future__ import annotations

from datetime import date
from unittest.mock import MagicMock, patch

import pytest

from am_common.jdbc import jdbc_to_pg_params, safe_jdbc_label
from am_mf_catalog.service import MfCatalogService, PRICE_TYPE, SOURCE


def test_jdbc_to_pg_params_strips_jdbc_and_honors_schema():
    p = jdbc_to_pg_params(
        "jdbc:postgresql://db.example:5432/platform_dev?currentSchema=mf_catalog",
        "u",
        "p",
    )
    assert p.host == "db.example"
    assert p.port == 5432
    assert p.dbname == "platform_dev"
    assert p.user == "u"
    assert p.password == "p"
    assert p.options == "-c search_path=mf_catalog"
    assert "password" in p.connect_kwargs()


def test_jdbc_rejects_empty_and_non_pg():
    with pytest.raises(ValueError):
        jdbc_to_pg_params("", "u", "p")
    with pytest.raises(ValueError):
        jdbc_to_pg_params("jdbc:mysql://h/db", "u", "p")


def test_safe_jdbc_label_no_password():
    label = safe_jdbc_label(
        "jdbc:postgresql://db.example:5432/platform?currentSchema=mf_catalog"
    )
    assert "db.example" in label
    assert "password" not in label.lower()


def test_not_configured_search_returns_error():
    svc = MfCatalogService("", "", "")
    out = svc.search_schemes("HDFC", None, None, 5)
    assert out["error"] == "Mutual fund catalog not configured"


def test_search_maps_camel_case_and_price_type():
    svc = MfCatalogService(
        "jdbc:postgresql://localhost:5432/db?currentSchema=mf_catalog",
        "u",
        "p",
    )
    row = (
        "SC1",
        "HDFC Flexi",
        "INF123",
        "HDFC",
        "Open",
        "Growth",
        "Y",
        123.45,
        date(2026, 11, 5),
    )
    cur = MagicMock()
    cur.fetchall.return_value = [row]
    cur.__enter__ = MagicMock(return_value=cur)
    cur.__exit__ = MagicMock(return_value=False)
    conn = MagicMock()
    conn.cursor.return_value = cur
    conn.__enter__ = MagicMock(return_value=conn)
    conn.__exit__ = MagicMock(return_value=False)

    with patch.object(svc, "_connect", return_value=conn):
        out = svc.search_schemes("HDFC", None, None, 5)

    assert out["source"] == SOURCE
    assert out["priceType"] == PRICE_TYPE
    assert out["count"] == 1
    item = out["items"][0]
    assert item["schemeCode"] == "SC1"
    assert item["nav"] == 123.45
    assert item["navDate"] == "2026-11-05"
    assert item["priceType"] == PRICE_TYPE


def test_details_orders_by_scheme_code():
    """ISIN lookup must use ORDER BY scheme_code (deterministic)."""
    svc = MfCatalogService(
        "jdbc:postgresql://localhost:5432/db?currentSchema=mf_catalog",
        "u",
        "p",
    )
    basic = (
        "SC1",
        "Fund",
        "INF1",
        "AMC",
        "Open",
        "G",
        "Y",
        "Y",
        "Y",
        "Y",
        "Y",
        "Y",
        "DP",
        "500",
        "DP",
        "CAMS",
        10.0,
        date(2026, 11, 1),
        "N",
        None,
        "N",
        None,
        None,
        None,
        None,
        None,
        None,
    )
    cur = MagicMock()
    cur.fetchone.return_value = basic
    cur.__enter__ = MagicMock(return_value=cur)
    cur.__exit__ = MagicMock(return_value=False)
    conn = MagicMock()
    conn.cursor.return_value = cur
    conn.__enter__ = MagicMock(return_value=conn)
    conn.__exit__ = MagicMock(return_value=False)

    with patch.object(svc, "_connect", return_value=conn):
        out = svc.get_scheme_details("INF1")

    assert out is not None
    assert out["schemeCode"] == "SC1"
    assert out["source"] == SOURCE
    sql = cur.execute.call_args[0][0]
    assert "ORDER BY" in sql.upper()
    assert "scheme_code" in sql.lower()


def test_history_requires_from_to_shape():
    svc = MfCatalogService(
        "jdbc:postgresql://localhost:5432/db?currentSchema=mf_catalog",
        "u",
        "p",
    )
    cur = MagicMock()
    cur.fetchall.return_value = [
        (date(2026, 11, 1), 10.0, "INF1", "Fund"),
        (date(2026, 11, 2), 10.5, "INF1", "Fund"),
    ]
    cur.__enter__ = MagicMock(return_value=cur)
    cur.__exit__ = MagicMock(return_value=False)
    conn = MagicMock()
    conn.cursor.return_value = cur
    conn.__enter__ = MagicMock(return_value=conn)
    conn.__exit__ = MagicMock(return_value=False)

    with patch.object(svc, "_connect", return_value=conn):
        out = svc.get_nav_history("SC1", date(2026, 11, 1), date(2026, 11, 2))

    assert out["count"] == 2
    assert out["priceType"] == PRICE_TYPE
    assert out["points"][1]["nav"] == 10.5
