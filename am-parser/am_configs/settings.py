"""
Centralized configuration for am-parser.

Priority in cluster (preprod/prod/dev): environment variables / Vault / Helm only.
Priority for local: ENVIRONMENT=local + optional am-parser/.env file.
"""

from pathlib import Path
from typing import Optional

from dotenv import load_dotenv
from pydantic_settings import BaseSettings, SettingsConfigDict

_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"
if _ENV_FILE.exists():
    load_dotenv(_ENV_FILE, override=False)


def _read_env_file_value(key: str) -> Optional[str]:
    if not _ENV_FILE.exists():
        return None
    for line in _ENV_FILE.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        k, value = stripped.split("=", 1)
        if k.strip() == key:
            return value.strip().strip('"').strip("'")
    return None


class Settings(BaseSettings):
    environment: str = "local"
    log_level: str = "INFO"

    mongo_uri: str = "mongodb://localhost:27017"
    mongo_db: str = "mutual_funds"
    etf_db_name: Optional[str] = None

    api_host: str = "127.0.0.1"
    api_port: int = 8000
    port: Optional[int] = None

    together_api_key: Optional[str] = None
    default_parse_method: str = "together"
    llm_provider: Optional[str] = None
    openai_api_key: Optional[str] = None
    openai_model: str = "gpt-4o-mini"

    market_data_url: str = "http://localhost:8093"
    etf_list_cache_minutes: int = 60
    etf_holdings_cache_days: int = 1
    moneycontrol_holdings_url_template: str = (
        "https://mf.moneycontrol.com/service/etf/v1/getSchemeHoldingData"
        "?isin={isin}&key=Stocks"
    )

    model_config = SettingsConfigDict(
        env_file=str(_ENV_FILE) if _ENV_FILE.exists() else None,
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    @property
    def effective_api_port(self) -> int:
        return self.port if self.port is not None else self.api_port

    @property
    def effective_etf_db(self) -> str:
        return self.etf_db_name or self.mongo_db

    @property
    def is_local(self) -> bool:
        return self.environment.strip().lower() == "local"

    def apply_local_dotenv_overrides(self) -> None:
        """Only for ENVIRONMENT=local: prefer am-parser/.env over stale OS env."""
        if not self.is_local:
            return
        file_uri = _read_env_file_value("MONGO_URI")
        if file_uri:
            object.__setattr__(self, "mongo_uri", file_uri)
        file_db = _read_env_file_value("MONGO_DB")
        if file_db:
            object.__setattr__(self, "mongo_db", file_db)


settings = Settings()
settings.apply_local_dotenv_overrides()


def refresh_settings_from_dotenv() -> None:
    """Reload settings; local .env overrides apply only when ENVIRONMENT=local."""
    if _ENV_FILE.exists():
        load_dotenv(_ENV_FILE, override=True)
    object.__setattr__(settings, "environment", settings.environment)
    settings.apply_local_dotenv_overrides()


def get_mongo_uri() -> str:
    return settings.mongo_uri


def get_mongo_target_label() -> str:
    uri = get_mongo_uri()
    return uri.split("@")[-1] if "@" in uri else uri


def get_mongo_debug_info() -> dict:
    import os

    return {
        "environment": settings.environment,
        "mongo_target": get_mongo_target_label(),
        "mongo_db": settings.mongo_db,
        "market_data_url": settings.market_data_url,
        "os_env_mongo_uri_set": bool(os.environ.get("MONGO_URI")),
        "os_env_mongo_tail": (
            os.environ.get("MONGO_URI", "").split("@")[-1]
            if os.environ.get("MONGO_URI")
            else None
        ),
        "file_uri_used": get_mongo_target_label(),
        "local_dotenv_override": settings.is_local,
    }


def get_mongo_db() -> str:
    return settings.mongo_db


def get_etf_db() -> str:
    return settings.effective_etf_db
