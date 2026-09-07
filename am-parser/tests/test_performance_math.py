"""Unit tests for fund performance math (no Mongo / market-data)."""
from datetime import date, timedelta

from am_etf.performance_math import (
    SPARKLINE_MAX,
    compute_performance_from_points,
    normalize_close_series,
    period_return_pct,
    sparkline_closes,
)


def _points(days_and_closes):
    base = date(2020, 1, 1)
    return [
        {"time": (base + timedelta(days=d)).isoformat(), "close": c}
        for d, c in days_and_closes
    ]


def test_normalize_and_period_return_1y():
    # ~2 years of weekly-ish points: start 100, after 365d → 110, end 120
    pts = _points(
        [
            (0, 100.0),
            (365, 110.0),
            (730, 121.0),
        ]
    )
    series = normalize_close_series(pts)
    assert len(series) == 3
    r1 = period_return_pct(series, 1.0)
    assert r1 is not None
    # 121/110 - 1 ≈ 10%
    assert abs(r1 - 10.0) < 0.2


def test_short_series_returns_null_for_long_window():
    pts = _points([(0, 100.0), (30, 105.0)])
    series = normalize_close_series(pts)
    assert period_return_pct(series, 5.0) is None


def test_sparkline_max_24():
    pts = _points([(i, 100.0 + i) for i in range(100)])
    series = normalize_close_series(pts)
    spark = sparkline_closes(series)
    assert spark is not None
    assert len(spark) == SPARKLINE_MAX
    assert spark[0] == 100.0
    assert spark[-1] == 199.0


def test_compute_performance_empty():
    out = compute_performance_from_points([])
    assert out["return1Y"] is None
    assert out["sparklineCloses"] is None
    assert out["returnsAsOf"] is None


def test_compute_performance_happy():
    pts = _points([(0, 100.0), (365, 110.0), (730, 121.0), (1095, 133.1), (1825, 160.0)])
    out = compute_performance_from_points(pts)
    assert out["returnsAsOf"] is not None
    assert out["return1Y"] is not None
    assert out["sparklineCloses"] is not None
    assert len(out["sparklineCloses"]) <= SPARKLINE_MAX
