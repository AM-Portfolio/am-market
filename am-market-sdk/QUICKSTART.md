# AM Market SDK - Quick Integration Guide

## 🎯 Current SDK Status

### Parser API ✅
- **Spec**: `parser-openapi.json` (28 endpoints) - **READY**
- **Java SDK**: Can be generated
- **Flutter SDK**: Can be generated

### Market Data API ⏸️
- **Spec**: Needs extraction from running service
- **Java SDK**: **Already exists** in `market-data-sdk/market-data-sdk-java/`
- **Flutter SDK**: **Already exists** in `market-data-sdk/market-data-sdk-flutter/`

---

## 🚀 Quick Start - Use What's Available

### Option 1: Use Existing Market Data SDK (Recommended for Now)

The existing Market Data SDK is **ready to use**:

**Java (Maven)**:
```xml
<!-- Add to your pom.xml -->
<dependency>
    <groupId>com.am</groupId>
    <artifactId>market-data-rest-client</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/../am-market-sdk/market-data-sdk/market-data-sdk-java/rest-client/target/rest-client-1.0.0.jar</systemPath>
</dependency>
```

**Flutter (pubspec.yaml)**:
```yaml
dependencies:
  market_data_client:
    path: ../am-market-sdk/market-data-sdk/market-data-sdk-flutter
```

### Option 2: Generate Fresh Parser SDK

```powershell
cd am-market-sdk
.\generate_multi_api_sdks.ps1 -ParserOnly
```

This creates:
- `java-sdk/parser-client/`
- `flutter-sdk/parser_client/`

---

## 📦 Understanding the SDK Structure

```
am-market-sdk/
│
├── parser-openapi.json          # ✅ Parser API spec (ready)
├── market-data-openapi.json     # ⏳ Needs extraction
│
├── market-data-sdk/              # 🟢 EXISTING & WORKING
│   ├── market-data-sdk-java/
│   │   ├── rest-client/          # REST API client
│   │   └── websocket-client/     # WebSocket client
│   └── market-data-sdk-flutter/  # Flutter package
│
├── java-sdk/                     # 🆕 NEW structure (when generated)
│   ├── parser-client/
│   └── market-client/
│
└── flutter-sdk/                  # 🆕 NEW structure (when generated)
    ├── parser_client/
    └── market_data_client/
```

---

## 🔧 Extract Market Data OpenAPI Spec (When Service is Running)

### Method 1: From Running Service
```powershell
# If your market-data service is already running on port 8020:
curl http://localhost:8020/v3/api-docs -o market-data-openapi.json
```

### Method 2: From Docker
```powershell
# If running in Docker via Traefik:
curl https://am.munish.org/market/v3/api-docs -o market-data-openapi.json
```

### Method 3: Start Service Temporarily
```powershell
cd am-market-data/market-data-app

# Ensure dependencies are running:
# - MongoDB (port 27017)
# - Redis (port 6379)
# - Kafka (port 9092) [optional]

# Start service
mvn spring-boot:run

# In another terminal, once started:
curl http://localhost:8020/v3/api-docs > ../../am-market-sdk/market-data-openapi.json
```

---

## 💻 Usage Examples

### Java - Market Data API (Existing SDK)

```java
import com.am.marketdata.api.MarketDataApi;
import com.am.marketdata.client.ApiClient;

ApiClient client = new ApiClient();
client.setBasePath("http://localhost:8020");

MarketDataApi api = new MarketDataApi(client);
var quote = api.getOHLC("RELIANCE", "1D", false);
```

### Flutter - Market Data API (Existing SDK)

```dart
import 'package:market_data_client/api.dart';

final api = MarketDataApi();
final quote = await api.getOHLC(
  symbol: 'RELIANCE',
  timeFrame: '1D',
  forceRefresh: false,
);
```

### Java - Parser API (Generate First)

```java
import com.am.portfolio.parser.api.*;
import com.am.portfolio.parser.model.*;

ApiClient client = new ApiClient();
client.setBasePath("http://localhost:8000");

DefaultApi parserApi = new DefaultApi(client);
// Use parser endpoints
```

---

## 🛠️ Troubleshooting

### Q: Market Data service won't start?

**A**: Check dependencies:
```powershell
# Start MongoDB
docker run -d -p 27017:27017 mongo:latest

# Start Redis
docker run -d -p 6379:6379 redis:latest

# Then retry service start
```

### Q: Build fails with test errors?

**A**: Skip tests:
```powershell
mvn install -DskipTests
```

### Q: Where is the OpenAPI spec in the JAR?

**A**: If service is deployed, access at:
- Local: `http://localhost:8020/v3/api-docs`
- Swagger UI: `http://localhost:8020/swagger-ui.html`

---

## 📋 Next Steps

1. **Immediate**: Use existing Market Data SDK from `market-data-sdk/`
2. **Parser API**: Generate SDK with `generate_multi_api_sdks.ps1 -ParserOnly`
3. **Later**: Extract Market Data spec and regenerate for consistency

---

## 📞 API Endpoints

- **Parser API**: `http://localhost:8000` ([Docs](http://localhost:8000/docs))
- **Market Data API**: `http://localhost:8020` ([Swagger](http://localhost:8020/swagger-ui.html))
- **Production**: Via Traefik at `https://am.munish.org/`

---

**Status**: ✅ Parser SDK Ready | 🟢 Market Data SDK Already Available  
**Last Updated**: 2026-01-08
