import os

class Settings:
    PROJECT_NAME: str = "Market Data Analysis Service"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    
    # Internal Market Data Service URL (Docker service name)
    # Use internal service for Docker-to-Docker communication
    MARKET_DATA_SERVICE_URL: str = os.getenv(
        "MARKET_DATA_SERVICE_URL", 
        "http://market-data-service:8092/api"
    )

settings = Settings()
