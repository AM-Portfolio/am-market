# Market Price Integrity, Index Performance, and Top Movers Fix

## Global-index integrity addendum — implementation in progress (2026-10-25)

**Kind / owner / target:** fix; `am-market-data` in `am-market` is lead, with existing Market UI packages in `am-modern-ui`; local implementation and tests first, preprod validation before any production request. Current checkout is `hotfix/hypercare-R6`; this addendum continues this price-integrity pack, rather than creating a second service or changing `/v1/indices/batch` JSON. The user has authorized implementation; no deployment, database reconciliation, or production mutation is authorized.

**Implementation update:** the code now uses one schedule-backed mapping for the eight exact Upstox candidates and fails closed for unmatched IXIC/SSEC. Source event timestamps, market-session-aware freshness, owner-safe cache writes, configured global subscriptions, opt-in Mongo config reconciliation, and honest UI pending/last-confirmed states are implemented. The full 13-module app compile passed and all seven focused backend freshness/session tests passed; UI model/widget tests and targeted analyzer checks passed earlier in this work. Still outstanding: identify entitled sources for IXIC/SSEC, run the opt-in reconciliation only in a reviewed non-production environment, historical exact-key bootstrap, full regressions, and preprod/Postman proof. No API contract was changed. Cold REST fan-out and synchronous Influx persistence remain known latency risks, not solved by this slice.

**Source-plan pointer and coverage:** the conversation's earlier proposal was “correct Upstox keys, use another source for IXIC/SSEC, reject stale prices, keep the response contract.” Verdict: **partial**. It omitted source identity/licensing, event-time versus receipt-time, market-calendar freshness, cold starts, historical bootstrap, migration of existing Mongo/Redis/Influx keys, non-global regression, and provider latency differences.

**Catalog and context (local):** the Market domain catalog maps `am-market-data` to the `am-market` monorepo and `am_market/{common,ui}` to `am-modern-ui`. Read `am-market/README.md`, `am-market/am-market-data/README.md`, `am-market/am-market-data/.am.yaml`, `am-modern-ui/README.md`, `am-modern-ui/ARCHITECTURE.md`, and `am-modern-ui/am_market/ui/.am.yaml`. The backend is the existing Spring Boot service on 8092; the UI is the existing Flutter monorepo. The nested UI README is a placeholder, so real Dart call sites are authoritative. No new deployable is proposed.

### Repro and verified current system

