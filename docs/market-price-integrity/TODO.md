# Market price integrity TODO

## Global-index integrity implementation (2026-10-25)

- [x] Compare all ten public symbols with the Upstox global instrument catalog: eight exact candidates and unmatched IXIC/SSEC; do not substitute benchmarks.
- [x] Trace provider→WebSocket/REST→Redis/Influx/Mongo→API→Flutter; preserve existing endpoint and response contracts.
- [x] Centralize public symbol mappings, exchange sessions, offsets, and provider-age settings in `global-market-schedule.yml`; unsupported symbols fail closed.
- [x] Seed empty config from that schedule and add an opt-in, field-only reconciler for existing config rows. It preserves sync state and historical records; it has not been run against a database.
- [x] Validate positive finite prices and provider event timestamps. During open sessions enforce the configured source-age window; while closed accept only a quote from the latest completed venue session.
- [x] Handle configured timezone, weekday/holiday schedule, and cross-midnight sessions in shared schedule logic; use provider event time for WebSocket cache/Influx writes and reject out-of-order cache updates.
- [x] Subscribe configured global instruments during streamer refresh and use the application's configured Jackson mapper for quote serialization.
- [x] Remove stale Mongo snapshot promotion in the global-index response path; preserve the existing response DTO and render missing quotes as pending rather than artificial zero in Market UI.
- [x] Add focused backend quote-freshness/session tests and UI availability model tests.
- [ ] Identify an exact, licensed, public-display-capable quote/history source for IXIC and SSEC. Do not proxy or fabricate either benchmark.
- [ ] Execute the opt-in Mongo config reconciliation in a controlled non-production environment, verify its diff/rollback, then disable the flag. No migration has been run.
- [ ] Bootstrap/backfill exact-key global history and verify timeframe baselines without rewriting old Influx provenance. (Code/tests are added; focused test execution, real provider data proof, and database backfill remain pending.)
- [ ] Reduce remaining cold REST call serialization and synchronous Influx persistence only after tracing provider limits and current writer behavior; these are not addressed by this change.
- [ ] Run full market-data and Indian-index regression tests; verify all ten mappings, exchange calendars/special closures, US date boundaries, and GIFT overnight edges.
- [ ] Run preprod Postman global-index matrix and review source-time, cache lineage, latency, and two exchange-timezone sessions before any production request.
- [ ] Create `REPORT.md` only after required tests and Postman validation have actually passed.


