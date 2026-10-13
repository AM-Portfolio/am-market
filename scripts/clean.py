#!/usr/bin/env python3
"""
Clean Script for am-market
Removes build targets, logs, and Python cache files across the project
"""
import os
import shutil
import sys

def delete_path(path):
    if os.path.isdir(path):
        try:
            shutil.rmtree(path)
            print(f"[DELETE] Deleted directory: {path}")
        except Exception as e:
            print(f"[WARNING] Could not delete directory {path}: {e}")
    elif os.path.isfile(path):
        try:
            os.remove(path)
            print(f"[DELETE] Deleted file: {path}")
        except Exception as e:
            print(f"[WARNING] Could not delete file {path}: {e}")

def clean_python_caches(start_dir):
    print(f"[CLEAN] Scanning for Python cache files in {start_dir}...")
    count = 0
    for root, dirs, files in os.walk(start_dir):
        # Clean __pycache__
        if "__pycache__" in dirs:
            pycache_path = os.path.join(root, "__pycache__")
            delete_path(pycache_path)
            count += 1
        # Clean .pytest_cache
        if ".pytest_cache" in dirs:
            pytest_path = os.path.join(root, ".pytest_cache")
            delete_path(pytest_path)
            count += 1
            
        # Clean stray .pyc/.pyo files
        for f in files:
            if f.endswith(('.pyc', '.pyo')):
                file_path = os.path.join(root, f)
                delete_path(file_path)
                count += 1
    print(f"[SUCCESS] Python cache cleaning complete. Cleaned {count} locations.")

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.dirname(script_dir)
    
    print("[CLEAN] Starting AM-Market cleanup process...")
    
    # 1. Clean Maven / Java Targets
    maven_modules = [
        os.path.join(repo_root, "am-market-data"),
        os.path.join(repo_root, "am-market-data", "market-data-app"),
        os.path.join(repo_root, "am-common-investment-data")
    ]
    for module in maven_modules:
        target = os.path.join(module, "target")
        if os.path.exists(target):
            delete_path(target)
            
    # 2. Clean Logs Directory
    logs_dir = os.path.join(repo_root, "logs")
    if os.path.exists(logs_dir):
        delete_path(logs_dir)
        
    # 3. Clean Python Caches
    clean_python_caches(repo_root)
    
    print("\n[SUCCESS] All clean operations completed successfully!")

if __name__ == "__main__":
    main()

