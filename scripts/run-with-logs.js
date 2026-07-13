/**
 * Run a shell command and tee stdout/stderr to am-market/logs/<timestamp>_<script>.log
 *
 * Usage (from am-market): node scripts/run-with-logs.js <command...>
 * npm_lifecycle_event is used for the log file prefix when set.
 */
const { spawn } = require("child_process");
const fs = require("fs");
const path = require("path");

const marketRoot = path.resolve(__dirname, "..");
const logsDir = path.join(marketRoot, "logs");

const rawArgs = process.argv.slice(2);
if (rawArgs.length === 0) {
  console.error("Usage: node scripts/run-with-logs.js <command> [args...]");
  process.exit(1);
}

const scriptName = (process.env.npm_lifecycle_event || "command").replace(/:/g, "-");
const startedAt = new Date();
const stamp = startedAt
  .toISOString()
  .replace(/\.\d{3}Z$/, "")
  .replace(/:/g, "-")
  .replace("T", "_");
const logFile = path.join(logsDir, `${stamp}_${scriptName}.log`);

fs.mkdirSync(logsDir, { recursive: true });

const logStream = fs.createWriteStream(logFile, { flags: "w" });

function writeLine(line, targetStdout = true) {
  const text = line.endsWith("\n") ? line : `${line}\n`;
  logStream.write(text);
  if (targetStdout) {
    process.stdout.write(text);
  } else {
    process.stderr.write(text);
  }
}

writeLine(
  `=== ${startedAt.toISOString()} | npm script: ${process.env.npm_lifecycle_event || "(direct)"} ===`
);
writeLine(`=== log file: ${logFile} ===`);
writeLine(`=== cwd: ${process.cwd()} ===`);
writeLine(`=== command: ${rawArgs.join(" ")} ===`);
writeLine("");

console.log(`[run-with-logs] Writing to ${logFile}`);

const child = spawn(rawArgs.join(" "), {
  cwd: process.cwd(),
  env: process.env,
  shell: true,
  stdio: ["inherit", "pipe", "pipe"],
});

child.stdout.on("data", (chunk) => {
  logStream.write(chunk);
  process.stdout.write(chunk);
});

child.stderr.on("data", (chunk) => {
  logStream.write(chunk);
  process.stderr.write(chunk);
});

child.on("error", (err) => {
  writeLine(`[run-with-logs] Failed to start process: ${err.message}`, false);
  logStream.end(() => process.exit(1));
});

child.on("close", (code, signal) => {
  const endedAt = new Date();
  const footer = [
    "",
    `=== ${endedAt.toISOString()} | exit code: ${code ?? "null"}${signal ? ` | signal: ${signal}` : ""} ===`,
  ].join("\n");
  logStream.write(`${footer}\n`);
  console.log(`[run-with-logs] Finished (exit ${code ?? "null"}). Log: ${logFile}`);
  logStream.end(() => process.exit(code === 0 ? 0 : code ?? 1));
});
