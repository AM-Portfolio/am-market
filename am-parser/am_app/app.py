"""
AM App - Main application interface
Provides unified access to all parsing functionality
"""
import json
import sys
from pathlib import Path
from typing import Dict, Any, Optional, List

# Add parent directory to path to find external modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_services import ManualParserService
from am_llm import LLMParserService


class AMApp:
    """
    Unified application interface for AM Parser
    Provides high-level methods to parse files using different strategies
    """
    
    def __init__(self):
        self.manual_parser = ManualParserService()
        self.llm_parser = LLMParserService()
    
    def parse_file(self, 
                   file_path: str | Path, 
                   *, 
                   method: str = "manual",
                   output_file: Optional[str | Path] = None,
                   sheet: Optional[str | int] = None,
                   header_map: Optional[str] = None,
                   show_preview: bool = False,
                   dry_run: bool = False,
                   api_key: Optional[str] = None) -> Dict[str, Any]:
        """
        Parse a mutual fund file using the specified method
        
        Args:
            file_path: Path to CSV/Excel file
            method: "manual", "llm", "together", or "both" (default: "manual")
            output_file: Optional output JSON file path
            sheet: Excel sheet name or index
            header_map: Header mapping key for manual parsing
            show_preview: Show debug preview for manual parsing
            dry_run: Show prompt without LLM call (LLM parsing only)
            api_key: API key for Together AI (if using "together" method)
            
        Returns:
            Parsed portfolio data as dictionary
        """
        
        if method == "manual":
            return self._parse_manual(file_path, sheet, header_map, show_preview, output_file)
        elif method == "llm":
            return self._parse_llm(file_path, sheet, dry_run, output_file)
        elif method == "together":
            return self._parse_together_ai(file_path, sheet, api_key, dry_run, output_file)
        elif method == "both":
            return self._parse_both(file_path, sheet, header_map, show_preview, dry_run, output_file)
        else:
            raise ValueError(f"Unknown method: {method}. Use 'manual', 'llm', 'together', or 'both'")
    
    def _parse_manual(self, file_path, sheet, header_map, show_preview, output_file):
        """Parse using manual/rule-based parser"""
        parser = ManualParserService(header_map_key=header_map)
        result = parser.parse(file_path, sheet=sheet, show_preview=show_preview)
        
        if output_file:
            self._write_output(result, output_file, suffix="_manual")
            
        return result
    
    def _parse_llm(self, file_path, sheet, dry_run, output_file):
        """Parse using LLM-based parser"""
        result = self.llm_parser.parse(file_path, sheet=sheet, dry_run=dry_run)
        
        if output_file and not dry_run:
            self._write_output(result, output_file, suffix="_llm")
            
        return result
    
    def _parse_together_ai(self, file_path, sheet, api_key, dry_run, output_file):
        """Parse using Together AI LLM service"""
        try:
            result = self.llm_parser.parse_with_together_ai(
                file_path, 
                sheet=sheet, 
                api_key=api_key,
                dry_run=dry_run,
                output_file=output_file
            )
            
            if output_file and not dry_run:
                self._write_output(result, output_file, suffix="_together")
                
            return result
        except ImportError as e:
            print(f"❌ Together AI not available: {e}")
            print("💡 Install with: pip install together")
            raise
        except Exception as e:
            print(f"❌ Together AI parsing failed: {e}")
            raise
    
    def _parse_both(self, file_path, sheet, header_map, show_preview, dry_run, output_file):
        """Parse using both methods and compare results"""
        print("🔄 Parsing with manual method...")
        manual_result = self._parse_manual(file_path, sheet, header_map, show_preview, None)
        
        print("🔄 Parsing with LLM method...")
        llm_result = self._parse_llm(file_path, sheet, dry_run, None)
        
        # Combine results
        combined_result = {
            "manual_result": manual_result,
            "llm_result": llm_result,
            "comparison": self._compare_results(manual_result, llm_result)
        }
        
        if output_file:
            self._write_output(combined_result, output_file, suffix="_both")
            
        return combined_result
    
    def _compare_results(self, manual_result: Dict, llm_result: Dict) -> Dict[str, Any]:
        """Compare results from manual and LLM parsing"""
        manual_holdings = len(manual_result.get("holdings", []))
        llm_holdings = len(llm_result.get("holdings", []))
        
        manual_total = manual_result.get("totals", {}).get("mkt_value", 0)
        llm_total = llm_result.get("totals", {}).get("mkt_value", 0)
        
        return {
            "holdings_count": {
                "manual": manual_holdings,
                "llm": llm_holdings,
                "match": manual_holdings == llm_holdings
            },
            "total_value": {
                "manual": manual_total,
                "llm": llm_total,
                "difference": abs(manual_total - llm_total),
                "match": abs(manual_total - llm_total) < 0.01
            }
        }
    
    def _write_output(self, data: Dict, output_file: str | Path, suffix: str = ""):
        """Write results to JSON file"""
        output_path = Path(output_file)
        if suffix:
            # Insert suffix before file extension
            stem = output_path.stem
            ext = output_path.suffix
            output_path = output_path.parent / f"{stem}{suffix}{ext}"
        
        output_path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
        print(f"✅ Results written to {output_path}")
    
    def batch_parse(self, 
                    file_paths: List[str | Path],
                    *,
                    method: str = "manual",
                    output_dir: Optional[str | Path] = None,
                    **kwargs) -> List[Dict[str, Any]]:
        """
        Parse multiple files in batch
        
        Args:
            file_paths: List of file paths to parse
            method: Parsing method ("manual", "llm", or "both")
            output_dir: Directory to save results (optional)
            **kwargs: Additional arguments passed to parse_file
            
        Returns:
            List of parsed results
        """
        results = []
        output_dir_path = Path(output_dir) if output_dir else None
        
        if output_dir_path:
            output_dir_path.mkdir(exist_ok=True)
        
        for i, file_path in enumerate(file_paths):
            print(f"📁 Processing file {i+1}/{len(file_paths)}: {file_path}")
            
            try:
                output_file = None
                if output_dir_path:
                    file_stem = Path(file_path).stem
                    output_file = output_dir_path / f"{file_stem}_result.json"
                
                result = self.parse_file(
                    file_path, 
                    method=method, 
                    output_file=output_file,
                    **kwargs
                )
                results.append({"file": str(file_path), "result": result, "status": "success"})
                
            except Exception as e:
                print(f"❌ Error processing {file_path}: {e}")
                results.append({"file": str(file_path), "error": str(e), "status": "error"})
        
        return results


# Convenience instance
app = AMApp()

# Convenience functions for direct usage
def parse_file(*args, **kwargs):
    """Convenience function to parse a single file"""
    return app.parse_file(*args, **kwargs)

def batch_parse(*args, **kwargs):
    """Convenience function to parse multiple files"""
    return app.batch_parse(*args, **kwargs)
