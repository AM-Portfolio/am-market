"""
Centralized configuration for am-parser.

Priority in cluster (preprod/prod/dev): environment variables / Vault / Helm only.
Priority for local: ENVIRONMENT=local + optional am-parser/.env file.
"""

from pathlib import Path
from typing import Optional

from dotenv import load_dotenv
from pydantic_settings import BaseSettings, SettingsConfigDict

# Local .env must override stale OS/user MONGO_URI (e.g. old port-forward 100.x:27017).
_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"
if _ENV_FILE.exists():
    load_dotenv(_ENV_FILE, override=True)


def _read_env_file_value(key: str) -> Optional[str]:
    """Read a value directly from am-parser/.env (ignores stale process env)."""
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

    # ==================== Logging ====================
    log_level: str = "INFO"
    """Console log level: DEBUG, INFO, WARNING, ERROR."""

    log_format: str = "json"
    """Application logging format: 'json' or 'text'."""

    # ==================== MongoDB Configuration ====================
    mongo_uri: str = "mongodb://admin:password123@localhost:27017"
    """MongoDB connection URI. Can include authentication credentials."""

    mongo_db: str = "mutual_funds"
    etf_db_name: Optional[str] = None

    # ==================== API ====================
    api_host: str = "127.0.0.1"
    api_port: int = 8000
    port: Optional[int] = None

    # ==================== LLM / Parsing ====================
    together_api_key: Optional[str] = None
    default_parse_method: str = "together"
    llm_provider: Optional[str] = None
    openai_api_key: Optional[str] = None
    openai_model: str = "gpt-4o-mini"
    """OpenAI model to use for parsing."""

    # ==================== Observability ====================
    service_name: str = "am-parser"
    """Name of the service for tracing and logging."""

    otel_exporter_otlp_endpoint: str = "http://otel-collector:4317"
    """OTEL collector gRPC endpoint."""

    otel_traces_exporter: str = "otlp"
    """OTEL traces exporter type."""

    # Legacy Logging service compatibility
    am_logging_base_url: str = "http://am-logging-svc"
    """URL of legacy AM Logging Service."""

    am_logging_persist_to_db: bool = False
    """Whether to persist logs using legacy service."""

    # ==================== Market Data / ETF ====================
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
        """Always prefer am-parser/.env over stale OS env (avoids stale port-forwards)."""
        file_uri = _read_env_file_value("MONGO_URI")
        if file_uri:
            object.__setattr__(self, "mongo_uri", file_uri)
        file_db = _read_env_file_value("MONGO_DB")
        if file_db:
            object.__setattr__(self, "mongo_db", file_db)


settings = Settings()
settings.apply_local_dotenv_overrides()


def refresh_settings_from_dotenv() -> None:
    """Reload Mongo settings from .env into the global singleton."""
    if _ENV_FILE.exists():
        load_dotenv(_ENV_FILE, override=True)
    settings.apply_local_dotenv_overrides()


def get_mongo_uri() -> str:
    """Mongo URI from am-parser/.env file (never stale OS env / port-forward)."""
    file_uri = _read_env_file_value("MONGO_URI")
    if file_uri:
        return file_uri
    return settings.mongo_uri


def get_mongo_target_label() -> str:
    """Host:port for logs (no credentials)."""
    uri = get_mongo_uri()
    return uri.split("@")[-1] if "@" in uri else uri


def get_mongo_debug_info() -> dict:
    """Safe Mongo config snapshot for /health and /debug/mongo."""
    import os

    file_uri = get_mongo_uri()
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
        "file_uri_used": file_uri.split("@")[-1] if "@" in file_uri else file_uri,
        "local_dotenv_override": settings.is_local,
    }


def get_mongo_db() -> str:
    return settings.mongo_db


def get_etf_db() -> str:
    return settings.effective_etf_db
