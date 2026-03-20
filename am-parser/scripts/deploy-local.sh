#!/bin/bash

APP_NAME="am-parser"
echo "Deploying $APP_NAME to local Kubernetes..."

# Change context to the helm directory where values files live
cd "$(dirname "$0")/../helm" || exit 1

# Try to find the universal chart natively if the developer checked it out side-by-side
CHART_PATH="../../../../am-cicd/helm/universal-chart"
TMP_DIR=".tmp_cicd"

if [ -d "$CHART_PATH" ]; then
    echo "✅ Found Universal Chart locally at $CHART_PATH"
else
    echo "⚠️ Universal Chart not found locally in the workspace."
    echo "⬇️  Cloning it directly from GitHub to avoid breaking your workflow..."
    
    rm -rf $TMP_DIR
    git clone --quiet --depth 1 https://github.com/AM-Portfolio/am-cicd.git $TMP_DIR
    
    CHART_PATH="./$TMP_DIR/helm/universal-chart"
fi

helm upgrade --install $APP_NAME $CHART_PATH \
    -f values.yaml \
    -f values.dev.yaml

# Cleanup temporary git clone so it doesn't clutter the developer's repository
rm -rf $TMP_DIR

echo "✅ Local Deployment requested successfully for $APP_NAME!"
