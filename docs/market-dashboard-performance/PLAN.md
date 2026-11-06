# Market Dashboard Charts and Monthly Performance

## Goal and boundaries

Fix the index-chart sawtooth, return correct stock/index history, and make repeat monthly-performance requests fast. Preserve public routes and JSON. Use existing services, scheduler, cache, and measurements. Production remains read-only; deployment, flag changes, cache invalidation, and data repair need separate approval.

## Evidence and confidence

- **Verified:** checkout `hotfix/priority` is `109f36a3`, matching the report. `MarketDataOrchestrator` calls the two-minute scraper without checking `scheduler.indices.enabled`; production logs show it runs repeatedly. `@ConditionalOnProperty` on the ordinary service method does not guard that call.
- **Verified:** the NSE scraper and Upstox stream can both write Indian index points to `market_index`. The exact stale NSE prices and their causal link to each reported chart spike still need reproduction.
- **Verified:** Indian index reads return raw `market_index` points; the common history service bypasses resampling for every non-empty interval. The chart API only sorts and removes exact timestamp duplicates. This supports the raw-point/comb diagnosis, but the report's `<50 ms` result is not an end-to-end benchmark.
- **Verified:** live writes partition equity/index/global counts, and historical index writes log `measurement=market_index`. This proves routing intent and observed routing, not that stored Influx data has no historical pollution.
- **Verified:** analysis-cache read/write key shapes match. Production logs report GrowthBook `redis-enabled=false`, explaining analysis-result cache misses. Redis host configuration is a separate, unverified issue: YAML derives `REDIS_HOSTNAME` from `INFRA_HOST`; `REDIS_HOST` alone is not used unless another deployment override supplies the host.
- **Verified:** raw-candle cache has a same-day mismatch: writer uses `stock:historical`, reader selects `stock:intraday` for today. Also, the feature-flagged template intercepts `multiGet`, but not `executePipelined`; disabled-cache reads may miss while pipeline writes still reach Redis.
- **Likely P0:** stock history can be dropped when `resolvedToOriginal` uses a prefixed symbol but the repository returns a cleaned symbol. The source mapping supports this failure; confirm on the production endpoint before claiming every stock is affected.
- **Unproven:** weekends/holidays alone force every request to Influx. The reader probes calendar dates, but retriever accepts partial history with a date tolerance. Measure actual misses before treating weekends as the cause.

## Recommended freshness

**Recommend Option A:** monthly results represent the latest completed market close and refresh once after close. This keeps the ten-year calculation out of the request path and is simplest to operate. The active month changes after a completed close, not intraday; make the as-of behavior clear in the existing UI if needed. Do not promise sub-50 ms until measured end to end. If live intraday updates are a confirmed requirement, consider Option B (live quote overlay) as a separate follow-up.

## Implementation sequence

### P0 - Stop conflicting index writes

1. Put an explicit `scheduler.indices.enabled` check at the scheduled entry/call boundary, or conditionally register the scheduler bean. With false, no NSE request/write; with true, retain the bounded run.
2. Use one accepted source per index/time window. Reject stale/out-of-order fallback data. Keep Upstox primary; retain NSE only with explicit source precedence.
3. Log safe writer identity, accepted/rejected counts, and data age; do not log prices or credentials.

### P0 - Restore stock history responses

1. Fix the `resolvedToOriginal` mapping so normalized repository keys map back to the requested response keys. Preserve exchange/instrument identity; allow a clean-symbol fallback only if unambiguous. Never merge NSE and BSE listings.
2. Test `RELIANCE`, `TCS`, and `INFY` through the normal full history/chart path, with prefixed and unprefixed identities. Confirm data and response keys; verify production behavior read-only before attributing the current outage to this bug.

### P0 - Fix index resampling correctly

1. Explicitly fix the bypass in `HistoricalDataServiceImpl`: for Indian indexes (`isIndexSymbol=true`, excluding `GLOBAL_INDEX`), raw index data must be aggregated unless the repository has already aggregated it. Do not merely flip the condition.
2. The existing generic `processDataByInterval` helper is not safe to enable unchanged: minute buckets anchor to the first point, and OHLC/volume aggregation may use daily snapshot fields. Correct it or use an index-specific Influx query.
3. For a one-day chart, derive five-minute OHLC from ordered last-price samples (first, max, min, last), aligned to 09:15–15:30 Asia/Kolkata. Aggregate volume only if source semantics are confirmed. Keep exactly one aggregation layer.
4. For ten-year monthly data, use one validated close per NSE trading date, deriving date in Asia/Kolkata. Validate finite positive prices and consistent adjustment basis. Never apply five-minute downsampling to monthly inputs.
5. Preserve public JSON. Sort/deduplicate after aggregation as a final guard.

