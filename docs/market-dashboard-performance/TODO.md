# Market dashboard performance TODO

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
