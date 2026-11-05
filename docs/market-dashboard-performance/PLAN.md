# Market Dashboard Performance and Long-Timeframe Correctness

## Index historical-candle integrity hotfix — 2026-11-05

**Kind:** bug fix
**Lead service:** `am-market-data`
**Target environment:** preprod; production stays read-only pending a separate approval.
**Source-plan pointer:** user targeted diagnostic report plus read-only production
traces for `NIFTY 50`, `NIFTY BANK`, `NIFTY IT`, `SENSEX`, `INFY`, and `TCS`.

### Goal

Restore multi-year monthly performance for Indian indices without changing the
public HTTP or Flutter JSON contracts, and prevent index candles from being
persisted as equity candles during sync or repair.

### Current system (verified)

- The Flutter card calls `GET /v1/analysis/performance/monthly` with
  `detailed=false`; its existing empty state is the intended reaction to an
  empty `yearlyPerformance`, not a decoding failure.
- `MarketDataHistoricalSyncService.fetchBatch` sends every historical batch as
  `STOCK` and omits `isIndexSymbol`.
- `HistoricalDataServiceImpl.saveHistoricalData` maps every historical payload
  to `EquityPrice` and writes it through the equity service.
- `HistoricalDataIngestionEvent` is a `Map<symbol, HistoricalData>` with no
  type field, while `MarketDataPersistenceService` already injects
  `IndexSymbolRegistry`. The event map key is therefore the backward-compatible
  source of classification at consume time; an event-schema revision is not
  required for this hotfix.
- `HistoricalDataRetriever` accepts partial stored history, removes that symbol
  from synchronous fallback, and only schedules an asynchronous repair.
- `MarketIndexIndicesRepositoryImpl` pivots `market_index` without grouping
  Flux tables or ordering the result by `_time`.
- Production returns an empty monthly matrix for NIFTY 50 even though the raw
  endpoint returns thousands of points confined to two current-session dates.
  This is an index persistence/read-path failure, not a Modern UI outage.

### Scope and decisions

1. Preserve index/equity identity through historical fetch, consumer, and
   persistence. The consumer/persistence layer classifies the existing event
   map key using `IndexSymbolRegistry`, so old and new Kafka events behave the
   same. Index daily candles write to `market_index`; equities write only to
   `equity`.
2. Keep the live quote/UI path non-blocking. Historical sync and repair remain
   asynchronous behind Kafka or the existing background executor.
3. A partial index read may schedule one repair, but its empty aggregation must
   not become a new cached historical-performance result. Successful durable
   writes invalidate matching `analysis:history:<SYMBOL>:<years>:<detailed>`
   keys only after persistence succeeds.
4. Make Flux results deterministic with merged grouping and `_time` ordering;
   defensively sort/deduplicate chart points in Java before response mapping.
5. Add value-safe telemetry for symbol, instrument type, interval, point count,
   oldest/newest timestamp, target measurement, repair, and cache invalidation.

Out of scope: Flutter/model changes, public API changes, Kafka retention
changes, a production migration, Redis deletion, deployment, or rollback.

### Target flow

1. Sync splits known index names from equities when requesting history; this
   removes the `STOCK` hardcode at the fetch boundary.
2. At consume time, persistence reclassifies the event map key using
   `IndexSymbolRegistry`; this protects already-queued/older Kafka events.
3. The consumer sends index candles through a point-to-`MarketIndexIndices`
   mapper using the provider candle timestamp and the canonical symbol for
   `key`, `index`, and `indexSymbol`. Stocks retain the existing
   `EquityPrice` mapper/repository.
4. The index read query uses canonical `indexSymbol`, daily candle instants,
   grouped Flux rows, and ordered results.
5. A repair write invalidates the exact aggregate cache after success; on
   failure it retains the last known-good cache and records the outcome.

### Corner-case matrix

| Case | Expected | Covered by |
|---|---|---|
| NIFTY 50 historical sync | `market_index`, never `equity` | unit + integration |
| INFY historical sync | `equity`, never `market_index` | unit + integration |
| Mixed Kafka batch | each entry routes independently | consumer test |
| Current-session-only index data | one repair; no new empty analysis cache value | retriever/service test |
| Repair failure | prior valid cache retained; safe log | persistence test |
| Disjoint Flux series / duplicates | chronological unique chart output | repository/analytics test |
| Provider has no candles | unchanged empty response with diagnostic | API regression |

### Test plan and rollout

1. Unit-test classification and compatible per-symbol Kafka routing.
2. Integration-test index tag/field mapping, idempotent write, and multi-year
   NIFTY 50 readback.
3. Regression-test NIFTY 50/NIFTY BANK and INFY monthly performance with the
   existing response shape.
4. Test partial repair, successful invalidation, failed-write cache retention,
   and chart sort/deduplication.
5. In preprod, use Postman plus logs/metrics to verify route, date coverage,
   write target, cache state, and matrix row count.
6. Before a production data write, provide an approved migration runbook with
   target symbols, source/target queries, readback checks, rollback, cache
   scope, and risk.

### Gap review and scorecard (planning pass 1)

The supplied diagnosis correctly captures the primary route/persistence/cache
failure. It remains partial because the event schema, current index mapper,
Influx duplicate-write semantics, production TTL override, and migration source
set need verification before implementation.

