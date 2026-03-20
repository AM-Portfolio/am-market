import argparse
import os
import sys

# --- Bootstrap am-scripts ---
script_dir = os.path.dirname(os.path.abspath(__file__))
repo_root = os.path.dirname(script_dir) # am-market
am_repos_root = os.path.dirname(repo_root) # am-repos
am_scripts_src = os.path.join(am_repos_root, "am-scripts", "src")

if os.path.exists(am_scripts_src):
    if am_scripts_src not in sys.path:
        sys.path.insert(0, am_scripts_src)
else:
    print(f"Error: am-scripts repository not found at {am_scripts_src}")
    sys.exit(1)

from am_scripts.deploy import main_deploy_logic
from am_scripts.utils import setup_logger, load_config, parse_env, run_command

logger = setup_logger("AM_Market_Deploy")

def market_helm_overrides(app_vars, helm_args):
    """
    am-market specific Helm overrides.
    """
    # This would typically set values in your Helm chart
    # e.g., helm_args.append('--set parser.env.MONGO_URI="..."')
    pass

def main():
    config_path = os.path.join(repo_root, "deploy_config.json")
    env_path = os.path.join(repo_root, ".env.deploy")
    
    # Load Configs
    config = load_config(config_path)
    env_vars = parse_env(env_path) if os.path.exists(env_path) else {}
    
    # Load App Secrets
    app_env_path = os.path.join(repo_root, ".env")
    app_env_vars = parse_env(app_env_path) if os.path.exists(app_env_path) else {}

    # Defaults
    defaults = {
        "namespace_prefix": env_vars.get("NAMESPACE_PREFIX", config.get("namespace_prefix", "am")),
        "kind_cluster": env_vars.get("KIND_CLUSTER_NAME", "am-preprod"),
        "skip_load": env_vars.get("SKIP_KIND_LOAD", "false").lower() == "true",
        "run_docker": env_vars.get("RUN_DOCKER", "false").lower() == "true",
        "skip_build": env_vars.get("SKIP_BUILD", "false").lower() == "true"
    }

    # Arguments
    parser = argparse.ArgumentParser(description="Build and deploy AM Market services locally.")
    parser.add_argument("--skip-build", "-k", action="store_true", default=defaults["skip_build"], help="Skip Docker builds")
    parser.add_argument("--build-only", "-b", action="store_true", help="Only build Docker images, do not deploy")
    parser.add_argument("--deploy-only", "-d", action="store_true", help="Only deploy via Helm, skip builds")
    parser.add_argument("--services", "-s", type=str, help="Comma-separated list of services to process")
    parser.add_argument("--namespace-prefix", "-p", type=str, default=defaults["namespace_prefix"], help=f"Prefix for namespaces")
    parser.add_argument("--kind-cluster-name", type=str, default=defaults["kind_cluster"], help=f"Name of the KIND cluster")
    parser.add_argument("--skip-kind-load", action="store_true", default=defaults["skip_load"], help="Skip automatically loading images into KIND")
    parser.add_argument("--run-docker", action="store_true", default=defaults["run_docker"], help="Run in Docker instead of K8s")
    
    args = parser.parse_args()

    # Manual build orchestration to handle different contexts
    if not (args.skip_build or args.deploy_only):
        services_to_build = config.get("services", [])
        if args.services:
             names = [n.strip() for n in args.services.split(",")]
             services_to_build = [s for s in services_to_build if s["name"] in names]

        for svc in services_to_build:
            svc_name = svc["name"]
            svc_type = svc.get("type")
            svc_path = svc["path"]
            logger.info(f"--- Custom Build for {svc_name} ---")
            
            image_tag = f"local/{svc_name}:latest"
            
            if svc_name == "am-market-data":
                # Special context for market-data
                dockerfile = os.path.join("am-market-data", "Dockerfile")
                build_context = repo_root
            elif svc_name == "market-data-web":
                # market-data-web: context is ../ (am-repos), dockerfile is am-market/am_market_ui/live/Dockerfile
                dockerfile = os.path.join("am-market", "am_market_ui", "live", "Dockerfile")
                build_context = am_repos_root
            else:
                dockerfile = "Dockerfile"
                build_context = os.path.join(repo_root, svc_path)

            # Build Args for GitHub
            gh_user = os.environ.get("GITHUB_PACKAGES_USERNAME") or app_env_vars.get("GITHUB_PACKAGES_USERNAME", "")
            gh_token = os.environ.get("GITHUB_PACKAGES_TOKEN") or app_env_vars.get("GITHUB_PACKAGES_TOKEN", "")
            
            build_args = ""
            if gh_user and gh_token:
                build_args = f'--build-arg GITHUB_PACKAGES_USERNAME="{gh_user}" --build-arg GITHUB_PACKAGES_TOKEN="{gh_token}"'

            cmd = f'docker build -t "{image_tag}" -f "{os.path.join(build_context, dockerfile) if not os.path.isabs(dockerfile) else dockerfile}" "{build_context}" {build_args}'
            run_command(cmd)
            
        # Skip standard build in main_deploy_logic
        args.skip_build = True

    # Run Main Logic
    main_deploy_logic(config, env_vars, app_env_vars, repo_root, args, helm_overrides_callback=market_helm_overrides)

if __name__ == "__main__":
    main()
