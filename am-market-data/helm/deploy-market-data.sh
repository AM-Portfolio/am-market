#!/bin/bash

# Market Data Service Deployment Script for Kind Cluster
# This script deploys the AM Market Data service with Vault integration

set -e

echo "🚀 Deploying Market Data Service to Kind Cluster..."
echo "=================================================="

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_step() {
    echo -e "${BLUE}📦 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Configuration
NAMESPACE="am-np-west"
RELEASE_NAME="market-data"
CHART_PATH="./market-data"
VALUES_FILE="./market-data/values/preprod.yaml"

# Check if namespace exists
if ! kubectl get namespace $NAMESPACE &> /dev/null; then
    print_step "Creating namespace: $NAMESPACE"
    kubectl create namespace $NAMESPACE
fi

# PHASE 1: Create Vault Service Account and RBAC
print_step "Phase 1: Creating Vault Service Account and RBAC"

cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ServiceAccount
metadata:
  name: vault-auth
  namespace: $NAMESPACE
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: vault-auth-delegator-$NAMESPACE
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: system:auth-delegator
subjects:
- kind: ServiceAccount
  name: vault-auth
  namespace: $NAMESPACE
EOF

print_success "Vault Service Account created"

# PHASE 2: Create GitHub Container Registry Secret (if needed)
print_step "Phase 2: Checking for GitHub Container Registry Secret"

if kubectl get secret ghcr-secret -n $NAMESPACE &> /dev/null; then
    print_success "GHCR secret already exists"
else
    print_warning "GHCR secret not found"
    echo ""
    echo "To pull images from GitHub Container Registry, you need to create a secret."
    echo "Run the following command with your GitHub credentials:"
    echo ""
    echo "kubectl create secret docker-registry ghcr-secret \\"
    echo "  --docker-server=ghcr.io \\"
    echo "  --docker-username=YOUR_GITHUB_USERNAME \\"
    echo "  --docker-password=YOUR_GITHUB_TOKEN \\"
    echo "  --namespace=$NAMESPACE"
    echo ""
    read -p "Press Enter to continue (skipping GHCR secret creation)..." 
fi

# PHASE 3: Verify Vault is ready
print_step "Phase 3: Verifying Vault is accessible"

if kubectl get pods -n vault -l app.kubernetes.io/name=vault | grep -q Running; then
    print_success "Vault is running"
else
    print_error "Vault is not running. Please deploy infrastructure first:"
    echo "  bash ../../am-infra/k8s/deploy-all-infra.sh"
    exit 1
fi

# PHASE 4: Deploy Market Data Service using Helm
print_step "Phase 4: Deploying Market Data Service with Helm"

# Check if helm is installed
if ! command -v helm &> /dev/null; then
    print_error "Helm is not installed. Please install Helm first."
    exit 1
fi

# Install or upgrade the helm chart
helm upgrade --install $RELEASE_NAME $CHART_PATH \
    --namespace $NAMESPACE \
    --values $VALUES_FILE \
    --create-namespace \
    --wait --timeout 10m

print_success "Market Data Service deployed"

# PHASE 5: Wait for deployment to be ready
print_step "Phase 5: Waiting for Market Data pods to be ready..."
kubectl wait --for=condition=ready pod \
    -l app.kubernetes.io/name=am-market-data \
    -n $NAMESPACE \
    --timeout=300s || {
        print_warning "Pods not ready within timeout. Checking status..."
        kubectl get pods -n $NAMESPACE
        kubectl describe pods -l app.kubernetes.io/name=am-market-data -n $NAMESPACE
    }

# PHASE 6: Verify deployment
print_step "Phase 6: Verifying deployment"

echo ""
echo "Pod Status:"
kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=am-market-data

echo ""
echo "Service Status:"
kubectl get svc -n $NAMESPACE -l app.kubernetes.io/name=am-market-data

echo ""
echo "=================================================="
print_success "Market Data Service Deployment Complete!"
echo "=================================================="
echo ""
echo "📊 Deployment Summary:"
echo "  ✅ Namespace: $NAMESPACE"
echo "  ✅ Release: $RELEASE_NAME"
echo "  ✅ Vault Integration: Enabled (service account: vault-auth)"
echo "  ✅ Monitoring: Prometheus metrics exposed"
echo ""
echo "🌐 Access Service:"
echo "  Via Port-Forward:"
echo "    kubectl port-forward svc/am-market-data 8080:8080 -n $NAMESPACE"
echo "    Then access: http://localhost:8080"
echo ""
echo "  Health Check:"
echo "    kubectl port-forward svc/am-market-data 8080:8080 -n $NAMESPACE &"
echo "    curl http://localhost:8080/actuator/health"
echo ""
echo "📝 View Logs:"
echo "  kubectl logs -f deployment/am-market-data -n $NAMESPACE"
echo ""
echo "📊 View in Grafana:"
echo "  kubectl port-forward svc/grafana 3000:3000 -n monitoring"
echo "  Open http://localhost:3000 and explore Loki logs"
echo "  Query: {namespace=\"$NAMESPACE\"}"
echo ""
echo "🔍 Verify Vault Secrets Injection:"
echo "  POD=\$(kubectl get pod -n $NAMESPACE -l app.kubernetes.io/name=am-market-data -o jsonpath='{.items[0].metadata.name}')"
echo "  kubectl exec -n $NAMESPACE \$POD -it -- ls /vault/secrets"
echo "  kubectl exec -n $NAMESPACE \$POD -it -- cat /vault/secrets/mongodb"
echo ""
