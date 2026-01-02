# Market Data Service

A modular Spring Boot application for handling market data processing, distribution, and analysis with resilient error handling, comprehensive metrics collection, and configurable deployment options.

## Architecture Overview

The Market Data Service is built using a modular architecture that separates concerns into specialized modules. The system follows an event-driven design pattern with resilient error handling, comprehensive metrics collection, and configurable deployment options.

## Module Structure

- **market-data-api**: REST/gRPC endpoints, controllers, DTOs, and OpenAPI configuration for external service interfaces
- **market-data-app**: Main application entry point that integrates all modules and provides the runtime environment
- **market-data-common**: Shared domain models, utilities, and configurations used across all modules
- **market-data-external-api**: Auto-configurable external API architecture with resilience patterns (retry, circuit breaker)
- **market-data-kafka**: Kafka integration for event publishing and consuming market data messages
- **market-data-processor**: Generic market data processing using Template Method pattern with event-based architecture
- **market-data-scheduler**: Scheduling components for market data collection aligned with market hours
- **market-data-scraper**: Data collection from external sources with retry mechanisms and cookie management
- **market-data-service**: Core business logic, cache layer, and service implementations

## Key Features

- **Resilient Error Handling**: Comprehensive retry mechanisms with exponential backoff
- **Partial Success Strategy**: Continues processing valid data even if some API calls fail
- **Metrics Collection**: Detailed performance and operational metrics for monitoring
- **Market Hours Alignment**: Scheduling aligned with trading hours (9:15 AM - 3:35 PM IST, weekdays only)
- **Configurable Architecture**: Environment-specific configurations with profile support
- **Monitoring Integration**: Prometheus and Grafana dashboards for real-time monitoring
- **Secure Cookie Management**: Cookie masking and secure handling of sensitive data
- **Event-Driven Design**: Loose coupling through event publishing and subscription
- **Docker Deployment**: Multi-stage Docker builds with optimized layer caching

## Technology Stack

- Java 17
- Spring Boot 3.2.0
- Apache Kafka
- Redis
- PostgreSQL
- Prometheus & Grafana
- Docker & Docker Compose
- Resilience4j
- Micrometer
- OpenAPI/Swagger
- Maven

## Module Details

### market-data-api

API layer providing REST and gRPC endpoints for external consumption of market data. Features include:

- OpenAPI/Swagger documentation
- DTO models for API requests/responses
- API versioning and backward compatibility
- Redis configuration for caching
- Controller implementations

### market-data-app

Main application module that bootstraps the entire system:

- Application entry point
- Module integration
- Runtime configuration
- Dependency management

### market-data-common

Common utilities and models shared across all modules:

- Domain models (StockIndices, MarketData, MetaData, ETFIndices)
- Utility classes
- Common configurations
- Shared constants
- Margin calculation models

### market-data-external-api

Manages external API integrations with resilient patterns:

- ExternalApiModuleConfig for auto-configuration
- ExternalApiAutoConfiguration for Spring Boot integration
- Resilience patterns (retry, circuit breaker)
- Centralized configuration management
- API client architecture

### market-data-kafka

Kafka integration for event publishing and consumption:

- Event publishers
- Message consumers
- Topic configuration
- Serialization/deserialization
- Error handling for Kafka operations

### market-data-processor

Generic market data processing using Template Method pattern:

- AbstractMarketDataProcessor as base template
- Concrete processors for different data types (ETF, Indices)
- Event-based architecture with ApplicationEventPublisher
- Comprehensive validation and transformation logic

### market-data-scheduler

Scheduling components for market data collection:

- Staggered timing for schedulers
- Market hours alignment (9:15 AM - 3:35 PM IST)
- Weekday-only processing
- Configurable cron expressions

### market-data-scraper

Data collection from external sources:

- NSE API client with retry mechanism
- Cookie management and refresh
- Detailed error handling and logging
- Metrics collection for API operations
- Parallel data fetching with CompletableFuture

### market-data-service

Core business logic and service implementations:

- Market data processing services
- Margin calculation services
- Cache management
- Transaction handling
- Thread pool management

## Monitoring

The system includes a comprehensive monitoring stack:

- Prometheus for metrics collection
- Grafana for visualization
- Custom dashboards for:
  - Market Data Processing Time
  - Success/Failure Rates
  - API Response Time
  - Error Tracking

## Configuration

Multi-profile configuration support:

- Common configuration in application-common.yml
- Profile-specific overrides:
  - application-dev.yml (development)
  - application-preprod.yml (pre-production)
  - application-prod.yml (production)
- Docker-specific configuration

## Building and Running

```bash
mvn clean install
```

## Running the Application

```bash
cd market-data-service
mvn spring-boot:run
```

The API documentation will be available at: http://localhost:8080/swagger-ui.html