- [x] Confirm production symptoms using read-only API/log inspection.
- [x] Map direct and indirect Modern UI consumers.
- [x] Identify the separate chart, card, and movers calculation paths and duplicate timeframe loading.
- [x] Redesign the plan around one authoritative Market Data performance result and one Market UI cache.
- [x] User reviews and approves this plan.
- [x] Create `fix/market-price-integrity` from current remote main without disturbing existing worktrees.
- [ ] Discovery gate: capture current endpoint contracts, cache schemas/keys, membership owner, provider limits, Influx mapping, and baseline request/latency/error metrics.
- [x] Record the approved `1M` definition and exact broker fixture: as-of time, start session/value, end session/value, and expected percentage.
- [x] Add focused backend regression tests for stale quote rejection, timeframe base selection, unchanged quotes, and zero/missing provider rows.
- [x] Implement idempotent symbol qualification and true Upstox instrument resolution.
- [x] Implement dated previous-close semantics and cache validation.
- [x] Verify `getMoversUnified` enriches once and sorts the same snapshot for gainers and losers; preserve this path.
- [x] Verify the Top Movers request can enter provider history fallback through the 9-argument overload, the existing batch LTP size is configured as 500, and current per-symbol coalescing is JVM-local.
- [x] Make Top Movers history reads cache/database-only; recover current-price misses through the batched LTP path under an owner-token Redis lease.
- [ ] Capture the preprod baseline discovery report for one 50-member index: canonical identity, requested session range, row count, newest row date, and rejection reason. Do not log secrets, prices, or raw Flux.
- [ ] Add a date-bounded multi-symbol Influx historical query in the existing common persistence/service modules; do not reuse latest-only `findByTradingSymbolIn` as a previous-close source.
- [ ] Replace `HistoricalDataRetriever`'s per-symbol database loop with the bounded historical batch path: 100 identities per chunk and at most two concurrent chunks.
- [x] Correct previous-close snapshot semantics to record quote session and base session separately; accept a verified unchanged (`0.00%`) stock.
- [x] Use verified roster membership, rather than index-name minimums, for all-index Movers coverage, including NIFTY BANK and NIFTY IT.
- [ ] Seed streamer instrument-key-to-canonical-ticker mappings at subscribe time and reject ISIN-only cache keys for ticker lookups.
- [ ] Add tests for 50-member no-N+1 retrieval, NIFTY 500 chunking, non-numeric sector rosters, identity mismatch, session/calendar boundaries, unchanged quotes, and partial chunk failure.
- [ ] Preprod proof: trace a complete-index Movers request and verify bounded historical chunk count, non-zero valid bases, unchanged public JSON, and no request-time historical Upstox calls.
- [x] Remove the after-hours `previousClose` OHLC overlay and preserve a positive current quote. Verified same-session snapshot support remains part of the baseline lifecycle work.
- [x] Make chart `isIndexSymbol` optional and resolve omitted symbols server-side from the active security/index registries; retain the existing response shape.
- [x] Add OHLC regression tests for prior-close preservation and chart tests for omitted stock/index type. Same-session snapshot and unresolved-symbol coverage remain part of the baseline lifecycle work.
- [ ] Trace the production TCS 1D chart request read-only and compare deployed version, resolved identity, cache/database/provider attempts, and returned point count with local before declaring the chart issue fixed.
- [x] Require both a positive current price and valid comparison base; accept equal current/base prices as unchanged and exclude missing-base rows instead of ranking them at 0.0%.
- [x] Remove `OHLCDataRetriever` zero placeholders from successful results and verify they cannot be persisted as valid quotes.
- [x] Build streamer membership from configured broad/sector NSE indexes using one batch membership lookup; retain existing defaults, ETFs, and active-symbol behavior.
- [x] Treat empty, duplicate, or undersized index documents as scraper-repair candidates; exclude them until a validated replacement is saved.
- [ ] Verify sector membership shrink with a second scrape; report coverage because the NSE response model exposes no source total count.
- [x] Add cross-pod quote-refresh coalescing with owner-token Redis leases, bounded wait/timeout, cooldown, and fail-closed behavior when Redis coordination is unavailable.
- [ ] Track quote receipt time/source and upstream event time when supplied; prevent older ticks from overwriting newer cache values without changing public quote DTOs.
- [x] Keep SENSEX out of complete Movers coverage until a validated BSE constituent source is available.
- [x] Return existing empty `gainers`/`losers` arrays for an index when validated membership/quote/base coverage is incomplete; log structured counts without changing the response contract.
- [ ] Correct Influx Vault mapping in the proper secret store; commit only the mapping name.
- [ ] Replace unsupported CSI template usage with supported Vault/CSI value mappings for Influx URL, organisation, bucket, and token; never store values in repository files.
- [ ] Remove empty-token fallback behaviour and make missing/blank Influx configuration fail startup with a safe diagnostic.
- [ ] Verify the running preprod pod receives non-empty configuration without printing secret values, then run a read-only historical-price probe.
- [ ] Inspect live-ingestion worker health: active provider, resolved/subscribed key count, latest tick age, and zero-price write/rejection counts.
- [ ] Reject unresolved, zero, invalid, and stale LTPs before Redis latest-price cache writes.
- [ ] Add quote availability/freshness metadata while preserving existing quote fields and client compatibility.
- [x] Update Paper Trading quote state: terminal unavailable/retry UI and block zero-price order calculations/submissions.
- [ ] Add regression tests for live LTP, stale/zero cache value, disconnected subscription, after-hours close, and blocked zero-price order.
- [x] Repair and validate NIFTY 50 constituent ingestion.
- [x] Add reusable membership validation for null, duplicate, parent-index, and incomplete `data` arrays.
- [ ] Route invalid index documents through the existing constituent scraper; retain the last valid `data` array when scrape validation fails.
- [ ] Limit the Upstox EOD index refresh to metadata updates; it must not create/overwrite constituent lists.
- [x] Add member-count fixtures for NIFTY 50, NIFTY 100, NIFTY 200, and normalized NIFTY 500.
- [x] Change Heatmap and Movers to report unavailable price/history data instead of successful `0.00%` placeholders.
- [x] Reject degenerate all-zero heatmap cache writes for every constituent index.
- [ ] Preprod proof: membership recovery, metadata preservation, invalid Upstox key rejection, Influx failure handling, and partial Heatmap/Movers response.
- [ ] Request explicit production approval before the one-time existing-scraper repair refresh.
- [ ] Add membership version/hash and normalized added/removed-member audit logging.
- [ ] Add atomic field-level membership/metadata updates with conflict retry.
- [ ] Add one coalesced asynchronous repair per invalid index; no user-request scraper wait.
- [ ] Version or invalidate per-index Heatmap and Movers cache after successful membership repair.
- [ ] Add an additive detailed availability response; preserve existing simple Heatmap contract.
- [ ] Add market-session/trade-date freshness checks for cached prices.
- [ ] Add recovery tests for partial lists, wrong-but-count-correct lists, concurrent price/membership writes, cache invalidation, and interrupted repair resume.
- [ ] Add an existing-service index catalogue with exchange/source path, versioned count policy, parent-row identity, and Heatmap/Mover eligibility.
- [ ] Add and validate SENSEX through its BSE membership path; return unavailable until valid membership exists.
- [ ] Add sector-index count-policy fixtures, including rebalanced NIFTY BANK and NIFTY IT.
- [ ] Rotate the database credential exposed outside Vault and confirm no secret is committed or logged.
- [ ] Remove authorization-bearing request logging.
- [ ] Add the canonical additive timeframe-performance contract, shared by charts and index cards.
- [ ] Replace overlapping Market UI history loads, duplicate caches, and stale response races.
- [ ] Add old/new cache and currently deployed client compatibility tests.
- [ ] Add exchange-calendar, close-boundary, rapid timeframe-change, corporate-action, new-listing, mixed-segment, rate-limit, token-expiry, and global-index tests.
- [ ] Add feature flags for strict close validation and resolver rollout.
- [ ] Run focused backend and Flutter checks.
- [ ] Deploy to preprod after approval and execute the Postman MCP test plan.
- [ ] Verify the Market UI cards, chart, and movers against the same fixture. Inspect non-Market consumers read-only and document separate issues.
- [ ] Ask for explicit production deployment approval with command, impact, risk, and rollback.
- [ ] Produce `REPORT.md` only after tests are verified.

