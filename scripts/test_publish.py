#!/usr/bin/env python3
"""
test_publish.py — Local test script to replicate the CI/CD SDK publish pipeline.

Supports:
  - Python SDKs  → Builds wheel/sdist and creates a GitHub Release
  - Java modules → Runs `mvn deploy` to publish to GitHub Packages Maven Registry
  - Flutter SDKs → Zips source and creates a GitHub Release

No external tools required (uses GitHub REST API directly).

Credentials loaded from (in priority order):
  1. Environment variables:  GITHUB_RELEASE_TOKEN (repo scope, for releases)
                             GITHUB_PACKAGES_TOKEN (write:packages scope, for maven)
                             GITHUB_ACTOR / GITHUB_PACKAGES_USERNAME
  2. .env file in the am-market root or scripts directory
  3. Interactive prompt (NOT saved to any file)

Logging:
  - Console + logs/publish_YYYY-MM-DD.log (append same day, new file next day)
  - logs/ is gitignored automatically
"""

import argparse
import getpass
import json
import logging
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
import zipfile
from datetime import datetime
from pathlib import Path

# ─────────────────────────────────────────────────────────────────────────────
# Directory layout
# ─────────────────────────────────────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT  = SCRIPT_DIR.parent   # am-market/
LOGS_DIR   = REPO_ROOT / "logs"

SDK_MAP = {
    # --- Market SDKs ---
    "sdk:market:python": {
        "type": "python",
        "path": REPO_ROOT / "am-market-sdk" / "python-market-sdk",
        "label": "python-market-sdk",
        "desc": "Python Market SDK  → GitHub Release (.whl)",
    },
    "sdk:market:java": {
        "type": "java",
        "path": REPO_ROOT / "am-market-sdk" / "java-market-sdk",
        "label": "java-market-sdk",
        "maven_repo_id": "github-investment",
        "maven_repo_url": "https://maven.pkg.github.com/AM-Portfolio/am-market",
        "desc": "Java Market SDK    → GitHub Packages (Maven)",
    },
    "sdk:market:flutter": {
        "type": "flutter",
        "path": REPO_ROOT / "am-market-sdk" / "flutter-market-sdk",
        "label": "flutter-market-sdk",
        "desc": "Flutter Market SDK → GitHub Release (.zip)",
    },
    # --- Parser SDKs ---
    "sdk:parser:python": {
        "type": "python",
        "path": REPO_ROOT / "am-market-sdk" / "python-parser-sdk",
        "label": "python-parser-sdk",
        "desc": "Python Parser SDK  → GitHub Release (.whl)",
    },
    "sdk:parser:java": {
        "type": "java",
        "path": REPO_ROOT / "am-market-sdk" / "java-parser-sdk",
        "label": "java-parser-sdk",
        "maven_repo_id": "github-investment",
        "maven_repo_url": "https://maven.pkg.github.com/AM-Portfolio/am-market",
        "desc": "Java Parser SDK    → GitHub Packages (Maven)",
    },
    "sdk:parser:flutter": {
        "type": "flutter",
        "path": REPO_ROOT / "am-market-sdk" / "flutter-parser-sdk",
        "label": "flutter-parser-sdk",
        "desc": "Flutter Parser SDK → GitHub Release (.zip)",
    },
    # --- Other modules ---
    "module:common:java": {
        "type": "java",
        "path": REPO_ROOT / "am-common-investment-data",
        "label": "am-common-investment-data",
        "maven_repo_id": "github",
        "maven_repo_url": "https://maven.pkg.github.com/AM-Portfolio/am-common-investment-data",
        "desc": "Java Common Data   → GitHub Packages (Maven)",
    },
}

ENV_FILES = [REPO_ROOT / ".env", SCRIPT_DIR / ".env"]


