# Market Data OpenAPI Extraction Script
# Starts the service temporarily, extracts the spec, and stops it

param(
    [int]$Port = 8020,
    [string]$OutputFile = "market-data-openapi.json"
)

$ErrorActionPreference = "Stop"

Write-Host "[START] Market Data OpenAPI Extraction" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan

# Paths
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SdkRoot = Split-Path -Parent $ScriptDir
$MarketDataApp = Join-Path (Split-Path -Parent $SdkRoot) "am-market-data\market-data-app"
$OutputPath = Join-Path $SdkRoot $OutputFile

Write-Host "[INFO] Market Data App: $MarketDataApp" -ForegroundColor Gray
Write-Host "[INFO] Output Path: $OutputPath" -ForegroundColor Gray

# Check if market-data-app exists
if (-not (Test-Path $MarketDataApp)) {
    Write-Host "[ERROR] Market Data App not found at: $MarketDataApp" -ForegroundColor Red
    exit 1
}

# Change to market-data-app directory
Push-Location $MarketDataApp

$process = $null

try {
    Write-Host ""
    Write-Host "[STEP 1] Starting Market Data Service..." -ForegroundColor Yellow
    
    # Start the Spring Boot application in background
    $process = Start-Process -FilePath "mvn" `
        -ArgumentList "spring-boot:run", "-Dspring-boot.run.jvmArguments=-Dserver.port=$Port" `
        -PassThru `
        -NoNewWindow `
        -RedirectStandardOutput "$env:TEMP\market-data-startup.log" `
        -RedirectStandardError "$env:TEMP\market-data-startup-error.log"
    
    Write-Host "   Process ID: $($process.Id)" -ForegroundColor Gray
    
    # Wait for service to be ready
    Write-Host "[STEP 2] Waiting for service startup..." -ForegroundColor Yellow
    $maxAttempts = 60
    $attempt = 0
    $ready = $false
    
    while ($attempt -lt $maxAttempts -and -not $ready) {
        Start-Sleep -Seconds 2
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$Port/actuator/health" `
                -TimeoutSec 2 `
                -ErrorAction SilentlyContinue
            
            if ($response.StatusCode -eq 200) {
                $ready = $true
                Write-Host "   [OK] Service is ready!" -ForegroundColor Green
            }
        }
        catch {
            $attempt++
            if ($attempt % 5 -eq 0) {
                Write-Host "   ... still waiting ($attempt/$maxAttempts)" -ForegroundColor Gray
            }
        }
    }
    
    if (-not $ready) {
        Write-Host "   [ERROR] Service failed to start within timeout" -ForegroundColor Red
        Write-Host "   [LOG] Check logs at: $env:TEMP\market-data-startup.log" -ForegroundColor Yellow
        throw "Service startup timeout"
    }
    
    # Extract OpenAPI spec
    Write-Host "[STEP 3] Extracting OpenAPI specification..." -ForegroundColor Yellow
    
    $openApiUrl = "http://localhost:$Port/v3/api-docs"
    Write-Host "   URL: $openApiUrl" -ForegroundColor Gray
    
    $spec = Invoke-RestMethod -Uri $openApiUrl -TimeoutSec 30
    
    # Save to file
    $spec | ConvertTo-Json -Depth 100 | Out-File -FilePath $OutputPath -Encoding UTF8
    
    Write-Host "   [OK] Spec extracted successfully!" -ForegroundColor Green
    Write-Host "   [INFO] Title: $($spec.info.title)" -ForegroundColor Gray
    Write-Host "   [INFO] Version: $($spec.info.version)" -ForegroundColor Gray
    Write-Host "   [INFO] Paths: $($spec.paths.Count)" -ForegroundColor Gray
    Write-Host "   [FILE] Saved to: $OutputPath" -ForegroundColor Gray
    
}
catch {
    Write-Host "[ERROR] Failed to extract OpenAPI spec: $_" -ForegroundColor Red
    throw
}
finally {
    # Stop the service
    Write-Host "[STEP 4] Stopping service..." -ForegroundColor Yellow
    
    if ($process -and -not $process.HasExited) {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        Write-Host "   [OK] Service stopped (PID: $($process.Id))" -ForegroundColor Green
    }
    
    Pop-Location
}

Write-Host ""
Write-Host "[SUCCESS] OpenAPI extraction complete!" -ForegroundColor Green
Write-Host "[OUTPUT] $OutputPath" -ForegroundColor Cyan
