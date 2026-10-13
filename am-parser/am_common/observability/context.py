from contextvars import ContextVar
from contextlib import contextmanager
from typing import Any, Dict, Optional
from opentelemetry import trace

# Mapped Diagnostic Context (MDC) using thread-safe / async-safe ContextVar
# Initialized with a default factory returning an empty dict
MDC_CONTEXT: ContextVar[Dict[str, Any]] = ContextVar("mdc_context", default={})

def get_context() -> Dict[str, Any]:
    """Retrieve a copy of the current MDC context dictionary."""
    return dict(MDC_CONTEXT.get())

def set_context(**kwargs) -> None:
    """Dynamically set or update key-value pairs in the active MDC context."""
    ctx = get_context()
    ctx.update(kwargs)
    MDC_CONTEXT.set(ctx)

def clear_context() -> None:
    """Clear all key-value pairs in the active MDC context."""
    MDC_CONTEXT.set({})

@contextmanager
def bind_context(**kwargs):
    """Context manager to bind key-value pairs to the MDC context, 
    automatically restoring the previous context on exit.
    
    Usage:
        with bind_context(correlationId="123", userId="user456"):
            logger.info("Some message")
    """
    # Backup current context
    token = MDC_CONTEXT.set({**get_context(), **kwargs})
    try:
        yield
    finally:
        # Reset to previous context using contextvar token
        MDC_CONTEXT.reset(token)

def get_current_trace_id() -> Optional[str]:
    """Retrieve the current active OpenTelemetry trace ID in standard 32-hex format."""
    try:
        span = trace.get_current_span()
        if span and span.get_span_context().is_valid:
            return f"{span.get_span_context().trace_id:032x}"
    except Exception:
        pass
    return None

def get_current_span_id() -> Optional[str]:
    """Retrieve the current active OpenTelemetry span ID in standard 16-hex format."""
    try:
        span = trace.get_current_span()
        if span and span.get_span_context().is_valid:
            return f"{span.get_span_context().span_id:016x}"
    except Exception:
        pass
    return None
