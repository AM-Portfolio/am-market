# Load .env file and set environment variables, replacing host.docker.internal with localhost for local run
$envFile = "a:\InfraCode\AM-Portfolio\am-market\.env"
$infraHostVar = ""
$infraHostVar = ""
if (Test-Path $envFile) {
    foreach ($line in Get-Content $envFile) {
        if ($line -match "^[^#\s][^=]*=[^=]*") {
            $name, $value = $line.Split('=', 2)
            $name = $name.Trim()
            $value = $value.Trim()
            
            # Capture if this line DEFINES it
            if ($name -eq "INFRA_HOST") {
                $infraHostVar = $value
            }

            # Simple variable expansion for INFRA_HOST
            if ($value -match "\$\{INFRA_HOST\}") {
                if ($infraHostVar) {
                    # Escape the curly braces for regex replacement
                    $value = $value -replace "\$\{INFRA_HOST\}", $infraHostVar
                }
            }
            
            [System.Environment]::SetEnvironmentVariable($name, $value, [System.EnvironmentVariableTarget]::Process)
        }
    }
}

# Run the application
cd "a:\InfraCode\AM-Portfolio\am-market\am-market-data\market-data-app"
java -jar target/market-data-app-1.0-SNAPSHOT.jar --spring.profiles.active=local
