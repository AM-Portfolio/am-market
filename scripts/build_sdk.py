#!/usr/bin/env python3
"""
SDK Builder Script for am-market services
Generates Python, Java, and Flutter SDKs from running services using OpenAPI Generator
"""
import argparse
import subprocess
import os
import sys
import urllib.request
import json

def run_cmd(cmd, cwd=None):
    print(f"\n[RUN] Running: {cmd}")
    result = subprocess.run(cmd, shell=True, cwd=cwd, text=True)
    if result.returncode != 0:
        print(f"[FAIL] Command failed with exit code {result.returncode}")
        sys.exit(result.returncode)
    print("[SUCCESS] Command executed successfully!")

def check_service_health(url):
    try:
        with urllib.request.urlopen(url, timeout=3) as response:
            return response.status == 200
    except Exception:
        return False

def generate_sdk(service_name, lang, spec_url, output_dir):
    print(f"\n[SDK] Generating SDK for {service_name.upper()} in {lang.upper()}...")
    
    generator_map = {
        "java": "java",
        "python": "python",
        "flutter": "dart"
    }
    
    target_lang = generator_map.get(lang.lower())
    if not target_lang:
        print(f"[FAIL] Unsupported language for SDK: {lang}")
        sys.exit(1)

    if not check_service_health(spec_url):
        print(f"[WARNING] Service spec at {spec_url} is not reachable.")
        print(f"[INFO] Make sure the {service_name} service is running locally before building its SDK.")
        sys.exit(1)
        
    os.makedirs(output_dir, exist_ok=True)
    cmd = f"npx @openapitools/openapi-generator-cli generate -i {spec_url} -g {target_lang} -o {output_dir} --skip-validate-spec"
    run_cmd(cmd)

def main():
    parser = argparse.ArgumentParser(description="SDK Builder for Market Services")
    parser.add_argument("--service", choices=["market", "parser", "all"], default="all", help="Target service")
    parser.add_argument("--lang", choices=["java", "python", "flutter", "all"], default="all", help="Target language")
    
    args = parser.parse_args()
    
    script_dir = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.dirname(script_dir)
    
    services = {
        "market": {
            "name": "AM Market Data",
            "spec_url": "http://localhost:8092/v3/api-docs",
            "sdk_dir": os.path.join(repo_root, "sdk", "market")
        },
        "parser": {
            "name": "AM Parser",
            "spec_url": "http://localhost:8022/openapi.json",
            "sdk_dir": os.path.join(repo_root, "sdk", "parser")
        }
    }
    
    target_services = [args.service] if args.service != "all" else ["market", "parser"]
    target_langs = [args.lang] if args.lang != "all" else ["java", "python", "flutter"]
    
    for s_key in target_services:
        s_info = services[s_key]
        for lang in target_langs:
            lang_dir = os.path.join(s_info["sdk_dir"], lang)
            generate_sdk(s_info["name"], lang, s_info["spec_url"], lang_dir)

if __name__ == "__main__":
    main()