| Dimension | Score | Remaining gap |
|---|---:|---|
| Product / user outcome | 9.5 | Existing honest empty state remains only while a bounded repair completes. |
| Architecture / design | 9.5 | Use event map key + existing registry; no schema break. |
| Data / contracts | 9.5 | App config confirms the 30-day analysis TTL and existing tag schema. |
| Reliability | 9.5 | Single repair, success-only invalidation, and durable-write gate are specified. |
| Security | 9.5 | Migration rules prohibit values/credentials in logs and require a reviewed runbook. |
| Observability | 9.5 | Route/repair/measurement telemetry is specified. |
| Testability | 9.5 | Fixture, integration, and readback coverage is explicit. |
| Operability / rollout | 9.5 | Preprod gate and production-runbook/approval gate are explicit. |
| TODO / implementability | 9.5 | File-level work is bounded to existing modules. |

**Self-rating: 9.5 / 10.** The plan is implementation-ready for the source
change and intentionally separates the later production migration into an
approved runbook. The remaining work is execution/verification rather than an
undecided design. **Agent satisfied: yes; user approval is still required
before code changes.**

## Historical-data endpoint latency revision — 2026-11-04

### Monthly-performance endpoint: production trace revision

The primary UI endpoint is:

`GET /v1/analysis/performance/monthly?symbol=NIFTY%2050&years=10`

Live production verification returned HTTP 200 in **61,369 ms** with a 2,558
byte response. The pod log trace was
`19b5f15317d8d7c9944d0b06e423df9b`, and its server-side request duration was
**60,560 ms**.

| Trace phase | Observed result | Approx. time | Decision |
|---|---|---:|---|
| Analysis Redis lookup | Miss; request proceeded to raw history | ~13 ms | Fix exact key warming and verify read-after-write |
| Raw history request | `NIFTY 50`, 2016-12-17 through 2026-11-04, daily, continuous | — | Keep the 15-day baseline buffer, but do not block on a 60s DB timeout |
| Influx/database lookup | One-symbol batch query timed out | **60,000 ms** | P0 fix; optimize query/index/timeout or bypass safely |
| Upstox fallback | 1/1 symbol, 2,448 points | ~120 ms | Keep as bounded fallback, not the normal path |
| Kafka + Redis backfill | Kafka event plus Redis write | ~338 ms | Make writes non-blocking and verify cache visibility |
| Monthly aggregation | 10 years / 120 months over 2,448 points | negligible compared with DB wait | Keep; algorithm is not the bottleneck |

The result contains ten years (`2017`–`2026`) and is mathematically small. The
latency is caused by the retrieval path, not by monthly aggregation or the
amount of JSON returned.

### Why it worked before and regressed

The repository history identifies a likely behavioral regression rather than a
market-data-volume problem:

- Before commit `4a3032bc` (batch historical Flux query), the retriever queried
  persistence per symbol and could accept already-available partial database
  data while scheduling repair. The new path replaced that with a synchronous
  batch Influx query before provider fallback.
- Commit `0da76f86` changed the Influx client to a **60-second read timeout** and
  reduced the repository chunk size from 50 to 25. On a slow or unhealthy
  Influx path, this converts the request into a deterministic 60-second stall.
- The monthly UI request uses `detailed=false`, so its analysis key is
  `analysis:history:NIFTY 50:10:false`. The scheduler currently warms
  `getHistoricalPerformance(symbol, 10, true, true)`, which uses
  `analysis:history:NIFTY 50:10:true` and deliberately bypasses the cache. The
  scheduler therefore does not warm the exact UI key.
- The raw index path independently logs `INDEX_CACHE MISS`, then a database
  miss, then Upstox. Its provider write does not produce a subsequent cache hit
  for the same index/date request. This explains why both layers can miss even
  after a successful response.

These changes explain the “previously fast, now slow” behavior: the old path
could return existing data or use a warm result, while the current path has a
cache-key mismatch plus a synchronous 60-second database timeout on a miss.
The commit history is evidence of a regression mechanism; production still
needs a before/after benchmark on the exact deployed artifact before assigning
sole blame to one commit.

### Combined solution for both endpoints

#### P0 — restore fast monthly UI response

1. Warm the exact UI cache key (`detailed=false`) after market close and on
   deployment/startup if missing. Do not use `bypassCache=true` for the normal
   warm path.
2. Add single-flight protection per `(symbol, years, detailed)` so concurrent
   misses do not launch identical ten-year calculations.
3. On a cache miss, use a bounded raw-history retrieval path with a short,
   explicit database budget. Do not allow one Influx read to consume the full
   HTTP request budget.
4. If the last successful monthly result exists, use stale-while-revalidate:
   return it immediately and refresh it asynchronously. If no prior result
   exists, use the provider fallback with an explicit degraded-but-correct
   response policy.

#### P0 — make raw index history cacheable and durable

1. Align index-cache reads and writes. Either write the dedicated
   `index:historical:<symbol>:<interval>:<from>:<to>` representation and return
   it directly, or remove the unused dedicated path and use the daily
   `stock:historical:<symbol>:<interval>:<date>` format consistently.
2. Add a read-after-write test for `NIFTY 50` covering the exact date range.
   The second request must not log `INDEX_CACHE MISS`, database miss, or
   provider fetch.
3. Replace the hardcoded 24-hour historical Redis TTL with the configured
   historical TTL, while treating the current trading day separately so stale
   data is not served.
