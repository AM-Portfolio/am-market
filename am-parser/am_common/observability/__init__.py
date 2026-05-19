import logging
from am_common.observability.config import ObservabilityConfig
from am_common.observability.tracing import configure_tracing, shutdown_tracing, get_tracer
from am_common.observability.logger_config import configure_logging
from am_common.observability.context import (
    bind_context,
    set_context,
    clear_context,
    get_context
)

def configure_observability(config: ObservabilityConfig) -> None:
    """Initialize both standard logging and OpenTelemetry tracing using a decoupled config."""
    configure_logging(config)
    configure_tracing(config)

def shutdown_observability() -> None:
    """Cleanly shut down and flush both log and trace pipelines on application exit."""
    shutdown_tracing()

def get_logger(name: str) -> logging.Logger:
    """Retrieve a standard Python Logger instance configured with the observability layout."""
    return logging.getLogger(name)

__all__ = [
    "ObservabilityConfig",
    "configure_observability",
    "shutdown_observability",
    "get_logger",
    "get_tracer",
    "bind_context",
    "set_context",
    "clear_context",
    "get_context"
]
