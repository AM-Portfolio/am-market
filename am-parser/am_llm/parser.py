from __future__ import annotations

import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional

# Add parent directory to path to find other external modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_services import Portfolio, Fund, Holding, Totals, load_tabular

# Import Together AI service if available
try:
    from .together_service import TogetherLLMService
except ImportError:
    TogetherLLMService = None


class LLMClient:
    def structured_portfolio_from_table(self, table_rows: List[Dict[str, Any]], *, system_prompt: str) -> Dict[str, Any]:
        # Heuristic fallback to ensure offline behavior
        keys = {
            "isin": ["isin", "isin code"],
            "ticker": ["ticker", "symbol"],
            "name": ["name", "security name", "company", "holding"],
            "sector": ["sector", "industry"],
            "qty": ["qty", "quantity", "units"],
            "mkt_value": ["market value", "mkt value", "mkt_value", "value", "amount"],
            "weight": ["weight", "%", "allocation", "portfolio %"],
        }

        def pick(row: Dict[str, Any], aliases):
            low = {str(k).lower(): v for k, v in row.items()}
            for a in aliases:
                if a in low:
                    return low[a]
            return None

        holdings: List[Dict[str, Any]] = []
        for r in table_rows:
            itm = {k: pick(r, v) for k, v in keys.items()}
            if itm.get("name") or itm.get("mkt_value"):
                holdings.append(itm)

        total = 0.0
        for h in holdings:
            try:
                total += float(h.get("mkt_value", 0) or 0)
            except (TypeError, ValueError):
                pass

        any_weight = any(h.get("weight") not in (None, "") for h in holdings)
        if not any_weight and total > 0:
            for h in holdings:
                try:
                    val = float(h.get("mkt_value", 0) or 0)
                except (TypeError, ValueError):
                    val = 0.0
                h["weight"] = round(100.0 * val / total, 4)

        portfolio = Portfolio(
            fund=Fund(),
            holdings=[Holding(**h) for h in holdings],
            totals=Totals(mkt_value=round(total, 4), weight=round(sum(float(h.get("weight", 0) or 0) for h in holdings), 4)),
            meta={"provider": self.__class__.__name__},
        )
        return portfolio.model_dump()


@dataclass
class OpenAIClient(LLMClient):
    api_key: Optional[str] = None
    model: Optional[str] = None

    def __post_init__(self):
        if not self.api_key:
            self.api_key = os.getenv("OPENAI_API_KEY")
        if not self.model:
            self.model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")


def get_llm_client(provider: Optional[str], model: Optional[str]) -> LLMClient:
    prov = (provider or os.getenv("LLM_PROVIDER") or "").lower()
    if prov == "openai":
        return OpenAIClient(model=model)
    return LLMClient()


PROMPT = (
    "You are given a portfolio table extracted from a mutual fund statement. "
    "Return a STRICT JSON object with keys: fund{name, report_date, currency}, holdings[list of {isin, ticker, name, sector, qty, mkt_value, weight}], totals{mkt_value, weight}. "
    "No commentary; use null for unknown fields."
)


@dataclass
class LLMParserService:
    provider: Optional[str] = None
    model: Optional[str] = None

    def parse(self, file_path: str | Path, *, sheet: Optional[str | int] = None, dry_run: bool = False) -> Dict[str, Any]:
        df = load_tabular(file_path, sheet=sheet)
        
        # Convert to table rows for processing
        table_rows = df.to_dict('records')
        
        # Use heuristic LLM client for parsing
        client = LLMClient()
        system_prompt = f"Extract portfolio holdings from {file_path}"
        
        result = client.structured_portfolio_from_table(table_rows, system_prompt=system_prompt)
        
        if dry_run:
            print("🔍 Dry run - would return:", result)
            return result
        
        return result
    
    def parse_with_together_ai(self, file_path: str | Path, *, sheet: Optional[str | int] = None, 
                              api_key: str = None, output_file: str = None, dry_run: bool = False) -> Dict[str, Any]:
        """
        Parse Excel file using Together AI LLM service
        
        Args:
            file_path: Path to Excel file
            sheet: Sheet name or index to process
            api_key: Together AI API key
            output_file: Optional output file path
            dry_run: If True, don't make actual API calls
            
        Returns:
            Extracted portfolio data
        """
        if not TogetherLLMService:
            raise ImportError("Together AI service not available. Install with: pip install together")
        
        file_path = Path(file_path)
        sheet_name = sheet if isinstance(sheet, str) else f"Sheet{sheet}" if sheet is not None else "Sheet1"
        
        if dry_run:
            print(f"🔍 Dry run - would extract from {file_path}, sheet '{sheet_name}' using Together AI")
            return {
                "mutual_fund_name": f"Mock Fund from {file_path.name}",
                "portfolio_date": "March 2025",
                "total_holdings": 100,
                "portfolio_holdings": [
                    {"name_of_instrument": "Mock Stock", "isin_code": "MOCK123", "percentage_to_nav": "1.0%"}
                ]
            }
        
        # Initialize Together AI service
        service = TogetherLLMService(api_key=api_key)
        
        # Extract portfolio data
        return service.extract_portfolio_from_excel(
            excel_file=str(file_path),
            sheet_name=str(sheet_name),
            output_file=output_file
        )
        df = load_tabular(file_path, sheet=sheet)
        table_json = df.to_dict(orient="records")
        if dry_run:
            return {"prompt": PROMPT, "extracted_table_preview": table_json[:10], "note": "Dry run - no LLM call"}
        client = get_llm_client(self.provider, self.model)
        return client.structured_portfolio_from_table(table_json, system_prompt=PROMPT)
