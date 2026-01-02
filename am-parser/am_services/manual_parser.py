from __future__ import annotations

import math
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional

import pandas as pd
import yaml

# Import models and utilities from their proper modules
from am_common import Portfolio, Fund, Holding, Totals, load_tabular


@dataclass
class ManualParserService:
    header_map_key: Optional[str] = None
    config_path: Optional[str | Path] = None

    def _load_header_map(self) -> Dict[str, str]:
        cfg_path: Path
        if self.config_path:
            cfg_path = Path(self.config_path)
        else:
            # Look for am_configs/header_maps.yaml in parent directory
            root_cfg = Path(__file__).resolve().parent.parent / "am_configs" / "header_maps.yaml"
            if root_cfg.exists():
                cfg_path = root_cfg
            else:
                # Fallback to inline default
                return {
                    "security name": "name",
                    "company": "name", 
                    "holding": "name",
                    "symbol": "ticker",
                    "isin code": "isin",
                    "quantity": "qty",
                    "units": "qty",
                    "market value": "mkt_value",
                    "mkt value": "mkt_value",
                    "amount": "mkt_value",
                    "allocation": "weight",
                    "portfolio %": "weight",
                    "%": "weight"
                }

        if not cfg_path.exists():
            return {}

        data = yaml.safe_load(cfg_path.read_text(encoding="utf-8")) or {}
        key = self.header_map_key or "default"
        mapping = data.get(key) or {}
        return {str(k).strip().lower(): str(v) for k, v in mapping.items()}

    def _normalize_columns(self, columns: List[str], mapping: Dict[str, str]) -> List[str]:
        return [mapping.get(str(c).strip().lower(), str(c).strip().lower()) for c in columns]

    def parse(self, file_path: str | Path, *, sheet: Optional[str | int] = None, show_preview: bool = False) -> Dict[str, Any]:
        df = load_tabular(file_path, sheet=sheet)
        df = df.copy()
        header_map = self._load_header_map()
        df.columns = self._normalize_columns([str(c) for c in df.columns], header_map)
        df = df.dropna(how="all")

        possible = {
            "isin": ["isin", "isin code"],
            "ticker": ["ticker", "symbol"],
            "name": ["name", "security name", "company", "holding"],
            "sector": ["sector", "industry"],
            "qty": ["qty", "quantity", "units"],
            "mkt_value": ["mkt_value", "market value", "mkt value", "value", "amount"],
            "weight": ["weight", "%", "allocation", "portfolio %"],
        }

        def pick(colnames: List[str]) -> Optional[str]:
            lower = [c.lower() for c in df.columns]
            for name in colnames:
                if name in lower:
                    return df.columns[lower.index(name)]
            return None

        def _none_if_nan(v: Any) -> Any:
            try:
                if pd.isna(v):
                    return None
            except Exception:
                pass
            if isinstance(v, float) and math.isnan(v):
                return None
            return v

        colmap: Dict[str, Optional[str]] = {k: pick(v) for k, v in possible.items()}

        holdings: List[Dict[str, Any]] = []
        for _, row in df.iterrows():
            itm: Dict[str, Any] = {}
            for key, src in colmap.items():
                if src is None:
                    continue
                val = _none_if_nan(row.get(src))
                if key in {"isin", "ticker", "name", "sector"}:
                    itm[key] = None if val is None else str(val)
                elif key in {"qty", "mkt_value", "weight"}:
                    if val is None or val == "":
                        itm[key] = None
                    else:
                        try:
                            itm[key] = float(val)
                        except (TypeError, ValueError):
                            itm[key] = None
                else:
                    itm[key] = val
            if not itm.get("name") and not itm.get("mkt_value"):
                continue
            holdings.append(itm)

        total_value = 0.0
        for h in holdings:
            try:
                val = float(h.get("mkt_value", 0) or 0)
            except (ValueError, TypeError):
                val = 0.0
            total_value += val

        any_weight = any(h.get("weight") not in (None, "") for h in holdings)
        if not any_weight and total_value > 0:
            for h in holdings:
                try:
                    val = float(h.get("mkt_value", 0) or 0)
                except (ValueError, TypeError):
                    val = 0.0
                h["weight"] = round(100.0 * val / total_value, 4)

        portfolio = Portfolio(
            fund=Fund(),
            holdings=[Holding(**h) for h in holdings],
            totals=Totals(mkt_value=round(total_value, 4), weight=round(sum(float(h.get("weight", 0) or 0) for h in holdings), 4)),
            meta={},
        )

        result: Dict[str, Any] = portfolio.model_dump()
        if show_preview:
            result.setdefault("debug", {})
            result["debug"]["columns"] = [str(c) for c in df.columns]
            result["debug"]["sample"] = df.head(5).to_dict(orient="records")

        return result
