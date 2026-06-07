import sys
import os
import subprocess
import shutil
import time
from typing import List

import sys
import os
import subprocess
import shutil
import time
from typing import List

# --- Auto-detect and run inside virtual environment ---
script_dir = os.path.dirname(os.path.abspath(__file__))
repo_root = os.path.dirname(script_dir) # am-market
am_repos_root = os.path.dirname(repo_root) # am-repos

venv_dir = os.path.join(am_repos_root, ".venv")
is_windows = os.name == "nt"
venv_python = os.path.join(venv_dir, "Scripts", "python.exe") if is_windows else os.path.join(venv_dir, "bin", "python")

# If we are not running from the venv_python, but it exists, re-execute under it
current_python = os.path.abspath(sys.executable)
target_python = os.path.abspath(venv_python)

if os.path.exists(target_python) and current_python != target_python:
    print(f"[VENV] Re-executing script using virtual environment Python: {venv_python}")
    new_env = dict(os.environ)
    new_env["PYTHONIOENCODING"] = "utf-8"
    ret = subprocess.call([target_python] + sys.argv, env=new_env)
    sys.exit(ret)

# --- Bootstrap am-scripts ---
am_scripts_src = os.path.join(am_repos_root, "am-scripts", "src")

# Ensure am-scripts is in the python path
if os.path.exists(am_scripts_src) and am_scripts_src not in sys.path:
    sys.path.insert(0, am_scripts_src)
    
use_fallback = False
try:
    from am_scripts.run_local import load_environment_variables, run_service
    
    def run_with_logging(cmd, cwd, env, log_name):
        """Local wrapper for of run_with_logging imported from am_scripts."""
        from am_scripts.run_local import run_with_logging as _run_with_logging
        logs_dir = os.path.join(repo_root, "logs")
        return _run_with_logging(cmd, cwd=cwd, env=env, logs_dir=logs_dir, log_name=log_name)
except ImportError:
    use_fallback = True
    print(f"Info: am-scripts repository not found at {am_scripts_src}. Using local fallback execution.")