### P1 - Keep measurement and cache paths coherent

1. Keep Indian indexes in `market_index`, equities in `equity`, globals in `global_market_index` for live and historical writes. Registry failure must not silently route a known index as equity.
2. Test mixed batches, aliases, explicit index keys, prefixed equities, and global keys; assert exact writer/repository calls and response keys. Audit Influx read-only for index identities in `equity` and equity identities in `market_index`; report counts/ranges, do not clean data here.
3. Verify effective Redis host/property and GrowthBook flag separately. Use one canonical host setting; do not silently fall back to localhost in production. Do not enable the flag until connection health is confirmed.
4. Apply the feature flag consistently to reads and pipeline writes. Disabled mode must not write through one path while forcing misses through another; logs must not label a no-op as a successful write.
5. Align today's daily-bar read/write prefix. Make cache coverage trading-calendar-aware and prove weekends are not treated as missing trading sessions.
6. Warm the exact monthly UI key (`analysis:history:<canonical-symbol>:10:false`) after close for configured indexes only, after source data is complete. Coordinate rebuilds across pods; bound cold-miss wait, Influx timeout, and missing-range-only fallback.
7. Cache only complete, validated matrices. A non-empty partial matrix is not a valid snapshot. Failed repair preserves the last valid result; successful durable repair refreshes the exact key.

## Acceptance matrix

| Case | Expected | Verification |
|---|---|---|
| Scheduler flag false/true | No NSE call/write when false; bounded run when true | Scheduler test + preprod logs |
| NSE/Upstox race or same timestamp | Deterministic source precedence; stale point cannot win | Race and tie fixtures |
| Stock key normalization | Correct data under requested key; no NSE/BSE collision | Full endpoint tests |
| 1D index chart | Session-aligned five-minute OHLC, chronological, no comb, same JSON | Repository/service test + end-to-end point/payload count |
| Ten-year monthly | Daily close per trading date; correct month/year/overall values | Independent candle fixtures |
| After-close snapshot | Active month as-of latest completed close | Scheduler/cache/API test |
| Mixed live/historical writes | Each instrument reaches only its measurement | Persistence tests + read-only Influx audit |
| Redis disabled/unavailable | Explicit bypass reason; coherent no-op; bounded fallback | Feature-flag/connection fault tests |
| Same-day candle/weekend | Reader finds written bar; absent non-trading dates do not invalidate coverage | Read-after-write + calendar fixture |
| Partial history/repair failure | No incomplete cache overwrite; preserve valid snapshot | Retriever/cache integration tests |
| Cold-cache burst/owner failure | One bounded rebuild; truthful waiter response; retry can recover | Multi-instance lease/timeout test |

Measure end-to-end cold/warm latency, DB time, response points/bytes, cache hit/bypass reason, and provider calls in preprod. Do not claim 150x or sub-50 ms from offline resampling alone.

## Rollout and plan review

1. Reconcile the stale `TODO.md` and `architecture.drawio` with this plan before implementation review.
2. Implement locally and run focused scheduler, identity, aggregation, cache, measurement-routing, and API contract tests.
3. Validate in preprod with source-specific logs, read-after-write, measurement audit, and cold/warm traces.
4. Keep production inspection read-only. Any flag/config change, cache invalidation, data repair, or deployment requires a separately reviewed action and explicit approval.

## Adversarial review and rating

The previous plan was **8.8/10**, but review found three material gaps: stock chart data loss was only P1; it did not explicitly require fixing the `isIndexSymbol` resampling bypass; and current-month freshness remained undecided. It also needed to warn that the existing generic resampler is unsafe to enable unchanged.

**Revised plan rating: 9.0/10.** P0 now includes stock-response restoration and index resampling semantics; Option A is the recommended freshness policy; tests and proof gates distinguish source code, logs, and stored-data evidence. It is not 9.5+ because production Redis settings and Influx contents remain unverified, reported stock failures and latency need live/preprod reproduction, and the existing TODO/architecture pack still needs reconciliation. The `<50 ms` / `150x` result is a target to measure, not a guarantee. No implementation is included in this plan update.
