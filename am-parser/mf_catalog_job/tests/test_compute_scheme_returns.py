"""Unit tests for returns lookback helper (no invent on thin history)."""
from __future__ import annotations

import sys
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from compute_scheme_returns import pct_return  # noqa: E402


def test_pct_return_null_when_history_too_thin() -> None:
    dates = [date(2026, 11, 24), date(2026, 11, 25)]
    navs = [100.0, 110.0]
    assert pct_return(dates, navs, 365) is None
    assert pct_return(dates, navs, 365 * 3) is None
    assert pct_return(dates, navs, 365 * 5) is None


def test_pct_return_1d_style_lookback() -> None:
    dates = [date(2026, 11, 24), date(2026, 11, 25)]
    navs = [100.0, 110.0]
    assert pct_return(dates, navs, 1) == 10.0


def test_pct_return_1y_when_span_covers() -> None:
    dates = [date(2025, 11, 25), date(2026, 5, 1), date(2026, 11, 25)]
    navs = [100.0, 105.0, 120.0]
    # lookback 365 from 2026-11-25 → target 2025-11-25 → start 100
    assert pct_return(dates, navs, 365) == 20.0
