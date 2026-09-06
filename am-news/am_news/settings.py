from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    environment: str = "local"
    mongo_uri: str = ""
    mongodb_url: str = ""
    mongo_db: str = "am_news"
    redis_hostname: str = ""
    redis_port: int = 6379
    redis_password: str = ""
    market_data_url: str = "http://am-market-data:8080"
    news_sync_enabled: bool = False
    news_docs_enabled: bool = True
    upstox_news_url: str = "https://api.upstox.com/v2/news"
    upstox_user_agent: str = (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    )
    oidc_issuer: str = ""
    oidc_jwks_url: str = ""
    raw_ttl_days: int = 14
    affairs_ttl_seconds: int = 120
    holdings_ttl_seconds: int = 60
    feed_lock_ttl_seconds: int = 900
    affairs_limit: int = 10
    holdings_limit: int = 15
    symbols_cap: int = 80
    instrument_batch_size: int = 30

    @property
    def mongo_connection(self) -> str:
        return self.mongo_uri or self.mongodb_url


settings = Settings()
