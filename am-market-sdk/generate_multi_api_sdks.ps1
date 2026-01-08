# Multi-API SDK Generation Script
# Generates Java and Flutter SDKs for both Parser API and Market Data API

param(
    [switch]$ParserOnly,
    [switch]$SkipJava,
    [switch]$SkipFlutter
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Multi-API SDK Generator" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Paths
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SdkRoot = $ScriptDir
$ParserSpec = Join-Path $SdkRoot "parser-openapi.json"
$MarketSpec = Join-Path $SdkRoot "market-data-openapi.json"

# Check specs
$hasParser = Test-Path $ParserSpec
$hasMarket = Test-Path $MarketSpec

Write-Host "[CHECK] Available OpenAPI Specifications:" -ForegroundColor Yellow
Write-Host "  Parser API:      $(if($hasParser){'[OK]'}else{'[MISSING]'})" -ForegroundColor $(if ($hasParser) { 'Green' }else { 'Red' })
Write-Host "  Market Data API: $(if($hasMarket){'[OK]'}else{'[MISSING]'})" -ForegroundColor $(if ($hasMarket) { 'Green' }else { 'Red' })
Write-Host ""

if (-not $hasParser -and -not $hasMarket) {
    Write-Host "[ERROR] No OpenAPI specs found. Run extraction scripts first:" -ForegroundColor Red
    Write-Host "  python scripts/extract_parser_openapi.py" -ForegroundColor Yellow
    Write-Host "  powershell scripts/extract_market_openapi.ps1" -ForegroundColor Yellow
    exit 1
}

# Generator configuration
$generatorVersion = "7.1.0"
$javaOutputBase = Join-Path $SdkRoot "java-sdk"
$flutterOutputBase = Join-Path $SdkRoot "flutter-sdk"

Write-Host "[STEP 1] Installing OpenAPI Generator CLI..." -ForegroundColor Yellow
npm install -g @openapitools/openapi-generator-cli | Out-Null
Write-Host "  [OK] Generator ready (v$generatorVersion)" -ForegroundColor Green
Write-Host ""

# ============================================
# JAVA SDK GENERATION
# ============================================
if (-not $SkipJava) {
    # -------------------------------------------------------------------------
    # 2. GENERATE JAVA CLIENTS (UNIFIED ARTIFACT)
    # -------------------------------------------------------------------------
    Write-Host "`n[STEP 2] Generating Unified Java SDK..." -ForegroundColor Cyan

    $tempMarketDir = Join-Path $sdkRoot "temp-market"
    $tempParserDir = Join-Path $sdkRoot "temp-parser"
    $unifiedJavaDir = Join-Path $sdkRoot "java-sdk"

    # Clean previous
    if (Test-Path $tempMarketDir) { Remove-Item -Recurse -Force $tempMarketDir }
    if (Test-Path $tempParserDir) { Remove-Item -Recurse -Force $tempParserDir }
    if (Test-Path $unifiedJavaDir) { Remove-Item -Recurse -Force $unifiedJavaDir }

    # 2.1 Generate Parser Client (Temp)
    if (Test-Path $ParserSpec) {
        Write-Host "  [2.1] Generating Parser Client Source..." -ForegroundColor Yellow
        npx @openapitools/openapi-generator-cli generate `
            -i $ParserSpec `
            -g java `
            -o $tempParserDir `
            --additional-properties="groupId=com.am.portfolio,artifactId=unified-client,apiPackage=com.am.portfolio.client.parser.api,modelPackage=com.am.portfolio.client.parser.model,invokerPackage=com.am.portfolio.client.parser.invoker,library=native"
    }

    # 2.2 Generate Market Client (Temp)
    if (Test-Path $MarketSpec) {
        Write-Host "  [2.2] Generating Market Client Source..." -ForegroundColor Yellow
        npx @openapitools/openapi-generator-cli generate `
            -i $MarketSpec `
            -g java `
            -o $tempMarketDir `
            --additional-properties="groupId=com.am.portfolio,artifactId=unified-client,apiPackage=com.am.portfolio.client.market.api,modelPackage=com.am.portfolio.client.market.model,invokerPackage=com.am.portfolio.client.market.invoker,library=native"
    }

    # 2.3 Merge into Unified SDK
    Write-Host "  [2.3] Merging into Unified SDK Project..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path $unifiedJavaDir | Out-Null
    
    # Init strict structure
    $srcMainJava = Join-Path $unifiedJavaDir "src\main\java"
    New-Item -ItemType Directory -Force -Path $srcMainJava | Out-Null

    # Copy Market Source
    if (Test-Path "$tempMarketDir\src\main\java") {
        Copy-Item -Recurse -Force "$tempMarketDir\src\main\java\*" $srcMainJava
        # Copy POM from Market as base
        Copy-Item -Force "$tempMarketDir\pom.xml" "$unifiedJavaDir\pom.xml"
        # Copy other metadata
        Copy-Item -Force "$tempMarketDir\.gitignore" "$unifiedJavaDir\.gitignore"
        Copy-Item -Force "$tempMarketDir\README.md" "$unifiedJavaDir\README_MARKET.md"
    }

    # Copy Parser Source (Merge)
    if (Test-Path "$tempParserDir\src\main\java") {
        Copy-Item -Recurse -Force "$tempParserDir\src\main\java\*" $srcMainJava
        Copy-Item -Force "$tempParserDir\README.md" "$unifiedJavaDir\README_PARSER.md"
    }

    # Cleanup Temps
    Remove-Item -Recurse -Force $tempMarketDir
    Remove-Item -Recurse -Force $tempParserDir

    Write-Host "    [OK] Unified Java SDK Generated at: $unifiedJavaDir" -ForegroundColor Green
    
    Write-Host ""
}

# ============================================
# FLUTTER SDK GENERATION
# ============================================
if (-not $SkipFlutter) {
    # -------------------------------------------------------------------------
    # 3. GENERATE FLUTTER SDK (SINGLE UNIFIED PACKAGE)
    # -------------------------------------------------------------------------
    Write-Host "`n[STEP 3] Generating Unified Flutter SDK..." -ForegroundColor Cyan

    $flutterSdkRoot = Join-Path $sdkRoot "flutter-sdk"
    $unifiedLibDir = Join-Path $flutterSdkRoot "lib"
    
    # Define Temp Paths
    $tempMarketDir = Join-Path $sdkRoot "temp-flutter-market"
    $tempParserDir = Join-Path $sdkRoot "temp-flutter-parser"

    # Clean previous
    if (Test-Path $tempMarketDir) { Remove-Item -Recurse -Force $tempMarketDir }
    if (Test-Path $tempParserDir) { Remove-Item -Recurse -Force $tempParserDir }
    if (Test-Path $flutterSdkRoot) { Remove-Item -Recurse -Force $flutterSdkRoot }

    # 3.1 Generate Market Client (Temp)
    if (Test-Path $MarketSpec) {
        Write-Host "  [3.1] Generating Market Client Source..." -ForegroundColor Yellow
        # We use the target package name 'am_market_sdk' so basic imports start right
        npx @openapitools/openapi-generator-cli generate `
            -i $MarketSpec `
            -g dart `
            -o $tempMarketDir `
            --additional-properties="pubName=am_market_sdk,pubVersion=1.0.0"
    }

    # 3.2 Generate Parser Client (Temp)
    if (Test-Path $ParserSpec) {
        Write-Host "  [3.2] Generating Parser Client Source..." -ForegroundColor Yellow
        npx @openapitools/openapi-generator-cli generate `
            -i $ParserSpec `
            -g dart `
            -o $tempParserDir `
            --additional-properties="pubName=am_market_sdk,pubVersion=1.0.0"
    }

    # 3.3 Merge and Rewrite
    Write-Host "  [3.3] Merging and Rewriting Imports..." -ForegroundColor Yellow
    
    # Create Structure
    New-Item -ItemType Directory -Force -Path $unifiedLibDir | Out-Null
    
    # Function to Move and Rewrite
    function Process-Dart-Sdk($sourceDir, $subPackageName, $libraryName) {
        $targetSubDir = Join-Path $unifiedLibDir $subPackageName
        
        if (Test-Path "$sourceDir\lib") {
            # Ensure target exists
            New-Item -ItemType Directory -Force -Path $targetSubDir | Out-Null
            
            # Copy lib contents to src/subPackage
            Copy-Item -Recurse -Force "$sourceDir\lib\*" $targetSubDir
            
            # Rewrite Imports and Library Declarations
            $dartFiles = Get-ChildItem -Path $targetSubDir -Recurse -Filter "*.dart"
            foreach ($file in $dartFiles) {
                $content = Get-Content -Path $file.FullName -Raw
                
                # 1. Rewrite package imports: package:am_market_sdk/ -> package:am_market_sdk/$subPackageName/
                $content = $content -replace "package:am_market_sdk/", "package:am_market_sdk/$subPackageName/"
                
                # 2. Rewrite library name: library openapi.api; -> library $libraryName;
                $content = $content -replace "library openapi.api;", "library $libraryName;"
                
                # 3. Rewrite part of: part of openapi.api; -> part of $libraryName;
                $content = $content -replace "part of openapi.api;", "part of $libraryName;"
                
                Set-Content -Path $file.FullName -Value $content -NoNewline
            }
        }
    }

    # Process Market
    if (Test-Path $tempMarketDir) {
        Process-Dart-Sdk -sourceDir $tempMarketDir -subPackageName "market" -libraryName "market.api"
    }

    # Process Parser
    if (Test-Path $tempParserDir) {
        Process-Dart-Sdk -sourceDir $tempParserDir -subPackageName "parser" -libraryName "parser.api"
    }

    # 3.4 Create Root Files
    
    # pubspec.yaml
    $pubspecContent = @"
name: am_market_sdk
description: Unified Market Data SDK (Single Package)
version: 1.0.0
environment:
  sdk: '>=2.18.0 <4.0.0'

dependencies:
  http: '>=0.13.0 <2.0.0'
  intl: '^0.20.0'
  meta: '^1.7.0'
  collection: '^1.16.0'

dev_dependencies:
  build_runner: ^2.1.4
  test: ^1.21.0
"@
    Set-Content -Path (Join-Path $flutterSdkRoot "pubspec.yaml") -Value $pubspecContent

    # library export file
    $exportContent = @"
library am_market_sdk;

export 'market/api.dart';
export 'parser/api.dart';
"@
    Set-Content -Path (Join-Path $unifiedLibDir "am_market_sdk.dart") -Value $exportContent

    # Cleanup
    if (Test-Path $tempMarketDir) { Remove-Item -Recurse -Force $tempMarketDir }
    if (Test-Path $tempParserDir) { Remove-Item -Recurse -Force $tempParserDir }

    Write-Host "    [OK] Unified Flutter SDK Generated at: $flutterSdkRoot" -ForegroundColor Green

    
    Write-Host ""
}

# ============================================
# SUMMARY
# ============================================
Write-Host "========================================"  -ForegroundColor Cyan
Write-Host " SDK Generation Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Generated SDKs:" -ForegroundColor Yellow
if ($hasParser) {
    if (-not $SkipJava) { Write-Host "  [Java]    Parser API    -> $javaOutputBase\parser-client" -ForegroundColor Gray }
    if (-not $SkipFlutter) { Write-Host "  [Flutter] Parser API    -> $flutterOutputBase\parser_client" -ForegroundColor Gray }
}
if ($hasMarket -and -not $ParserOnly) {
    if (-not $SkipJava) { Write-Host "  [Java]    Market Data   -> $javaOutputBase\market-client" -ForegroundColor Gray }
    if (-not $SkipFlutter) { Write-Host "  [Flutter] Market Data   -> $flutterOutputBase\market_data_client" -ForegroundColor Gray }
}
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Import Java clients as Maven dependencies" -ForegroundColor Gray
Write-Host "  2. Add Flutter clients to pubspec.yaml" -ForegroundColor Gray
Write-Host "  3. Build and test integrations" -ForegroundColor Gray
Write-Host ""
