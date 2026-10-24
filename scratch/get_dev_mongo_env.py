import subprocess, json, os

kubeconfig = os.path.expandvars(r"%USERPROFILE%\Downloads\kubeconfig.am-vps-nonprod.yaml")
out = subprocess.check_output(['kubectl', '--kubeconfig', kubeconfig, '-n', 'am-apps-dev', 'get', 'pod', 'am-market-data-dev-5bdf69cf8-kgj9g', '-o', 'json'])
data = json.loads(out)
for e in data['spec']['containers'][0]['env']:
    if 'mongo' in e['name'].lower() or 'db' in e['name'].lower():
        print(f"{e['name']}: {e.get('value')}")
