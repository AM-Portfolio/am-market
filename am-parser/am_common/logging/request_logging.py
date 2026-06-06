"""Standard console logging for am-parser (endpoint + service visibility)."""
import logging
import sys
from typing import Optional

_CONFIGURED = False


def setup_parser_logging(level: str = "INFO") -> None:
    global _CONFIGURED
    if _CONFIGURED:
        return
    from am_common.observability.config import ObservabilityConfig
    from am_common.observability.logger_config import configure_logging
    
    config = ObservabilityConfig(log_level=level, log_format="json")
    configure_logging(config)
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
    _CONFIGURED = True


def get_logger(component: str) -> logging.Logger:
    return logging.getLogger(f"am-parser.{component}")
