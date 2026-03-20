# Simplified Helm Configuration for `am-parser`

Because we have migrated to a **Versioned Universal Chart**, this directory no longer contains complex Kubernetes templates (like `deployment.yaml` or `Chart.yaml`). 

## Directory Structure
To keep things as simple as possible, all configurations are entirely flat. There are no subfolders.

- `values.yaml` - The base configuration (ports, image repository name, etc).
- `values.dev.yaml` - Overrides for local/development environments.
- `values.preprod.yaml` - Overrides applied only to Pre-Production.
- `values.prod.yaml` - Overrides applied only to Production.

## How does it work?
When a deployment is triggered via GitHub Actions, the CI pipeline checks out the central `AM-Portfolio/am-cicd` repository. It grabs the Universal Helm Chart from there, and merges your flat `values.yaml` files into it. 

You no longer explicitly construct the Kubernetes `Service` and `Deployment` files yourself. You just supply the variable data!

## How to Deploy Locally
Deploying locally is completely path independent.

```bash
cd am-parser/helm
./deploy-local.sh
```
This script acts as a bridge. It will intelligently deploy using the Universal Chart regardless of where this directory is located:
1. **Fast Local Path**: If `am-cicd` is checked out next to `am-market`, it uses the local files instantly.
2. **Git Fallback**: If you only cloned `am-market` and nothing else, the script silently shallow-clones the Universal Chart from GitHub directly into a temporary folder, runs the deployment using your `values.yaml`, and instantly cleans up. You do not need to manage complex repository structures!

## How to Deploy Locally
Because `am-cicd` and `am-market` are both checked out in your workspace (`/workspaces/am-repos/`), you can easily deploy locally using the provided bash script.

```bash
cd am-parser/helm
./deploy-local.sh
```
This script acts as a bridge, running `helm upgrade` by grabbing the Universal Chart from its physical path (`../../../am-cicd/helm/universal-chart`) and applying the local `values.yaml`!
