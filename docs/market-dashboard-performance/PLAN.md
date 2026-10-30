# Market Dashboard Performance and Long-Timeframe Correctness

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
