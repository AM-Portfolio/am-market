# Market Price Integrity, Index Performance, and Top Movers Fix

> **Document Status:** Aggregated & Active  
> **Target Branch:** `fix/market-price-integrity`  
> **Latest Deployed Image Tag:** `38220022089` (Live on Dev: `am-dev-apps`)  
> **Current Phase:** Phase 1 (Backend Integrity & Review) Complete; Phase 2 (Data Seeding & Verification) In Progress.

---

## 1. Executive Summary & Goals

### 1.1 Goal
Ensure the Market UI displays **one single, authoritative, accurate price and return calculation** for every selected timeframe across all widgets (top index cards, charts, heatmaps, and Top Movers).
- Eliminate duplicate calculations between client and server.
- Prevent `0.00%` and `-100%` corrupted placeholders when markets are closed or when quotes are cold.
- Ensure Top Movers and Heatmap gracefully handle incomplete data without crashing or degrading into slow per-member network calls.

### 1.2 Retained Architecture Principles
- **Backend Single Source of Truth:** `am-market-data` calculates performance; Market UI renders it without recalculating.
- **Strict Backward Compatibility:** Public contracts (`/v1/analysis/movers`, `/v1/analysis/heatmap`, `/v1/indices/batch`, `/v1/analysis/historical-charts`) keep exact JSON keys, array structures, and query parameters.
- **Fail-Safe Integrity:** When fewer than required members have valid prices (e.g. `<45` out of 50 for NIFTY 50), Movers safely returns `{"gainers":[],"losers":[]}` rather than publishing corrupt or partial rankings.
- **Production Safety:** All live cluster mutations require explicit review; secrets, tokens, and credentials remain in Vault and are never hardcoded or printed to logs.

---

## 2. Root Cause & Verified Architecture Findings

### 2.1 The Multi-Calculation Discrepancy
Previously, the same percentage return was calculated independently by three separate subsystems from different, out-of-sync data sources:
```
Chart:      GET /v1/analysis/historical-charts → Historical candles → Chart calculates return
Top Cards:  GET /v1/indices/batch + /v1/market-data/historical-data → UI calculates return
Top Movers: GET /v1/analysis/movers → Scrapes/Redis quotes → Analytics calculates return
```
- **Timeframe Mismatches:** On a `1M` selection, the chart could show `-5.14%` while the top card showed `-4.64%` because the chart used session candle endpoints while the card calculated against an unaligned historical close.
- **Cold-Cache Movers Failure:** Top Movers attempted serial Upstox historical provider calls when cache missed, leading to 25+ second requests or 30-second ingress timeouts.
- **Previous-Close Corruption:** Closed-market quotes previously overwrote yesterday's close with today's LTP, producing artificial `0.00` changes and `0.00%` returns after market close.

### 2.2 Ingestion & Environment Divergence (Dev vs. Prod)
Investigation of cluster topology and Helm values revealed why Dev returned empty movers while Prod worked:

| Environment | Helm Config | Ingestion Worker | Redis / Mongo Data | Movers Behavior |
| :--- | :--- | :--- | :--- | :--- |
| **Prod** (`am-apps-prod`) | `values.prod.yaml`: `scheduler.ingestion.enabled=true` | Streams live Upstox WebSocket (09:15–15:30 IST) | Fully populated (50/50 NIFTY 50 quotes present) | **Returns full Top Movers** (TRENT +11.31%, HDFCLIFE +6.80%, etc.) |
| **Dev** (`am-apps-dev`) | `values.dev.yaml`: `scheduler.ingestion.enabled=false` | Disabled to protect vendor quotas | Empty (0 cached quotes) | **Returns `{"gainers":[],"losers":[]}`** safely (`validPrices=0 < 45`) |

---

## 3. Implemented Components (PR #64 & Follow-ups)

The following core modules were implemented, reviewed, tested, and pushed to `fix/market-price-integrity`:

### 3.1 `SmartStockService` — Coalesced Quote Recovery & Baseline Integrity
- **Multi-Key Atomic Redis Leases:** Replaced single-lock bottlenecks with atomic Lua scripts (`ACQUIRE_REFRESH_LOCKS` and `RELEASE_REFRESH_LOCKS`). Claimed leases per-instrument; overlapping cross-pod requests now recover disjoint remainder sets without duplicate provider calls.
- **Identity-Based Upstox Key Resolution:** Built provider keys from verified ticker and ISIN (`NSE_EQ|<ISIN>`) rather than sending bare tickers into Upstox, resolving the `UDAPI1021` provider rejection.
- **Date-Aware Previous Close:** Accepted verified session baselines even when the stock is flat/unchanged (`Double.compare(prevClose, lastPrice) == 0`). Decoupled previous close from assumptions about today's candle.

