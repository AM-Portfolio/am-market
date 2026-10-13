from dataclasses import dataclass

@dataclass
class ObservabilityConfig:
    """Decoupled configuration dataclass for the observability system.
    
    Contains no application-specific settings imports, allowing direct reuse 
    and packaging into an external library later.
    """
    service_name: str = "am-parser"
    otel_exporter_otlp_endpoint: str = "http://otel-collector:4317"
    otel_traces_exporter: str = "otlp"
    log_level: str = "INFO"
    log_format: str = "json"
