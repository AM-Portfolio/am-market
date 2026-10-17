# Market dashboard performance TODO

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