### 3.2 `AnalysisService` — Heatmap Quality & Bounded Execution
- **Pre-Cache Roster Validation:** Evaluated roster size against `minimumMembershipSize(symbol)` before caching.
- **85% Coverage Threshold:** Tolerates missing candles (e.g. suspended stocks) only if at least 85% and `>= minimumRequired` constituents are valid. Incomplete or degenerate all-zero heatmaps are never persisted to Redis.
- **Preserved Prior Cache:** On temporary source gaps or partial refreshes, prior valid cache entries are retained rather than erased.

### 3.3 `StockIndicesService` & `MarketDataProcessingService` — Membership Reliability
- **Standalone Benchmark Handling:** Explicitly exempted standalone indexes like `INDIA VIX` from constituent roster repair requirements.
- **Duplicate Member Normalization:** Safely normalized duplicate ticker rows from scraper feeds while strictly rejecting conflicting ISINs/identities.
- **Index Minimums Enforced:**
  - `NIFTY 50` / `NIFTY 50 USD` / `NIFTY BANK`: `>= 45` / `>= 10`
  - `NIFTY 100` / `NIFTY 200` / `NIFTY 500`: `>= 90` / `>= 180` / `>= 450`

### 3.4 `MarketDataAdminController` & Runtime Infrastructure
- **Managed Background Execution:** Replaced unmanaged daemon `new Thread(...)` calls with Spring's managed `marketDataTaskExecutor` (`submitAdminTask`), providing graceful shutdown and queue backpressure.
- **Local Runner Portability:** Updated `run_local.py` to auto-detect Maven wrappers and native Maven binaries across platforms with opt-in offline mode.
- **InfluxDB URL Clarity:** Replaced brittle default expressions with explicit URL configuration and fail-fast validation.

---

## 4. Current Deployment & Verification State

