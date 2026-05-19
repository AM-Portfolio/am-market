import logging
from typing import Optional
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.resources import Resource
from am_common.observability.config import ObservabilityConfig

logger = logging.getLogger(__name__)

# Keep a reference to the active provider to allow clean shutdown
_TRACER_PROVIDER: Optional[TracerProvider] = None

def configure_tracing(config: ObservabilityConfig) -> None:
    """Configure standard OpenTelemetry tracing with OTLP/gRPC exporter."""
    global _TRACER_PROVIDER
    
    # Avoid duplicate initialization
    if _TRACER_PROVIDER is not None:
        return
        
    # Define standard OTEL resource metadata
    resource = Resource.create(attributes={
        "service.name": config.service_name,
        "compose_service": config.service_name,
    })
    
    provider = TracerProvider(resource=resource)
    trace.set_tracer_provider(provider)
    _TRACER_PROVIDER = provider
    
    # Setup OTLP/gRPC exporter
    if config.otel_traces_exporter == "otlp":
        try:
            # Explicitly load and configure the OTLP/gRPC span exporter
            from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
            
            exporter = OTLPSpanExporter(
                endpoint=config.otel_exporter_otlp_endpoint,
                insecure=True
            )
            
            span_processor = BatchSpanProcessor(exporter)
            provider.add_span_processor(span_processor)
            logger.info(f"OpenTelemetry tracing initialized successfully with OTLP/gRPC exporter at {config.otel_exporter_otlp_endpoint}")
        except Exception as e:
            # Fallback strategy: Active but local-only to avoid application crash under load
            logger.warning(
                f"Failed to initialize OTLP/gRPC tracing exporter: {e}. "
                f"Tracing will remain active locally but not exported to OTLP collector."
            )

def shutdown_tracing() -> None:
    """Flush spans and cleanly shut down the active trace provider."""
    global _TRACER_PROVIDER
    if _TRACER_PROVIDER:
        try:
            _TRACER_PROVIDER.shutdown()
            _TRACER_PROVIDER = None
            logger.info("OpenTelemetry trace provider cleanly shut down.")
        except Exception as e:
            logger.error(f"Error shutting down OpenTelemetry trace provider: {e}")

def get_tracer(name: str) -> trace.Tracer:
    """Retrieve a standard named tracer instance."""
    return trace.get_tracer(name)
