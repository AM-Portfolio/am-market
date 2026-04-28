const { spawn } = require('child_process');
const path = require('path');

const projectParam = process.argv[2];
const moduleParam = process.argv[3];

if (!projectParam || !moduleParam) {
  console.error('Usage: node build-module.js <project> <module>');
  console.error('Example: node build-module.js market-data kafka');
  process.exit(1);
}

const projectDir = projectParam === 'common' ? 'am-common-investment-data' : 'am-market-data';
const prefix = projectParam === 'common' ? 'am-common-investment' : 'market-data';

// Prepend prefix if it's missing, unless it's ALL
const moduleName = (moduleParam.toUpperCase() === 'ALL')
  ? null
  : (moduleParam.startsWith(prefix) ? moduleParam : `${prefix}-${moduleParam}`);

console.log(moduleName ? `🚀 Building module: ${moduleName} in ${projectDir}...` : `🚀 Building ALL modules in ${projectDir}...`);

const mvn = process.platform === 'win32' ? 'mvn.cmd' : 'mvn';
const pomPath = path.join(__dirname, '..', projectDir, 'pom.xml');

const args = [
  '-f', pomPath,
  'clean', 'install',
  '-DskipTests'
];

if (moduleName) {
  args.push('-am', '-pl', moduleName);
}

const child = spawn(mvn, args, { stdio: 'inherit', shell: true });

child.on('exit', (code) => {
  process.exit(code);
});
