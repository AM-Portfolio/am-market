#!/usr/bin/env python3
"""
Map VPS Vault backup JSON → .env.preprod or .env.dev (gitignored)

Usage (from am-market-data/):
  npm run env:preprod
  npm run env:dev
  python scripts/map_env_from_vault_backup.py --env preprod
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
WORKSPACE_ROOT = REPO_ROOT.parent.parent

BACKUP_DIRS = (
    WORKSPACE_ROOT / "VPS" / "vault" / "backups",
    WORKSPACE_ROOT / "am-auth" / "vault" / "backups",
)

DEFAULT_SERVER_PORT = "8092"


def _get(data: dict, path: str) -> dict:
    return data.get(path) or {}


def _mongo_market_data_url(mongo: dict) -> str:
    base = (mongo.get("url") or "").strip()
    if "mongodb.infra.svc.cluster.local" in base:
        base = base.replace("mongodb.infra.svc.cluster.local:27017", "mongodb.asrax.in:8888")
    elif "mongodb.infra.svc.cluster.local" in (mongo.get("host") or ""):
        mongo = mongo.copy()
        mongo["host"] = "mongodb.asrax.in"
        mongo["port"] = "8888"

    if not base:
        user = mongo.get("username", "admin")
        password = mongo.get("password", "")
        host = mongo.get("host", "localhost")
        port = mongo.get("port", "27017")
        base = f"mongodb://{user}:{password}@{host}:{port}"

    if "/market_data" in base:
        if "authSource" not in base:
            sep = "&" if "?" in base else "?"
            base = f"{base}{sep}authSource=admin&directConnection=true"
        return base

    if "?" in base:
        path_part, query = base.split("?", 1)
        path_part = path_part.rstrip("/")
        return f"{path_part}/market_data?{query}"

    return f"{base.rstrip('/')}/market_data?authSource=admin&directConnection=true"


def _find_latest_backup(explicit: Path | None) -> Path:
    if explicit:
        if not explicit.is_file():
            raise SystemExit(f"Backup file not found: {explicit}")
        return explicit

    candidates: list[Path] = []
    for directory in BACKUP_DIRS:
        if directory.is_dir():
            candidates.extend(directory.glob("vps_vault_full_backup_*.json"))

    if not candidates:
        searched = ", ".join(str(d) for d in BACKUP_DIRS)
        raise SystemExit(
            f"No vps_vault_full_backup_*.json found. Searched:\n  {searched}\n"
            "Run from am-auth: npm run vault:backup\n"
            "Or pass: npm run env:from-vault -- --env preprod --backup <path>"
        )

    return sorted(candidates)[-1]


def build_env(env_name: str, data: dict) -> str:
    prefix = f"apps/{env_name}"
    mongo = _get(data, f"{prefix}/infra/mongodb")
    kafka = _get(data, f"{prefix}/infra/kafka")
    redis = _get(data, f"{prefix}/infra/redis")
    influx = _get(data, f"{prefix}/infra/influxdb")
    market = _get(data, f"{prefix}/services/am-market-data")

    redis_host = redis.get('host', 'redis.asrax.in')
    redis_port = redis.get('port', '8889')
    if redis_host == 'redis.infra.svc.cluster.local':
        redis_host = 'redis.asrax.in'
        redis_port = '8889'

    upstox_token = (
        market.get("UPSTOX_ACCESS_TOKEN")
        or market.get("UPSTOCK_ACCESS_TOKEN")
        or ""
    )

    run_script = "run:preprod" if env_name == "preprod" else "run:dev"

    lines = [
        f"# Auto-mapped from Vault backup — apps/{env_name}/*",
        f"# Run: npm run {run_script}",
        "# Helm vault paths: am-market-data/helm/vault-mappings.yaml (upstox, mongodb, redis, kafka)",
        "",
        f"SPRING_PROFILES_ACTIVE={env_name}",
        f"SERVER_PORT={DEFAULT_SERVER_PORT}",
        "",
        f"MONGODB_URL={_mongo_market_data_url(mongo)}",
        "MONGODB_DATABASE=market_data",
        f"MONGODB_USERNAME={mongo.get('username', 'admin')}",
        f"MONGODB_PASSWORD={mongo.get('password', '')}",
        "",
        f"REDIS_HOSTNAME={redis_host}",
        f"REDIS_PORT={redis_port}",
        f"REDIS_PASSWORD={redis.get('password', '')}",
        "",
        f"INFLUXDB_URL=http://{influx.get('host', 'influxdb.asrax.in')}:{influx.get('port', '8086')}",
        f"INFLUXDB_TOKEN={influx.get('token', '')}",
        f"INFLUXDB_ORG={influx.get('org', 'am-portfolio')}",
        f"INFLUXDB_BUCKET={influx.get('bucket', 'market-data')}",
        "",
        "# Laptop: use VPS Kafka host (Vault bootstrap_servers is in-cluster)",
        "KAFKA_BOOTSTRAP_SERVERS=kafka.asrax.in:9092",
        f"KAFKA_USERNAME={kafka.get('username', 'kafkaUser')}",
        f"KAFKA_PASSWORD={kafka.get('password', '')}",
        "MARKET_DATA_KAFKA_ENABLED=false",
        "",
        f"UPSTOX_API_KEY={market.get('UPSTOX_API_KEY', '')}",
        f"UPSTOX_SECRET_KEY={market.get('UPSTOX_SECRET_KEY', '')}",
        f"UPSTOX_REDIRECT_URI={market.get('UPSTOX_REDIRECT_URI', f'http://localhost:{DEFAULT_SERVER_PORT}/v1/market-data/auth/session')}",
        f"UPSTOX_ACCESS_TOKEN={upstox_token}",
        "UPSTOX_CODE=",
        "",
        f"JWT_SECRET={market.get('JWT_SECRET_KEY', '')}",
        f"INTERNAL_JWT_SECRET={market.get('SECRET_KEY', '')}",
        "",
        f"STOKUPATE_TOPIC_NAME={market.get('STOCK_PRICE_UPDATE_TOPIC', 'am-stock-price-update')}",
        "",
        "BATCH_SEARCH_CACHE_ENABLED=true",
        "",
    ]
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Map Vault backup (apps/preprod/* or apps/dev/*) to .env.preprod / .env.dev"
    )
    parser.add_argument(
        "--env",
        choices=("preprod", "dev"),
        default="preprod",
        help="Vault apps/{env}/* prefix to map (default: preprod)",
    )
    parser.add_argument("--backup", type=Path, help="Path to vps_vault_full_backup_*.json")
    args = parser.parse_args()

    backup_path = _find_latest_backup(args.backup)

    with open(backup_path, encoding="utf-8") as f:
        payload = json.load(f)
    data = payload.get("data", {})

    env_path = REPO_ROOT / f".env.{args.env}"
    env_path.write_text(build_env(args.env, data), encoding="utf-8")
    print(f"Wrote {env_path}")
    print(f"Source: {backup_path}")
    print(f"Next: npm run run:{args.env}")


if __name__ == "__main__":
    main()
