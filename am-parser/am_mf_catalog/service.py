"""Read MF schemes + EOD NAV from Postgres mf_catalog (mirrors Java MutualFundCatalogService)."""

from __future__ import annotations

from datetime import date, datetime, timedelta
from typing import Any, Optional

from am_common.jdbc import jdbc_to_pg_params, safe_jdbc_label
from am_common.logging.request_logging import get_logger
from am_configs.settings import settings

log = get_logger("mf_catalog")

SOURCE = "nse-master-download"
PRICE_TYPE = "EOD_NAV_NOT_LIVE_LTP"

_DETAILS_SQL = """
SELECT s.scheme_code, s.scheme_name, s.isin, s.amc_code, s.scheme_type, s.plan_type,
       s.purchase_allowed, s.redemption_allowed, s.sip_allowed, s.stp_enabled, s.swp_enabled,
       s.switch_allowed, s.purchase_transaction_mode, s.new_purchase_min_amount,
       s.redemption_transaction_mode, s.rta_agent_code, s.latest_nav, s.latest_nav_date,
       s.exit_load_flag, s.exit_load, s.lock_in_period_flag, s.lock_in_period,
       e.aum, e.expense_ratio, e.riskometer, e.sebi_category, e.sebi_sub_category
FROM mf_scheme s
LEFT JOIN mf_scheme_enrichment e ON e.isin = s.isin
WHERE s.scheme_code = %s OR s.isin = %s
ORDER BY s.scheme_code
LIMIT 1
"""

_DETAILS_BASIC_SQL = """
SELECT scheme_code, scheme_name, isin, amc_code, scheme_type, plan_type,
       purchase_allowed, redemption_allowed, sip_allowed, stp_enabled, swp_enabled,
       switch_allowed, purchase_transaction_mode, new_purchase_min_amount,
       redemption_transaction_mode, rta_agent_code, latest_nav, latest_nav_date,
       exit_load_flag, exit_load, lock_in_period_flag, lock_in_period
FROM mf_scheme
WHERE scheme_code = %s OR isin = %s
ORDER BY scheme_code
LIMIT 1
"""

_HISTORY_SQL = """
SELECT nav_date, nav, isin, scheme_name FROM mf_nav_eod
WHERE (scheme_code = %s OR isin = %s) AND nav_date >= %s AND nav_date <= %s
ORDER BY nav_date ASC
"""

_PERF_SQL = """
SELECT nav_date, nav FROM mf_nav_eod
WHERE scheme_code = %s OR isin = %s
ORDER BY nav_date ASC
"""


