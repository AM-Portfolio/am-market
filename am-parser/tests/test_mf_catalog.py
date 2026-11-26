"""Unit tests for MF catalog JDBC helper + service (mocked DB; env-independent)."""

from __future__ import annotations

from datetime import date
from unittest.mock import MagicMock, patch

import pytest

from am_common.jdbc import jdbc_to_pg_params, safe_jdbc_label
from am_mf_catalog.service import (
    MfCatalogService,
    PRICE_TYPE,
    SOURCE,
    _normalize_purchase_allowed,
)


def test_normalize_purchase_allowed_boolish():
    assert _normalize_purchase_allowed("true") == "Y"
    assert _normalize_purchase_allowed("1") == "Y"
    assert _normalize_purchase_allowed("yes") == "Y"
    assert _normalize_purchase_allowed("Y") == "Y"
    assert _normalize_purchase_allowed("false") == "N"
    assert _normalize_purchase_allowed(None) is None


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
    # Enriched + returns + source (no SIP lateral — list search must stay fast)
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
        "1000",
        5000.0,
        0.45,
        "MODERATE",
        "Equity",
        "Flexi Cap",
        1.2,
        3.4,
        12.5,
        None,
        None,
        "amfi",
    )
    cur = MagicMock()
    cur.fetchall.return_value = [row]
    cur.__enter__ = MagicMock(return_value=cur)
    cur.__exit__ = MagicMock(return_value=False)
    cur.connection = MagicMock()
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
    assert item["newPurchaseMinAmount"] == "1000"
    assert item["aum"] == 5000.0
    assert item["expenseRatio"] == 0.45
    assert item["returnsPct"]["1Y"] == 12.5
    assert item["enrichmentSource"] == "amfi"
    assert "minSip" not in item


def test_empty_query_order_by_avoids_scheme_name_only():
    from am_mf_catalog.service import _search_order_by, _SEARCH_SELECT_ENRICH_ONLY

    order = _search_order_by(
        empty_query=True, alias="s.", select_sql=_SEARCH_SELECT_ENRICH_ONLY
    )
    assert "e.aum" in order
    assert "s.amc_code" in order
    typed = _search_order_by(
        empty_query=False, alias="s.", select_sql=_SEARCH_SELECT_ENRICH_ONLY
    )
    assert typed.strip().startswith("ORDER BY s.scheme_name")


def test_search_degrades_when_enrichment_missing():
    svc = MfCatalogService(
        "jdbc:postgresql://localhost:5432/db?currentSchema=mf_catalog",
        "u",
        "p",
    )
    basic = (
        "SC1",
        "SBI Bluechip",
        "INF999",
        "SBI",
        "Open",
        "Growth",
        "Y",
        50.0,
        date(2026, 11, 5),
        "500",
    )
    cur = MagicMock()
    cur.connection = MagicMock()

    def execute(sql, params=None):
        if "mf_scheme_enrichment" in sql or "mf_scheme_returns" in sql or "mf_sip_rule" in sql:
            raise Exception('relation "mf_scheme_enrichment" does not exist')
        cur.fetchall.return_value = [basic]

    cur.execute.side_effect = execute
    cur.__enter__ = MagicMock(return_value=cur)
    cur.__exit__ = MagicMock(return_value=False)
    conn = MagicMock()
    conn.cursor.return_value = cur
    conn.__enter__ = MagicMock(return_value=conn)
    conn.__exit__ = MagicMock(return_value=False)

    with patch.object(svc, "_connect", return_value=conn):
        out = svc.search_schemes("SBI", None, None, 5)

    assert out["count"] == 1
    assert out["items"][0]["schemeName"] == "SBI Bluechip"
    assert out["items"][0]["newPurchaseMinAmount"] == "500"


def test_details_orders_by_scheme_code():
    """ISIN lookup must use ORDER BY scheme_code (deterministic)."""
    svc = MfCatalogService(
        "jdbc:postgresql://localhost:5432/db?currentSchema=mf_catalog",
        "u",
        "p",
    )
    # 0..24 SCH (+ face/cutoffs), 25..30 enrich (+ source), optional vendor after.
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
        "10",  # face_value
        "14:30",  # purchase_cutoff_time
        "14:30",  # redemption_cutoff_time
        12000.0,  # aum
        0.55,  # expense_ratio
        "VERY HIGH",
        "Equity",
        "Flexi Cap",
        "amfi",
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
    assert out.get("enrichmentSource") == "amfi"
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