# ─────────────────────────────────────────────────────────────────────────────
# Info / quick-reference
# ─────────────────────────────────────────────────────────────────────────────
INFO_TEXT = """
╔══════════════════════════════════════════════════════════════╗
║              AM SDK Publish — Quick Command Guide            ║
╠══════════════════════════════════════════════════════════════╣
║  TARGETED COMMANDS (npm run):                                ║
╠══════════════════════════════════════════════════════════════╣
║  [Python]                                                    ║
║    sdk:market:python                                         ║
║    sdk:parser:python                                         ║
║                                                              ║
║  [Java]                                                      ║
║    sdk:market:java                                           ║
║    sdk:parser:java                                           ║
║    common:data:java                                          ║
║    am-common-investment-data:publish                         ║
║                                                              ║
║  [Flutter]                                                   ║
║    sdk:market:flutter                                        ║
║    sdk:parser:flutter                                        ║
║                                                              ║
║  [Utility]                                                   ║
║    publish:test      → interactive SDK picker                ║
║    publish:dry       → build only, no upload                 ║
║    publish:info      → show this guide                       ║
╠══════════════════════════════════════════════════════════════╣
║  REQUIRED TOKENS IN .env:                                    ║
║    GITHUB_RELEASE_TOKEN (repo scope)                         ║
║    GITHUB_PACKAGES_TOKEN (write:packages scope)              ║
╚══════════════════════════════════════════════════════════════╝
"""