4. Correct response metadata so `source` reports the actual tier rather than
   `forceRefresh`.

#### P0 — fix the Influx one-symbol/long-range query

1. Benchmark the exact Flux query for `NIFTY 50`, 10 years, daily interval,
   including server-side query duration and returned rows.
2. Verify the Influx measurement, symbol tags, bucket, retention policy, and
   time-range/index configuration. Production currently reports no rows before
   timing out, so “small data” does not mean “cheap query.”
3. Add a bounded query timeout shorter than the HTTP budget and return partial
   database results where valid. Provider fallback should receive only missing
   symbols/ranges.
4. Reuse the existing 25-symbol chunking for multi-symbol requests; introduce
   bounded parallel chunks only after measuring Influx capacity and connection
   pool limits. Do not add an unbounded fan-out.

#### P1 — remove repeated orchestration work

- Reuse the initial symbol-resolution map instead of calling
  `resolveSymbols(List.of(symbol), ...)` inside the loop.
- Avoid repeating calendar/configuration lookups within one request.
- Keep Kafka batch-size changes out of the critical fix until payload-size and
  broker tests prove a larger batch is safe.

### Freshness design for the current month

The current implementation stores one whole analysis JSON value under
`analysis:history:<symbol>:<years>:<detailed>`; there is no dedicated monthly
bucket collection. Therefore it cannot update only November in place today.

The plan has two options:

- **Lower-risk option:** recompute the full aggregate asynchronously after the
  market calendar closes, then atomically replace the aggregate cache value.
  This is simplest after the Influx path is fixed.
- **More granular option:** introduce a normalized monthly materialization
  keyed by symbol/year/month and assemble the existing response from those
  buckets. On a market day, update only the active month; on non-market days,
  keep the prior month. This reduces recomputation but requires an agreed schema,
  backfill, invalidation, and migration plan before implementation.

The implementation should start with the lower-risk option unless the measured
post-fix full recomputation is still too slow. No permanent cache of arbitrary
user queries is required.

### Combined plan gap review and final rating

Initial plan rating: **8.5/10**. The first version correctly identified the
multi-symbol Influx timeout but underweighted the one-symbol monthly path and
the analysis-cache key mismatch.

After the new production trace, source-history review, cache-path review, and
freshness trade-off analysis, the revised plan rating is **9.2/10**.

Remaining gaps before code implementation:

- Confirm the exact production image commit against remote `main`.
- Run an Influx query benchmark and inspect the production bucket/measurement
  configuration without mutating data.
- Verify the Redis feature flag/circuit state and prove why a provider write is
  not visible to the next identical read.
- Decide whether the lower-risk aggregate refresh is sufficient before adopting
  a new monthly materialization schema.

Verdict: **the root cause is sufficiently established to prepare the hotfix,
but implementation should begin only after user approval of this revised plan**.

### Decision scope

This revision covers `POST /v1/market-data/historical-data`, especially the
default request for an index such as `NIFTY 50` where `isIndexSymbol` is false
and the service expands the index into constituents. The objective is to
reduce cold and warm latency without caching every arbitrary request forever or
changing the response contract.

### Evidence-based diagnosis

- The production request for `nifty 50`, 2026-06-01 through 2026-09-19, returned
  HTTP 200 in about 78 seconds. The service normalized the Saturday end date to
  2026-09-18 and returned 51 symbols / 3,978 points.
- The same request with `isIndexSymbol=true` returned in about 1.2 seconds
  with one index and 78 points. The dominant cost is therefore constituent
  expansion and retrieval, not the index candle query itself.
- The supplied production trace shows a roughly 60-second InfluxDB batch-query
  timeout, followed by roughly 16 seconds of provider fallback for 50
  constituents. This is the primary latency blocker.
- The source also contains an N+1 resolver path: after an initial batch
  resolution, `getHistoricalDataMultipleSymbols` calls
  `resolveSymbols(List.of(originalSymbol), ...)` once per symbol. This adds
  repeated Mongo/instrument lookups and must be removed or proven harmless.
- The repository version already contains sequential 25-symbol Influx chunks,
  while the production evidence describes a 50-symbol timeout. The deployed
  artifact must therefore be compared with `origin/main` before changing the
  query layer; adding another chunking layer blindly could make the system
  worse.
- Redis misses are confirmed, but the claim that an Influx timeout opened the
  Redis circuit is not yet proven. `RedisAvailabilityGuard` opens on Redis
  operation failures, so the implementation must record the actual circuit
  reason instead of inferring it from an Influx failure.
- The response metadata currently labels the source from `forceRefresh`, not
  from the actual cache/database/provider tier. This can make a cache-backed
  response appear authoritative when it was obtained through fallback.

### Proposed implementation sequence

1. Pull and inspect remote `main`, compare it with the deployed production
   artifact, and create a hotfix branch only after this plan is approved.
2. Remove the resolver N+1 by reusing the initial batch resolution/mapping.
   Add tests proving that a 51-symbol request does not perform 51 additional
   resolver queries.
3. Fix the Influx retrieval path using one bounded, configurable strategy:
   reuse the existing symbol chunking, benchmark safe chunk sizes, and only
   introduce bounded parallelism if the Influx connection pool and server can
   sustain it. Apply per-chunk timeouts and fall back only for missing/failed
   symbols, not for the whole batch.