if use_fallback:
    def load_environment_variables(service_dir: str, am_repos_root: str, repo_root: str) -> dict:
        """Fallback implementation to load environment variables from .env files when am-scripts is not present."""
        env = dict(os.environ)
        
        # Read service-specific .env files
        am_env = env.get("AM_ENV", "preprod")
        
        candidates = [
            os.path.join(service_dir, f".env.{am_env}"),
            os.path.join(service_dir, ".env"),
            os.path.join(repo_root, f".env.{am_env}"),
            os.path.join(repo_root, ".env"),
        ]
        
        env_file = None
        for path in candidates:
            if os.path.exists(path):
                env_file = path
                break
                
        if env_file:
            print(f"SUCCESS: Environment variables loaded from {os.path.basename(env_file)} file")
            try:
                from dotenv import dotenv_values
                env.update({k: v for k, v in dotenv_values(env_file).items() if v is not None})
            except ImportError:
                # Manual fallback parser
                with open(env_file, "r", encoding="utf-8") as f:
                    for line in f:
                        line = line.strip()
                        if not line or line.startswith("#"):
                            continue
                        if "=" not in line:
                            continue
                        k, v = line.split("=", 1)
                        k = k.strip()
                        v = v.strip()
                        if (v.startswith('"') and v.endswith('"')) or (v.startswith("'") and v.endswith("'")):
                            v = v[1:-1]
                        env[k] = v
        else:
            print(f"Warning: No .env file found in {service_dir}")
            
        return env

    def run_with_logging(cmd, cwd, env, log_name):
        """Fallback run_with_logging implementation when am-scripts is not present."""
        logs_dir = os.path.join(repo_root, "logs")
        os.makedirs(logs_dir, exist_ok=True)
        log_file_path = os.path.join(logs_dir, f"{log_name}.log")
        
        print(f"[RUN] Running command: {' '.join(cmd)}")
        print(f"[LOG] Logging output to {log_file_path}")
        
        is_windows = os.name == "nt"
        use_shell = is_windows and (cmd[0] in ["mvn", "npm", "flutter"])
        
        with open(log_file_path, "a", encoding="utf-8") as log_file:
            log_file.write(f"\n\n=== Run starting at {time.strftime('%Y-%m-%d %H:%M:%S')} ===\n")
            log_file.write(f"Command: {' '.join(cmd)}\n")
            log_file.write(f"Cwd: {cwd}\n\n")
            log_file.flush()
            
            process = subprocess.Popen(
                cmd,
                cwd=cwd,
                env=env,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                encoding="utf-8",
                errors="replace",
                shell=use_shell
            )
            
            while True:
                line = process.stdout.readline()
                if not line and process.poll() is not None:
                    break
                if line:
                    sys.stdout.write(line)
                    sys.stdout.flush()
                    log_file.write(line)
                    log_file.flush()
                    
            process.wait()
            return process.returncode

    def run_service(service_name: str, service_dir: str, port: int, am_repos_root: str, repo_root: str, app_entry: str, log_name: str):
        """Fallback run_service implementation when am-scripts is not present."""
        print(f"--- Starting {service_name} on port {port} ---")
        
        merged_env = load_environment_variables(service_dir, am_repos_root, repo_root)
        
        # Ensure PYTHONPATH from os.environ is preserved/preferred if it was programmatically altered
        if "PYTHONPATH" in os.environ:
            merged_env["PYTHONPATH"] = os.environ["PYTHONPATH"]
            
        cmd = [
            sys.executable, "-m", "uvicorn",
            app_entry,
            "--host", "0.0.0.0",
            "--port", str(port)
        ]
        
        # Add reload flags if dev environment
        if merged_env.get("AM_ENV") == "dev" or os.environ.get("AM_ENV") == "dev":
            cmd.append("--reload")
            
        try:
            run_with_logging(cmd, cwd=service_dir, env=merged_env, log_name=log_name)
        except KeyboardInterrupt:
            print(f"\nStopped {service_name}.")

def run_market():
    """Run AM Market Data (Java/Maven)."""
    service_name = "AM Market Data"
    target_module = "market-data-app"
    target_dir = os.path.join(repo_root, "am-market-data", target_module)
    
    port = "8092"
    # Detect --run flag
    run_only = "--run" in sys.argv or "--run-only" in sys.argv
    
    if len(sys.argv) > 1 and sys.argv[1].isdigit():
        port = sys.argv[1]
        
    print(f"--- Starting {service_name} on port {port} ---")
    
    # Load all env overrides using am-scripts
    # Using am-market-data root for service_dir to pick up .env
    market_data_root = os.path.join(repo_root, "am-market-data")
    merged_env = load_environment_variables(market_data_root, am_repos_root, repo_root)
    merged_env["SERVER_PORT"] = port
    
    is_windows = os.name == "nt"
    
    try:
        if run_only:
            # Run without building (java -jar)
            target_jar_dir = os.path.join(target_dir, "target")
            if os.path.exists(target_jar_dir):
                jars = [f for f in os.listdir(target_jar_dir) if f.endswith(".jar") and "repackaged" not in f and "original" not in f]
                if jars:
                    jar_path = os.path.join(target_jar_dir, jars[0])
                    print(f"🚀 Running pre-built JAR: {jars[0]}...")
                    cmd = ["java", "-jar", jar_path]
                    # Update to use logging
                    run_with_logging(cmd, cwd=target_dir, env=merged_env, log_name="market-data-app")
                    return
            print("❌ Error: No built JAR found in target/. Run standard 'market' command first to build.")
            return

        cmd = ["mvn", "spring-boot:run"]
        run_with_logging(cmd, cwd=target_dir, env=merged_env, log_name="market-data-app")
    except KeyboardInterrupt:
        print(f"\nStopped {service_name}.")

