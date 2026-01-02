import requests
import time
import os
from datetime import datetime, timedelta
from typing import Optional, List, Dict
from app.core.config import settings

class AuthClient:
    """
    Client for handling authentication with am-auth service.
    Designed to be reusable for other Python services.
    """
    def __init__(self, auth_service_url: str = None, service_id: str = "market-data-analysis-py"):
        # Default to am-auth service (port 8001 based on SERVICES.md)
        self.auth_service_url = auth_service_url or os.getenv("AUTH_SERVICE_URL", "https://am.munish.org/api/v1/internal")
        self.service_id = service_id
        self._token: Optional[str] = None
        self._token_expiry: float = 0
        
    def get_token(self) -> str:
        """
        Get a valid service token. 
        Returns cached token if valid, otherwise fetches a new one.
        """
        if self._token and time.time() < self._token_expiry - 60: # Buffer of 60 seconds
            return self._token
            
        return self._fetch_new_token()
        
    def _fetch_new_token(self) -> str:
        """Fetch a new token from the auth service."""
        try:
            url = f"{self.auth_service_url}/service-token"
            payload = {
                "service_id": self.service_id,
                "service_name": "Market Data Analysis Service",
                "permissions": ["read:market-data", "read:historical-data"]
            }
            
            response = requests.post(url, json=payload, timeout=5)
            response.raise_for_status()
            
            data = response.json()
            self._token = data["access_token"]
            # Set expiry (subtract buffer)
            expires_in = data.get("expires_in", 3600)
            self._token_expiry = time.time() + expires_in
            
            return self._token
            
        except requests.RequestException as e:
            print(f"Failed to authenticate with am-auth: {e}")
            # In a real scenario, we might want to retry or raise
            raise Exception(f"Authentication failed: {e}") from e

    def get_headers(self) -> Dict[str, str]:
        """Get headers with authentication token."""
        token = self.get_token()
        return {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