4. Verify and correct the index-cache fast path so a valid index cache hit is
   converted and returned rather than falling through to stock-cache logic.
5. Keep Redis as a best-effort accelerator for raw candles, with batched reads,
   accurate hit/miss/circuit metrics, and no correctness dependence on a
   permanent query-result cache.
6. Prevent duplicate provider calls and preserve provider rate limits. Leave
   Kafka batch-size changes out of P0 unless payload-size testing proves they
   are safe; they are not the 60-second blocker.
7. Add observability for resolver-call count, cache tiers, Influx chunk count
   and duration, provider symbols, fallback symbols, and final source. This is
   important because production tracing is currently disabled.

### Correctness and cache policy

The endpoint should cache reusable daily candles under the existing normalized
symbol/date/interval scheme, not every arbitrary multi-symbol/date query. The
database remains the durable source of truth; provider fallback fills missing
data; a market-calendar-aware refresh can be handled separately for monthly
analysis materialization. This avoids stale forever-caching while retaining
fast repeat reads.

### Verification and acceptance criteria

- Preserve the current response shape, date normalization, symbol counts, and
  index-vs-constituent semantics.
- A warm cache request must not query Influx or the provider.
- A database partial hit must fetch only missing symbols from the provider.
- A failed Influx chunk must not stall the whole request for the full 60-second
  timeout.
- The 51-symbol path must have one batch resolver operation, not one resolver
  operation per constituent.
- Test cold cache, warm cache, Redis unavailable, slow/failed Influx, provider
  timeout/rate-limit, partial database results, one-symbol requests, index-only
  requests, and ten-year ranges.
- Report before/after latency by cache, resolver, database, provider, Kafka, and
  total request phases. Do not claim a fixed target until the pre-production
  benchmark is measured.

### Plan self-review and verdict

Initial rating: **8.5/10**. The diagnosis is strongly supported by the
production timing and source inspection, but implementation must first resolve
the deployed-artifact/source mismatch and validate Influx capacity. The main
trade-offs are bounded concurrency versus database pressure, and partial
fallback complexity versus avoiding a full-batch provider retry.

After the gap review, the safer verdict is **Proceed to implementation only
after user approval of this plan**. The plan explicitly avoids inventing a new
cache collection, avoids an unbounded parallel query fan-out, keeps Kafka batch
changes out of the critical fix, and treats the Redis-circuit explanation as an
unverified hypothesis until instrumented.

**Kind:** bug fix  
**Lead service:** `am-market-data`  
**Target environment:** preprod  
**Current checkout branch:** `hotfix/hypercare-R6` (kept; no branch switch requested)
**Original plan status:** proposed — superseded by the current implementation status below.

**Status:** owner-approved local implementation complete for the October 22
large-index latency slice below. Production remains read-only and is not being
changed or deployed.

**October 18 revision:** The production-connectivity and design decisions in
the final section below supersede conflicting earlier statements. Existing
ratings are historical, not evidence that the revised plan is verified.

**October 22 heatmap latency implementation scope:** The owner approved the
Redis multi-symbol pipeline and large-index heatmap warming only. The proposed
Influx parallel-query change (source-plan fix 1) is explicitly deferred. No API,
DTO, response shape, query strategy, or production configuration is changed.

**October 22 review correction:** The first warmer implementation was too narrow:
it named two NSE indexes, selected three windows, and embedded a 100-member gate
in Java. The scheduler now reads index names from the existing `nseindices.yml`
registry, fetches their memberships in one batch, and uses loaded application
configuration for the minimum roster size and the heatmap windows already
supported by the analysis path (`1D`, `5D`, `1W`, `1M`, `3M`, `6M`, `1Y`, `3Y`,
and `5Y`). `5D` is retained as the API's existing alias for the one-week window.
`application-scheduler.yml` is not imported by the runtime app; these settings
belong in the loaded `application.yml`. This broadens warmed coverage without
changing any API. The cache warmer still cannot restore history that is absent
from Redis, InfluxDB, and provider results, and the first cold request can still
wait on the unchanged sequential Influx path.

The default minimum is 100 constituents to cap daily batch work. Lower it to
`0` to warm every configured NSE index with a non-empty roster; indexes below
the threshold remain directly queryable but will not be prewarmed. Warming every
index costs more scheduled history queries, so the threshold is intentionally
an operator-controlled trade-off.

## Goal

Make Dashboard Heatmap and Top Movers fast and correct for every existing
timeframe without changing any endpoint, query parameter, JSON key, DTO, or
response type.

## Source and verified current system

Source: local reproduction on `http://127.0.0.1:8092`, October 2026. The local
Actuator reports Mongo and Redis `UP`; Influx health responds successfully.
This local observation does not establish production database availability.

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

For the October 22 latency slice, additionally in scope are only (a) writing
the already-fetched historical bars for all returned symbols through the
existing Redis pipeline in bounded groups of at most 50 symbols and (b)
extending the existing daily analysis scheduler to refresh heatmaps for every
index in the configured NSE registry whose valid roster exceeds a configurable
minimum. Index membership is read in one batched lookup; the threshold and shared
heatmap timeframe list are loaded from `application.yml`. The
25-symbol sequential Influx reads remain unchanged. Thus this reduces Redis
write round trips and makes subsequent requests cache hits, but does not promise
that the first cold request will complete before its gateway timeout.