def build_market():
    """Build specific module or all of AM Market Data with Maven."""
    # Check if 'data' was passed as alias to build am-common-investment-data
    if len(sys.argv) > 1:
        args = sys.argv[1:]
        if args[0] == "build":
            args = args[1:]
        if "data" in args:
            install_common_data()
            # Remove 'data' to allow building other modules if listed
            args = [a for a in args if a != "data"]
            if not args:
                return # Only wanted data

    target_dir = os.path.join(repo_root, "am-market-data")
    print(f"🚀 Running 'mvn clean install' in {target_dir}...")
    cmd = ["mvn", "clean", "install"]
    
    # Append and expand arguments
    if 'args' in locals() and args:
        modules = []
        other_args = []
        for arg in args:
            if not arg.startswith("-"):
                full_name = arg if arg.startswith("market-data-") else f"market-data-{arg}"
                if os.path.exists(os.path.join(target_dir, full_name)):
                    modules.append(f":{full_name}")
                else:
                    other_args.append(arg)
            else:
                other_args.append(arg)
                
        if modules:
            cmd += ["-pl", ",".join(modules)]
        cmd += other_args
        
    print(f"💡 Expanded command: {' '.join(cmd)}")
    
    # Merge environment to preserve PATH and load any overrides
    merged_env = load_environment_variables(target_dir, am_repos_root, repo_root)
    final_env = dict(os.environ)
    final_env.update(merged_env)
    
    # Run with logging
    run_with_logging(cmd, cwd=target_dir, env=final_env, log_name="market-data-build")
    
def run_parser():
    """Run AM Parser (Python/FastAPI)."""
    target_dir = os.path.join(repo_root, "am-parser")
    # Add am-platform-security library to PYTHONPATH
    security_lib = os.path.join(am_repos_root, "am-platform", "libraries", "am-platform-security")
    os.environ["PYTHONPATH"] = f"{target_dir}{';' if os.name == 'nt' else ':'}{security_lib}{';' if os.name == 'nt' else ':'}" + os.environ.get("PYTHONPATH", "")
    # am-parser uses am_api.api:app
    run_service("AM Parser API", target_dir, 8022, am_repos_root, repo_root, app_entry="am_api.api:app", log_name="am-parser")

def run_analysis():
    """Run Market Data Analysis (Python/FastAPI)."""
    target_dir = os.path.join(repo_root, "market-data-analysis-py")
    # market-data-analysis uses app.main:app
    run_service("Market Data Analysis API", target_dir, 8010, am_repos_root, repo_root, app_entry="app.main:app", log_name="market-data-analysis")

def get_best_flutter_device():
    """Identify available devices and return the best option (chrome or web-server fallback)."""
    try:
        output = subprocess.check_output(["flutter", "devices"], text=True)
        if "chrome" in output.lower():
            return "chrome"
        return "web-server"
    except Exception:
        return "web-server"

def run_ui():
    """Run AM Market UI (Local Flutter)."""
    target_dir = os.path.join(repo_root, "am_market_ui")
    print(f"🚀 Starting Market UI locally in {target_dir}...")
    
    device = get_best_flutter_device()
    print(f"💡 Selected Flutter Device: {device}")
    
    cmd = ["flutter", "run", "-d", device]
    if device in ["chrome", "web-server"]:
        cmd += ["--web-port=9000"]
        
    is_windows = os.name == "nt"
    
    logs_dir = os.path.join(repo_root, "logs")
    os.makedirs(logs_dir, exist_ok=True)
    log_file = os.path.join(logs_dir, "market-ui.log")
    
    print(f"📖 Logging output to {log_file}")
    try:
        if is_windows:
            subprocess.run(cmd, cwd=target_dir, shell=True)
        else:
            # Use | tee pipeline to preserve interactive stdin keys like r/R for hot reload
            cmd_str = " ".join(cmd) + f" | tee -a {log_file}"
            subprocess.run(cmd_str, cwd=target_dir, shell=True)
    except KeyboardInterrupt:
        print("\nStopped Market UI.")