class MfCatalogService:
    """Env-agnostic catalog reader — JDBC URL/user/password come only from Settings."""

    def __init__(
        self,
        jdbc_url: str,
        jdbc_user: str,
        jdbc_password: str,
    ) -> None:
        self._jdbc_url = (jdbc_url or "").strip()
        self._jdbc_user = (jdbc_user or "").strip()
        self._jdbc_password = jdbc_password or ""
        self._pool = None

    def is_configured(self) -> bool:
        return bool(self._jdbc_url and self._jdbc_user)

    def _connect(self):
        import psycopg

        if not self.is_configured():
            raise RuntimeError("Mutual fund catalog not configured")
        params = jdbc_to_pg_params(
            self._jdbc_url, self._jdbc_user, self._jdbc_password
        )
        return psycopg.connect(**params.connect_kwargs())

    def close(self) -> None:
        if self._pool is not None:
            try:
                self._pool.close()
            except Exception:
                pass
            self._pool = None

    def search_schemes(
        self,
        query: Optional[str],
        amc: Optional[str],
        purchase_allowed: Optional[str],
        limit: int,
    ) -> dict[str, Any]:
        out: dict[str, Any] = {}
        if not self.is_configured():
            out["error"] = "Mutual fund catalog not configured"
            return out

        capped = max(1, min(limit if limit and limit > 0 else 50, 200))
        sql = (
            "SELECT scheme_code, scheme_name, isin, amc_code, scheme_type, plan_type, "
            "purchase_allowed, latest_nav, latest_nav_date "
            "FROM mf_scheme WHERE 1=1 "
        )
        params: list[Any] = []
        if query and query.strip():
            sql += (
                "AND (LOWER(scheme_name) LIKE %s OR LOWER(scheme_code) LIKE %s "
                "OR LOWER(isin) LIKE %s) "
            )
            q = f"%{query.strip().lower()}%"
            params.extend([q, q, q])
        if amc and amc.strip():
            sql += "AND UPPER(amc_code) = %s "
            params.append(amc.strip().upper())
        if purchase_allowed and purchase_allowed.strip():
            sql += "AND UPPER(purchase_allowed) = %s "
            params.append(purchase_allowed.strip().upper())
        sql += "ORDER BY scheme_name LIMIT %s"
        params.append(capped)

        items: list[dict[str, Any]] = []
        try:
            with self._connect() as conn:
                with conn.cursor() as cur:
                    cur.execute(sql, params)
                    for row in cur.fetchall():
                        items.append(
                            {
                                "schemeCode": row[0],
                                "schemeName": row[1],
                                "isin": row[2],
                                "amcCode": row[3],
                                "schemeType": row[4],
                                "planType": row[5],
                                "purchaseAllowed": row[6],
                                "nav": float(row[7]) if row[7] is not None else None,
                                "navDate": _date_str(row[8]),
                                "priceType": PRICE_TYPE,
                            }
                        )
        except Exception as e:
            log.error(
                "MF catalog search failed q=%s target=%s err=%s",
                query,
                safe_jdbc_label(self._jdbc_url),
                e,
            )
            out["error"] = "Mutual fund search failed"
            out["message"] = str(e)
            return out

        out["items"] = items
        out["count"] = len(items)
        out["source"] = SOURCE
        out["priceType"] = PRICE_TYPE
        return out

    def get_scheme_details(self, scheme_code: str) -> Optional[dict[str, Any]]:
        if not self.is_configured():
            return None
        try:
            with self._connect() as conn:
                with conn.cursor() as cur:
                    try:
                        cur.execute(_DETAILS_SQL, (scheme_code, scheme_code))
                        row = cur.fetchone()
                        if row is None:
                            return None
                        m = _map_scheme_row_enriched(row)
                    except Exception as e:
                        if "mf_scheme_enrichment" not in str(e).lower():
                            raise
                        conn.rollback()
                        cur.execute(_DETAILS_BASIC_SQL, (scheme_code, scheme_code))
                        row = cur.fetchone()
                        if row is None:
                            return None
                        m = _map_scheme_row_basic(row)
        except Exception as e:
            log.error(
                "MF catalog details failed schemeCode=%s target=%s err=%s",
                scheme_code,
                safe_jdbc_label(self._jdbc_url),
                e,
            )
            return None
        m["source"] = SOURCE
        m["priceType"] = PRICE_TYPE
        return m

    def get_nav_history(
        self,
        scheme_code: str,
        from_date: date,
        to_date: date,
    ) -> Optional[dict[str, Any]]:
        if not self.is_configured():
            return None
        try:
            with self._connect() as conn:
                with conn.cursor() as cur:
                    cur.execute(
                        _HISTORY_SQL,
                        (
                            scheme_code,
                            scheme_code,
                            from_date.isoformat(),
                            to_date.isoformat(),
                        ),
                    )
                    points: list[dict[str, Any]] = []
                    isin = None
                    name = None
                    for row in cur.fetchall():
                        if isin is None:
                            isin = row[2]
                            name = row[3]
                        points.append(
                            {
                                "date": _date_str(row[0]),
                                "nav": float(row[1]) if row[1] is not None else None,
                            }
                        )
        except Exception as e:
            log.error(
                "MF catalog history failed schemeCode=%s err=%s",
                scheme_code,
                e,
            )
            return None

        return {
            "schemeCode": scheme_code,
            "isin": isin,
            "schemeName": name,
            "from": from_date.isoformat(),
            "to": to_date.isoformat(),
            "points": points,
            "count": len(points),
            "source": SOURCE,
            "priceType": PRICE_TYPE,
        }

    def get_performance(self, scheme_code: str) -> Optional[dict[str, Any]]:
        if not self.is_configured():
            return None
        details = self.get_scheme_details(scheme_code)
        if details is None:
            return None
        code = str(details.get("schemeCode") or scheme_code)
        dates: list[date] = []
        navs: list[float] = []
        try:
            with self._connect() as conn:
                with conn.cursor() as cur:
                    cur.execute(_PERF_SQL, (scheme_code, scheme_code))
                    for row in cur.fetchall():
                        d = _to_date(row[0])
                        if d is None or row[1] is None:
                            continue
                        dates.append(d)
                        navs.append(float(row[1]))
        except Exception as e:
            log.error("MF performance query failed schemeCode=%s err=%s", scheme_code, e)
            return None

        return {
            "schemeCode": code,
            "isin": details.get("isin"),
            "schemeName": details.get("schemeName"),
            "latestNav": details.get("nav"),
            "latestNavDate": details.get("navDate"),
            "pointCount": len(navs),
            "returnsPct": {
                "1D": _pct_return(dates, navs, 1),
                "1M": _pct_return(dates, navs, 30),
                "3M": _pct_return(dates, navs, 90),
                "1Y": _pct_return(dates, navs, 365),
                "3Y": _pct_return(dates, navs, 365 * 3),
                "5Y": _pct_return(dates, navs, 365 * 5),
            },
            "source": SOURCE,
            "priceType": PRICE_TYPE,
            "note": (
                "Returns require accumulated daily NAV history; "
                "thin until multi-day ingest or AMFI backfill"
            ),
        }


