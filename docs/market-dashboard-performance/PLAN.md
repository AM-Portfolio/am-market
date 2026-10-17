# Market Dashboard Performance and Long-Timeframe Correctness

**Kind:** bug fix  
**Lead service:** `am-market-data`  
**Target environment:** preprod  
**Branch:** `fix/market-dashboard-performance`  
**Status:** proposed — awaiting owner approval

## Goal

Make Dashboard Heatmap and Top Movers fast and correct for every existing
timeframe without changing any endpoint, query parameter, JSON key, DTO, or
response type.

## Source and verified current system

Source: local reproduction on `http://127.0.0.1:8092`, October 2026. The local
Actuator reports Mongo and Redis `UP`; Influx health responds successfully.
This is not a database-availability issue.

- `AnalysisController` exposes the existing Heatmap route; `AnalysisService`
  calculates `1D` from current OHLC and longer timeframes from history.
- `MarketAnalyticsService.getMoversUnified` enriches the whole index; its
  `SmartStockService` dependency finds a timeframe comparison close.
- `AnalysisRedisCache` already owns the existing Heatmap cache; the scheduler
  already owns off-request analysis work.
- Modern UI merges Heatmap values in `market_provider.dart`; an absent value can
  fall back to current `pChange`, which makes invalid data appear as `0.00%`.

Evidence: Heatmap `1D` returns valid NIFTY 50 values; `1W` returns all 50 as
`-100%`; `1M/3M/6M` contain isolated invalid `-100%`; direct Movers `1W` is
non-empty but takes about 4.5 seconds, and `1M` matched `1D`. A cached Heatmap
`1W` took about 73 ms. Therefore request-time Movers enrichment and invalid
long-timeframe baselines are the primary defects.

**New `1D` evidence:** the dashboard ranks five Gainers and five Losers while
every displayed badge reads `+0.00%`. The UI model accepts both existing
`percentChange` and `pChange` response fields, and the widget renders that
model field. The backend currently selects Movers by positive/negative absolute
`change`, then maps a separate `percentChange`. Therefore an item can be ranked
as a mover with a zero percentage. This is a correctness defect, not a styling
issue. The raw deployed response must be captured before implementation to
confirm whether the zero originates before or after serialization.

## Scope

In scope:

- One internal, canonical timeframe baseline/end-price calculation.
- Existing Heatmap and Movers result validation and cache resilience.
- Background warming of existing dashboard snapshots.
- One dashboard merge fix for unavailable data.
- Observability and focused tests.

Out of scope:

- New API routes, DTOs, response fields, database schemas, or database seeding.
- Gateway/authentication/public-market work.
- Broker, Paper Trading, portfolio, or unrelated chart UI changes.

## Design

### Approved technical solution — refine existing paths first

No new resolver class, service, endpoint, DTO, or database table is introduced.

1. Tighten the existing `SmartStockService.applyTimeFrameBase` and
   `AnalysisService.getHeatmap` paths to use the same documented rule: resolve
   in `Asia/Kolkata`, choose the preceding completed trading session as the
   baseline, then use a validated end session. A small existing-service helper
   is permitted only if private duplication cannot be avoided.
2. `MarketAnalyticsService` emits a Mover only when `lastPrice`,
   `previousClose`, `change`, and percentage are mutually consistent. It
   recalculates percentage from those two validated prices before sorting, then
   maps existing `pChange` and `percentChange` to the identical value. A
   non-zero Mover can never display `+0.00%`.
3. Continue using existing `AnalysisRedisCache` and the existing scheduler.
   Cache only a complete, fresh, validated result for the current trade date and
   membership version. Expired or session-stale cache entries are never served.
4. On a cache miss or invalid raw history, use the existing retrieval sequence:
   bounded Redis/Mongo/Influx read → existing provider fetch only if needed →
   validate → persist through the existing retriever/cache write path →
   calculate → serve. One existing Redis lease coordinates all pods.
5. The visitor request waits for that single bounded fetch/validation cycle. The
   UI keeps its existing loading/skeleton state; it does not display unavailable,
   stale, artificial zero, or a prior snapshot. Transient failures retry through
   existing scheduler/UI retry mechanics while the neutral loading state remains.
6. The scheduler prewarms these same fresh paths before users arrive. It never
   manufactures data or persists invalid cache entries.

### Request flow

1. The UI sends the existing Heatmap or Movers request unchanged.
2. The service reads an existing validated snapshot from Redis and returns it
   immediately. Cache hits make no Redis write and no provider call.
3. If Redis is unavailable, the service treats it as a cache miss and uses the
   bounded Mongo/Influx path. Redis must never make a valid response empty.
4. If a snapshot is missing or stale, one bounded single-flight refresh runs in
   the scheduler. Visitors receive the last validated snapshot when available.
5. The calculator selects a calendar-valid base close and valid end close. A
   missing, non-finite, non-positive, or inconsistent result is omitted.
   For every emitted Mover: `change = end - base` and
   `percentChange = (change / base) * 100` using the same rounded/raw policy.
   The sort sign is the percentage sign, not only an absolute change sign.
