from pathlib import Path
from typing import List, Optional

import pandas as pd
from pydantic import BaseModel, Field


class Fund(BaseModel):
    """Represents a mutual fund."""
    name: Optional[str] = None
    report_date: Optional[str] = Field(default=None, description="YYYY-MM-DD")
    currency: Optional[str] = None


class Holding(BaseModel):
    """Represents a single holding in a portfolio."""
    isin: Optional[str] = None
    ticker: Optional[str] = None
    name: Optional[str] = None
    sector: Optional[str] = None
    qty: Optional[float] = None
    mkt_value: Optional[float] = None
    weight: Optional[float] = None


class Totals(BaseModel):
    """Represents portfolio totals."""
    mkt_value: Optional[float] = None
    weight: Optional[float] = None


class Portfolio(BaseModel):
    """Represents a complete portfolio with fund info, holdings, and totals."""
    fund: Fund
    holdings: List[Holding]
    totals: Totals
    meta: dict | None = None


def load_tabular(
    file_path: str | Path, *, sheet: Optional[str | int] = None
) -> pd.DataFrame:
    """Load CSV or Excel into a DataFrame."""
    path = Path(file_path)
    if not path.exists():
        raise FileNotFoundError(str(path))

    suffix = path.suffix.lower()
    if suffix == ".csv":
        return pd.read_csv(path)

    if suffix in {".xlsx", ".xls", ".xlsm"}:
        if sheet is not None:
            return pd.read_excel(path, sheet_name=sheet, engine="openpyxl")
        xls = pd.ExcelFile(path, engine="openpyxl")
        for s in xls.sheet_names:
            df = pd.read_excel(path, sheet_name=s, engine="openpyxl")
            if not df.dropna(how="all").empty:
                return df
        return pd.read_excel(path, sheet_name=0, engine="openpyxl")

    return pd.read_csv(path)