- Dev's `global_index_config` has ten symbols with old-style `GLOBAL_INDEX|...` keys. `GlobalIndexSeeder` seeds only when the collection is empty, so editing defaults alone cannot repair deployed records (`market-data-service/.../GlobalIndexSeeder.java`).
- On the 2026-10-24 official Upstox global instrument file, exact corresponding entries exist for DJI→`GLOBAL_INDEX|^DJI`, SPX→`GLOBAL_INDEX|^GSPC`, FTSE→`GLOBAL_INDEX|^FTSE`, GDAXI→`GLOBAL_INDEX|^GDAXI`, FCHI→`GLOBAL_INDEX|^FCHI`, N225→`GLOBAL_INDEX|^N225`, HSI→`GLOBAL_INDEX|^HSI`, and GIFTNIFTY→`GLOBAL_INDEX|SGX NIFTY`. No exact IXIC (Nasdaq Composite) or SSEC (Shanghai Composite) entry exists. `GLOBAL_INDEX|IXIX` is labelled US Tech 100 and is not a safe IXIC substitution. Catalog: https://assets.upstox.com/market-quote/instruments/exchange/global.json.gz ; API support: https://upstox.com/developer/api-documentation/announcements/global-instruments/ . Catalog presence is **not** an authenticated quote/feed test.
- `GlobalIndexService` reads Redis, tries a per-key REST refresh after a fixed five minutes, then returns stale Redis or an old Mongo `StockIndicesMarketData` even when provider retrieval failed. It does not set `metadata.change` in the Redis response and does not validate the Mongo snapshot's source trading date. `market-data-api/.../GlobalIndexService.java`.
- `StreamerManager` initially has an empty subscription set; its `@PostConstruct` is disabled. The global-market vote can connect the streamer before `refreshSubscriptions` has populated keys. Its Redis timestamp is receipt time, not necessarily the provider's quote time. `market-data-service/.../websocket/service/StreamerManager.java`.
- Upstox REST mapping derives `quoteTradeDate` in `Asia/Kolkata` for every market; US/Asian exchange session identity can differ near midnight. WebSocket global ticks are serialized by a newly constructed `ObjectMapper`, so Java-time fields need a verified round-trip. `market-data-provider/.../UpstoxMarketDataProvider.java` and `StreamerManager.java`.
- `GlobalIndexCacheWriter` uses `Instant.now()` for Influx ticks and looks up a display name by stripping the provider key suffix; caret-prefixed keys will not match the stable public symbol. `GlobalIndexHistoricalSyncService` incorrectly asserts that Upstox has no global history, while `GlobalIndexGapFillService` skips symbols with no prior Influx point. `market-data-service/.../global/`.
- `GlobalIndexService` walks ten indices one by one, and `GlobalIndexCacheWriter.writeTick` calls Influx synchronously from the streaming path. A cold dashboard or slow Influx may therefore delay responses or tick processing. The configured GIFT trading window ends at 23:30 IST whereas the provider catalog describes an overnight end; Jollyday national holidays are not an exchange-specific trading calendar. These must be measured/verified, not assumed correct.
- The UI parser defaults absent numeric fields to zero and the index card does not honor `suspended`, so missing change may appear as `+0.00`. `am-modern-ui/am_market/common/lib/models/market_data.dart` and `am-modern-ui/am_market/ui/lib/features/market/widgets/index_card.dart`.

**Expected:** each of the ten public symbols represents the named benchmark; current/last-close price and 1D change are from the same verified trading session, or the UI does not present them as current. Historical timeframes have real, identity-matched candles. **Actual:** wrong provider identifiers cause missing quotes; fallback can present 2024 values as current, with zero change. The audit insert time is not proof that the quote itself is recent.

### Reviewed design and decision gates

1. **Identity first.** Keep public symbols and `/v1/indices/batch` request/response shape unchanged. Build a reviewed symbol→provider-key matrix against the daily provider instrument catalog. Do not silently map Nasdaq Composite to US Tech 100, Shanghai Composite to another China index, or Dow spot to US 30 futures. Missing/renamed provider entries fail a startup/health validation for that *symbol*, not for Indian-market service startup.
2. **Close the two-source gap before declaring 10/10.** Select and validate an entitled provider for the exact IXIC and SSEC benchmarks, including live/delayed quote, prior close, timestamp, daily history, public redistribution permission, limits, and failure behavior. Reuse the existing provider abstraction; do not add a public endpoint. This is an owner decision because cost/licence and source latency are unknown. Upstox remains preferred for the eight exact matches. No fabricated or proxy prices.
3. **Safe data migration.** In a reversible, audited operation, update the ten existing Mongo config records only after mapping approval; do not drop/reseed the collection. Invalidate only superseded global Redis keys. Preserve old Influx points for audit; backfill verified candles under canonical new provider keys and public symbol mapping rather than rewriting provenance blindly. Keep config, schedule YAML, cache, historical and UI symbol identity aligned.
4. **Source-time freshness and session rules.** Capture provider event/trade time when supplied; keep receipt time separately. Derive trade date in the configured exchange timezone, not always IST. Reject non-finite/non-positive prices, future timestamps outside allowed clock skew, out-of-order ticks, and a current value older than the latest completed exchange session. Verify actual exchange calendars, special closures, and cross-midnight GIFT hours; national holidays alone are insufficient. Use provider-published latency (20/120/900 seconds) instead of the global fixed five-minute rule. A closed-market last close is valid *only* with a verified session; it must not be tagged as live. Missing source timestamp is not silently replaced by `now()` for integrity decisions.
5. **Bounded recovery.** Populate subscriptions on startup and config refresh even in global-only hours. Coalesce missing Upstox symbols into a bounded batch rather than making ten sequential provider calls. Validate source time before caching success; failure cannot bless stale Redis/Mongo as current. Expired lock release must be owner-safe. Keep Influx persistence off the blocking WebSocket callback or bound its failure time so Indian ticks are not stalled. Verify Java-time Redis serialization. Use an existing verified last-close snapshot as a clearly non-live value if the current contract/UI can identify it; otherwise show a non-numeric pending state, never a false price. The UI may use the existing `suspended` field; no JSON field removal or type change.
6. **History.** For Upstox's eight supported global instruments, use its documented historical-candle API to bootstrap absent Influx history and fill gaps; verify interval semantics, date boundaries, provider retention/limits, and previous trading-session baseline. The selected alternate provider must pass equivalent checks for IXIC/SSEC. Never compute daily change from a prior minute's close. Cache by canonical public symbol + provider identity + interval + date window so a provider switch does not cross-contaminate history.
7. **UI and observability.** Calculate `change = last - verified previousClose` and percent from the same quote. The index card distinguishes current/verified last close/pending using existing fields; no artificial `+0.00` on missing data. Log public symbol, provider, hashed/approved key identifier, source-time age, cache path, rejection reason, and subscription result without tokens or raw sensitive payloads. Preserve Indian market paths.

