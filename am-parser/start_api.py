#!/usr/bin/env python3


import sys
from pathlib import Path

# Add parent directory to path to find external modules
sys.path.insert(0, str(Path(__file__).parent))

if __name__ == "__main__":
    import uvicorn
    
    print("🚀 Starting Mutual Fund Portfolio API Server")
    print("=" * 45)
    print("📍 Server will be available at:")
    print("   🌐 API: http://127.0.0.1:8000")
    print("   📚 Docs: http://127.0.0.1:8000/docs")
    print("   📖 ReDoc: http://127.0.0.1:8000/redoc")
    print("=" * 45)
    
    uvicorn.run(
        "am_api.api:app",
        host="127.0.0.1",
        port=8000,
        reload=True,
        log_level="info"
    )