The daily analysis scheduler already runs under the existing tracked/distributed
lock and now runs the large-index warm first, before its existing per-symbol
workload. It caches only results passing the current Heatmap coverage checks;
if source history is absent or incomplete, the cache remains unwarmed and a
request can still be slow. This slice adds no cache, worker, endpoint, or data
source.

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
   the scheduler. A prior snapshot is served only if it still satisfies the
   freshness and session rules; otherwise do not display it as current.
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
Adversarial review (original pass): no contract or security change is required;
Redis loss, cold cache, provider failure, partial history, and cache stampede
are handled. Agent satisfied: yes. User approval is still required before code.

## Revision: production connectivity, recovery and all-index coverage

**Status: design review only; no production change authorized.** This revision
supersedes the old evidence, design claims and score above. Kind: fix; lead
`am-market/am-market-data`; UI `am-modern-ui` only if existing presentation
cannot safely handle missing results. Target preprod; production is a separate,
explicit approval. Source: this conversation's dashboard-performance plan and
the user's October 18 production diagnostic. Catalog: Market domain, existing
service in `am-market`, existing Flutter UI in `am-modern-ui`; analysis is
local, not a direct audit of production. Context read: both repository READMEs,
`am-market-data/README.md`, its `.am.yaml`, and UI `ARCHITECTURE.md`.
Coverage of the prior plan: **partial**; it omitted prod drift, true batched
Influx retrieval, all-index coverage, and the quote-map failure distinction.

### Evidence and unresolved checks

- User-reported production pod DNS resolves the Influx host internally;
  HTTPS 443 is refused and HTTP 8086 reports healthy. Deployed URL reportedly
  points to 443; `am-market-data/helm/values.prod.yaml` currently sets
  HTTP 8086. This is strong evidence of runtime config drift, not a verified
  authoritative deployment source. Trace source of the override, precedence,
  image SHA, namespace, service endpoint, org/bucket and access rights first.
- HTTP 8086 is plaintext. Its use needs explicit security review for the actual
  network path and Influx token; prefer approved TLS/service endpoint where
  required. Do not assume a healthy unauthenticated endpoint proves queries,
  bucket contents, freshness or permission to transmit credentials securely.
- User trace reports local batch 43,218 ms of 43,815 ms. In
  `market-data-service/.../util/HistoricalDataRetriever.java` the database
  loop reads each remaining symbol sequentially. A per-symbol latency,
  cause of an empty query, and production behavior are not yet established.
  Obtain full traces across HTTP, auth, service, Redis, Influx, metadata and
  provider spans; report counts and critical-path durations without double count.
- `SmartStockService.applyTimeFrameBase` disables request-time provider
  fallback and rejects missing baselines; `StockDataEnricher` logs “No price
  data” for absent quote objects. Confirm which stage drops quotes. Preserve
  fail-closed comparison validation and the existing source/provider limits.
- Existing local controller-specific 1M/3M enum disambiguation must be matched
  to the production image before assuming it is deployed. Do not alter shared
  parser semantics or other consumers without a separate compatibility audit.

### Revised solution and decisions

1. **Separate restore from code:** inspect configuration sources and secure
   in-cluster access read-only; verify exact effective config and authenticated
   read of representative candle windows without exposing tokens. Propose a
   durable GitOps/Helm fix at the actual source of drift. The user's suggested
   8086 setting is a candidate, not an approved production change.
2. **Timeframe semantics:** for each supported dashboard interval `1D, 1W,
   1M, 3M, 6M, 1Y, 5Y`, select the exchange-calendar reference session on
   or before the requested boundary and an eligible end price, with matching
   corporate-action adjustment basis. Define and test live/session-close
   behavior and market-holiday boundaries against authoritative fixtures before
   sharing calculations. Never fall back from month to day, synthesize zeros or
   silently relabel old data as current.
3. **All indices:** use the verified current roster, canonical exchange
   identity and established index-specific thresholds, not a fixed 45 for all.
   Explicitly test small sectoral and large 500-member indices, membership
   changes, newly listed stocks and incomplete histories. If trustworthy
   coverage is insufficient, do not publish a misleading ranking.
4. **Bounded retrieval:** first inspect and reuse existing batch persistence
   operations. If absent, add an internal batched historical read with bounded
   chunk size, concurrency and deadlines; preserve symbol/exchange/range/interval
   and existing public contracts. Benchmark query count, p50/p95 and warm/cold
   paths; do not promise an unmeasured latency.
5. **Recovery:** distinguish Redis miss, DB outage, valid empty data, quote
   lookup failure and provider failure. Reuse the existing scheduler/provider
   path for missing eligible candles, with cross-pod single-flight, bounded
   concurrency, quotas, timeout and idempotent validated persistence.
   Never use unbounded per-request provider fallback. If a recovery cannot
   finish within the bounded wait, UI may show neutral loading/retry, but must
   eventually expose a truthful failure instead of spinning forever. No stale
   snapshot or fabricated value presented as current.
6. **Consistency:** trace Heatmap and Movers source/date/price and reconcile
   their differences before unifying calculation; a percent-only Heatmap cache
   is insufficient to construct Movers' existing price and baseline fields.
   Preserve legitimate zero changes and valid large moves. Existing cache keys,
   TTL, membership identity and requested limit must not cross-contaminate
   consumers; invalidate only demonstrably bad entries with separate approval.
