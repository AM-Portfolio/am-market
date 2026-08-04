# Local environment (am-market-data)

Run **market-data-app** on port **8092** with secrets from Vault, without committing them.

## Prerequisites

- **Java 17**, **Maven**, **Node.js** (for `npm` / `dotenv-cli`)
- **Python 3** (for `npm run env:preprod` / `env:dev`)
- Network access to VPS MongoDB, Redis, Influx (or override in gitignored env files)

## Files

| File | Git | Purpose |
|------|-----|---------|
| `.env.template` | committed | Placeholders — copy or generate into gitignored files |
| `.env.preprod` | ignored | Preprod → `npm run run:preprod` |
| `.env.dev` | ignored | Dev → `npm run run:dev` |

`dotenv-cli` loads `.env.preprod` or `.env.dev` only (no committed `.env`).

## End-to-end workflow

### 1. Refresh Vault backup

From **am-auth** (needs `scripts/.env` with `VAULT_TOKEN`):

```bash
cd a:\InfraCode\AM-Portfolio-grp\am-auth
npm run vault:backup
```

Backup locations (newest wins):

- `a:\InfraCode\AM-Portfolio-grp\VPS\vault\backups\vps_vault_full_backup_<timestamp>.json`
- `a:\InfraCode\AM-Portfolio-grp\am-auth\vault\backups\vps_vault_full_backup_<timestamp>.json`

### 2. Generate gitignored env files

From **am-market-data**:

```bash
cd a:\InfraCode\AM-Portfolio-grp\am-market\am-market-data
npm run env:preprod
npm run env:dev
```

Explicit backup:

```bash
npm run env:from-vault -- --env preprod --backup a:\InfraCode\AM-Portfolio-grp\VPS\vault\backups\vps_vault_full_backup_20260523_012147.json
```

Manual alternative:

```bash
copy .env.template .env.preprod
# Fill UPSTOX_API_KEY, UPSTOX_SECRET_KEY, UPSTOX_ACCESS_TOKEN, MONGODB_URL, REDIS_PASSWORD, etc.
```

### 3. Run locally (port 8092)

From **am-market-data** (recommended):

```bash
cd a:\InfraCode\AM-Portfolio-grp\am-market\am-market-data
npm run run:preprod
```

Split steps:

```bash
npm run preprod:compile && npm run preprod:start
```

From **am-market** root (same dotenv flow):

```bash
cd a:\InfraCode\AM-Portfolio-grp\am-market
npm run run:data:preprod
```

Health: `http://localhost:8092/actuator/health`

## Market calendar (local/dev)

After `npm run run:dev`, exercise holidays/timings (JWT required when `security.enabled=true`):

```powershell
$base = "http://localhost:8092"
curl -s "$base/v1/market-calendar/holidays?year=2026&exchange=NSE"
curl -s "$base/v1/market-calendar/status?exchange=NSE"
curl -s -X POST "$base/v1/admin/sync/market-calendar?exchange=NSE"
```

Config: `market-data.calendar.source=upstox` (swappable), nightly sync `scheduler.market-calendar.sync-cron`.

## npm scripts (am-market-data)

| Script | What it does |
|--------|----------------|
| `env:preprod` / `env:dev` | Map Vault → `.env.preprod` / `.env.dev` |
| `env:from-vault` | Mapper; pass `-- --env preprod\|dev` and optional `--backup <path>` |
| `preprod:compile` / `dev:compile` | `mvn -pl market-data-app -am compile -DskipTests` |
| `preprod:start` / `dev:start` | `dotenv-cli -e .env.* -- mvn -f market-data-app/pom.xml spring-boot:run` |
| `run:preprod` / `run:dev` | compile + start |

## npm scripts (am-market root)

| Script | What it does |
|--------|----------------|
| `run:data:preprod` | `npm run run:preprod` in am-market-data |
| `run:data:dev` | `npm run run:dev` in am-market-data |
| `run:data` | Same as `run:data:preprod` (loads `.env.preprod` via dotenv) |

## Vault → `.env` mapping

| Variable | Vault path | Notes |
|----------|------------|--------|
| `UPSTOX_API_KEY` | `apps/{env}/services/am-market-data` | Required for startup (`upstox.auth.api-key`) |
| `UPSTOX_SECRET_KEY` | same | |
| `UPSTOX_ACCESS_TOKEN` | `UPSTOX_ACCESS_TOKEN` or `UPSTOCK_ACCESS_TOKEN` | Vault typo alias handled in mapper |
| `UPSTOX_REDIRECT_URI` | same | Default local redirect if missing |
| `UPSTOX_CODE` | — | OAuth code; empty for local unless refreshing token |
| `MONGODB_URL` | `apps/{env}/infra/mongodb` → `url` | Mapper appends `/market_data` |
| `REDIS_*` | `apps/{env}/infra/redis` | VPS Redis (e.g. port `8889`) |
| `INFLUXDB_*` | `apps/{env}/infra/influxdb` | |
| `KAFKA_*` | `apps/{env}/infra/kafka` | Bootstrap forced to `kafka.asrax.in:9092` for laptop |
| `MARKET_DATA_KAFKA_ENABLED` | — | **`false`** for local run |
| `JWT_SECRET` | `am-market-data` → `JWT_SECRET_KEY` | |
| `SERVER_PORT` | — | **`8092`** (`.am.yaml` port) |

Kubernetes uses Helm + `helm/vault-mappings.yaml`, not these `.env` files.

## Root cause of `UPSTOX_API_KEY` startup failure

`application.yml` binds `upstox.auth.api-key` to `${UPSTOX_API_KEY}` with no default. `npm run run:data` via `am run` only injected `.am.yaml` env (`MONGODB_DATABASE`) and optional `am-market-data/.env`, not Upstox keys from Vault.

## Blockers

| Issue | Mitigation |
|-------|------------|
| No Vault backup JSON | `npm run vault:backup` in am-auth |
| Missing `.env.preprod` | `npm run env:preprod` or copy `.env.template` |
| Mongo/Redis unreachable | VPN/firewall to VPS hosts |
| Port in use | Change `SERVER_PORT` in `.env.preprod` |
