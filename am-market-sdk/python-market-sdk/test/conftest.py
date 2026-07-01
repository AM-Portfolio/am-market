import subprocess
import sys
import os

# Automatically bootstrap package installation during pytest collection in CI/CD environments
try:
    import pydantic
except ImportError:
    print("[BOOTSTRAP] Pydantic is missing. Installing package and its dependencies...")
    # Find directory containing setup.py/pyproject.toml (one level up from test/)
    pkg_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    subprocess.check_call([sys.executable, "-m", "pip", "install", pkg_dir])
    print("[BOOTSTRAP] Package and dependencies installed successfully!")