def create_mf_catalog_service() -> MfCatalogService:
    return MfCatalogService(
        jdbc_url=settings.am_market_mf_jdbc_url,
        jdbc_user=settings.am_market_mf_jdbc_user,
        jdbc_password=settings.am_market_mf_jdbc_password,
    )


def _date_str(v: Any) -> Optional[str]:
    if v is None:
        return None
    if isinstance(v, datetime):
        return v.date().isoformat()
    if isinstance(v, date):
        return v.isoformat()
    return str(v)


def _to_date(v: Any) -> Optional[date]:
    if v is None:
        return None
    if isinstance(v, datetime):
        return v.date()
    if isinstance(v, date):
        return v
    try:
        return date.fromisoformat(str(v)[:10])
    except ValueError:
        return None


def _pct_return(
    dates: list[date], navs: list[float], lookback_days: int
) -> Optional[float]:
    if not dates or not navs:
        return None
    last = len(dates) - 1
    end = dates[last]
    target = end - timedelta(days=lookback_days)
    end_nav = navs[last]
    start_nav: Optional[float] = None
    for i in range(last, -1, -1):
        if dates[i] <= target:
            start_nav = navs[i]
            break
    if start_nav is None or start_nav == 0:
        if len(dates) >= 2 and (end - dates[0]).days >= lookback_days:
            start_nav = navs[0]
        else:
            return None
    return ((end_nav - start_nav) / start_nav) * 100.0


def _map_scheme_row_basic(row: tuple) -> dict[str, Any]:
    return {
        "schemeCode": row[0],
        "schemeName": row[1],
        "isin": row[2],
        "amcCode": row[3],
        "schemeType": row[4],
        "planType": row[5],
        "purchaseAllowed": row[6],
        "redemptionAllowed": row[7],
        "sipAllowed": row[8],
        "stpEnabled": row[9],
        "swpEnabled": row[10],
        "switchAllowed": row[11],
        "purchaseTransactionMode": row[12],
        "newPurchaseMinAmount": row[13],
        "redemptionTransactionMode": row[14],
        "rtaAgentCode": row[15],
        "nav": float(row[16]) if row[16] is not None else None,
        "navDate": _date_str(row[17]),
        "exitLoadFlag": row[18],
        "exitLoad": row[19],
        "lockInPeriodFlag": row[20],
        "lockInPeriod": row[21],
    }


def _map_scheme_row_enriched(row: tuple) -> dict[str, Any]:
    m = _map_scheme_row_basic(row[:22])
    m["aum"] = float(row[22]) if row[22] is not None else None
    m["expenseRatio"] = float(row[23]) if row[23] is not None else None
    m["riskometer"] = row[24]
    m["sebiCategory"] = row[25]
    m["sebiSubCategory"] = row[26]
    return m