**Latency/consistency:** Upstox itself advertises 20-, 120-, or 900-second index latencies. A UI request should use a validated warm snapshot immediately; a cold refresh is bounded and coalesced, with no invented p99 promise until preprod traces. During provider timeout the user sees a verified last close only if session identity is known; otherwise pending, not a 2024 value. Historical bootstrap runs outside the interactive request.

### Edge cases and executable proof

| Case | Expected | Test-plan proof |
|---|---|---|
| All ten mappings, source rename/removal | Exact benchmark or symbol-specific degraded state; no proxy substitution | T1 catalog identity/unit matrix + signed source mapping |
| Provider quote missing/zero/NaN/old/future | Reject as current; no zero or stale display | T2 adapter fixtures and batch API assertions |
| Closed session, holiday, DST, GIFT overnight | Last completed *local-exchange* session wins; no five-minute live assumption | T3 timezone/calendar fixtures |
| US quote near IST midnight; special exchange closure | Source trade date follows venue calendar, not server/IST date | T3 source-time adapter fixtures |
| 900-second Upstox instrument | No needless refresh loop; published latency respected | T4 fake-clock + provider-call-count test |
| Cold startup during global-only hours | Correct subscriptions; first valid tick reaches Redis and Influx | T5 isolated streamer integration test |
| WebSocket quote has Java-time fields; Influx times out | Redis round-trip succeeds; Indian streaming is not blocked by global persistence | T5 serialization + bounded-write test |
| Redis missing/unavailable, provider timeout, concurrent requests | Bounded one-owner recovery; no old cache promoted | T6 failure/concurrency tests + trace |
| Ten cold index cards load together | Coalesced provider requests stay within documented limits; response does not serialize ten slow calls | T6 call-count + latency trace |
| Mongo record recently inserted with 2024 quote | Audit timestamp cannot prove freshness; reject as current | T7 seeded legacy-document test |
| Provider switches/key changes with old Influx history | Provenance retained; no mixed-key candle series | T8 migration/backfill test |
| All UI timeframes and missing baseline | Only verified candles/change; no synthetic `0.00` | T9 Postman batch/history matrix + Flutter widget tests |
| Existing Indian indices and strict clients | Unchanged endpoints, JSON types, auth and Indian values | T10 contract snapshots + Indian regression |
| Public display rights absent for alternate source | Do not launch IXIC/SSEC as public licensed quotes | T11 documented licence/entitlement gate |

**Test sequence:** unit T1–T8 and T10; preprod Postman collection `Market Data API` global-index requests plus direct provider probes for each of ten symbols and each advertised timeframe (T2/T9); market-open WebSocket observation, after-close/weekend retry, Redis/Mongo/Influx lineage, UI T9, latency traces, and Indian regression. Two market sessions across distinct timezones are needed before saying live streaming is proven. No production mutation without separate explicit approval and rollback description.

