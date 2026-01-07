#!/bin/bash

# Define paths
COMMON_UI_SRC="../../../am_common_ui"
VENDOR_DIR="vendor/am_common_ui"

# Check if Common UI exists
if [ -d "$COMMON_UI_SRC" ]; then
    echo "Found am_common_ui at $COMMON_UI_SRC"
    
    # Create vendor directory
    mkdir -p vendor
    
    # Remove existing vendor/am_common_ui to ensure clean copy
    rm -rf "$VENDOR_DIR"
    
    # Copy Common UI
    echo "Copying to $VENDOR_DIR..."
    cp -R "$COMMON_UI_SRC" "$VENDOR_DIR"
    
    # Remove .git from vendor copy to avoid submodule issues
    rm -rf "$VENDOR_DIR/.git"
    
    echo "Done! am_common_ui is now vendored."
    echo "IMPORTANT: Update pubspec.yaml to point to './vendor/am_common_ui' before creating the final commit or building Docker."
else
    echo "Error: am_common_ui not found at $COMMON_UI_SRC"
    exit 1
fi
