import os
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

cwd = str(ROOT)
subprocess.run(['mvn.cmd', '-o', '-pl', 'market-data-app', 'spring-boot:run'], cwd=cwd, env=env)
