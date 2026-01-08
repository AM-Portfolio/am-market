# Multi-API SDK Integration Guide

## 📦 Overview

This repository contains SDKs for accessing **AM Portfolio APIs**:
- **Parser API** (FastAPI) - Document parsing and portfolio analysis
- **Market Data API** (Spring Boot) - Real-time market data and analytics

---

## 🎯 Current Status

| API | OpenAPI Spec | Java SDK | Flutter SDK |
|-----|--------------|----------|-------------|
| **Parser API** | ✅ Available | ⏳ Ready to Generate | ⏳ Ready to Generate |
| **Market Data API** | ⏳ Pending | ✅ Existing (legacy) | ✅ Existing (legacy) |

**Note**: Market Data SDK exists in `market-data-sdk/` but is from the old structure. Parser API SDK can be generated fresh.

---

## 🚀 Quick Start

### 1. Extract OpenAPI Specifications

#### Parser API (Python/FastAPI)
```powershell
cd am-market-sdk
python scripts/extract_parser_openapi.py
```
✅ **Output**: `parser-openapi.json` (28 endpoints)

#### Market Data API  (Java/Spring Boot)
```powershell
cd am-market-sdk  
powershell scripts/extract_market_openapi.ps1
```
⚠️ **Requires**: Market Data service running OR manual extraction

**Alternative - Manual Extraction**:
1. Start market-data-app: `mvn spring-boot:run`
2. Once running: `curl http://localhost:8020/v3/api-docs > market-data-openapi.json`

---

## 📚 SDK Generation

### Option A: Generate All SDKs
```powershell
.\generate_multi_api_sdks.ps1
```

### Option B: Generate Parser SDK Only
```powershell
.\generate_multi_api_sdks.ps1 -ParserOnly
```

### Option C: Skip Java or Flutter
```powershell
.\generate_multi_api_sdks.ps1 -SkipJava    # Flutter only
.\generate_multi_api_sdks.ps1 -SkipFlutter # Java only
```

---

## 📂 Generated SDK Structure

```
am-market-sdk/
├── parser-openapi.json           # Parser API spec
├── market-data-openapi.json      # Market Data API spec (when available)
│
├── java-sdk/                     # Java/Maven clients
│   ├── parser-client/            # Parser API Java client
│   │   ├── pom.xml
│   │   └── src/
│   └── market-client/            # Market Data Java client
│       ├── pom.xml
│       └── src/
│
└── flutter-sdk/                  # Flutter/Dart clients
    ├── parser_client/            # Parser API Flutter package
    │   ├── pubspec.yaml
    │   └── lib/
    └── market_data_client/       # Market Data Flutter package
        ├── pubspec.yaml
        └── lib/
```

---

## 💻 Usage Examples

### Java SDK

**Maven Dependency** (after local install):
```xml
<dependency>
    <groupId>com.am.portfolio</groupId>
    <artifactId>parser-client</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>com.am.marketdata</groupId>
    <artifactId>market-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Usage**:
```java
import com.am.portfolio.parser.api.*;
import com.am.portfolio.parser.model.*;

// Configure client
ApiClient client = new ApiClient();
client.setBasePath("http://localhost:8000");

// Use Parser API
DefaultApi parserApi = new DefaultApi(client);
```

### Flutter SDK

**pubspec.yaml**:
```yaml
dependencies:
  parser_client:
    path: ../am-market-sdk/flutter-sdk/parser_client
  market_data_client:
    path: ../am-market-sdk/flutter-sdk/market_data_client
```

**Usage**:
```dart
import 'package:parser_client/api.dart';
import 'package:market_data_client/api.dart';

// Use Parser API
final parserApi = DefaultApi();

// Use Market Data API
final marketApi = MarketDataApi();
```

---

## 🔄 Existing Market Data SDK

The legacy Market Data SDK is located in `market-data-sdk/`:
- **Java**: `market-data-sdk/market-data-sdk-java/`
  - `rest-client/` - REST API client
  - `websocket-client/` - WebSocket client
- **Flutter**: `market-data-sdk/market-data-sdk-flutter/`

**Migration Path**:
1. Generate fresh SDK using `generate_multi_api_sdks.ps1`
2. Compare with legacy implementation
3. Migrate custom WebSocket logic if needed
4. Deprecate old structure

---

## 🛠️ Troubleshooting

### "Market Data service won't start"

**Issue**: `extract_market_openapi.ps1` times out

**Solutions**:
1. **Check dependencies**: MongoDB, Redis, Kafka must be running
2. **Manual start**: Run in separate terminal: `cd am-market-data/market-data-app && mvn spring-boot:run`
3. **Use existing**: If service is already deployed, curl the endpoint directly

### "OpenAPI Generator errors"

**Issue**: Generation fails with parameter errors

**Solution**: Check OpenAPI spec validity:
```powershell
npx @openapitools/openapi-generator-cli validate -i parser-openapi.json
```

---

## 📋 Next Steps

1. ✅ **Parser API**: Spec extracted & SDKs can be generated
2. ⏳ **Market Data API**: Extract spec when service is available
3. ⏳ **Integration**: Update client apps to use new SDKs
4. ⏳ **Testing**: Verify SDK functionality end-to-end
5. ⏳ **Documentation**: Add API usage examples

---

## 📞 Support

- **Parser API Docs**: `http://localhost:8000/docs` (when running)
- **Market Data API Docs**: `http://localhost:8020/v3/api-docs` (when running)
- **OpenAPI Generator**: https://openapi-generator.tech/

---

**Last Updated**: 2026-01-08  
**Status**: ✅ Parser SDK Ready | ⏳ Market Data Pending Service Start
