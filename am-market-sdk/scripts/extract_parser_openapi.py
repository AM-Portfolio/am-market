#!/usr/bin/env python3
"""
Extract OpenAPI specification from the Parser FastAPI application.
This script imports the FastAPI app and dumps its OpenAPI schema to JSON.
"""

import json
import sys
from pathlib import Path

def extract_parser_openapi():
    """Extract OpenAPI spec from am-parser FastAPI app."""
    try:
        # Add am-parser to path (it's in am-market/am-parser)
        parser_path = Path(__file__).parent.parent.parent / "am-parser"
        sys.path.insert(0, str(parser_path))
        
        print(f"📂 Looking for FastAPI app in: {parser_path}")
        
        # Import the FastAPI app
        from am_api.api import app
        
        print("✅ Successfully imported FastAPI app")
        
        # Generate OpenAPI spec
        openapi_spec = app.openapi()
        
        # Determine output path
        output_path = Path(__file__).parent.parent / "parser-openapi.json"
        
        # Write to file
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(openapi_spec, f, indent=2)
        
        print(f"✅ Extracted Parser OpenAPI spec to: {output_path.absolute()}")
        print(f"📊 Title: {openapi_spec.get('info', {}).get('title')}")
        print(f"📊 Version: {openapi_spec.get('info', {}).get('version')}")
        print(f"📊 Paths: {len(openapi_spec.get('paths', {}))}")
        
        return True
        
    except ImportError as e:
        print(f"❌ Failed to import FastAPI app: {e}")
        print(f"💡 Ensure am-parser is in the correct location and dependencies are installed")
        return False
    except Exception as e:
        print(f"❌ Error extracting OpenAPI spec: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = extract_parser_openapi()
    sys.exit(0 if success else 1)
