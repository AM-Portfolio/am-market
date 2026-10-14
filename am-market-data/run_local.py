import os
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent
env_file = ROOT / ".env"
env = os.environ.copy()

if env_file.exists():
    with open(env_file, 'r') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                k, v = line.split('=', 1)
                env[k.strip()] = v.strip()

if 'REDIS_FORCE_ENABLED' not in env:
    env['REDIS_FORCE_ENABLED'] = 'true'

wrapper = ROOT / ('mvnw.cmd' if os.name == 'nt' else 'mvnw')
maven = str(wrapper) if wrapper.exists() else (shutil.which('mvn') or shutil.which('mvn.cmd'))
if not maven:
    raise SystemExit('Maven was not found. Install Maven or add the Maven wrapper to this repository.')

command = [maven, '-pl', 'market-data-app', 'spring-boot:run']
if env.get('AM_MAVEN_OFFLINE', '').strip().lower() in {'1', 'true', 'yes'}:
    command.insert(1, '-o')

result = subprocess.run(command, cwd=str(ROOT), env=env)
raise SystemExit(result.returncode)
