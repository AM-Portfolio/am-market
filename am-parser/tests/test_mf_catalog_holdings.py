"""Unit tests for mf_catalog holdings_json normalization."""
from am_mf_catalog.holdings import parse_holdings_json, scheme_details_to_fund_document


def test_parse_holdings_json_list():
    rows = parse_holdings_json(
        [
            {"stock_name": "Reliance", "isin_code": "INE002A01018", "percentage": 8.5},
            {"stockName": "TCS", "isin": "ine467b01029", "weight": "4.2%"},
        ]
    )
    assert len(rows) == 2
    assert rows[0]["stock_name"] == "Reliance"
    assert rows[0]["isin_code"] == "INE002A01018"
    assert rows[0]["percentage"] == 8.5
    assert rows[1]["isin_code"] == "INE467B01029"
    assert rows[1]["percentage"] == 4.2


def test_parse_holdings_json_wrapped_string():
    raw = '{"holdings":[{"name":"HDFC Bank","isinCode":"INE040A01034","percentage_to_nav":7.1}]}'
    rows = parse_holdings_json(raw)
    assert len(rows) == 1
    assert rows[0]["stock_name"] == "HDFC Bank"
    assert rows[0]["percentage"] == 7.1


def test_scheme_details_to_fund_document_requires_holdings():
    assert scheme_details_to_fund_document({"schemeName": "X", "isin": "INF123"}) is None
    doc = scheme_details_to_fund_document(
        {
            "schemeCode": "120503",
            "schemeName": "Demo Fund",
            "isin": "INF109K01VQ1",
            "sebiCategory": "Equity",
            "holdingsJson": [
                {"stock_name": "ITC", "isin_code": "INE154A01025", "percentage": 3.0}
            ],
        }
    )
    assert doc is not None
    assert doc["productType"] == "MUTUAL_FUND"
    assert doc["symbol"] == "120503"
    assert doc["isin"] == "INF109K01VQ1"
    assert doc["holdingsCount"] == 1
