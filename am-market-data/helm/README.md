# Simplified Helm Configuration for `am-market-data`

Because we have migrated to a **Versioned Universal Chart**, this directory no longer contains complex Kubernetes templates (like `deployment.yaml` or `Chart.yaml`). 

## Directory Structure
To keep things as simple as possible, all configurations are entirely flat. There are no subfolders.

- `values.yaml` - The base configuration (ports, image repository name, etc).
- `values.preprod.yaml` - Changes applied only when deploying to Pre-Production (like replica counts).
- `values.prod.yaml` - Changes applied only when deploying to Production.

## How does it work?
When a deployment is triggered via GitHub Actions, the CI pipeline checks out the central `AM-Portfolio/am-cicd` repository. It grabs the Universal Helm Chart from there, and merges your flat `values.yaml` files into it. 

You no longer explicitly build the Deployment—you just supply the values!
