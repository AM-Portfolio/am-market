#!/usr/bin/env python3
import argparse
import subprocess
import os
import sys

def run_cmd(cmd, cwd=None):
    print(f"\n🚀 Running: {cmd}")
    result = subprocess.run(cmd, shell=True, cwd=cwd, text=True)
    if result.returncode != 0:
        print(f"❌ Command failed with exit code {result.returncode}")
        sys.exit(result.returncode)
    print("✅ Success!")

def main():
    parser = argparse.ArgumentParser(description="Simulate the GitHub Actions Helm Deployment Logic Locally")
    parser.add_argument("module", help="Name of the module to deploy (e.g., am-parser, am-common-investment-app)")
    parser.add_argument("--env", choices=["preprod", "prod"], default="preprod", help="Target environment (preprod or prod)")
    parser.add_argument("--image-tag", default="latest", help="Image tag to deploy")
    parser.add_argument("--use-runner", action="store_true", help="Run the helm command directly inside the am-github-runner Docker container to test its exact network and permissions")
    
    args = parser.parse_args()
    
    # 1. Resolve paths
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    module_path = os.path.join(repo_root, args.module)
    
    if not os.path.isdir(module_path):
        print(f"❌ Module directory not found: {module_path}")
        sys.exit(1)
        
    values_file = os.path.join(args.module, "helm", f"values.{args.env}.yaml")
    full_values_path = os.path.join(repo_root, values_file)
    
    if not os.path.isfile(full_values_path):
        print(f"❌ Values file not found: {full_values_path}")
        sys.exit(1)

    # 2. Extract base namespace and app name from the values file path structure
    print(f"🔍 Analyzing module: {args.module} for {args.env.upper()}")
    
    app_name = args.module
    namespace = f"am-apps-{args.env}"

    print(f"📦 Target App: {app_name}")
    print(f"🌐 Target Namespace: {namespace}")
    print(f"📄 Values File: {values_file}")
    
    # The pipelines repo is identically structured entirely above the current repo
    pipelines_repo = os.path.abspath(os.path.join(repo_root, "..", "am-pipelines", "helm", "universal-chart"))
    
    if not os.path.isdir(pipelines_repo):
        print(f"❌ Could not find universal chart at {pipelines_repo}")
        print("💡 Ensure the am-pipelines repository is cloned next to am-market!")
        sys.exit(1)

    # 3. Construct the exact helm command used in central-build-publish.yml
    helm_cmd = (
        f"helm upgrade --install {app_name} {pipelines_repo} "
        f"--namespace {namespace} "
        f"--values {full_values_path} "
        f"--set image.tag={args.image_tag}"
    )

    if args.use_runner:
        print("\n🐳 Executing strictly inside the Docker am-github-runner container...")
        # To run in docker, we need paths relative to the container's workspace mount
        # docker-compose maps ../:/workspaces/am-repos
        container_pipelines = "/workspaces/am-repos/am-pipelines/helm/universal-chart"
        container_values = f"/workspaces/am-repos/am-market/{values_file}"
        
        container_helm_cmd = (
            f"helm upgrade --install {app_name} {container_pipelines} "
            f"--namespace {namespace} "
            f"--values {container_values} "
            f"--set image.tag={args.image_tag}"
        )
        
        docker_cmd = f"docker exec am-github-runner bash -c \"{container_helm_cmd}\""
        run_cmd(docker_cmd, cwd=repo_root)
        
        print("\n🔍 Verifying Rollout inside the runner...")
        verify_cmd = f"docker exec am-github-runner bash -c \"kubectl rollout status deployment/{app_name} -n {namespace} --timeout=120s\""
        run_cmd(verify_cmd, cwd=repo_root)
    else:
        print("\n💻 Executing natively in the DevContainer...")
        run_cmd(helm_cmd, cwd=repo_root)
        
        print("\n🔍 Verifying Rollout locally...")
        verify_cmd = f"kubectl rollout status deployment/{app_name} -n {namespace} --timeout=120s"
        run_cmd(verify_cmd, cwd=repo_root)

if __name__ == "__main__":
    main()
