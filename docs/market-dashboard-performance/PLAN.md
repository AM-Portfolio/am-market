# Market Dashboard Chart — Logic-Only Plan

## Goal and constraints

During market hours, Upstox continues its existing WebSocket → Kafka → database flow. Chart requests read stored data and return clean 5-minute candles; they must not fetch from Upstox, publish to Kafka, or trigger background backfills. Keep API/DTOs, Kafka topics, measurements/tags, database writes, and the existing service phases unchanged. Reuse the existing retriever, index resampler, cache, and chart components where possible.

## Findings and gaps

- `HistoricalDataRetriever` can call Upstox on a cache/DB miss, enqueue fetched data to Kafka, and schedule single-flight backfills for partial DB results. Disabling only its synchronous provider tier would not prevent all provider calls.
- Current-day Redis data can pass the intraday freshness check based on date alone, even when newer WebSocket ticks are in Influx.
- Indian index reads return stored `market_index` points. The existing hotfix resamples them to 5-minute buckets, but duplicate timestamps and source conflicts need deterministic handling.
- NSE polling and broker streaming both write `market_index`. Existing tags identify the instrument, not necessarily the producer. With DB writes/tags fixed, source-specific NSE exclusion is impossible when both writers use the same tag set; do not claim otherwise.
- Modern UI aligns timestamps but carries missing intraday values forward. That creates false flat lines. Raw tick rendering also incurs repeated full-list scans; the backend’s 5-minute response should keep UI input small.
- Fixed 75 candles is valid only for a full regular NSE session. Holidays, late starts, missing ticks, early closes, pre-open, and after-hours can legitimately produce fewer points.

## Solution

1. **Use a chart-only read policy:** Intraday charts read Influx directly, bypass Redis, provider fallback, `saveDataAsync`, and single-flight backfills. A miss/partial result stays a miss/partial result in the existing response shape. Other retriever callers retain their current cache/provider behavior. This favors fresh persisted ticks over a potentially stale chart cache; DB read latency remains to be measured.
2. **Aggregate stored data once:** Reuse the Indian-index resampler. Sort by event time; reject null, non-finite, non-positive, and out-of-session values; align time-based buckets to 09:15 Asia/Kolkata. For raw ticks, OHLC is first/max/min/last LTP, never repeated day-level high/low snapshots. Deduplicate equal same-time prices; if same-time prices conflict and existing tags cannot identify the source, omit those ambiguous samples and log the count. Missing buckets stay missing.
3. **Handle UI gaps without changing API:** Keep the existing payload. Index each series by normalized timestamp once, add empty 5-minute slots within a same-day 1D view, and split rendered lines at null slots. Never carry a prior value across an intraday gap. Preserve longer-range continuity rules.
4. **Respect session/time edge cases:** Keep actual available buckets rather than requiring 75. Account for weekends/holidays, partial sessions, market open/close, UTC/IST conversion, and delayed Kafka persistence. The newest chart value is the newest persisted tick, not zero-lag live data.
5. **Protect unrelated behavior:** Do not disable/remove the NSE scraper or change its writes in this scope. Keep stock key mapping, daily-cache key alignment, and monthly-performance caching separate.

## Validation and review

- Verify cold and repeated intraday chart requests produce zero provider calls, Kafka publishes, and background backfills; WebSocket ingestion remains unchanged.
- Compare sample 5-minute OHLC to raw stored points. Cover out-of-order/duplicate ticks, same-time source conflicts, invalid prices, partial/stale data, missing one index, weekends/holidays, early close, and active final bucket.
- Confirm unchanged API JSON, sorted points, no fabricated flat segments, accurate session timestamps, and bounded backend/UI work. Measure DB latency before adding any new cache or rollup.
- Keep meaningful comments at non-obvious policy boundaries (chart-only retrieval, bucket/OHLC rules, timestamp assumptions, and no-forward-fill). Explain the invariant so reviewers can assess intent; avoid comments that merely restate code.

## Limitation and score

Without a source tag or write-path change, the chart cannot reliably distinguish NSE from broker rows when their existing tag sets match. This plan preserves the requested DB behavior and improves retrieval, aggregation, and rendering, but cannot guarantee producer-specific filtering. **Plan rating: 8.8/10**; the remaining 1.2 is this source-provenance limit plus production freshness thresholds that still need measurement. No API, schema, or DB-write changes are proposed.
