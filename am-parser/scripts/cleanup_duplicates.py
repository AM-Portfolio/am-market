#!/usr/bin/env python3
"""
Cleanup script to remove duplicate/old directories from AM Parser
"""

import shutil
from pathlib import Path

def cleanup_duplicates():
    """Remove duplicate and old directory structures."""
    root = Path("c:/Users/drabh/Downloads/am-parser")
    
    # Directories that should be removed if they exist
    to_remove = [
        # External versions
        "am_api_external", "am_services_external", "am_llm_external",
        "am_persistence_external", "am_common_external", "am_configs_external",
        # Hyphenated versions  
        "am-api", "am-services", "am-llm", "am-persistence", "am-common", "am-configs",
        # Old versions without am_ prefix
        "api", "services", "llm", "persistence", "common", "configs"
    ]
    
    removed = []
    not_found = []
    
    for dir_name in to_remove:
        dir_path = root / dir_name
        if dir_path.exists():
            try:
                if dir_path.is_dir():
                    shutil.rmtree(dir_path)
                    removed.append(dir_name)
                    print(f"🗑️  Removed: {dir_name}/")
                else:
                    dir_path.unlink()
                    removed.append(dir_name)
                    print(f"🗑️  Removed file: {dir_name}")
            except Exception as e:
                print(f"❌ Failed to remove {dir_name}: {e}")
        else:
            not_found.append(dir_name)
    
    print(f"\n📊 CLEANUP SUMMARY:")
    print(f"   ✅ Removed: {len(removed)} items")
    print(f"   ℹ️  Not found: {len(not_found)} items")
    
    if removed:
        print(f"\n🧹 REMOVED DIRECTORIES:")
        for item in removed:
            print(f"   📁 {item}")
    
    print(f"\n✅ Cleanup complete! Structure is now clean.")

if __name__ == "__main__":
    cleanup_duplicates()