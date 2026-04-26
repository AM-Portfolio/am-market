#!/usr/bin/env python3
import os
import subprocess
import sys
import argparse

def load_env(env_file):
    """Loads variables from a .env file into a dictionary."""
    env_vars = {}
    if os.path.exists(env_file):
        with open(env_file, 'r') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#') or '=' not in line:
                    continue
                key, value = line.split('=', 1)
                env_vars[key.strip()] = value.strip()
    return env_vars

def run_command(cmd_parts):
    """Runs a command as a list of arguments to avoid shell-specific quoting issues."""
    print(f"\n🚀 Running: {' '.join(cmd_parts)}")
    # Use shell=False (default) and pass as list for maximum cross-platform safety
    result = subprocess.run(cmd_parts, shell=(os.name == 'nt'))
    if result.returncode != 0:
        print(f"\n❌ Command failed with exit code {result.returncode}")
        sys.exit(result.returncode)

def main():
    parser = argparse.ArgumentParser(description="Cross-platform Local Deployment Script")
    parser.add_argument("app_name", help="Name of the app (am-market-data or am-parser)")
    parser.add_argument("--tag", help="Image tag to deploy (overrides .env)")
    parser.add_argument("--env-file", default=".env.deploy", help="Path to env file")
    parser.add_argument("--use-runner", action="store_true", help="Run strictly inside the Docker am-infra-gh-runner container")
    args = parser.parse_args()

    # 1. Load configuration
    env = load_env(args.env_file)
    
    # 2. Resolve Variables
    chart_path = env.get("HELM_CHART_PATH", "../am-pipelines/helm/universal-chart")
    registry = env.get("CONTAINER_REGISTRY", "ghcr.io/AM-Portfolio")
    namespace = env.get("PREPROD_NAMESPACE", "am-apps-preprod")
    tag = args.tag or env.get("DEFAULT_TAG", "latest")

    # 3. Determine App Specifics
    if args.app_name == "am-market-data":
        working_dir = "am-market-data"
        language = "java"
    elif args.app_name == "am-parser":
        working_dir = "am-parser"
        language = "python"
    else:
        print(f"❌ Error: Unknown app name '{args.app_name}'. Supported: am-market-data, am-parser")
        sys.exit(1)

    # 4. Set Paths (ensure forward slashes for Helm even on Windows)
    base_values = os.path.join(working_dir, "helm", "values.yaml").replace('\\', '/')
    preprod_values = os.path.join(working_dir, "helm", "values.preprod.yaml").replace('\\', '/')
    chart_path_norm = chart_path.replace('\\', '/')

    # 5. Handle Runner Execution
    if args.use_runner:
        print("\n🐳 Mode: Docker Runner (am-infra-gh-runner)")
        
        # Detect which container is actually running
        container_name = "am-infra-gh-runner"
        check_cmd = ["docker", "ps", "--format", "{{.Names}}", "--filter", f"name={container_name}"]
        result = subprocess.run(check_cmd, capture_output=True, text=True, shell=(os.name=='nt'))
        
        if container_name not in result.stdout:
            fallback_name = "am-github-runner"
            print(f"⚠️  {container_name} not found, checking for {fallback_name}...")
            check_fallback = subprocess.run(["docker", "ps", "--format", "{{.Names}}", "--filter", f"name={fallback_name}"], capture_output=True, text=True, shell=(os.name=='nt'))
            if fallback_name in check_fallback.stdout:
                container_name = fallback_name
            else:
                print(f"❌ Error: Neither {container_name} nor {fallback_name} are running.")
                print("💡 Please run 'cd ../am-infra && docker-compose -f docker-compose.infra.yaml up -d'")
                sys.exit(1)

        print(f"🔍 Using Runner Container: {container_name}")
        
        container_chart = "/workspaces/am-repos/am-pipelines/helm/universal-chart"
        container_base = f"/workspaces/am-repos/am-market/{base_values}"
        container_preprod = f"/workspaces/am-repos/am-market/{preprod_values}"
        
        helm_cmd = (
            f'helm upgrade --install "{args.app_name}" "{container_chart}" '
            f'--namespace "{namespace}" --create-namespace '
            f'--set global.image.registry="{registry}" '
            f'--set global.image.tag="{tag}" '
            f'--set global.image.pullPolicy="Always" '
            f'--set language="{language}" '
            f'-f "{container_base}" '
            f'-f "{container_preprod}"'
        )
        
        run_command(["docker", "exec", container_name, "bash", "-c", helm_cmd])
        
        print("\n⏳ Verifying Rollout inside the runner...")
        run_command(["docker", "exec", container_name, "bash", "-c", f"kubectl rollout status deployment/{args.app_name} -n {namespace} --timeout=120s"])
    else:
        print("\n💻 Mode: Native DevContainer")
        helm_cmd = [
            "helm", "upgrade", "--install", args.app_name, chart_path_norm,
            "--namespace", namespace, "--create-namespace",
            "--set", f"global.image.registry={registry}",
            "--set", f"global.image.tag={tag}",
            "--set", "global.image.pullPolicy=Always",
            "--set", f"language={language}",
            "-f", base_values,
            "-f", preprod_values
        ]
        run_command(helm_cmd)
        print("\n⏳ Verifying Rollout locally...")
        verify_cmd = ["kubectl", "rollout", "status", f"deployment/{args.app_name}", "-n", namespace, "--timeout=5m"]
        run_command(verify_cmd)
    
    print(f"\n✅ Success! {args.app_name} is running in {namespace}")

if __name__ == "__main__":
    main()
