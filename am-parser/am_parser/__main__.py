"""
CLI entry point for am_parser package
Delegates to am_api for actual CLI functionality
"""
import sys
from pathlib import Path

# Add parent directory to path to find external modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_api.cli import cli

if __name__ == "__main__":
    cli()
