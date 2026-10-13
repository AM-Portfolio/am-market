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

class AMLogger:
    """Legacy AMLogger refactored to act as a compatibility wrapper.
    
    Delegates all local logging to the standard Python logging system of our new 
    observability framework, while preserving fire-and-forget DB shipping to the 
    legacy CLS API only when explicitly enabled.
    """
    
    def __init__(self, service_name: str, cls_url: str = "http://am-logging-svc", persist_to_db: Optional[bool] = None):
        self.service_name = service_name
        self.cls_url = cls_url
        if persist_to_db is None:
            self.persist_to_db = os.getenv("AM_LOGGING_PERSIST_TO_DB", "False").lower() == "true"
        else:
            self.persist_to_db = persist_to_db
            
        # Retrieve the standard Logger configured under our new observability framework
        self.logger = logging.getLogger(service_name)

    def _format_message(self, level: str, trace_id: str, span_id: str, clazz: str, method: str, message: str, context_dict: dict) -> str:
        timestamp = datetime.datetime.now(datetime.timezone.utc).isoformat()
        return f"[{timestamp}] | [{self.service_name}] | [{trace_id}:{span_id}] | [{level}] | [{clazz}.{method}] | {message} | {json.dumps(context_dict)}"

    async def _send_to_cls(self, log_entry: dict):
        try:
            async with httpx.AsyncClient() as client:
                await client.post(f"{self.cls_url}/v1/logs", json=log_entry, timeout=2.0)
        except Exception as e:
            # Silent fallback to console logger
            self.logger.warning(f"Failed to send log to legacy CLS: {e}")

    def log(self, level: str, message: str, context_dict: Optional[dict] = None, trace_id: Optional[str] = None, span_id: Optional[str] = None, persist_to_db: Optional[bool] = None):
        from am_common.observability import context as obs_ctx
        
        # Retrieve active OTEL trace/span details or fallback to parameters
        active_trace = obs_ctx.get_current_trace_id() or trace_id or str(uuid.uuid4())
        active_span = obs_ctx.get_current_span_id() or span_id or "root"
        context_dict = context_dict or {}
        
        actual_persist = persist_to_db if persist_to_db is not None else self.persist_to_db
        
        # Frame inspection is only preserved inside this legacy wrapper
        frame = inspect.currentframe().f_back
        clazz = frame.f_locals.get('self', None).__class__.__name__ if 'self' in frame.f_locals else "Global"
        method = frame.f_code.co_name
        
        log_level_num = getattr(logging, level.upper(), logging.INFO)
        
        # Bind details to contextvars temporarily so structured logger formats them
        with obs_ctx.bind_context(
            correlationId=active_trace,
            traceId=active_trace,
            spanId=active_span,
            **{
                "caller.class": clazz,
                "caller.method": method,
                **context_dict
            }
        ):
            self.logger.log(log_level_num, message)

        # Async send to CLS database only if explicitly active
        if actual_persist:
            log_entry = {
                "trace_id": active_trace,
                "span_id": active_span,
                "service": self.service_name,
                "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat() + "Z",
                "log_type": "TECHNICAL",
                "level": level,
                "payload": {"message": message},
                "context": {
                    "class": clazz,
                    "method": method,
                    "inputs": context_dict.get("inputs"),
                    "outputs": context_dict.get("outputs")
                },
                "metadata": {
                    "persist_to_db": str(actual_persist).lower()
                }
            }
            try:
                # Dispatch async fire-and-forget call safely
                asyncio.create_task(self._send_to_cls(log_entry))
            except Exception:
                pass

def audit_activity(logger: AMLogger):
    """Decorator to audit execution latency and input/output parameters."""
    def decorator(func: Callable):
        @functools.wraps(func)
        async def wrapper(*args, **kwargs):
            from am_common.observability import context as obs_ctx
            
            trace_id = obs_ctx.get_current_trace_id() or str(uuid.uuid4())
            span_id = obs_ctx.get_current_span_id() or str(uuid.uuid4())
            
            inputs = {k: str(v) for k, v in kwargs.items()}
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