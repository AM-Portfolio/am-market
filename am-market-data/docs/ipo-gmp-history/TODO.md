# TODO — ipo-gmp-history

Pack: am-market-data/docs/ipo-gmp-history/

## Plan loop

- [x] Ingest user plan and API references.
- [x] Resolve catalog: am-market → am-market-data; market domain.
- [x] Read repo README, deploy identity, controllers, DTOs, services, Mongo models, Redis cache, admin routes/security, schedulers, and Postman collection.
- [x] Coverage check: partial; plan fills matching, optional response, history, cache, admin, quota, and failure gaps.
- [x] Confirm no related IPO GMP requirement pack exists.
- [x] Find live Postman Market Data API collection; note checked-in collection is stale for IPO requests.
- [x] Set P0/P1/P2 and backend/UI boundary.
- [x] Fill first-run, latency, identity, isolation, failure, corner matrix, and review roles.
- [x] Create PLAN.md.
- [x] Create seven-sheet architecture.drawio.
- [x] UI previews remain not requested.
- [x] Scorecard, adversarial review, and executable test plan drafted.
- [ ] User reviews PLAN, architecture, and TODO; confirm or adjust scope.
- [ ] Do not execute implementation until explicit user approval.

## Backend implementation — after user approval

### 1. IPO Guru provider

- [ ] Add server-side config for API base URL and secret key using existing secret/Vault conventions.
- [ ] Add typed HTTP client for one batch GET /api/v2/ipos?months=0 request per run.
- [ ] Model response envelope, IPO identity metadata, current GMP, plan/rate headers, and null/raw-string semantics.
- [ ] Add timeout, bounded retry for transient errors, Retry-After handling, and no retry for invalid key/plan/daily quota.
- [ ] Never include API key or raw auth headers in logs/errors.

### 2. Match and persist

- [ ] Keep Upstox document ID as internal ID and persist IPO Guru slug separately.
- [ ] Add named GMP and daily point models to UpstoxIpoDocument.
- [ ] Match only with agreed high-confidence fields; report unmatched cases instead of guessing.
- [ ] Append one history point for every successful capture; deduplicate retries with provider timestamp plus capture run id and reject older source timestamps for current GMP.
- [ ] Do not add a point for null/absent GMP; treat string zero as a valid reading.
- [ ] Stop future capture after listed state; retain collected history.
- [ ] Change full Upstox document writes so they preserve GMP enrichment/history.
- [ ] Keep partial provider responses from clearing existing values.

### 3. Scheduler, guard, metadata

- [ ] Add an independent IPO Guru GMP scheduler in market-data-scheduler.
- [ ] Add configurable enable/cron settings; default daily at 00:15 Asia/Kolkata after the IPO refresh.
- [ ] Require the latest successful IPO refresh marker; record BLOCKED_BY_IPO_REFRESH when absent/failed.
- [ ] Add one bounded startup catch-up for a missed IST date and an admin combined IPO-then-GMP recovery action.
- [ ] Use a dedicated tracked scheduler name and a shared run guard for scheduler/manual paths.
- [ ] Do not run on application startup.
- [ ] Record run type, times, source/error, and received/matched/unmatched/updated/skipped counts in existing IPO sync metadata.
- [ ] Confirm scheduler lock and metadata are visible to operations.

### 4. Admin operations

- [ ] Add POST /v1/admin/sync/ipo-gmp to trigger the same guarded batch refresh.
- [ ] Add GET /v1/admin/sync/ipo-gmp/status for last run and counts.
- [ ] Enforce explicit admin authority, not authentication alone.
- [ ] Return stable outcomes for accepted, already running, rejected, and failed states.
- [ ] Admin panel UI is a later am-modern-ui task after backend review.

### 5. API and cache

- [ ] Add optional current GMP to summary and detail response DTOs with named schema.
- [ ] Keep internal IPO id as the UI key; include optional provider/providerSlug metadata and keep all provider-added fields optional.
- [ ] Omit GMP when never available; preserve response behavior for all other IPOs.
- [ ] Return current GMP `updatedAt` and estimated listing price as optional fields; expose capture history through the history endpoint.
- [ ] Return optional oldest-first `gmpHistory` on the existing IPO detail response; do not add a separate history route in this slice.
- [ ] Invalidate the affected Redis IPO list/detail cache entries after a successful write.
- [ ] Update live Postman Market Data API collection; do not create a duplicate collection.
- [ ] Refresh OpenAPI and generated SDK only when its supported API workflow is chosen.

### 6. Tests and review

- [ ] Unit tests for request/response parsing, zero/null, timestamp ordering, mapping confidence, same-day idempotency, and failures.
- [ ] Regression test: routine Upstox full detail sync does not erase GMP/history.
- [ ] Security test: non-admin cannot trigger or inspect privileged sync status.
- [ ] Cache test: successful refresh returns new GMP on the next API read.
- [ ] Test that one external request serves all IPO rows and no per-IPO request loop exists.
- [ ] Run service unit tests after implementation.
- [ ] Run Postman rows 1–7 with provider mocked for failures; avoid burning the user's Free quota.
- [ ] Verify API key is read from local .env only for local testing and from Vault mappings in deployed environments; never commit or log it.
- [ ] Obtain written IPO Guru confirmation before enabling production history capture/display on the paid plan.
- [ ] Review implementation with am-code-review if user requests or before merge.

## Frontend phase — after backend approval and completion

- [ ] Admin panel uses status endpoint and manual refresh endpoint; show last run, counts, and error.
- [ ] Customer IPO views render GMP only when available, show provider update time, and link IPO Guru visibly.
- [ ] History chart consumes the history endpoint; empty history does not render a broken chart.
- [ ] Add UI flows and accessibility checks in am-modern-ui.
