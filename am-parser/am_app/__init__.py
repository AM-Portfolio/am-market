"""
AM App - Unified application interface
Single entry point for all AM Parser functionality
"""
import sys
from pathlib import Path

# Add parent directory to path to find external modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_services import ManualParserService
from am_llm import LLMParserService
from am_common import Portfolio

# Import the main app components
from .app import AMApp, app, parse_file, batch_parse
from .cli import cli

__all__ = [
    "AMApp", 
    "app", 
    "parse_file", 
    "batch_parse", 
    "cli",
    "ManualParserService", 
    "LLMParserService", 
    "Portfolio"
]
