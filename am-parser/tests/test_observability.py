import pytest
import asyncio
import logging
import json
from io import StringIO
from am_common.observability import (
    ObservabilityConfig,
    configure_observability,
    get_logger,
    bind_context,
    get_context,
    set_context,
    clear_context
)
from am_common.logging.core import AMLogger

@pytest.mark.asyncio
async def test_mdc_isolation():
    """Verify that contextvars MDC does not leak state between concurrent async tasks."""
    clear_context()
    
    async def task_one():
        with bind_context(correlationId="TASK-ONE", userId="USER-1"):
            await asyncio.sleep(0.05)
            # Ensure values remain active in task context
            assert get_context().get("correlationId") == "TASK-ONE"
            assert get_context().get("userId") == "USER-1"
            
            # Modify context inside task
            set_context(flow_step="STEP-1")
            assert get_context().get("flow_step") == "STEP-1"
            
    async def task_two():
        with bind_context(correlationId="TASK-TWO", userId="USER-2"):
            await asyncio.sleep(0.02)
            assert get_context().get("correlationId") == "TASK-TWO"
            assert get_context().get("userId") == "USER-2"
            
            set_context(flow_step="STEP-2")
            assert get_context().get("flow_step") == "STEP-2"
            
    # Run concurrently
    await asyncio.gather(task_one(), task_two())
    
    # Assert outer thread context remains unaffected/empty
    assert get_context() == {}

def test_log_json_structure():
    """Capture stdout logging output and verify Loki JSON structured attributes."""
    # Configure json logging manually on a mock stream
    config = ObservabilityConfig(
        service_name="test-service",
        log_level="INFO",
        log_format="json"
    )
    
    logger = get_logger("test.observability")
    
    # Setup custom string capture handler
    log_capture = StringIO()
    handler = logging.StreamHandler(log_capture)
    
    from am_common.observability.logger_config import ObservabilityJsonFormatter
    formatter = ObservabilityJsonFormatter(config, "%(message)s")
    handler.setFormatter(formatter)
    
    # Clear existing handlers to isolate
    logger.handlers = []
    logger.propagate = False
    logger.addHandler(handler)
    logger.setLevel(logging.INFO)
    
    # Log a message within an active MDC context
    with bind_context(correlationId="TEST-CORRELATION-ID", userId="TEST-USER-ID"):
        logger.info("Observability verification check")
        
    log_output = log_capture.getvalue().strip()
    assert log_output != ""
    
    # Parse output as JSON and validate fields
    log_json = json.loads(log_output)
    
    assert "@timestamp" in log_json
    assert log_json["level"] == "INFO"
    assert log_json["service"] == "test-service"
    assert log_json["logger"] == "test.observability"
    assert log_json["correlationId"] == "TEST-CORRELATION-ID"
    assert log_json["userId"] == "TEST-USER-ID"
    assert log_json["caller.file"] == "test_observability.py"
    assert "caller.line" in log_json
    assert log_json["caller.method"] == "test_log_json_structure"

def test_legacy_compatibility():
    """Verify legacy AMLogger routes log fields correctly to the new standard."""
    config = ObservabilityConfig(
        service_name="legacy-compat-service",
        log_level="INFO",
        log_format="json"
    )
    
    # Reconfigure root/compatibility logger
    configure_observability(config)
    
    am_logger = AMLogger(service_name="legacy-compat-service", persist_to_db=False)
    
    # Capture root logger streams
    log_capture = StringIO()
    handler = logging.StreamHandler(log_capture)
    from am_common.observability.logger_config import ObservabilityJsonFormatter
    formatter = ObservabilityJsonFormatter(config, "%(message)s")
    handler.setFormatter(formatter)
    
    root_logger = logging.getLogger()
    for h in root_logger.handlers[:]:
        root_logger.removeHandler(h)
    root_logger.addHandler(handler)
    
    # Log a message via legacy interface
    am_logger.log("INFO", "Legacy compatibility audit log", {"custom_key": "custom_val"})
    
    log_output = log_capture.getvalue().strip()
    assert log_output != ""
    
    log_json = json.loads(log_output)
    assert log_json["level"] == "INFO"
    assert log_json["service"] == "legacy-compat-service"
    assert log_json["custom_key"] == "custom_val"
    assert log_json["caller.class"] == "Global"
    assert log_json["caller.method"] == "test_legacy_compatibility"