7. **Failures:** retain existing API paths, request and response types. No
   assumption that correct complete data can be guaranteed during provider and
   datastore outages. Record operational failure/coverage reason internally;
   avoid confusing a failure with a legitimate empty ranking.

### Implementation file map

| File | Purpose / boundary |
|---|---|
| `am-market-data/helm/values.prod.yaml`, actual deployment/GitOps source | Inspect precedence; repo value already differs from reported deployment. Change only authoritative source if needed, with production approval. |
| `market-data-service/.../util/HistoricalDataRetriever.java` and `.../MarketDataPersistenceService.java` | Batched bounded read; verify other callers and existing persistence semantics. |
| `am-common-investment-data/.../EquityPriceMeasurementRepository{,Impl}.java`, `EquityService{,Impl}.java`, `HistoricalDataService{,Impl}.java` | Add an internal multi-symbol range read in bounded chunks; additive defaults preserve existing shared callers. |
| `am-market-data/pom.xml` | Consume the in-workspace shared library version containing the additive methods. |
| `market-data-service/.../SmartStockService.java` | Reference-session, quote-map diagnosis and controlled repair integration. |
| `market-data-analysis/.../AnalysisService.java`, `.../MarketAnalyticsService.java`, `.../util/StockDataEnricher.java` | Shared verified calculations, coverage and quote diagnostics only where needed. |
| `market-data-scheduler/.../MarketAnalysisSchedulerService.java`, `market-data-redis/.../AnalysisRedisCache.java` | Reuse existing scheduler/cache if needed for safe repair and freshness; no parallel service. |
| `am-modern-ui/am_market/common/lib/providers/market_provider.dart`, dashboard widget | Conditional only if UI misrepresents omitted values, errors or request races. |
| Existing corresponding backend/UI tests and Postman collection | Compatibility, accuracy, failure and all-index/timeframe coverage. |

### Corner cases and test matrix

| Case | Expected | Coverage |
|---|---|---|
| 1m/3m versus 1M/3M; all periods | Exact existing meanings; no shared parser regression | unit + endpoint contract |
| All configured indices, including 10/50/500 members | Index-specific roster and limits; no hard-coded 45 | parameterized service + preprod matrix |
| Missing quote versus missing candle | Distinct diagnostics; no fake baseline | unit + full-trace test |
| Holiday, weekend, exchange timezone, market open/close | Correct session boundary/end, consistent 1D and longer | clock/calendar fixtures |
| New listing, delisting, split, dividend, index rebalance | No fabricated five-year value; adjustment/membership rules verified | fixture + integration |
| Redis unavailable, Influx unavailable, partial batch, 401/429, provider timeout | Bounded retries; no stale/fabricated result; accurate failure | dependency fault tests |
| Cold cache, concurrent requests, lease expiry/owner crash | Bounded single-flight recovery and eventual retry | multi-instance test |
| Fresh cache, old/as-of cache, cache-key/limit isolation | Correct freshness and no cross-index/timeframe leakage | cache integration |
| Zero moves, fewer than five gainers/losers, extreme valid moves | Correct ranking without invented rows or blanket rejection | unit |
| Rapid UI changes, gateway/session/client contract | No old selection overwrite; response format/security unchanged | UI + Postman contract |
| Mixed environments, HTTP credential transport, prod drift | No local writes to prod; confirm source, secure route and explicit approval | ops review |

### Review, trade-offs and rollout

First-pass rating **6/10**: strong production connection evidence, but prior plan
assumed healthy production, combined incompatible stale/loading behavior, and
did not prove quote loss, provider data, full spans or source-of-truth config.
Revised design: batch reads improve scale but add query complexity; background
repair limits provider load but can make the first cold request wait; strict
freshness/coverage avoids wrong answers but cannot guarantee results during
outages; exact month parsing fixes local paths but preserves shared ambiguity.

Revised rating **8/10 (provisional)**: safer and actionable, but cannot honestly
claim a fully verified design while production config ownership, transport
security, complete trace and same-price-basis fixtures remain unverified.
Agent satisfied: **no**; prior 9.6 rating is superseded. Review plan, TODO and
diagram; investigate remaining items read-only before claiming completion.

Preprod: run unit, integration, Postman contract and browser matrix; compare
prices to independent timestamped candles; measure warm/cold traces. Then ask
separately for production approval including exact authoritative change,
deployment target, expected rolling effect, security exposure, validation and
rollback. Never patch production directly or assume a rollout is approved.

### Implementation progress

The first code slice replaces sequential equity historical-store reads with an
Influx range query using bounded 50-symbol chunks. It keeps the Redis batch
lookup first, preserves the existing per-symbol API and index-specific path,
and adds default shared-service methods so other callers remain compatible.
No endpoint, DTO, response, schema, credential, or production setting changes.
The existing production runtime URL discrepancy is not fixed by this code and
must be resolved at its authoritative, security-reviewed configuration source.

### October 22 implementation slice — requested for owner review

**Source-plan pointer:** user's attached production/dev findings report
(`57b276ad-dee6-4f9a-94e4-e8674f248191`) and follow-up request to implement the
safe batching, candle-window, baseline, and diagnostic-log changes. Do not copy
the attachment into the repository.