**Change scope (only after approval):** `GlobalIndexSeeder`, `global-market-schedule.yml`, `GlobalIndexService`, `StreamerManager`, `GlobalIndexCacheWriter`, `GlobalIndexHistoricalSyncService`/`GlobalIndexGapFillService`, provider adapters only if IXIC/SSEC source is approved, focused tests, and Market UI model/card/provider only where existing suspended/last-close handling is insufficient. One targeted config-data migration, not a schema rewrite. Do not touch heatmaps, movers, paper trading, gateway routes, or unrelated modules.

**Trade-offs:** a second source adds licence/cost and a separate latency profile; retaining last close is useful but must be visibly non-live; a strict freshness gate may temporarily show pending rather than a false number; historical backfill consumes provider quota, so it is bounded/background. Compatibility is preserved at the endpoint shape but UI rendering must become more honest.

**Review and rating:** the implementation plan rates **8/10**. It addresses identity, stale data, session dates, compatibility and safe rollout boundaries. It is not 10/10 because exact IXIC/SSEC source/licensing, historical bootstrap, full integration regressions, and preprod behavior remain unverified. No claim is made that all ten symbols currently return live data. UI preview: n/a (bug fix; not requested). `REPORT.md` remains gated on completed tests.

---

> **Document Status:** Aggregated & Active  
> **Target Branch:** `fix/market-price-integrity`  
> **Latest Deployed Image Tag:** `38220022089` (Live on Dev: `am-dev-apps`)  
> **Current Phase:** Phase 1 (Backend Integrity & Review) Complete; Phase 2 (Data Seeding & Verification) In Progress.

## Refined implementation slice: Upstox global-index history (2026-10-25)

**Goal:** Enable historical candles for every configured global index whose exact Upstox `GLOBAL_INDEX|...` key is present, without hardcoding an index list or changing any public API request/response contract. IXIC/SSEC stay skipped until an exact key and public-display entitlement are verified.

**Verified code evidence:** `UpstoxMarketDataProvider.getHistoricalData()` resolves configured instrument keys and routes them through `UpstoxSdkService.fetchHistoricalCandleDirect()`, which currently builds V2 historical URLs for all keys (`am-market-data/market-data-provider/.../UpstoxMarketDataProvider.java`, `UpstoxSdkService.java`). Upstox’s current docs explicitly support global indexes on V3 history and intraday endpoints. `GlobalIndexHistoricalSyncService` currently only checks Influx timestamps/status; it does not request historical candles. `GlobalIndexInfluxRepository` already has `saveAll`, but its history query applies `mean` to every OHLC field, which can return incorrect weekly/monthly bars after daily backfill. `HistoricalDataServiceImpl` uses that repository for existing global history responses. No public API DTO or route needs to change.

**Refined design:**

1. Route only keys beginning `GLOBAL_INDEX|` through documented V3 daily/weekly/monthly history paths. Keep the V2 URL construction unchanged for all minute/hour requests and all non-global instruments; current Upstox docs only promise minute-level global intraday candles for the current session, while this history task handles daily bootstrap. Preserve the exact encoded key and bearer-token handling; never log the token.
2. Make the existing admin global sync perform idempotent daily-history bootstrap when a configured symbol is PENDING, or when an operator explicitly uses the existing `force=true`. Keep `force=false` + COMPLETE as a no-op. Set the existing status to IN_PROGRESS before each eligible request and skip a record already marked IN_PROGRESS; return it to PENDING for an empty/unusable response and mark FAILED on exceptions. An interrupted IN_PROGRESS record is recoverable with `force=true`. Process Mongo-configured records sequentially; blank/unverified keys are skipped individually. No startup-wide fetch, new route, schema migration, database purge, or hardcoded symbol/timeframe list.
3. Bound lookback with one property (`market.global.history-backfill-years`, default 10, validated 1–10 to match Upstox’s documented daily limit). Fetch daily bars, sort ascending, deduplicate by provider timestamp, reject malformed/non-finite/non-positive or inconsistent OHLC rows, preserve the source instant in UTC, derive previous close only from the prior returned session, and bulk-save through the existing `global_market_index` repository. Empty/failed fetches must not erase data or mark the sync COMPLETE. Repeated forced runs are safe at the Influx series+timestamp identity, but consume provider quota.
4. Correct global-history resampling to aggregate OHLC by semantics (first open, maximum high, minimum low, last close), not mean. Retain the existing date range, exchange offset, output DTO, and timeframe naming. This is required because daily history and live tick snapshots can coexist in a resampled window.
5. Add focused URL-routing/encoding, candle validation/order/deduplication, sync gating/idempotency, and OHLC aggregation regression tests. Then run focused Maven tests and full market-data regression. Preprod credential-backed provider probe + Postman verification are separate release gates; do not run backfill against any database in this code task, and do not deploy or mutate production.

