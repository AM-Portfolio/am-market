"""
Centralized Configuration Management for am-parser

This module provides a single source of truth for all application configuration.
Uses Pydantic BaseSettings for type-safe, validated configuration with automatic
environment variable loading and .env file support.

Usage:
    from am_configs.settings import settings
    
    # Access configuration
    mongo_uri = settings.mongo_uri
    db_name = settings.mongo_db
"""

from pathlib import Path

from dotenv import load_dotenv
from pydantic_settings import BaseSettings
from typing import Optional

# Local .env must override stale OS/user MONGO_URI (e.g. old port-forward 100.x:27017).
_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"
if _ENV_FILE.exists():
    load_dotenv(_ENV_FILE, override=True)


def _read_mongo_uri_from_env_file() -> Optional[str]:
    """Read MONGO_URI directly from am-parser/.env (ignores stale process env)."""
    if not _ENV_FILE.exists():
        return None
    for line in _ENV_FILE.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        if key.strip() == "MONGO_URI":
            return value.strip().strip('"').strip("'")
    return None


def _read_mongo_db_from_env_file() -> Optional[str]:
    if not _ENV_FILE.exists():
        return None
    for line in _ENV_FILE.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        if key.strip() == "MONGO_DB":
            return value.strip().strip('"').strip("'")
    return None


class Settings(BaseSettings):
    """Centralized configuration for am-parser service
    
    All configuration values can be set via environment variables or .env file.
    Default values are provided as fallbacks.
    """
    
    # ==================== Environment Configuration ====================
    environment: str = "local"
    """Application environment: local, dev, preprod, prod."""

    log_level: str = "INFO"
    """Console log level: DEBUG, INFO, WARNING, ERROR."""

    # ==================== MongoDB Configuration ====================
    mongo_uri: str = "mongodb://admin:password123@localhost:27017"
    """MongoDB connection URI. Can include authentication credentials."""
    
    mongo_db: str = "mutual_funds"
    """Default MongoDB database name for mutual fund data."""
    
    etf_db_name: Optional[str] = None
    """Optional separate database for ETF data. Defaults to mongo_db if not specified."""
    
    # ==================== Server Configuration ====================
    api_host: str = "127.0.0.1"
    """API server host address."""
    
    api_port: int = 8000
    """API server port number."""
    
    # Alternative PORT env var for compatibility
    port: Optional[int] = None
    """Alternative port configuration (for compatibility)."""
    
    # ==================== LLM Configuration ====================
    together_api_key: Optional[str] = None
    """Together AI API key for LLM-based parsing."""
    
    default_parse_method: str = "together"
    """Default parsing method: 'together' or 'manual'."""
    
    llm_provider: Optional[str] = None
    """LLM provider name (together, openai, etc.)."""
    
    openai_api_key: Optional[str] = None
    """OpenAI API key for GPT-based parsing."""
    
    openai_model: str = "gpt-4o-mini"
    """OpenAI model to use for parsing."""
    
    # ==================== Observability Configuration ====================
    service_name: str = "am-parser"
    """Name of the service for tracing and logging."""
    
    otel_exporter_otlp_endpoint: str = "http://otel-collector:4317"
    """OTEL collector gRPC endpoint."""
    
    otel_traces_exporter: str = "otlp"
    """OTEL traces exporter type."""
    
    log_level: str = "INFO"
    """Application logging level."""
    
    log_format: str = "json"
    """Application logging format: 'json' or 'text'."""
    
    # Legacy Logging service compatibility
    am_logging_base_url: str = "http://am-logging-svc"
    """URL of legacy AM Logging Service."""
    
    am_logging_persist_to_db: bool = False
    """Whether to persist logs using legacy service."""

    model_config = {

        "env_file": str(_ENV_FILE) if _ENV_FILE.exists() else ".env",
        "env_file_encoding": "utf-8",
        "case_sensitive": False,
        "extra": "ignore",
    }
    
    @property
    def effective_api_port(self) -> int:
        """Returns the effective API port, preferring 'port' over 'api_port' if set."""
        return self.port if self.port is not None else self.api_port
    
    @property
    def effective_etf_db(self) -> str:
        """Returns ETF database name, defaulting to main database if not specified."""
        return self.etf_db_name or self.mongo_db

    def model_post_init(self, __context) -> None:
        """Always prefer am-parser/.env over OS env (avoids stale 100.x:27017 port-forward)."""
        file_uri = _read_mongo_uri_from_env_file()
        if file_uri:
            object.__setattr__(self, "mongo_uri", file_uri)
        file_db = _read_mongo_db_from_env_file()
        if file_db:
            object.__setattr__(self, "mongo_db", file_db)


def refresh_settings_from_dotenv() -> None:
    """Reload Mongo settings from .env into the global singleton."""
    if _ENV_FILE.exists():
        load_dotenv(_ENV_FILE, override=True)
    file_uri = _read_mongo_uri_from_env_file()
    if file_uri:
        object.__setattr__(settings, "mongo_uri", file_uri)
    file_db = _read_mongo_db_from_env_file()
    if file_db:
        object.__setattr__(settings, "mongo_db", file_db)


# ==================== Global Singleton Instance ====================
settings = Settings()
"""
Global configuration instance.

Import this singleton to access configuration throughout the application:
    from am_configs.settings import settings
"""


# ==================== Backward Compatibility Helpers ====================
def get_mongo_uri() -> str:
    """Mongo URI from am-parser/.env file (never stale OS env / port-forward)."""
    file_uri = _read_mongo_uri_from_env_file()
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
        "mongo_target": get_mongo_target_label(),
        "mongo_db": settings.mongo_db,
        "os_env_mongo_uri_set": bool(os.environ.get("MONGO_URI")),
        "os_env_mongo_tail": (
            os.environ.get("MONGO_URI", "").split("@")[-1]
            if os.environ.get("MONGO_URI")
            else None
        ),
        "file_uri_used": file_uri.split("@")[-1] if "@" in file_uri else file_uri,
    }


def get_mongo_db() -> str:
    """Get MongoDB database name from settings (backward compatibility helper)."""
    return settings.mongo_db


def get_etf_db() -> str:
    """Get ETF database name from settings (backward compatibility helper)."""
    return settings.effective_etf_db
