# Core.ps1 - Common SDK Generation Utilities

function Check-LastExit {
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
        [string]$SdkRoot = $PSScriptRoot
    )

    # 1. Clean and Prepare Output Directory
    if (Test-Path $OutDir) { Remove-Item -Recurse -Force $OutDir }
    New-Item -ItemType Directory -Path $OutDir -Force | Out-Null

    Write-Host "  [$Label] Generating SDK at $OutDir..." -ForegroundColor Yellow
    
    # 2. Write Temporary JSON Config
    $configPath = Join-Path $SdkRoot "gen-config-$Label.json"
    $Config | ConvertTo-Json | Set-Content -Path $configPath

    # 3. Run Generator
    $genArgs = @(
        "generate",
        "-i", $Spec,
        "-g", $Generator,
        "-o", $OutDir,
        "-c", $configPath,
        "--skip-validate-spec"
    )
    
    # Use npx with splatting
    npx --yes @openapitools/openapi-generator-cli @genArgs
    
    Check-LastExit "$Label Generation Failed"

    # 4. Write CI Trigger
    Set-Content -Path (Join-Path $OutDir "ci-trigger.txt") -Value $Label
    
    # 5. Cleanup Config
    if (Test-Path $configPath) { Remove-Item -Force $configPath }

    Write-Host "    [OK] $Label" -ForegroundColor Green
}

# Export functions
# End of Core.ps1

