# Python.ps1 - Python SDK Generation Plugins

# Dot-source core if not already present
. $PSScriptRoot\Core.ps1

function Invoke-PythonGen {
    param(
        [Parameter(Mandatory=$true)][string]$Spec,
        [Parameter(Mandatory=$true)][string]$OutDir,
        [Parameter(Mandatory=$true)][string]$PackageName,
        [Parameter(Mandatory=$true)][string]$ProjectName,
        [Parameter(Mandatory=$true)][string]$PyDesc,
        [hashtable]$BaseConfig = @{ },
        [string]$Label = "python-sdk"
    )

    $fullConfig = $BaseConfig.Clone()
    $fullConfig["packageName"] = $PackageName
    $fullConfig["projectName"] = $ProjectName
    $fullConfig["packageVersion"] = "1.0.0"

    # Call core generator
    Invoke-OpenApiGen -Spec $Spec -OutDir $OutDir -Generator "python" -Config $fullConfig -Label $Label

    # Remove redundant/conflicting files produced by the generator
    $redundantFiles = @("setup.py", "setup.cfg", "requirements.txt", "test-requirements.txt", "tox.ini", ".travis.yml", ".gitlab-ci.yml", "git_push.sh")
    foreach ($f in $redundantFiles) {
        $path = Join-Path $OutDir $f
        if (Test-Path $path) { 
            Write-Host "  [INFO] Removing redundant file: $f" -ForegroundColor Yellow
            Remove-Item -Force $path 
        }
    }

    # Write modernized pyproject.toml
    $pyproject = @"
[build-system]
requires = ["setuptools>=61.0", "wheel"]
build-backend = "setuptools.build_meta"

[project]
name = "$ProjectName"
version = "1.0.0"
description = "$PyDesc"
readme = "README.md"
requires-python = ">=3.7"
authors = [
    { name = "AM Portfolio Team", email = "support@amportfolio.com" }
]
keywords = ["OpenAPI", "OpenAPI-Generator", "$ProjectName", "AM-Portfolio"]
dependencies = [
    "urllib3 >= 1.25.3",
    "python-dateutil",
    "pydantic >= 2",
    "typing-extensions >= 4.7.1",
]

[tool.setuptools.packages.find]
where = ["."]
include = ["${PackageName}*"]
"@
    Set-Content -Path (Join-Path $OutDir "pyproject.toml") -Value $pyproject
}

# End of Python.ps1

