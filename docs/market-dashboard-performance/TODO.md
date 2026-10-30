# Market dashboard performance TODO

## October 22 large-index cold-cache latency slice — owner-approved scope

- [x] Keep InfluxDB 25-symbol sequential chunking unchanged (explicitly
  deferred; this is source-plan fix 1).
- [x] Convert one fetched history batch into `StockBars` for all symbols and
  pass it through bounded 50-symbol batches of the existing Redis
  historical-bar pipeline; preserve the current symbol/interval/date keys,
  TTL, and best-effort cache semantics.
- [x] Extend the existing daily analysis job to read all index names from the
  shared `nseindices.yml` registry, load each roster in one batched membership
  lookup, and warm indexes above the configurable minimum constituent count.
- [x] Use the loaded `application.yml` as the single scheduler settings source
  for the constituent threshold, warming toggle, and nine supported analysis
  windows (`1D`, `5D`, `1W`, `1M`, `3M`, `6M`, `1Y`, `3Y`, `5Y`).
- [x] Log roster size, timeframe, result coverage, and elapsed time for each
  scheduled large-index warm; do not cache incomplete results or invent values.
- [x] Add focused tests for multi-symbol cache pipelining inputs, empty/partial
  history, large-index roster gating, and scheduler failures; run relevant
  service/scheduler test reactors and verify the existing API response contract.
- [x] Confirm the cold request can still time out while scheduled warming is
  pending; no latency guarantee is made until the deferred Influx query work is
  separately approved and measured.

Verification: focused Maven reactor for `market-data-service` and
`market-data-scheduler` plus dependencies passed after the review correction
(2026-10-22); 12 focused service/scheduler/cache tests passed. No production
deployment or database mutation.

## October 22 requested code slice — implementation approved

- [x] Owner reviews the October 22 scoped plan and candle-boundary diagram
  before code execution.
- [x] Batch validated SmartStock quote-cache persistence once per existing
  recovery batch, including the daily previous-close recovery paths.
- [x] Narrow non-1D Heatmap history reads to the required baseline and ending
  windows while preserving both selected candle values and current `1D` path.
- [x] Remove the future-candle baseline fallback; omit symbols without a valid
  candle on/before the target boundary.
- [x] Add one aggregate, price/secret-free log per operation with batch sizes,
  returned candle counts, resolved/omitted counts, windows, and elapsed time.
- [x] Add focused service tests for one-batch persistence, unchanged Heatmap
  calculations, weekends/holidays, missing/future-only candles, and cache errors.
- [x] Run focused Maven tests, then the market-data analysis/service reactor;
  verify API response JSON remains unchanged (no response types or DTOs changed).
- [ ] Compare cold/warm timings and returned candle counts; record actual
  measurements without an assumed speedup. No production mutation/deploy.

Implementation verification: focused SmartStock, Heatmap, and Movers tests
passed after the final edits; the full market-data service/analysis reactor also
passed. Runtime cold/warm measurements remain pending; no performance claim is
made from unit-test timings.

## October 18 review — implementation in progress, production not approved

- [ ] Verify deployed image SHA, authoritative Influx URL/port configuration,
  override precedence and exact namespace using read-only inspection.
- [ ] Review plaintext HTTP/token transport and verify authorized read-only
  candle queries, bucket, org, identity, date windows and environment isolation.
- [ ] Correlate current quote-map loss, baseline loss and production trace;
  classify every trace category and measure warm/cold batch behavior.
- [ ] Verify every supported timeframe and index with calendar, membership,
  listing and corporate-action fixtures; compare Heatmap and Movers prices.
- [x] Add a bounded 50-symbol multi-symbol Influx range read after the existing
  Redis batch lookup; preserve old single-symbol methods through additive
  defaults. Compile both shared and market-data modules.
- [ ] Specify bounded existing scheduler/provider recovery, single-flight,
  idempotent persistence and failure behavior without fabricated/stale data.
- [ ] Run backend, UI as needed, Postman contract and fault tests in preprod.
- [ ] Obtain separate explicit production approval for exact config/deploy
  command, transport security, expected rollout, verification and rollback.
- [ ] Update architecture and final score only after unresolved checks; review
  revised plan, TODO and diagram with the user before implementation.

The checklist below records earlier work and hypotheses; it does not assert
that production is fixed or that prior checkmarks cover the new findings.

- [ ] Capture and classify warm/cold traces for Heatmap and Movers (`1D`, `1W`, `1M`).
- [x] Correct existing `AnalysisService` and `SmartStockService` timeframe paths;
  introduce a helper only if private duplication cannot be avoided. Do not add a
  route, DTO, table, service, or deployable.
- [ ] Capture the raw deployed `1D` Movers payload and prove whether the zero
  percentage arises in backend calculation or serialization before changing UI.
- [x] Require every emitted Mover to satisfy `change = end - base` and
  `percentChange = change/base*100`; rank by the validated percentage, not
  absolute change alone.
- [ ] Reject invalid price points and degenerate long-timeframe result maps;
  fetch, validate, and persist missing source history before serving.
- [ ] Never serve an expired/session-stale or prior snapshot after a failed refresh.
- [ ] Cache the full existing Movers ranking by index/timeframe/trade-date/
  membership-version, then apply the existing request `limit` after cache read.
- [ ] Add bounded single-flight background warming to the existing scheduler.
- [ ] Treat Redis failure as cache miss with bounded primary-store/provider fetch,
  validate, persist, then serve.
- [ ] Configure cadence/timeout limits without secrets, only if properties are absent.
- [x] Stop the Market UI heatmap merge from rendering unavailable as `0.00%`.
- [ ] Guard existing dashboard Movers state against rapid index/timeframe response races.
- [ ] Add backend, Flutter model/widget, contract, and trace regression coverage
  described in PLAN, including non-zero `1D` percentage badges.
- [ ] Deploy to preprod after owner approval; invalidate only proven-bad cache keys.
- [ ] Create REPORT.md only after checks and Postman regression pass.

Implementation evidence so far: common historical-data tests pass (3/3), the
new market-data symbol-normalization/batch-mapping test passes (1/1), and the
focused market-data-service + analysis reactor passes (all 133 tests). The first
unflagged common run was blocked by Java 23 versus pinned Byte Buddy support;
the successful runs used `-Dnet.bytebuddy.experimental=true`. No production
mutation, database write, API contract change, or deployment occurred. The full
13-module market-data application reactor also packaged successfully with tests
skipped for this final compatibility compile.