def test_am_logger_fallback():
    """Verify that AMLogger resolves and binds traceId and spanId when OTel context is empty."""
    config = ObservabilityConfig(
        service_name="am-logger-fallback-test",
        log_level="INFO",
        log_format="json"
    )
    am_logger = AMLogger(service_name="am-logger-fallback-test", persist_to_db=False)
    
    log_capture = StringIO()
    handler = logging.StreamHandler(log_capture)
    from am_common.observability.logger_config import ObservabilityJsonFormatter
    formatter = ObservabilityJsonFormatter(config, "%(message)s")
    handler.setFormatter(formatter)
    
    root_logger = logging.getLogger()
    for h in root_logger.handlers[:]:
        root_logger.removeHandler(h)
    root_logger.addHandler(handler)
    
    am_logger.log("INFO", "Testing amlogger fallback tracing IDs")
    
    log_output = log_capture.getvalue().strip()
    assert log_output != ""
    
    log_json = json.loads(log_output)
    assert log_json["level"] == "INFO"
    # Ensure generated traceId and spanId fallbacks are present in MDC log structure
    assert "traceId" in log_json and log_json["traceId"] != ""
    assert "spanId" in log_json and log_json["spanId"] != ""
    assert log_json["correlationId"] == log_json["traceId"]


def test_logger_mixin_fallback():
    """Verify that LoggerMixin resolves and binds traceId and spanId when OTel context is empty."""
    config = ObservabilityConfig(
        service_name="logger-mixin-fallback-test",
        log_level="INFO",
        log_format="json"
    )
    
    from am_common.logging.am_logging_client import LoggerMixin
    
    class MixedClass(LoggerMixin):
        def do_log(self):
            self._log_async("INFO", "Testing mixin fallback tracing IDs")
            
    mixed_instance = MixedClass()
    
    log_capture = StringIO()
    handler = logging.StreamHandler(log_capture)
    from am_common.observability.logger_config import ObservabilityJsonFormatter
    formatter = ObservabilityJsonFormatter(config, "%(message)s")
    handler.setFormatter(formatter)
    
    # Configure logging for 'logger-mixin-fallback-test' specifically or mixin module
    logger_name = mixed_instance._service_name
    mixin_logger = logging.getLogger(logger_name)
    mixin_logger.handlers = []
    mixin_logger.propagate = False
    mixin_logger.addHandler(handler)
    mixin_logger.setLevel(logging.INFO)
    
    mixed_instance.do_log()
    
    log_output = log_capture.getvalue().strip()
    assert log_output != ""
    
    log_json = json.loads(log_output)
    assert log_json["level"] == "INFO"
    assert "traceId" in log_json and log_json["traceId"] != ""
    assert "spanId" in log_json and log_json["spanId"] != ""


def test_mdc_priority_over_otel():
    """Verify that active OpenTelemetry IDs take priority and MDC cannot overwrite them."""
    # We can mock get_current_trace_id and get_current_span_id to return dummy values
    from am_common.observability import context as obs_ctx
    
    orig_get_trace = obs_ctx.get_current_trace_id
    orig_get_span = obs_ctx.get_current_span_id
    
    obs_ctx.get_current_trace_id = lambda: "ACTIVE-OTEL-TRACE-ID"
    obs_ctx.get_current_span_id = lambda: "ACTIVE-OTEL-SPAN-ID"
    
    try:
        config = ObservabilityConfig(
            service_name="otel-priority-test",
            log_level="INFO",
            log_format="json"
        )
        
        logger = get_logger("otel.priority.test")
        log_capture = StringIO()
        handler = logging.StreamHandler(log_capture)
        from am_common.observability.logger_config import ObservabilityJsonFormatter
        formatter = ObservabilityJsonFormatter(config, "%(message)s")
        handler.setFormatter(formatter)
        
        logger.handlers = []
        logger.propagate = False
        logger.addHandler(handler)
        logger.setLevel(logging.INFO)
        
        # Attempt to overwrite active OTel traceId/spanId via MDC context binding
        with bind_context(traceId="STALE-MDC-TRACE-ID", spanId="STALE-MDC-SPAN-ID", extra_field="value"):
            logger.info("Verifying OTel tracing ID priority")
            
        log_output = log_capture.getvalue().strip()
        assert log_output != ""
        
        log_json = json.loads(log_output)
        
        # Ensure active OTel traceId / spanId are emitted rather than MDC ones
        assert log_json["traceId"] == "ACTIVE-OTEL-TRACE-ID"
        assert log_json["spanId"] == "ACTIVE-OTEL-SPAN-ID"
        # Standard MDC fields should still exist
        assert log_json["extra_field"] == "value"
        
    finally:
        obs_ctx.get_current_trace_id = orig_get_trace
        obs_ctx.get_current_span_id = orig_get_span
