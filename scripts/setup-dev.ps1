# One-time dev setup: repo .venv + am-cli + parser dependencies
$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$VenvPython = Join-Path $RepoRoot ".venv\Scripts\python.exe"

if (-not (Test-Path $VenvPython)) {
    Write-Host "Creating .venv at $RepoRoot ..."
    python -m venv (Join-Path $RepoRoot ".venv")
}

& $VenvPython -m pip install --upgrade pip
& $VenvPython -m pip install -e (Join-Path $RepoRoot "amctl")
& $VenvPython -m pip install -r (Join-Path $RepoRoot "am-market\am-parser\requirements.txt")

Write-Host "Done. From am-market run: npm run run:parser"