6. A result is stored only if roster coverage and price coverage meet existing
   thresholds and it passes the degenerate-result guard. A failed refresh cannot
   overwrite the last valid snapshot.

### Cadence and latency

- `1D`: refresh only during market hours, every 15–30 seconds.
- `1W`, `1M`, `3M`, `6M`, `1Y`: refresh every 5–15 minutes during market hours;
  once after close is sufficient outside market hours.
- Read path target: Redis only. No promise is made until traces establish a
  measured preprod budget.
- Influx reads: at most 50 symbols per query and at most two bounded concurrent
  chunks. Mongo `$in`: 100–250 identities. Redis: one pipelined/batched read.

### Failure and consistency rules

| Case | Result |
|---|---|
| Bad current refresh | reject it; keep loading and retry fetch, never serve it |
| No valid snapshot yet | existing loading/skeleton continues; no availability text or fake number |
| Redis outage | bounded primary-store/provider fetch, validate, persist, then serve |
| Influx partial failure | fetch/repair missing source rows before serving; never partially rank |
| 100 concurrent cache misses | one lease-holder fetches/persists; other requests wait on that result |
| Raw candle genuinely absent | omit affected member; investigate/backfill only after proof |
| Positive absolute change but zero/invalid percentage | reject row; do not rank it as a Mover |
| Corporate action / legitimate extreme move | validate source and session identity; do not use a blanket `-100%` rejection |
| Rapid timeframe/index switching | discard an older Movers/Heatmap response when selection has changed |

## File plan

| File | Change | Why it is necessary |
|---|---|---|
| `market-data-analysis/.../AnalysisService.java` | Correct its existing `1W+` base/end session selection; reject invalid Heatmap maps before cache. | Owns broken `1W+` Heatmap logic. |
| `market-data-service/.../SmartStockService.java` | Correct its existing timeframe-base lookup and fetch/persist behavior. | Owns the `1M` behaving as `1D` symptom. |
| `market-data-analysis/.../MarketAnalyticsService.java` | Enforce the Mover percentage invariant; sort valid percentage rows; read/cache a full existing ranking then slice by the existing `limit`. | Owns the 4.5-second orchestration and current change/percentage mismatch. |
| `market-data-redis/.../AnalysisRedisCache.java` | Validate trade date/membership version before cache is served; add validated Movers snapshot operations. | Prevents stale/cross-rebalance rankings and cache-key collisions. |
| `market-data-scheduler/.../MarketAnalysisSchedulerService.java` | Warm only dashboard index/timeframe snapshots through bounded single-flight work. | Keeps expensive calculation off the visitor path. |
| Existing market-data application configuration | Add non-secret cadence/timeout limits only if absent. | Makes refresh behavior environment-configurable. |
| `am_market/common/lib/providers/market_provider.dart` | Preserve omitted values as unavailable. | Prevents misleading `0.00%` fallback. |
| `am_market/ui/.../user_dashboard_page.dart` | Add a selection/request sequence guard around existing Movers loads. | Prevents a slow old timeframe response replacing the newly selected view. |

No controller, DTO, OpenAPI, gateway, schema, or public-market file changes.

## Test plan

1. `AnalysisServiceHeatmapTest`: every timeframe, invalid start/end, `-100%`
   bulk guard, cache preservation, partial coverage.
2. `SmartStockServiceTest` and `SmartStockPreviousCloseSnapshotTest`:
   exchange-calendar choice, prior completed-session selection,
   provider-persistence-before-response, and no `1M` fallback to `1D`.
4. `MarketAnalyticsServiceMoversTest`: a row with positive `change` and zero
   percentage is rejected; emitted values obey the percentage invariant; cached
   full ranking respects every existing `limit` request.
5. `top_mover_stock` model/widget test: existing `pChange` and
   `percentChange` payload variants both render the exact non-zero percentage.
6. `dashboard_heatmap_merge_test.dart`: missing value is unavailable, not zero.
7. Postman regression: capture deployed raw Movers body; existing URLs,
   parameters, and JSON schema remain compatible.
8. Preprod trace proof: classify all HTTP, security, service, Redis, Mongo,
   Influx, metadata, and provider spans for warm/cold requests.

## Rollout

1. Implement and run focused tests locally.
2. Deploy to preprod after approval.
3. Warm snapshots; validate all timeframes and payload compatibility.
4. Invalidate only cache keys proved invalid; do not seed raw stores by default.
5. Compare preprod trace latency and correctness against fixtures.
6. Request separate production approval with exact rollout and rollback plan.

## Scorecard

Delivery: 10/10. Design: 10/10. Agent score: 9.6/10.  
Adversarial review: no contract, security, or data-store mutation is required;
Redis loss, cold cache, provider failure, partial history, and cache stampede
are handled. Agent satisfied: yes. User approval is still required before code.
