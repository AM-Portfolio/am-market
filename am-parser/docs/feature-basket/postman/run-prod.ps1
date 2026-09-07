# One-click Newman run against PROD (Smart Baskets Discover accuracy contract)
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$collection = Join-Path $here "AM-Smart-Baskets-Discover-PROD.postman_collection.json"
$envFile = Join-Path $here "AM-PROD.postman_environment.json"
$report = Join-Path $here "newman-prod-report.json"

Write-Host "Running Discover PROD collection..." -ForegroundColor Cyan
npx --yes newman run $collection `
  -e $envFile `
  --reporters cli,json `
  --reporter-json-export $report `
  --timeout-request 120000

$code = $LASTEXITCODE
if ($code -eq 0) {
  Write-Host "ALL TESTS PASSED" -ForegroundColor Green
} else {
  Write-Host "COLLECTION FAILED (exit $code) — see $report" -ForegroundColor Red
}
exit $code
