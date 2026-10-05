import os
import subprocess

env_file = r'c:\Users\ASUS\Desktop\AM-PORTFOLIO\am-market\am-market-data\.env'
env = os.environ.copy()

if os.path.exists(env_file):
    with open(env_file, 'r') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                k, v = line.split('=', 1)
                env[k.strip()] = v.strip()

cwd = r'c:\Users\ASUS\Desktop\AM-PORTFOLIO\am-market\am-market-data\market-data-app'
subprocess.run(['mvn.cmd', 'spring-boot:run'], cwd=cwd, env=env)
