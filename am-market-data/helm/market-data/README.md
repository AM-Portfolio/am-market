# Market Data Service Helm Chart

This Helm chart deploys the Market Data Service and its dependencies to AKS (Azure Kubernetes Service).

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- Azure Kubernetes Service (AKS)
- Nginx Ingress Controller
- Cert-Manager (for TLS)

## Features

- Automated deployment of Market Data Service with optimized configurations
- Built-in retry mechanism with configurable parameters
- Comprehensive metrics collection via Prometheus and InfluxDB
- Grafana dashboards for monitoring
- Horizontal Pod Autoscaling based on CPU and Memory
- Persistent storage for all stateful components
- TLS support via cert-manager

## Installation

1. Add required Helm repositories:
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add influxdata https://helm.influxdata.com/
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```

2. Create a values override file (e.g., `custom-values.yaml`) and update the following:
   - Image repository and credentials
   - Domain name in ingress configuration
   - Database credentials
   - InfluxDB organization and token
   - Kafka configuration if needed

3. Install the chart:
```bash
helm install market-data ./market-data -f custom-values.yaml -n market-data --create-namespace
```

## Configuration

### Critical Settings

1. Market Data Processing:
```yaml
config:
  marketData:
    maxRetries: 3              # Maximum retry attempts for API calls
    retryDelayMs: 1000        # Base delay between retries
    threadPoolSize: 5         # Thread pool size for parallel processing
    threadQueueCapacity: 10   # Queue capacity for pending tasks
    maxAgeMinutes: 15        # Maximum age of market data
```

2. Resource Allocation:
```yaml
resources:
  limits:
    cpu: 1000m
    memory: 1Gi
  requests:
    cpu: 500m
    memory: 512Mi
```

3. Autoscaling:
```yaml
autoscaling:
  enabled: true
  minReplicas: 1
  maxReplicas: 3
  targetCPUUtilizationPercentage: 80
  targetMemoryUtilizationPercentage: 80
```

### Dependencies

1. PostgreSQL:
- Persistent storage: 10Gi
- Optimized for time-series data
- Configurable credentials

2. Kafka:
- 3 replicas for high availability
- Zookeeper cluster included
- JMX metrics enabled
- Persistent storage: 10Gi

3. InfluxDB:
- Prometheus endpoint enabled
- 30-day data retention
- Persistent storage: 10Gi

4. Grafana:
- Auto-configured datasources
- Persistent storage: 5Gi
- Pre-configured dashboards

5. Prometheus:
- 30-day metrics retention
- Persistent storage: 10Gi
- AlertManager included

## Health Monitoring

The service includes comprehensive health checks:
- Liveness probe: `/actuator/health/liveness`
- Readiness probe: `/actuator/health/readiness`
- Metrics endpoint: `/actuator/prometheus`

## Upgrading

To upgrade the deployment:
```bash
helm upgrade market-data ./market-data -f custom-values.yaml -n market-data
```

## Uninstallation

To remove the deployment:
```bash
helm uninstall market-data -n market-data
```

## Troubleshooting

1. Check pod status:
```bash
kubectl get pods -n market-data
```

2. View pod logs:
```bash
kubectl logs -f deployment/market-data -n market-data
```

3. Check service health:
```bash
kubectl port-forward svc/market-data 8080:8080 -n market-data
curl http://localhost:8080/actuator/health
```
