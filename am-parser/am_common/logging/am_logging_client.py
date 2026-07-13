# DO NOT EDIT: THIS FILE IS AUTO-GENERATED FROM OPENAPI SPEC
"""
Auto-generated Python SDK for AM Logging API
Generated from OpenAPI spec version 1.0.0
"""

import httpx
import asyncio
import os
from typing import Dict, Any, Optional, List
from datetime import datetime

class AMLoggingClient:
    """Fire-and-forget logging client for AM Logging Service"""
    
    def __init__(self, base_url: str = "http://am-logging-svc/v1", timeout: float = 2.0, persist_to_db: Optional[bool] = None):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        # Default to environment variable AM_LOGGING_PERSIST_TO_DB or False
        if persist_to_db is None:
            self.persist_to_db = os.getenv("AM_LOGGING_PERSIST_TO_DB", "False").lower() == "true"
        else:
            self.persist_to_db = persist_to_db
        
    async def _send_log_async(self, log_entry: Dict[str, Any]) -> bool:
        """Send log asynchronously - fire and forget"""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                await client.post(f"{self.base_url}/logs", json=log_entry)
                return True
        except Exception:
            # Silent fail for fire-and-forget
            return False
    
    def send_log(self, log_entry: Dict[str, Any]) -> None:
        """Fire-and-forget log sending - non-blocking"""
        if self._validate_log_entry(log_entry):
            asyncio.create_task(self._send_log_async(log_entry))
    
    def _validate_log_entry(self, log_entry: Dict[str, Any]) -> bool:
        """Validate log entry against OpenAPI schema"""
        required_fields = set(['trace_id', 'span_id', 'service', 'timestamp', 'log_type', 'level', 'payload'])
        return required_fields.issubset(set(log_entry.keys()))
    
    def create_log_entry(
        self,
        trace_id: str,
        span_id: str,
        service: str,
        level: str,
        payload: Dict[str, Any],
        log_type: str = "TECHNICAL",
        context: Optional[Dict[str, Any]] = None,
        exception: Optional[Dict[str, Any]] = None,
        metadata: Optional[Dict[str, str]] = None,
        persist_to_db: Optional[bool] = None
    ) -> Dict[str, Any]:
        """Create a properly formatted log entry"""
        metadata = metadata or {}
        if persist_to_db is not None:
            metadata["persist_to_db"] = str(persist_to_db).lower()
        elif self.persist_to_db is not None:
            metadata["persist_to_db"] = str(self.persist_to_db).lower()

        return {
            "trace_id": trace_id,
            "span_id": span_id,
            "service": service,
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "log_type": log_type,
            "level": level,
            "payload": payload,
            "context": context or {},
            "exception": exception,
            "metadata": metadata or {}
        }

# Convenience functions for different log levels
class LoggerMixin:
    """Mixin class to add logging capabilities to any class"""
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._log_client = AMLoggingClient()
        self._service_name = getattr(self, 'service_name', self.__class__.__module__.split('.')[0])
    
    def _log_async(self, level: str, message: str, **kwargs):
        """Async logging method"""
        import uuid
        import inspect
        
        trace_id = kwargs.get('trace_id', str(uuid.uuid4()))
        span_id = kwargs.get('span_id', str(uuid.uuid4()))
        
        frame = inspect.currentframe().f_back
        class_name = frame.f_locals.get('self', None).__class__.__name__ if 'self' in frame.f_locals else "Global"
        method_name = frame.f_code.co_name
        
        log_entry = self._log_client.create_log_entry(
            trace_id=trace_id,
            span_id=span_id,
            service=self._service_name,
            level=level,
            payload={"message": message},
            context={"class": class_name, "method": method_name},
            metadata=kwargs.get('metadata'),
            persist_to_db=kwargs.get('persist_to_db')
        )
        
        self._log_client.send_log(log_entry)
    
    def log_info(self, message: str, **kwargs):
        self._log_async("INFO", message, **kwargs)
    
    def log_error(self, message: str, **kwargs):
        self._log_async("ERROR", message, **kwargs)
    
    def log_debug(self, message: str, **kwargs):
        self._log_async("DEBUG", message, **kwargs)
    
    def log_warn(self, message: str, **kwargs):
        self._log_async("WARN", message, **kwargs)
    
    def log_critical(self, message: str, **kwargs):
        self._log_async("CRITICAL", message, **kwargs)
