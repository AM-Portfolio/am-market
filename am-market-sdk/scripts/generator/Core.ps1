# Core.ps1 - Common SDK Generation Utilities

function Assert-LastExit {
    param([string]$Message)
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] $Message (Exit Code: $LASTEXITCODE)" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

function Invoke-OpenApiGen {
    param(
        [Parameter(Mandatory=$true)][string]$Spec,
        [Parameter(Mandatory=$true)][string]$OutDir,
        [Parameter(Mandatory=$true)][string]$Generator,
        [Parameter(Mandatory=$true)][hashtable]$Config,
        [Parameter(Mandatory=$true)][string]$Label,
        [string]$SdkRoot = $PSScriptRoot,
        [string[]]$AdditionalArgs = @()
    )

    # 1. Clean and Prepare Output Directory
    if (Test-Path $OutDir) {
        # Try to remove the whole directory, ignore errors if some files are locked
        Remove-Item -Recurse -Force $OutDir -ErrorAction SilentlyContinue
        
        # If it still exists (due to locks), at least try to clean what we can
        if (Test-Path $OutDir) {
            Get-ChildItem -Path $OutDir -Recurse | Remove-Item -Force -Recurse -ErrorAction SilentlyContinue
        }
    }
    if (-not (Test-Path $OutDir)) {
        New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
    }

    Write-Host "  [$Label] Generating SDK at $OutDir..." -ForegroundColor Yellow
    
    # 2. Write Temporary JSON Config
    $configPath = Join-Path $SdkRoot "gen-config-$Label.json"
    $Config | ConvertTo-Json | Set-Content -Path $configPath -Encoding UTF8

    # Verify config file exists and is not empty
    if (-not (Test-Path $configPath)) {
        Write-Host "[ERROR] Failed to create config file at $configPath" -ForegroundColor Red
        exit 1
    }

    # 3. Run Generator
    $genArgs = @(
        "generate",
        "-i", $Spec,
        "-g", $Generator,
        "-o", $OutDir,
        "-c", $configPath,
        "--skip-validate-spec"
    )

    if ($AdditionalArgs.Count -gt 0) {
        $genArgs += $AdditionalArgs
    }

    Write-Host "    [DEBUG] Running: npx.cmd --yes @openapitools/openapi-generator-cli $($genArgs -join ' ')" -ForegroundColor Gray
    
    # Use absolute path to npx.cmd
    & "C:\Program Files\nodejs\npx.cmd" --yes @openapitools/openapi-generator-cli @genArgs
    
    if ($LASTEXITCODE -ne 0) {
        Assert-LastExit "$Label Generation Failed"
    }

    # 4. Write CI Trigger
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    "Update: $timestamp ($Label)" | Set-Content -Path (Join-Path $OutDir "ci-trigger.txt")
    
    # 5. Cleanup Config
    if (Test-Path $configPath) { Remove-Item -Force $configPath }

    Write-Host "    [OK] $Label" -ForegroundColor Green
}

# Export functions
# End of Core.ps1

