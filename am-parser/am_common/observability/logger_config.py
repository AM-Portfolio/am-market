import logging
import sys
import datetime
import traceback
from pythonjsonlogger import jsonlogger
from am_common.observability.config import ObservabilityConfig
from am_common.observability import context

class ObservabilityJsonFormatter(jsonlogger.JsonFormatter):
    """Custom JSON formatter to produce structured logs in perfect alignment 
    with the Spring Boot logback Loki schema.
    """
    
    def __init__(self, config: ObservabilityConfig, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.config = config
        
    def add_fields(self, log_record: dict, record: logging.LogRecord, message_dict: dict) -> None:
        super().add_fields(log_record, record, message_dict)
        
        # 1. `@timestamp` formatted as ISO 8601 UTC string (e.g. 2026-05-18T14:49:58Z)
        timestamp = datetime.datetime.fromtimestamp(record.created, datetime.timezone.utc).isoformat()
        if timestamp.endswith("+00:00"):
            timestamp = timestamp[:-6] + "Z"
        log_record["@timestamp"] = timestamp
        
        # Remove standard default asctime to keep JSON output clean
        log_record.pop("asctime", None)
        
        # 2. General logging fields
        log_record["level"] = record.levelname
        log_record["service"] = self.config.service_name
        log_record["logger"] = record.name
        log_record["thread"] = record.threadName
        
        # Remove duplicate levelname if present
        log_record.pop("levelname", None)
        
        # 3. Inject trace and span context dynamically from OpenTelemetry (or fallback to MDC)
        trace_id = context.get_current_trace_id()
        span_id = context.get_current_span_id()
        
        mdc = context.get_context()
        if not trace_id and mdc.get("traceId"):
            trace_id = mdc["traceId"]
        if not span_id and mdc.get("spanId"):
            span_id = mdc["spanId"]
            
        if trace_id:
            log_record["traceId"] = trace_id
        if span_id:
            log_record["spanId"] = span_id
            
        # 4. Inject MDC variables (correlationId, userId, flow parameters)
        # Prevent MDC from overriding active OpenTelemetry IDs
        for k, v in mdc.items():
            if v is not None and k not in {"traceId", "spanId"}:
                log_record[k] = v
                
        # 5. Inject lightweight caller metadata (optimized, non-blocking)
        log_record["caller.file"] = log_record.get("caller.file") or record.filename
        log_record["caller.line"] = log_record.get("caller.line") or record.lineno
        log_record["caller.method"] = log_record.get("caller.method") or record.funcName

        
        # 6. Formatting stack traces
        if record.exc_info:
            log_record["stack_trace"] = "".join(traceback.format_exception(*record.exc_info))

def configure_logging(config: ObservabilityConfig) -> None:
    """Set up application logging dynamically based on config."""
    root_logger = logging.getLogger()
    
    # Clear existing handlers to prevent duplicates
    for handler in root_logger.handlers[:]:
        root_logger.removeHandler(handler)
        
    # Map configuration level string to standard level constants
    level_num = getattr(logging, config.log_level.upper(), logging.INFO)
    root_logger.setLevel(level_num)
    
    handler = logging.StreamHandler(sys.stdout)
    
    if config.log_format == "json":
        # Cloud/Kubernetes Profile: Structured JSON stdout
        formatter = ObservabilityJsonFormatter(config, "%(message)s")
        handler.setFormatter(formatter)
        root_logger.addHandler(handler)
    else:
        # Local Development Profile: Plain Text
        class ObservabilityTextFormatter(logging.Formatter):
            def format(self, record: logging.LogRecord) -> str:
                trace_id = context.get_current_trace_id() or ""
                span_id = context.get_current_span_id() or ""
                trace_info = f",{trace_id},{span_id}" if trace_id else ""
                
                # Format timestamp
                t = datetime.datetime.fromtimestamp(record.created, datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
                
                # Standard spring-like output layout
                msg = f"{t} {record.levelname:<5} [{config.service_name}{trace_info}] {record.name} - {record.getMessage()}"
                
                # Formatted exceptions
                if record.exc_info:
                    msg += "\n" + "".join(traceback.format_exception(*record.exc_info))
                return msg
                
        handler.setFormatter(ObservabilityTextFormatter())
        root_logger.addHandler(handler)
