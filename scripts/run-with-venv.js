/**
 * Run `am run` using the repo-root .venv Python (no manual activation required).
 * Usage: node scripts/run-with-venv.js <service-dir>   e.g. am-parser
 */
const { spawn } = require("child_process");
const path = require("path");

const serviceDir = process.argv[2];
if (!serviceDir) {
  console.error("Usage: node scripts/run-with-venv.js <service-dir>");
  process.exit(1);
}

const marketRoot = path.resolve(__dirname, "..");
const repoRoot = path.resolve(marketRoot, "..");
const isWin = process.platform === "win32";
const python = path.join(
  repoRoot,
  ".venv",
  isWin ? "Scripts/python.exe" : "bin/python"
);
const cwd = path.join(marketRoot, serviceDir);

/** Load am-parser/.env so it overrides stale Windows/user MONGO_URI etc. */
function loadDotEnv(dir) {
  const envPath = path.join(dir, ".env");
  const out = {};
  if (!require("fs").existsSync(envPath)) return out;
  for (const line of require("fs").readFileSync(envPath, "utf8").split(/\r?\n/)) {
    const t = line.trim();
    if (!t || t.startsWith("#")) continue;
    const i = t.indexOf("=");
    if (i === -1) continue;
    const k = t.slice(0, i).trim();
    let v = t.slice(i + 1).trim();
    if (
      (v.startsWith('"') && v.endsWith('"')) ||
      (v.startsWith("'") && v.endsWith("'"))
    ) {
      v = v.slice(1, -1);
    }
    out[k] = v;
  }
  return out;
}

const localEnv = loadDotEnv(cwd);

const child = spawn(python, ["-m", "am_cli.main", "run"], {
  cwd,
  stdio: "inherit",
  env: {
    ...process.env,
    ...localEnv,
    PYTHONIOENCODING: "utf-8",
  },
});

child.on("exit", (code) => process.exit(code ?? 0));