## Movers and heatmap cache reliability (API-safe follow-up)

- [x] Carry index-member ticker/ISIN identity into the existing single batched Upstox LTP recovery; use a validated ISIN key only when the provider-master mapping is absent or conflicts, and keep ticker/API response identity unchanged.
- [ ] Capture preprod traces for NIFTY 50 and one broad-index Movers/Heatmap request, including cache, database, provider, and response spans.
- [x] Add `SmartStockService` latest-price-first lookup before provider recovery; full calendar trade-date validation remains tracked separately.
- [ ] Prove one cold-roster recovery uses one bounded provider batch and the second request is Redis-only.
- [ ] Keep full validated-roster coverage for Movers across every index; never use an index-name count or return a partial ranking.
- [x] Generalize Heatmap degenerate-result protection to every validated constituent index.
- [x] Reject missing, non-finite, and non-positive start/end Heatmap points; do not emit or cache false `0.00%` or `-100%`.
- [x] Prevent interactive broad-index Heatmap requests from invoking per-member historical provider fallback; use the existing scheduler repair path.
- [x] Add named `TopMoversResponse` DTO while preserving current list/object JSON shapes; OpenAPI response annotation remains pending.
- [ ] Add/update Postman requests and assertions for canonical Movers and Heatmap routes, cold/warm data, large indexes, and contract compatibility.
- [ ] Verify current UI rendering of omitted Heatmap symbols; add only the necessary availability-state UI support if omission currently appears as `0.00%`.
- [ ] Run focused unit/integration tests, Postman regression, and preprod trace review before any production approval.

## Latency-engineering release gate

