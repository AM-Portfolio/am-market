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

from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """Centralized configuration for am-parser service
    
    All configuration values can be set via environment variables or .env file.
    Default values are provided as fallbacks.
    """
    
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
    
    class Config:
        """Pydantic configuration"""
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False  # Allow MONGO_URI or mongo_uri
        extra = "ignore"  # Ignore extra env vars
    
    @property
    def effective_api_port(self) -> int:
        """Returns the effective API port, preferring 'port' over 'api_port' if set."""
        return self.port if self.port is not None else self.api_port
    
    @property
    def effective_etf_db(self) -> str:
        """Returns ETF database name, defaulting to main database if not specified."""
        return self.etf_db_name or self.mongo_db


# ==================== Global Singleton Instance ====================
settings = Settings()
"""
Global configuration instance.

Import this singleton to access configuration throughout the application:
    from am_configs.settings import settings
"""


# ==================== Backward Compatibility Helpers ====================
def get_mongo_uri() -> str:
    """Get MongoDB URI from settings (backward compatibility helper)."""
    return settings.mongo_uri


def get_mongo_db() -> str:
    """Get MongoDB database name from settings (backward compatibility helper)."""
    return settings.mongo_db


def get_etf_db() -> str:
    """Get ETF database name from settings (backward compatibility helper)."""
    return settings.effective_etf_db