**Catalog/context:** `am-market` is the Market domain repo; `am-market-data` is
the existing backend service. Analysis source is local. Read the repo and
nested service READMEs, nested `.am.yaml`, existing Market Dashboard plan/TODO,
and current `AnalysisService`, `SmartStockService`, cache service, and tests.
The repo root has no `.am.yaml`; no new service is proposed.

**Coverage verdict: partial.** The report correctly identifies repeated
single-symbol cache persistence calls and Heatmap's wide daily history range.
Its “baseline-only 15-day query” is incomplete: current non-1D Heatmap uses the
last returned candle as its end price, so narrowing only around the start would
silently change results. The current quote-cache path normalizes exchange
prefixes, so the reported Redis key mismatch is not included without a
reproducible miss on the exact history-cache path.

**Current system (verified):**

- `SmartStockService.applyTimeFrameBase` requests a small daily window around
  the timeframe boundary; it chooses the latest candle on/before that boundary,
  but falls forward to the earliest later candle if none exists.
- Smart quote recovery calls `MarketDataCacheService.cacheLatestPrices` once
  per validated recovered symbol. That method already accepts a map and batches
  Redis writes and Mongo `saveAll` within each invocation.
- The daily previous-close recovery has additional per-symbol calls to the same
  batch-capable cache method; batching only the quote-recovery loop would leave
  those paths unchanged.
- `AnalysisService.getHeatmap` fetches daily history from the start-boundary
  buffer through `to`, then uses the latest valid point at/before the start
  boundary and the last returned point as the end price. The `1D` live-OHLC
  path remains distinct and is out of this optimization slice.
- Existing service/cache paths, response DTOs, enum values, and public routes
  are retained. Current evidence is from source and the user's report; exact
  speedup is not guaranteed until measured with warm/cold traces.

**Scoped change:**

1. Accumulate only already-validated, deduplicated quotes in each existing
   SmartStock recovery path and call `cacheLatestPrices` once per batch. Keep
   the current validation, normalized identities, cache/Mongo write semantics,
   and response assembly unchanged.
2. For non-1D Heatmap, preserve the exact baseline/end selection rule while
   replacing the full-window read with bounded reads for (a) the baseline
   boundary and (b) the end boundary. Reuse the existing batch-history service;
   combine overlapping windows where safe. Resolve both values only from
   candles on/before their respective boundaries. Do not use future candles.
   Leave `1D`, public API, DTOs, response keys, and result-cache behavior as-is.
3. In `SmartStockService`, remove only the future-candle fallback. If a valid
   prior baseline is absent within the existing bounded lookback, leave that
   symbol unranked through the existing validation path rather than calculating
   a misleading return.
4. Add one aggregate structured log per batch/Heatmap request: timeframe or
   operation, index, requested symbol count, query windows, returned symbol and
   candle counts, resolved baseline/end counts, omitted count, cache-write
   batch size/call count, and elapsed milliseconds. Do not log prices, tokens,
   or per-symbol info lines. Keep existing low-level error logs intact.

**Compatibility and safety boundaries:** no public contract, shared timeframe
parser, data schema, Redis key format, provider fallback policy, scheduler,
production configuration, or full Movers response cache changes. `1D` behavior
is unchanged. Missing/invalid history remains fail-closed; cache or Mongo write
failure must not invalidate a valid in-memory calculation or turn a successful
request into fabricated data.

**Regression checks before code:**

- SmartStock batches multiple quotes in one cache call; empty/invalid/duplicate
  inputs do not create extra writes; cache failure remains non-fatal.
- Heatmap values before/after optimization match for the same fixtures across
  supported timeframes; query ranges are bounded and include both needed ends.
- Exact boundary, weekend/holiday, no-prior-candle, only-future-candle,
  missing-end-candle, invalid price, and overlapping-window cases are covered.
- `1D`, existing cache hit, coverage threshold, index normalization, API JSON,
  and response ordering remain unchanged.
- Logs prove one batch write and show candle counts/windows/duration without
  exposing market prices or secrets. Compare cold and warm query duration and
  candle counts; report measured values, not predicted speedup.

**Corner-case matrix:**

| Case | Trigger | Expected | Test coverage |
|---|---|---|---|
| Weekend/holiday baseline | Target date has no candle | Use latest valid candle on/before target | SmartStock + Heatmap boundary unit tests |
| Future-only candle | Lookback has no prior candle | Omit symbol; never use future close | SmartStock regression test |
| Missing end candle | End window has no valid point | Omit symbol; do not fabricate an end price | Heatmap regression test |
| Overlapping boundary windows | Short timeframe windows overlap | Merge/dedupe points and preserve same calculation | Heatmap unit test |
| Duplicate prefixed symbol | Same instrument arrives in two formats | One canonical write; preserve response keys | SmartStock/cache unit test |
| Redis/Mongo cache write failure | Persistence errors after quote validation | Keep valid in-memory response; log failure | Cache failure test |
| Existing cache hit / `1D` request | Optimized path is bypassed | Existing result and query behavior unchanged | Existing Heatmap + mover tests |
| Invalid/non-positive close | Bad source candle | Omit symbol; do not cache a misleading result | Heatmap/SmartStock unit tests |