### 4.1 Build & Image Status
- **GitHub Actions Run:** [Run #38220022089](https://github.com/AM-Portfolio/am-market/actions/runs/38220022089)
- **Built Image Tag:** `ghcr.io/am-portfolio/am-market/am-market-data:38220022089`
- **Build Status:** Succeeded (All 13 Maven modules, unit tests, secret scans, and container layers passed).

### 4.2 Live Dev Cluster State (`am-dev-apps`)
- **Pod:** `am-market-data-dev-5cdc6d59c7-sk2p8`
- **Node:** `am-dev-apps-worker2` (`10.244.2.100`)
- **Container Image:** `ghcr.io/am-portfolio/am-market/am-market-data:38220022089`
- **Status:** **`1/1 Running`** (Restarts: 0)
- **Health Check:** `HTTP 200 UP` (`https://am-dev.asrax.in/market/actuator/health`)

---

## 5. Standardized Timeframe Calculation Contract

Before UI alignment, the approved `1M` and multi-timeframe rules are defined as follows:

```
base price     = close of the approved start trading session
end price      = latest valid price during market hours, otherwise last completed close
change         = end price - base price
change percent = (change / base price) × 100
```

| Parameter | Canonical Rule |
| :--- | :--- |
| **Timezone** | `Asia/Kolkata` (IST) |
| **`1D`** | End Price = Live LTP (or today's official close). Base = Yesterday's verified completed session close. |
| **`1M`** | Rolling 1 calendar month (e.g. Oct 12 → Sept 12). If Sept 12 is a weekend/holiday, use the nearest *preceding* completed trading session. |
| **`3M` / `6M` / `1Y` / `5Y`** | Rolling calendar duration backwards to nearest preceding trading session. |
| **Unchanged Stock** | If `lastPrice == previousClose`, change is `0.00` and percentage is `0.00%` (valid flat trading day, not an error). |

---

## 6. Execution Roadmap & Next Steps

```mermaid
flowchart TD
    subgraph Phase 1: Core Backend Integrity [COMPLETED]
        P1A["PR #64 Core Implementation"] --> P1B["Multi-key Redis Leases & Ticker/ISIN Recovery"]
        P1B --> P1C["Unit & Regression Tests (100% Passed)"]
        P1C --> P1D["CI Build & Tag 38220022089 Pushed"]
        P1D --> P1E["Deployed Live to Dev Cluster"]
    end

    subgraph Phase 2: Data Seeding & Dev Ingestion [IN PROGRESS]
        P2A["Verify Ingestion Difference (Dev vs Prod)"] --> P2B["Seed Dev Redis with NIFTY 50 Quotes"]
        P2B --> P2C["Verify Dev Movers Endpoint Returns Live Ranks"]
    end

    subgraph Phase 3: Contract & Latency Verification [PENDING]
        P3A["Postman MCP Collection Run"] --> P3B["Full Tempo Trace Audit (7 Span Groups)"]
        P3B --> P3C["Preprod Deployment Approval"]
    end

    subgraph Phase 4: UI Single Source Alignment [PLANNED]
        P4A["Market UI Timeframe Coordinator"] --> P4B["Eliminate Duplicate Client-Side Return Calculations"]
        P4B --> P4C["End-to-End Visual Verification"]
    end

    Phase 1 --> Phase 2
    Phase 2 --> Phase 3
    Phase 3 --> Phase 4
```

### Action Items for Current & Next Phases

1. **Phase 2 (Dev Environment Quote Seeding):**
   - Populate Dev Redis (`redis.asrax.in`) with current NIFTY 50 quote hashes and previous close keys (`market:latest-price:*`, `market:prev-close:*`).
   - Run verification `curl` against `https://am-dev.asrax.in/market/v1/analysis/movers?type=all&limit=10&indexSymbol=NIFTY%2050` to confirm live gainers/losers populate.

2. **Phase 3 (Preprod & Testing Gates):**
   - Execute Postman collection tests against `/v1/analysis/movers` and `/v1/analysis/heatmap`.
   - Audit Tempo distributed traces for cold vs warm runs to ensure zero synchronous historical loops.

3. **Phase 4 (Market UI / Modern UI):**
   - Update `user_dashboard_page.dart` and `market_provider.dart` to rely solely on the server's authoritative performance payload.
   - Remove redundant client-side historical base-price calculations from `index_card.dart`.

4. **Phase 5 (Optional Quote Details & Depth - Future Additive Slice):**
   - Extend `OHLCQuote` additively with optional order book depth (`price`, `quantity`, `orders`).
   - Retain ephemeral depth in Redis only; do not write order books into historical InfluxDB or MongoDB.

---

## 7. Acceptance & Verification Matrix

| ID | Scenario | Required Behavior | Status |
| :--- | :--- | :--- | :--- |
| **MR-01** | Warm Redis cache with valid quotes | Movers returns ranking with 0 provider calls. | **PASS** (Verified locally & Prod) |
| **MR-02** | Cold cache with missing LTP | Single coalesced batch LTP recovery under Redis lease; no per-symbol loops. | **PASS** (Unit & Integration tests) |
| **MR-03** | Roster coverage `< minimumMembers` (e.g. Dev has 0 quotes) | Safely return `{"gainers":[],"losers":[]}`; do not serve corrupt 0% rankings. | **PASS** (Verified on live Dev cluster) |
| **MR-04** | Stock is unchanged (`LTP == prevClose`) | Accepted as legitimate `0.00%` return; not rejected as invalid baseline. | **PASS** (Verified in test suite) |
| **MR-05** | Standalone index (INDIA VIX) | Bypasses constituent repair; valid document preserved. | **PASS** (Verified in test suite) |
| **HM-01** | Broad index Heatmap (NIFTY 500) | No serial historical provider loop; returns within ingress deadline. | **PASS** (Verified in test suite) |
| **HM-02** | Degenerate all-zero heatmap | Guard rejects caching; triggers background refresh. | **PASS** (Verified in test suite) |
| **HM-03** | Partial Heatmap with `<85%` valid cells | Discards incomplete result; preserves previous validated cache. | **PASS** (Verified in test suite) |
| **DEP-01**| Image Build & Push | Automated CI packaging and GHCR push with all scans green. | **PASS** (`Tag 38220022089`) |
| **DEP-02**| Live Dev Rollout | ArgoCD rollout to Contabo dev cluster with zero crash loops. | **PASS** (Running on `worker2`) |
| **UI-01** | Timeframe switch 1D → 1M | Card and chart display identical return percentage and as-of baseline. | **PENDING** (Phase 4) |
| **TRC-01**| Tempo trace latency audit | Zero synchronous historical provider spans on interactive requests. | **PENDING** (Phase 3) |