- [ ] Export and classify every Tempo span for warm/cold Movers and NIFTY 50/NIFTY 500 Heatmap requests across all seven required categories.
- [ ] Add safe Redis-exception fallback to the primary database path; prove valid database data does not become an empty API response when Redis is unavailable.
- [ ] Enforce Redis (<=500), Mongo (100-250), and Influx (<=50, max two concurrent) micro-chunks with partial-chunk handling.
- [ ] Prove cache hits make zero synchronous Redis writes and broad-index Heatmap requests make zero synchronous historical-provider calls.
- [ ] Record a preprod latency histogram and approve the measured release budget before production rollout.

## PR #64 review follow-ups

- [x] Keep the shared index-size matcher; verify 150/250/500 boundary tests.
- [x] Keep the seeder environment-only Mongo connection and preserve prices during roster updates.
- [x] Keep date-aware previous-close selection when today's candle is absent.
- [x] Preserve standalone index documents such as INDIA VIX without scheduling constituent repairs.
- [x] Deduplicate identical index members, but reject conflicting duplicate identities.
- [x] Cache valid partial heatmaps only when both the roster and result meet explicit coverage thresholds; preserve the last valid cache on an incomplete refresh.
- [x] Trust a same-session previous-close snapshot even when the stock is unchanged.
- [x] Coalesce overlapping mover recovery across pods with owner-token per-instrument leases and bounded waits.
- [ ] Replace derived-index ticker sets with validated official feeds after confirming feed shape and minimum counts.
- [x] Make the local runner portable and make offline Maven mode opt-in.
- [x] Use a clear empty Influx URL default while retaining fail-fast validation when required configuration is absent.
- [x] Run admin background work on a bounded Spring-managed executor and keep the existing endpoint response contract.
- [x] Add focused regression tests for VIX, duplicate roster entries, partial heatmap cache behavior, and unchanged snapshots.
- [ ] Add a Redis-backed concurrency regression test for overlapping quote recovery.
- [ ] Run focused module tests when the private Spring Boot dependency metadata is available locally.

## Optional quote details and market depth

- [ ] Inventory quote endpoint consumers and identify any strict JSON decoders before extending `/quotes`.
- [ ] Add typed provider-neutral quote snapshot/depth DTOs and map available provider fields in each adapter.
- [ ] Extend Redis real-time quote snapshot serialization/read paths to retain optional fields; preserve legacy cache entries without a flush.
- [ ] Keep transient depth out of historical Influx candles and Mongo security documents unless a separate proven access pattern requires persistence.
- [ ] Extend the quotes response additively with optional fields while preserving existing field names, types, and envelope; update OpenAPI and Postman.
- [ ] Validate previous close/change against the same quote snapshot and market session; omit unsupported or inconsistent values.
- [x] Apply one provider/cache depth freshness rule: allow up to 2 seconds of clock skew and omit missing or stale depth rather than representing it as zero.
- [ ] Add adapter, Redis round-trip, API compatibility, legacy-cache, stale-depth, partial-provider, and multi-provider tests.
- [ ] Verify Paper Trading consumes returned depth, then capture a preprod warm-cache and cold-cache trace before release.

## Upstox V3 global-index history slice

- [x] Add global-only V3 URL routing for daily/weekly/monthly history; retain existing V2 paths for minute/hour requests and non-global instruments.
- [x] Implement validated, config-driven daily backfill in the existing admin global sync; pending records and explicit `force=true` only.
- [x] Bound lookback to configured 1–10 years; skip blank/mismatched keys and keep per-symbol failures isolated.
- [x] Preserve provider timestamp instants and only persist valid, deduplicated candles under the existing instrument key/measurement.
- [x] Replace global OHLC `mean` aggregation with first-open / max-high / min-low / last-close semantics.
- [x] Add provider URL, candle validation, sync gating/error, timestamp, idempotency, and aggregation regression tests (including forced refresh, no completed session, provider/Influx failures, and duplicate-sync gating; execution pending).
- [ ] Run focused provider, persistence, service tests; then market-data/Indian-index regression.
- [ ] Preprod only: verify live Upstox history for every configured exact key, requested range/row counts, timeframe baselines, and Postman contract snapshots. No production calls or mutations without separate approval.
- [ ] Leave IXIC/SSEC, Mongo reconciliation, actual database backfill, and provider redistribution approval gated until separately verified/authorized.