**Latency and consistency:** Movers continue to use the current validated quote
as the end price and a prior daily close as the timeframe base. Heatmap preserves
its existing daily-history end-price semantics; this slice changes only how the
two required boundary values are retrieved. No provider fallback or added wait
is introduced. Measure cold/warm calls and returned candle counts; no hard p95
target or speedup is assumed.

**Scoped scorecard (pre-implementation):** Delivery 10/10; Design 10/10; Product 9/10; Architecture
9/10; Data/contracts 10/10; Calculation correctness 10/10; Reliability 9/10;
Security 10/10; Observability 10/10; Testability 10/10; Operability 9/10;
TODO implementability 10/10. Overall 9.6/10. Adversarial review: no blocker or
major remains in this local-only slice; cache failures, weekend/holiday,
future-only and missing-end windows are covered. Agent satisfied: yes for this
scoped slice; the user approved implementation on October 22.

**Execution status:** scoped code changes and local regression checks are
complete. Focused SmartStock/Heatmap tests passed after final edits, and the
market-data service/analysis reactor passed. Runtime cold/warm timings remain
pending; no speedup is claimed. Production remains read-only and was not part of
this implementation.

## November 4 contract verification — index flag and mixed historical data

The requested historical-data contract is now explicitly covered by this plan:

| Request | Required behavior | Current code verdict |
|---|---|---|
| `isIndexSymbol=true`, index payload | Do not expand constituents; query only the index through the `market_index` path; return only the index key and its historical points. | **Implemented in the public request path.** `processHistoricalDataRequest` disables expansion and the fetch service routes the batch with `isIndexSymbol=true`. |
| `isIndexSymbol=false`, index payload | Expand to the index plus its constituents; route the index to `market_index`, route constituents to `equity`; merge both result maps without losing symbols. | **Implemented for the expanded controller path.** The fetch service partitions `indexSymbols` and `stockSymbols`, performs separate batch reads, and merges the results. |
| `isIndexSymbol=false`, direct unexpanded caller | Expand and preserve a distinct response entry for the index and every constituent. | **Implemented.** The compatibility branch now maps a multi-result expansion to each resolved symbol, preventing constituent results from overwriting the index key. A focused regression test covers this contract. |

### Evidence-based routing chain

The verified chain is:

`MarketDataController` → `processHistoricalDataRequest` → `InstrumentUtils.resolveSymbols` →
`MarketDataFetchServiceImpl` partition → `MarketDataService.getHistoricalDataBatch` →
`HistoricalDataRetriever` → `HistoricalDataServiceImpl`.

When the batch flag is true, the shared service uses `MarketIndexIndicesService`,
whose persistence query targets the `market_index` measurement. When false, it
uses `EquityService`, whose persistence query targets the `equity` measurement.
The public false-flag index flow therefore does not intentionally query the index
through the equity measurement after the hotfix routing change.

### Plan additions after gap review

1. Add contract tests for index-only, expanded mixed index/equity, direct
   unexpanded callers, duplicate/aliased symbols, and partial results. Assert
   exact response keys and the measurement flag passed to each batch call.
2. Replace the direct-caller compatibility mapping with an explicit
   `resolvedSymbol → responseSymbol` contract so the index and every constituent
   remain distinct; never use a map keyed only by the original index input.
3. Make index classification resilient: explicit index keys remain authoritative;
   plain names require a batched metadata lookup; metadata failure must produce a
   truthful classification failure rather than silently querying `equity`.
4. Verify `market_index` coverage for the complete ten-year NIFTY 50 range using
   a read-only authenticated Influx query. If coverage is incomplete, backfill
   only missing index candles with idempotent writes and validate range/point
   counts before publishing the aggregate.
5. Reduce the synchronous Influx timeout to a bounded endpoint budget and apply
   missing-symbol-only provider fallback. A database timeout must not trigger a
   full provider fetch for symbols that already have valid database data.

### Trade-offs

- Mixed requests require two database reads and two cache paths, but this is
  required for measurement correctness; one mixed query would reintroduce the
  index/equity measurement error.
- Metadata classification adds one batched Mongo lookup on a cold request. This
  is cheaper and safer than 51 per-symbol lookups, but its failure state must be
  observable.
- Returning index plus constituents increases response size and serialization
  time. That is part of the requested contract; pagination or a separate
  constituent endpoint would change the API semantics and is out of scope.
- A shorter Influx timeout improves user latency but may return partial data more
  often. Fallback must identify only missing/failed symbols and must not present
  an incomplete result as complete.
- Backfill improves future latency and database coverage but adds provider quota,
  write load, and freshness coordination. It must remain bounded, idempotent,
  and outside the synchronous user request where possible.

### Unbiased plan review and rating

Strengths: the public true/false behavior is mapped to concrete code and
measurements; the N+1 resolver regression is addressed; the plan preserves the
response contract and identifies the remaining production proof requirements.

Gaps: complete `market_index` ten-year coverage is not yet proven, and live
post-deployment latency is not yet measured. Bounded microchunking is now
implemented with environment overrides, while full dependency tests remain
blocked by an unrelated stale test constructor in `SmartRecommendationServiceTest`.

**Plan rating: 9.1/10.** The core routing, N+1 avoidance, Redis write
validation, cache-key alignment, bounded timeout, and bounded Influx
microchunking are implemented and source compiled. The score remains below 10
because production `market_index` coverage, backfill need, and post-deployment
latency still require read-only verification.
