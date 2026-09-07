"""Pure helpers: period returns and sparklines from ordered close series."""
from __future__ import annotations

from datetime import date, datetime, timedelta
from typing import List, Optional, Sequence, Tuple

SPARKLINE_MAX = 24


def _as_date(value) -> Optional[date]:
    if value is None:
        return None
    if isinstance(value, date) and not isinstance(value, datetime):
        return value
    if isinstance(value, datetime):
        return value.date()
    text = str(value).strip()
    if not text:
        return None
    # ISO date or datetime
    try:
        if "T" in text:
            return datetime.fromisoformat(text.replace("Z", "+00:00")).date()
        return date.fromisoformat(text[:10])
    except ValueError:
        return None


def normalize_close_series(
    points: Sequence[dict],
) -> List[Tuple[date, float]]:
    """Extract (date, close) ascending; skip invalid points."""
    series: List[Tuple[date, float]] = []
    for p in points or []:
        if not isinstance(p, dict):
            continue
        close = p.get("close")
        if close is None:
            continue
        try:
            close_f = float(close)
        except (TypeError, ValueError):
            continue
        d = _as_date(p.get("time") or p.get("timestamp") or p.get("date"))
        if d is None:
            continue
        series.append((d, close_f))
    series.sort(key=lambda x: x[0])
    return series


def _close_on_or_before(
    series: Sequence[Tuple[date, float]], target: date
) -> Optional[float]:
    chosen: Optional[float] = None
    for d, c in series:
        if d <= target:
            chosen = c
        else:
            break
    return chosen


def period_return_pct(
    series: Sequence[Tuple[date, float]], years: float
) -> Optional[float]:
    """((latest / past) - 1) * 100 where past is closest close on/before latest-years."""
    if not series or years <= 0:
        return None
    latest_date, latest_close = series[-1]
    if latest_close == 0:
        return None
    target = latest_date - timedelta(days=int(round(years * 365.25)))
    past = _close_on_or_before(series, target)
    if past is None or past == 0:
        # Not enough history: try earliest point if it is older than ~80% of window
        first_date, first_close = series[0]
        if first_close == 0:
            return None
        span_days = (latest_date - first_date).days
        if span_days < int(round(years * 365.25 * 0.8)):
            return None
        past = first_close
    return round(((latest_close / past) - 1.0) * 100.0, 4)


def sparkline_closes(
    series: Sequence[Tuple[date, float]], max_points: int = SPARKLINE_MAX
) -> Optional[List[float]]:
    if not series:
        return None
    closes = [c for _, c in series]
    if len(closes) <= max_points:
        return closes
    # Even sample including first and last
    n = len(closes)
    idxs = [round(i * (n - 1) / (max_points - 1)) for i in range(max_points)]
    return [closes[i] for i in idxs]


def compute_performance_from_points(
    points: Sequence[dict],
) -> dict:
    """Return dict with return1Y/3Y/5Y, sparklineCloses, returnsAsOf (or nulls)."""
    series = normalize_close_series(points)
    if not series:
        return {
            "return1Y": None,
            "return3Y": None,
            "return5Y": None,
            "sparklineCloses": None,
            "returnsAsOf": None,
        }
    as_of = series[-1][0].isoformat()
    return {
        "return1Y": period_return_pct(series, 1.0),
        "return3Y": period_return_pct(series, 3.0),
        "return5Y": period_return_pct(series, 5.0),
        "sparklineCloses": sparkline_closes(series),
        "returnsAsOf": as_of,
    }
