"""Standard console logging for am-parser (endpoint + service visibility)."""
import logging
import sys
from typing import Optional

_CONFIGURED = False


def setup_parser_logging(level: str = "INFO") -> None:
    global _CONFIGURED
    if _CONFIGURED:
        return
    log_level = getattr(logging, (level or "INFO").upper(), logging.INFO)
    logging.basicConfig(
        level=log_level,
        format="%(asctime)s | %(levelname)-7s | %(name)s | %(message)s",
        datefmt="%H:%M:%S",
        stream=sys.stdout,
        force=True,
    )
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
    _CONFIGURED = True


def get_logger(component: str) -> logging.Logger:
    return logging.getLogger(f"am-parser.{component}")