# ─────────────────────────────────────────────────────────────────────────────
# Logging
# ─────────────────────────────────────────────────────────────────────────────
def setup_logging() -> logging.Logger:
    LOGS_DIR.mkdir(exist_ok=True)
    today    = datetime.now().strftime("%Y-%m-%d")
    log_file = LOGS_DIR / f"publish_{today}.log"

    logger = logging.getLogger("test_publish")
    logger.setLevel(logging.DEBUG)
    fmt = logging.Formatter("%(asctime)s [%(levelname)s] %(message)s", datefmt="%Y-%m-%d %H:%M:%S")

    ch = logging.StreamHandler(sys.stdout)
    ch.setLevel(logging.INFO)
    ch.setFormatter(fmt)
    logger.addHandler(ch)

    fh = logging.FileHandler(log_file, mode="a", encoding="utf-8")
    fh.setLevel(logging.DEBUG)
    fh.setFormatter(fmt)
    logger.addHandler(fh)

    with open(log_file, "a") as f:
        f.write(f"\n{'─' * 60}\n")
        f.write(f"  Session: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"{'─' * 60}\n")
    return logger


# ─────────────────────────────────────────────────────────────────────────────
# Gitignore protection
# ─────────────────────────────────────────────────────────────────────────────
def ensure_gitignore(log: logging.Logger):
    gitignore = REPO_ROOT / ".gitignore"
    entries   = [".env", "dist/", "build/", "*.egg-info/", "__pycache__/", "logs/"]
    existing  = gitignore.read_text() if gitignore.exists() else ""
    missing   = [e for e in entries if e not in existing]
    if missing:
        with gitignore.open("a") as f:
            f.write("\n# Added by test_publish.py\n")
            for e in missing:
                f.write(f"{e}\n")
        log.info(f"🛡️  Added to .gitignore: {missing}")


# ─────────────────────────────────────────────────────────────────────────────
# Credential helpers
# ─────────────────────────────────────────────────────────────────────────────
def load_env_file(log: logging.Logger) -> dict:
    for p in ENV_FILES:
        if p.exists():
            log.info(f"📄 Loading credentials from: {p}")
            vals = {}
            for line in p.read_text().splitlines():
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                k, _, v = line.partition("=")
                vals[k.strip()] = v.strip().strip('"').strip("'")
            return vals
    return {}


def resolve_release_token(env_vars: dict, log: logging.Logger) -> str:
    """Token with 'repo' scope — needed to create GitHub Releases."""
    for key in ("GITHUB_RELEASE_TOKEN", "GITHUB_TOKEN"):
        val = os.environ.get(key) or env_vars.get(key)
        if val and val.strip():
            log.info(f"✅ Release Token: found via {key}")
            return val.strip()
    log.warning("⚠️  No GITHUB_RELEASE_TOKEN found in .env")
    val = getpass.getpass("   Enter GitHub Release Token (repo scope): ").strip()
    if not val:
        log.error("❌ Token required. Exiting.")
        sys.exit(1)
    return val


def resolve_packages_token(env_vars: dict, log: logging.Logger) -> str:
    """Token with 'write:packages' scope — needed for Maven/GitHub Packages."""
    for key in ("GITHUB_PACKAGES_TOKEN", "GITHUB_RELEASE_TOKEN", "GITHUB_TOKEN"):
        val = os.environ.get(key) or env_vars.get(key)
        if val and val.strip():
            log.info(f"✅ Packages Token: found via {key}")
            return val.strip()
    log.warning("⚠️  No GITHUB_PACKAGES_TOKEN found in .env")
    val = getpass.getpass("   Enter GitHub Packages Token (write:packages scope): ").strip()
    if not val:
        log.error("❌ Token required. Exiting.")
        sys.exit(1)
    return val


def resolve_actor(env_vars: dict, log: logging.Logger) -> str:
    for key in ("GITHUB_ACTOR", "GITHUB_USERNAME", "GITHUB_PACKAGES_USERNAME"):
        val = os.environ.get(key) or env_vars.get(key)
        if val and val.strip():
            log.info(f"✅ GitHub Username: {val.strip()} (via {key})")
            return val.strip()
    val = input("   Enter GitHub Username: ").strip()
    if not val:
        log.error("❌ Username required. Exiting.")
        sys.exit(1)
    return val


def detect_repo(env_vars: dict, log: logging.Logger) -> str:
    repo = env_vars.get("GITHUB_REPOSITORY") or os.environ.get("GITHUB_REPOSITORY", "")
    if repo:
        return repo
    try:
        remote = subprocess.check_output(
            ["git", "remote", "get-url", "origin"],
            cwd=str(REPO_ROOT), text=True, stderr=subprocess.DEVNULL,
        ).strip()
        m = re.search(r"github\.com[:/](.+?/.+?)(?:\.git)?$", remote)
        if m:
            return m.group(1)
    except Exception:
        pass
    return input("   Enter GitHub repository (owner/repo): ").strip()


# ─────────────────────────────────────────────────────────────────────────────
# Interactive SDK picker
# ─────────────────────────────────────────────────────────────────────────────
def pick_sdk() -> str:
    keys = list(SDK_MAP.keys())
    print("\n Available SDKs to publish:\n")
    for i, k in enumerate(keys, 1):
        print(f"  [{i}] {k:25} | {SDK_MAP[k]['desc']}")
    print()
    while True:
        raw = input("  Select SDK number (or type name): ").strip()
        if raw.isdigit() and 1 <= int(raw) <= len(keys):
            return keys[int(raw) - 1]
        if raw in SDK_MAP:
            return raw
        print(f"  ❌ Invalid choice: '{raw}'. Try again.")


# ─────────────────────────────────────────────────────────────────────────────
# GitHub REST API
# ─────────────────────────────────────────────────────────────────────────────
def gh_api(method: str, path: str, token: str, data: dict = None, log: logging.Logger = None) -> dict:
    url = f"https://api.github.com{path}"
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    body = json.dumps(data).encode() if data else None
    req  = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        msg = e.read().decode()
        if log:
            log.error(f"GitHub API {method} {path} → HTTP {e.code}: {msg}")
            if e.code == 401:
                log.error("  ❌ 401 Unauthorized: token invalid or incorrect scopes.")
            if e.code == 403:
                log.error(f"  ❌ 403 Forbidden: check organization policy or token expiration.")
        raise


def gh_upload_asset(upload_url: str, file_path: Path, token: str, log: logging.Logger):
    base_url = upload_url.split("{")[0]
    url      = f"{base_url}?name={file_path.name}"
    headers  = {
        "Authorization": f"token {token}",
        "Content-Type": "application/octet-stream",
        "Accept": "application/vnd.github+json",
    }
    with open(file_path, "rb") as f:
        data = f.read()
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            result = json.loads(resp.read())
            log.info(f"  ✅ Uploaded: {file_path.name}")
    except urllib.error.HTTPError as e:
        log.error(f"  ❌ Failed to upload {file_path.name}: {e.code}")
        raise


def create_or_update_release(repo: str, tag: str, title: str, notes: str,
                              dist_dir: Path, token: str, log: logging.Logger):
    try:
        log.info(f"  Creating release tag: {tag}")
        release = gh_api("POST", f"/repos/{repo}/releases", token, {
            "tag_name": tag, "name": title, "body": notes,
        }, log)
    except urllib.error.HTTPError as e:
        if e.code == 422:
            log.warning(f"  ⚠️  Tag '{tag}' exists, updating assets...")
            release = gh_api("GET", f"/repos/{repo}/releases/tags/{tag}", token, log=log)
        else:
            raise

    for asset in dist_dir.iterdir():
        gh_upload_asset(release["upload_url"], asset, token, log)
    log.info(f"  ✅ Release ready: {release['html_url']}")


# ─────────────────────────────────────────────────────────────────────────────
# Builders
# ─────────────────────────────────────────────────────────────────────────────
def python_build(sdk_dir: Path, log: logging.Logger) -> Path:
    dist = sdk_dir / "dist"
    if dist.exists(): shutil.rmtree(dist)
    log.info(f"📦 Building Python: {sdk_dir}")
    subprocess.run([sys.executable, "-m", "pip", "install", "-q", "--upgrade", "build"], check=True)
    subprocess.run([sys.executable, "-m", "build", str(sdk_dir)], check=True)
    return dist


def flutter_build(sdk_dir: Path, log: logging.Logger) -> Path:
    dist = sdk_dir / "dist"
    if dist.exists(): shutil.rmtree(dist)
    dist.mkdir(parents=True)
    log.info(f"📦 Zipping Flutter: {sdk_dir}")
    
    # Read version from pubspec.yaml and strip quotes
    yaml = (sdk_dir / "pubspec.yaml").read_text()
    m = re.search(r'^version:\s*["\']?([^"\'\s+]+)', yaml, re.MULTILINE)
    version = m.group(1) if m else "1.0.0"
    
    zip_path = dist / f"{sdk_dir.name}-{version}.zip"
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as z:
        for root, dirs, files in os.walk(sdk_dir):
            if "dist" in dirs: dirs.remove("dist")
            if ".git" in dirs: dirs.remove(".git")
            for file in files:
                p = Path(root) / file
                z.write(p, p.relative_to(sdk_dir))
    log.info(f"  Created: {zip_path.name}")
    return dist


def read_version(sdk_dir: Path, sdk_type: str) -> str:
    if sdk_type == "python":
        toml = (sdk_dir / "pyproject.toml").read_text()
        m = re.search(r'^version\s*=\s*["\']([^"\']+)["\']', toml, re.MULTILINE)
        return m.group(1) if m else "1.0.0"
    if sdk_type == "flutter":
        yaml = (sdk_dir / "pubspec.yaml").read_text()
        m = re.search(r'^version:\s*["\']?([^"\'\s+]+)', yaml, re.MULTILINE)
        return m.group(1) if m else "1.0.0"
    return "1.0.0"


# ─────────────────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="AM SDK Local Publish")
    parser.add_argument("--sdk", default=None, choices=list(SDK_MAP.keys()))
    parser.add_argument("--repo", default=None)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--info", action="store_true")
    args = parser.parse_args()

    if args.info:
        print(INFO_TEXT)
        return

    log = setup_logging()
    sdk_name = args.sdk or pick_sdk()
    ensure_gitignore(log)
    env_vars = load_env_file(log)
    sdk_info = SDK_MAP[sdk_name]
    
    log.info("=" * 50)
    log.info(f"  Target: {sdk_name}")
    log.info("=" * 50)

    if sdk_info["type"] == "java":
        token = resolve_packages_token(env_vars, log)
        actor = resolve_actor(env_vars, log)
        if args.dry_run:
            log.info("[DRY RUN] Would mvn deploy...")
        else:
            # Build and deploy java via temp settings.xml as before
            settings_xml = f"<settings><servers><server><id>{sdk_info['maven_repo_id']}</id><username>{actor}</username><password>{token}</password></server></servers></settings>"
            with tempfile.NamedTemporaryFile(mode="w", suffix=".xml", delete=False) as f:
                f.write(settings_xml)
                s_path = f.name
            try:
                subprocess.run(["mvn", "-B", "package", "-DskipTests"], cwd=str(sdk_info["path"]), check=True)
                subprocess.run(["mvn", "-B", "deploy", "-DskipTests", f"--settings={s_path}"], cwd=str(sdk_info["path"]), check=True)
                log.info(f"✅ Published: {sdk_info['maven_repo_url']}")
            finally:
                os.unlink(s_path)
    else:
        # Python/Flutter both use GitHub Releases
        token = resolve_release_token(env_vars, log)
        repo  = args.repo or detect_repo(env_vars, log)
        version = read_version(sdk_info["path"], sdk_info["type"])
        
        # Use a clean label for tag and title (e.g. flutter-market-sdk)
        label = sdk_info["label"]
        tag   = f"{label}-v{version}"
        title = f"{label} v{version}"
        
        if sdk_info["type"] == "python":
            dist = python_build(sdk_info["path"], log)
        else:
            dist = flutter_build(sdk_info["path"], log)
            
        if args.dry_run:
            log.info(f"[DRY RUN] Would create release: {tag}")
            log.info(f"Found: {[f.name for f in dist.iterdir()]}")
        else:
            notes = f"## {label}\nAutomated release of v{version}"
            create_or_update_release(repo, tag, title, notes, dist, token, log)
    
    log.info("✅ Done.")

if __name__ == "__main__":
    main()