**Edge-case matrix:**

| Case | Expected behavior | Proof |
|---|---|---|
| Ordinary NSE stock/index historical call | Existing V2 URL and response parsing unchanged | Provider URL routing regression |
| Global key has spaces, caret, or pipe | Single URL encoding; V3 endpoint receives exact identity | URL builder tests |
| Mongo config has blank/unknown key (IXIC/SSEC) | Skip only that symbol; no alias/proxy or false COMPLETE | Sync-service test |
| Provider returns empty, error, malformed candle, NaN/zero, or inverted OHLC | Do not overwrite existing data or mark COMPLETE; structured reason/count log | Validation and sync tests |
| Provider rows are reverse-ordered or duplicate timestamps | Sort/dedupe; deterministic repeated save | Mapper test |
| Timestamps contain timezone offsets / host is non-UTC | Persist original provider instant, not a host-zone-shifted time | Timestamp round-trip test |
| Repeated `force=true` | Upsert same exact key/time points; no deletes; bounded to configured 1–10 years | Repository/sync idempotency test |
| V3 call rate limit/timeout/token expiry | Existing retry/error path; mark that symbol failed/pending, continue later symbols, do not mark complete | Provider and sync failure tests |
| Live ticks overlap a daily provider candle | Same configured identity; resampled OHLC selects first/max/min/last; do not average | Flux aggregation test/integration fixture |
| Provider omits older history for one index | Store only verified rows; do not claim full lookback or complete baseline | Live probe gate |
| Influx unavailable during save | Do not mark COMPLETE; keep existing data; log failure | Mocked repository failure test |

**Trade-offs / exclusions:** single-symbol history requests are sequential and therefore bounded but slower than fan-out; repeated or simultaneous explicit force refreshes are idempotent at Influx identity but may consume duplicate API quota. Daily bars restore multi-day/week/month/year chart history but do not create historical minute candles (Upstox documents intraday candles as current-trading-day data). This slice does not guarantee IXIC/SSEC, grant redistribution rights, run the pending Mongo reconciliation, execute a backfill, alter UI/API contracts, or deploy. Existing global tick ingest remains the source for intraday history.

**Review pass 1:** 7/10. Gaps found: the prior plan said “use the documented historical API” without identifying that our adapter calls V2; it did not protect non-global V2 callers; it missed bad OHLC averaging; it didn’t define rerun/status semantics, UTC timestamp safety, API/data failure handling, or current-day-only intraday limitations.

**Refined review pass 2:** 9.5/10 for this bounded implementation slice. The plan now covers exact identity, scoped V3 routing, all configured keys without a hardcoded index list, no contract/schema change, bounded opt-in backfill, safe validation/upserts, semantically correct resampling, compatibility tests, and explicit preprod/licensing gates. Remaining 0.5 is external proof: live Upstox entitlement/data availability per configured key, exact IXIC/SSEC mappings and redistribution permission, and preprod verification. Agent satisfied: yes for implementation of the bounded slice; no claim that all ten benchmarks or public redistribution are cleared.

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
