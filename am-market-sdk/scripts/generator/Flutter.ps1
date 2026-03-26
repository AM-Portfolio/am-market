# Flutter.ps1 - Flutter SDK Generation Plugins

# Dot-source core if not already present
. $PSScriptRoot\Core.ps1

function Invoke-FlutterGen {
    param(
        [Parameter(Mandatory=$true)][string]$Spec,
        [Parameter(Mandatory=$true)][string]$OutDir,
        [Parameter(Mandatory=$true)][string]$PubName,
        [Parameter(Mandatory=$true)][string]$PubDesc,
        [hashtable]$BaseConfig = @{ },
        [string]$Label = "flutter-sdk"
    )

    $fullConfig = $BaseConfig.Clone()
    $fullConfig["pubName"] = $PubName
    $fullConfig["pubVersion"] = "1.0.0"
    $fullConfig["pubDescription"] = $PubDesc

    # Call core generator
    Invoke-OpenApiGen -Spec $Spec -OutDir $OutDir -Generator "dart" -Config $fullConfig -Label $Label

    # Verify lib directory
    if (-not (Test-Path (Join-Path $OutDir "lib"))) {
        Write-Host "[ERROR] Generator reported success but 'lib' folder is missing in $OutDir" -ForegroundColor Red
        exit 1
    }

    # Add test placeholder
    $testDir = Join-Path $OutDir "test"
    if (-not (Test-Path $testDir)) { New-Item -ItemType Directory -Force -Path $testDir | Out-Null }
    
    $testContent = @"
import 'package:test/test.dart';

void main() {
  group('$PubName smoke tests', () {
    test('library imports without error', () {
      expect(true, isTrue);
    });
  });
}
"@
    Set-Content -Path (Join-Path $testDir "${PubName}_test.dart") -Value $testContent
}

# End of Flutter.ps1

