# DO NOT EDIT: THIS FILE IS AUTO-GENERATED
import logging
import json
import uuid
import datetime
import time
import functools
import inspect
import os
import httpx
import asyncio
from typing import Any, Dict, Optional, Callable

# Generated Pattern: [{timestamp}] | [{service}] | [{trace_id}:{span_id}] | [{level}] | [{class}.{method}] | {message} | {context}

class AMLogger:
    def __init__(self, service_name: str, cls_url: str, persist_to_db: Optional[bool] = None):
        self.service_name = service_name
        self.cls_url = cls_url
        if persist_to_db is None:
            self.persist_to_db = os.getenv("AM_LOGGING_PERSIST_TO_DB", "False").lower() == "true"
        else:
            self.persist_to_db = persist_to_db
        self.logger = logging.getLogger(service_name)
        self.logger.setLevel(logging.INFO)
        if not self.logger.handlers:
            handler = logging.StreamHandler()
            self.logger.addHandler(handler)

    def _format_message(self, level: str, trace_id: str, span_id: str, clazz: str, method: str, message: str, context: dict) -> str:
        timestamp = datetime.datetime.now(datetime.timezone.utc).isoformat()
        # Enforcing Pattern: [{timestamp}] | [{service}] | [{trace_id}:{span_id}] | [{level}] | [{class}.{method}] | {message} | {context}
        return f"[{timestamp}] | [{self.service_name}] | [{trace_id}:{span_id}] | [{level}] | [{clazz}.{method}] | {message} | {json.dumps(context)}"

    async def _send_to_cls(self, log_entry: dict):
        try:
            async with httpx.AsyncClient() as client:
                await client.post(f"{self.cls_url}/v1/logs", json=log_entry, timeout=2.0)
        except Exception as e:
            # Fallback to local console if CLS is down (Zero Log Loss strategy)
            self.logger.error(f"Failed to send log to CLS: {e}")

    def log(self, level: str, message: str, context: Optional[dict] = None, trace_id: Optional[str] = None, span_id: Optional[str] = None, persist_to_db: Optional[bool] = None):
        trace_id = trace_id or str(uuid.uuid4())
        span_id = span_id or "root"
        context = context or {}
        
        # Determine actual persistence
        actual_persist = persist_to_db if persist_to_db is not None else self.persist_to_db
        
        # Get caller info
        frame = inspect.currentframe().f_back
        clazz = frame.f_locals.get('self', None).__class__.__name__ if 'self' in frame.f_locals else "Global"
        method = frame.f_code.co_name
        
        formatted_msg = self._format_message(level, trace_id, span_id, clazz, method, message, context)
        self.logger.log(getattr(logging, level), formatted_msg)

        # Async send to CLS
        log_entry = {
            "trace_id": trace_id,
            "span_id": span_id,
            "service": self.service_name,
            "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
            "log_type": "TECHNICAL",
            "level": level,
            "payload": {"message": message},
            "context": {
                "class": clazz,
                "method": method,
                "inputs": context.get("inputs"),
                "outputs": context.get("outputs")
            },
            "metadata": {
                "persist_to_db": str(actual_persist).lower()
            }
        }
        asyncio.create_task(self._send_to_cls(log_entry))

def audit_activity(logger: AMLogger):
    def decorator(func: Callable):
        @functools.wraps(func)
        async def wrapper(*args, **kwargs):
            trace_id = str(uuid.uuid4())
            span_id = str(uuid.uuid4())
            clazz = args[0].__class__.__name__ if args and hasattr(args[0], '__class__') else "Global"
            
            # Capture inputs (Sanitized)
            inputs = {k: v for k, v in kwargs.items()}
            
            logger.log("INFO", f"ENTERING {func.__name__}", {"inputs": inputs}, trace_id, span_id)
            
            start_time = time.time()
            try:
                if asyncio.iscoroutinefunction(func):
                    result = await func(*args, **kwargs)
                else:
                    result = func(*args, **kwargs)
                latency = round((time.time() - start_time) * 1000, 2)
                logger.log("INFO", f"EXITING {func.__name__}", {"outputs": {"result": "success"}, "latency_ms": latency}, trace_id, span_id)
                return result
            except Exception as e:
                logger.log("ERROR", f"FAILED {func.__name__}", {"exception": str(e)}, trace_id, span_id)
                raise
        return wrapper
    return decorator