def install_common_data():
    """Run `mvn clean install` in `am-common-investment-data`."""
    target_dir = os.path.join(repo_root, "am-common-investment-data")
    print(f"🚀 Running 'mvn clean install' in {target_dir}...")
    cmd = ["mvn", "clean", "install"]
    
    merged_env = load_environment_variables(target_dir, am_repos_root, repo_root)
    final_env = dict(os.environ)
    final_env.update(merged_env)
    
    try:
         run_with_logging(cmd, cwd=target_dir, env=final_env, log_name="common-investment-data-build")
    except KeyboardInterrupt:
         print("\nStopped.")

def run_all():
    """Run all services concurrently."""
    print("🚀 Starting all Market services...")
    
    processes = []
    
    # 1. Start Parser (Python)
    parser_dir = os.path.join(repo_root, "am-parser")
    parser_env = load_environment_variables(parser_dir, am_repos_root, repo_root)
    security_lib = os.path.join(am_repos_root, "am-platform", "libraries", "am-platform-security")
    parser_env["PYTHONPATH"] = f"{parser_dir}{';' if os.name == 'nt' else ':'}{security_lib}{';' if os.name == 'nt' else ':'}" + parser_env.get("PYTHONPATH", "")
    p_parser = subprocess.Popen(
        [sys.executable, "-m", "uvicorn", "am_api.api:app", "--host", "0.0.0.0", "--port", "8022"],
        cwd=parser_dir, env=parser_env
    )
    processes.append(("Parser", p_parser))
    
    # 2. Start Analysis (Python)
    analysis_dir = os.path.join(repo_root, "market-data-analysis-py")
    analysis_env = load_environment_variables(analysis_dir, am_repos_root, repo_root)
    analysis_env["PYTHONPATH"] = f"{analysis_dir}{';' if os.name == 'nt' else ':'}" + analysis_env.get("PYTHONPATH", "")
    p_analysis = subprocess.Popen(
        [sys.executable, "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8010"],
        cwd=analysis_dir, env=analysis_env
    )
    processes.append(("Analysis", p_analysis))
    
    # 3. Start Market Data (Java - slow)
    mvn_exe = shutil.which("mvn")
    if mvn_exe:
        market_dir = os.path.join(repo_root, "am-market-data")
        market_app_dir = os.path.join(market_dir, "market-data-app")
        market_env = load_environment_variables(market_dir, am_repos_root, repo_root)
        market_env["SERVER_PORT"] = "8092"
        # Ensure JAVA_HOME is forwarded if set in current process
        if "JAVA_HOME" not in market_env and os.environ.get("JAVA_HOME"):
            market_env["JAVA_HOME"] = os.environ["JAVA_HOME"]
        p_market = subprocess.Popen(
            [mvn_exe, "spring-boot:run"],
            cwd=market_app_dir, env=market_env, shell=(os.name == "nt")
        )
        processes.append(("Market-Data", p_market))
    else:
        print("⚠️  WARNING: 'mvn' not found on PATH — Market-Data (Java) service skipped.")
        print("   Install Maven: winget install Apache.Maven")
        print("   Restart your terminal after installation.")


    print("✅ All services started. Press Ctrl+C to stop all.")
    
    try:
        while True:
            time.sleep(1)
            for name, p in processes:
                if p.poll() is not None:
                    print(f"⚠️ Service {name} exited with code {p.returncode}")
                    # Optionally restart or just exit
    except KeyboardInterrupt:
        print("\nStopping all services...")
        for name, p in processes:
            print(f"Terminating {name}...")
            p.terminate()
        for name, p in processes:
            p.wait()
        print("All services stopped.")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        cmd = sys.argv[1]
        if cmd == "market": run_market()
        elif cmd == "build": build_market()
        elif cmd == "parser": run_parser()
        elif cmd == "analysis": run_analysis()
        elif cmd == "ui": run_ui()
        elif cmd == "install-data": install_common_data()
        elif cmd == "all": run_all()
        else: print(f"Unknown command: {cmd}")
    else:
        run_all()
