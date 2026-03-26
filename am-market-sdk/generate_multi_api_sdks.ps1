# generate_multi_api_sdks.ps1
# 
# Usage:
#   .\generate_multi_api_sdks.ps1 [-MarketOnly] [-ParserOnly] [-SkipJava] [-SkipFlutter] [-SkipPython]
#
# Prerequisite:
#   npm install @openapitools/openapi-generator-cli -g
#   OR have npx available.

param(
    [switch]$MarketOnly,
    [switch]$ParserOnly,
    [switch]$SkipJava,
    [switch]$SkipFlutter,
    [switch]$SkipPython
)

# ============================================================
# SETUP & DEPENDENCIES
# ============================================================
$ScriptRoot = $PSScriptRoot
$SdkRoot = $ScriptRoot

# Import modular generator scripts
. "$ScriptRoot\scripts\generator\Core.ps1"
. "$ScriptRoot\scripts\generator\Java.ps1"
. "$ScriptRoot\scripts\generator\Flutter.ps1"
. "$ScriptRoot\scripts\generator\Python.ps1"

# OpenAPI Specs
$MarketSpec = Join-Path $ScriptRoot "market-data-openapi.json"
$ParserSpec = Join-Path $ScriptRoot "parser-openapi.json"

# Check specs
$hasMarket = Test-Path $MarketSpec
$hasParser = Test-Path $ParserSpec

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Multi-API SDK Generator (Modular Mode) " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[CHECK] Available OpenAPI Specifications:"
Write-Host "  Parser API:      $(if($hasParser){'[OK]'}else{'[MISSING]'})" -ForegroundColor $(if($hasParser){'Green'}else{'Red'})
Write-Host "  Market Data API: $(if($hasMarket){'[OK]'}else{'[MISSING]'})" -ForegroundColor $(if($hasMarket){'Green'}else{'Red'})
Write-Host ""

if (-not $hasMarket -and -not $hasParser) {
    Write-Host "[ERROR] No OpenAPI specifications found. Exiting." -ForegroundColor Red
    exit 1
}

# ============================================================
# GENERATION LOGIC
# ============================================================

# --- STEP 1: Java SDKs ---
if (-not $SkipJava) {
    Write-Host "[STEP 1] Generating Java SDKs..." -ForegroundColor Cyan
    
    if ($hasMarket -and -not $ParserOnly) {
        Invoke-JavaGen `
            -Spec $MarketSpec `
            -OutDir (Join-Path $SdkRoot "java-market-sdk") `
            -ArtifactId "am-market-client" `
            -Description "AM Market Data API Java Client" `
            -ApiPackage "com.am.portfolio.client.market.api" `
            -ModelPackage "com.am.portfolio.client.market.model" `
            -InvokerPackage "com.am.portfolio.client.market.invoker" `
            -Label "java-market"
    }

    if ($hasParser -and -not $MarketOnly) {
        Invoke-JavaGen `
            -Spec $ParserSpec `
            -OutDir (Join-Path $SdkRoot "java-parser-sdk") `
            -ArtifactId "am-parser-client" `
            -Description "AM Parser API Java Client" `
            -ApiPackage "com.am.portfolio.client.parser.api" `
            -ModelPackage "com.am.portfolio.client.parser.model" `
            -InvokerPackage "com.am.portfolio.client.parser.invoker" `
            -Label "java-parser"
    }
    Write-Host ""
}

# --- STEP 2: Flutter SDKs ---
if (-not $SkipFlutter) {
    Write-Host "[STEP 2] Generating Flutter SDKs..." -ForegroundColor Cyan

    if ($hasMarket -and -not $ParserOnly) {
        Invoke-FlutterGen `
            -Spec $MarketSpec `
            -OutDir (Join-Path $SdkRoot "flutter-market-sdk") `
            -PubName "am_market_client" `
            -PubDesc "AM Market Data API Flutter Client" `
            -Label "flutter-market"
    }

    if ($hasParser -and -not $MarketOnly) {
        Invoke-FlutterGen `
            -Spec $ParserSpec `
            -OutDir (Join-Path $SdkRoot "flutter-parser-sdk") `
            -PubName "am_parser_client" `
            -PubDesc "AM Parser API Flutter Client" `
            -Label "flutter-parser"
    }
    Write-Host ""
}

# --- STEP 3: Python SDKs ---
if (-not $SkipPython) {
    Write-Host "[STEP 3] Generating Python SDKs..." -ForegroundColor Cyan

    if ($hasMarket -and -not $ParserOnly) {
        Invoke-PythonGen `
            -Spec $MarketSpec `
            -OutDir (Join-Path $SdkRoot "python-market-sdk") `
            -PackageName "am_market_client" `
            -ProjectName "am-market-client" `
            -PyDesc "AM Market Data API Python Client" `
            -Label "python-market"
    }

    if ($hasParser -and -not $MarketOnly) {
        Invoke-PythonGen `
            -Spec $ParserSpec `
            -OutDir (Join-Path $SdkRoot "python-parser-sdk") `
            -PackageName "am_parser_client" `
            -ProjectName "am-parser-client" `
            -PyDesc "AM Parser API Python Client" `
            -Label "python-parser"
    }
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " SDK Generation Complete! " -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host ""
Write-Host "Packages generated (in am-market-sdk/):"
Write-Host "  [Java]    java-market-sdk/"
Write-Host "  [Java]    java-parser-sdk/"
Write-Host "  [Flutter] flutter-market-sdk/"
Write-Host "  [Flutter] flutter-parser-sdk/"
Write-Host "  [Python]  python-market-sdk/"
Write-Host "  [Python]  python-parser-sdk/"
Write-Host ""
