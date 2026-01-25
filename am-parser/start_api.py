#!/usr/bin/env python3


import sys
from pathlib import Path

# Add parent directory to path to find external modules
sys.path.insert(0, str(Path(__file__).parent))

if __name__ == "__main__":
    import uvicorn
    from am_configs.settings import settings
    
    print("🚀 Starting Mutual Fund Portfolio API Server")
    print("=" * 45)
    print("📍 Server will be available at:")
    port = settings.effective_api_port
    
    print(f"   🌐 API: http://127.0.0.1:{port}")
    print(f"   📚 Docs: http://127.0.0.1:{port}/docs")
    print(f"   📖 ReDoc: http://127.0.0.1:{port}/redoc")
    print("=" * 45)
    
    uvicorn.run(
        "am_api.api:app",
        host="0.0.0.0",
        port=port,
        reload=True,
        log_level="info"
    )