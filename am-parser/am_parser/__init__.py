"""
AM Parser - Backward compatibility wrapper
Imports from external modules for compatibility
"""
import sys
from pathlib import Path

# Add parent directory to path to find external modules
sys.path.insert(0, str(Path(__file__).parent.parent))

# Import main services for backward compatibility
from am_services import ManualParserService
from am_llm import LLMParserService
from am_common import Portfolio, Fund, Holding, Totals

# Legacy aliases for backward compatibility
ManualParser = ManualParserService
LLMParser = LLMParserService

__all__ = [
    "__version__",
    "ManualParserService",
    "LLMParserService", 
    "ManualParser",  # Legacy alias
    "LLMParser",     # Legacy alias
    "Portfolio",
    "Fund",
    "Holding",
    "Totals"
]

__version__ = "0.1.0"
