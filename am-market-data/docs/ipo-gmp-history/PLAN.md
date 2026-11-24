# PLAN — IPO Guru GMP and daily history

| Field | Value |
|---|---|
| Kind | feature |
| Slug | ipo-gmp-history |
| Lead repo | AM-Portfolio/am-market |
| Lead service | am-market-data |
| Other repos | None in the backend slice; am-modern-ui is a later UI slice |
| Target env | preprod after backend review and implementation |
| Branch | feature/ipo-gmp-history |
| Delivery rating | 9/10 |
| Design rating | 9/10 |
| Scorecard overall | 9/10 |
| Agent satisfied | yes — plan ready for product-owner review; no implementation authorized yet |

**Execution gate:** This pack is for review. Do not implement until the user explicitly approves it.

## Goal

Add IPO Guru current GMP to the existing Upstox IPO API, collect one daily GMP observation per IPO, retain those observations in the existing **upstox_ipos** MongoDB collection, and expose current GMP and history without changing responses for IPOs that have no GMP.

Provide an independent scheduler and a protected manual refresh/status API so an administrator can recover a missed or failed scheduled run. This slice is backend-first. The admin panel and customer UI are a follow-on UI slice after backend approval.

## Source plan (pointer)

| Field | Value |
|---|---|
| Origin | pasted-chatgpt |
| Pointer | User request in this conversation; IPO Guru API docs and changelog supplied by the user |
| Summary | Separate daily GMP scheduler; store one sample per day; use existing Upstox IPO Mongo collection; include optional GMP so missing values do not break consumers; add manual admin refresh; backend first, UI later; test on Free and use paid plan for production; retain our own history because provider history is not included in the test plan. |
| Catalog | Market domain; repo am-market; nested service am-market-data; core feature Upstox IPO data. |
| Context files | am-market/README.md describes am-market-data as the Spring Boot market API; am-market-data/.am.yaml identifies the deployable; IPO controller, service, Mongo document, Redis cache, admin controller, and scheduler were inspected. |
| Analysis source | local |
| Coverage | partial — current GMP and stored history are missing; two IPO paths and cache/document replacement hazards needed to be accounted for. |
| AM gaps | Filled below: identity mapping, optional contracts, idempotent daily history, shared run lock, cache refresh, admin authorization, failure/latency behavior, and backend/frontend boundary. |

**Context verified from repo:** The IPO list route is served by UpstoxIpoController; the provider path caches summaries/details in Redis and falls back to MongoDB; nightly Upstox and three-hour NSE jobs are already separate.

## Images

- No user attachments.
- No UI preview requested or generated.

## Prerequisites

| Check | Status | Notes |
|---|---|---|
| IPO Guru API | available | User supplied the key for Free-tier testing. Store it server-side only. |
| Existing API | available | Dev IPO list curl returned 200 at /market/v1/market-data/ipo?status=open. |
| Existing Postman collection | found | Live Market Data API collection contains IPO requests. Checked-in collection is missing those requests; update live collection during implementation using am-postman. |
| Production data terms | pending before production | Confirm with IPO Guru in writing that the chosen paid plan permits storing/displaying our own daily captured snapshots. Free is for evaluation; provider GMP history is Standard. |
| Scheduler/runtime | available | am-market-data already has tracked and locked scheduled jobs. |
| Local runtime | not running during discovery | localhost:8092 did not accept a connection; Dev endpoint was used for the read-only response check. |

## World-class feature map

### P0 — backend slice

- Add a server-side IPO Guru client using HTTPS and the X-API-KEY header.
- Make one batch request per scheduled/manual run. Preferred request: GET /api/v2/ipos?months=0. Its rows include the Guru slug, type/status, dates, price band, listed state, and current GMP together, supporting matching without one request per IPO. If live contract verification finds a required identity field missing, use one batch GET /api/v2/gmp plus persisted mapping; never poll once per IPO.
- Match an IPO to the internal Upstox document by persisted Guru slug after first association. For new associations, require agreement on normalized name, issue board/type, open date when known, and price band. Use symbol/ISIN only when Guru supplies it. Do not auto-attach an ambiguous match.
- Add optional current GMP and a GMP history array to the existing upstox_ipos document. Every successful scheduler/manual capture appends a new observation; the current GMP is replaced only when the incoming provider timestamp is newer. A retry with the same provider timestamp and capture run id is idempotently ignored, preventing accidental duplicate rows.
- Expose optional current GMP on the existing summary response and current GMP plus optional `gmpHistory` on the existing detail response. Do not add a separate history route in this first version.
- Add a separate configurable IPO Guru scheduler in am-market-data. Run after the successful IPO refresh, with a default at 00:15 IST (the existing Upstox refresh is around midnight). Do not merge it into the Upstox or NSE scheduler and do not add an automatic startup request.
- Make the dependency explicit: the GMP run reads the latest completed IPO refresh marker. If that marker is missing or failed, skip GMP capture and record `BLOCKED_BY_IPO_REFRESH`; retry once after a bounded delay and expose a manual combined recovery action. If the server was down at the scheduled time, a startup catch-up checks for the missing local date and runs the same guarded sequence once; it does not blindly replay old provider data.
- Add manual admin refresh and last-run status APIs. Both scheduler and manual call use the same run guard; an active run cannot be duplicated.
- Keep run outcome/counts (received, matched, unmatched, updated, skipped, error) in the existing IPO sync metadata collection.
- Protect refresh/status with an admin authority check. Existing production config authenticates /v1/** but does not currently show an admin-role restriction.
- Invalidate affected Redis list/detail entries after a successful GMP write so the response reflects the refresh immediately.
- Preserve GMP fields when Upstox refreshes IPO documents. Missing GMP or Guru rows must never clear history or break the existing IPO response.
- Add named OpenAPI DTO schemas and update the live Postman collection for response/admin operations.
- Keep the response provider-neutral: the UI supplies the internal IPO `id`; responses include that same internal `id` plus an optional `gmp` object with `provider` and `providerSlug`. A future provider adapter can replace IPO Guru without changing the UI route or internal identity.
- Keep all newly introduced provider fields optional. The stable contract is the internal IPO id, optional current GMP, and optional history; unknown provider fields are not required by clients.

### P1 — after backend is reviewed

- Admin UI surface to show last run, counts, error, and a manual refresh action.
- Customer-facing GMP display and history chart. Hide the GMP section when no reading exists; include source timestamp and required Data: IPO Guru link.
- Generate/update relevant SDKs from backend OpenAPI when the UI slice begins.

### P2 — later

- More than one daily observation or intraday history.
- IPO Guru subscription, anchor, company financial, or other endpoint data.
- Automatic recovery/backfill of missed days. Free/Basic does not provide provider history, so a missed day cannot be reconstructed from our own captures.

## First-run / virtual value UX

N/A — market data, not portfolio balance or virtual capital. For an IPO with no GMP, backend returns the existing IPO response shape; existing consumers continue to render the IPO.

## Latency & consistency

- One batch IPO Guru request per scheduled/manual run; never one call per IPO.
- Free key limits: 1 request/minute and 10/day. Honor Retry-After, inspect X-RateLimit-Remaining, and do not retry a daily quota failure before reset.
- Keep provider timestamp and our capture timestamp. Use source timestamp to reject out-of-order values. Store observation date in Asia/Kolkata.
- Daily capture is not intraday freshness. Show the provider timestamp; do not call GMP live when the timestamp is old.
- On timeout, 429/402/5xx, or malformed data, keep previous successful values/history; report the run failure in admin status. IPO list/detail must continue working.
- Successful refresh invalidates affected list/detail cache entries. Redis remains a cache, not the source of truth.

## Current system (verified)

- UpstoxIpoController serves GET /v1/market-data/ipo and GET /v1/market-data/ipo/{id}. Dev response had name, symbol, ISIN, board, dates, issue size, band, subscription, but no GMP.
- UpstoxIpoDocument maps to MongoDB upstox_ipos; it has no GMP fields.
- UpstoxIpoService reads Redis, then MongoDB, then Upstox. Its full detail writer builds a new document before saving, so extensions must preserve GMP/history or use a targeted update.
- UpstoxIpoCacheService caches IPO lists/details. It has calendar invalidation but no IPO list/detail invalidation.
- UpstoxIpoSyncScheduler runs nightly at midnight IST and on startup. IpoSyncScheduler runs current/upcoming/subscription every three hours. Both use separate tracked scheduler locks.
- MarketDataAdminController already has /v1/admin/sync/ipo for the separate NSE feed. No IPO Guru control exists. Production security authenticates /v1/**; inspected config does not enforce a distinct admin role.
- A second NSE-oriented IPO API writes to ipo_issues; this plan does not merge pipelines.
- Guru and Upstox slugs differed in the live Shree Siddhivinayak example; normalized name, SME type, date, and band aligned. Guru’s symbol was null.
- Observed IPO Guru /gmp response included Closed and Upcoming records, although endpoint overview describes open/upcoming. Validate lifecycle and timestamps rather than capturing every row blindly.

Code references: [UpstoxIpoController.java](../market-data-api/src/main/java/com/am/marketdata/api/controller/UpstoxIpoController.java), [UpstoxIpoDocument.java](../market-data-service/src/main/java/com/am/marketdata/service/model/UpstoxIpoDocument.java), [UpstoxIpoService.java](../market-data-service/src/main/java/com/am/marketdata/service/ipo/UpstoxIpoService.java), [UpstoxIpoCacheService.java](../market-data-service/src/main/java/com/am/marketdata/service/redis/UpstoxIpoCacheService.java), [UpstoxIpoSyncScheduler.java](../market-data-scheduler/src/main/java/com/am/marketdata/scheduler/UpstoxIpoSyncScheduler.java), [IpoSyncScheduler.java](../market-data-scheduler/src/main/java/com/am/marketdata/scheduler/IpoSyncScheduler.java), [MarketDataAdminController.java](../market-data-app/src/main/java/com/am/marketdata/controller/MarketDataAdminController.java), [SecurityConfig.java](../market-data-api/src/main/java/com/am/marketdata/api/config/SecurityConfig.java).

## Target journeys

1. **Daily capture:** Separate Asia/Kolkata scheduler acquires shared run guard, fetches batch IPO/GMP once, validates rows, maps known IPOs, updates current GMP and same-day history in upstox_ipos, records run metadata, then invalidates relevant Redis entries.
2. **Optional current data:** Existing IPO list/detail adds a named optional GMP object only when a successful reading exists. IPOs without GMP return normally without an empty placeholder.
3. **History read:** The existing IPO detail response returns optional oldest-to-newest `gmpHistory`. Existing IPOs with no points omit the optional field.
4. **Admin recovery:** Admin views last-run metadata, triggers the same batch sync, and sees counts or an “already running” result. Refresh does not mutate IPO source fields.
5. **Frontend later:** Customer pages render GMP/history only when available and show timestamp and IPO Guru credit. Admin UI calls protected status/refresh API.

## Identity & ownership

- Internal identity remains existing Upstox document _id; Guru and Upstox slugs are not interchangeable.
- Store external stable identity as providerSlug in the GMP block.
- First association uses normalized legal/display name, board, opening date, and band. Pre-RHP dates may be null; match only if remaining keys are unique, otherwise report unmatched.
- The Upstox IPO record owns GMP and its history because user selected upstox_ipos. No new Mongo collection proposed.
- API DTOs are named. GMP/history are additive and optional; omit GMP block when no successful value exists.
- Daily point fields: IST date, raw GMP price/percentage, raw kostak/subject-to-sauda, estimated listing price, provider update timestamp (`updatedAt`), local capture timestamp, capture run id, provider slug. Preserve raw fields; "0" is valid and null is unknown. `updatedAt` is returned to clients as the source freshness timestamp.

## State & money

- No portfolio money. GMP is provider data; estimated listing price is not advice or a listing prediction.
- Current GMP is most recent successful source reading. History has one point per IPO per IST date, oldest-first on read.
- Every successful capture appends history; same provider timestamp plus run id is deduplicated, and an older provider timestamp cannot replace current GMP. Manual recovery therefore adds a real observation when the provider changed, without creating retry noise.
- Stop capturing after IPO is listed; retain saved history and last reading.
- Missing rows/null GMP do not append or erase history.
- Expose source timestamp.

## Isolation

- New job has own scheduler name/config, run metadata, and shared scheduler/admin execution guard.
- Upstox and NSE jobs remain independent. GMP failure does not affect them or normal IPO API responses.
- History is one point/day and stops growing after listing. Read history from selected IPO document only.
- Full Upstox writes preserve GMP subdocument/history. GMP updates target GMP fields only.
- Invalidate affected cache entries; never clear the entire Redis database/keyspace.

## Failure modes

- Missing/invalid key: report source unavailable; no secret in logs or response.
- 429/quota/timeout/5xx: retain data; mark run failed or partial; honor provider retry/reset guidance.
- Parse drift: reject malformed items individually; no mass clear on partial response.
- Ambiguous match: count unmatched and do not attach.
- Null GMP: no new sample; zero remains a real sample.
- Non-admin refresh/status: 401/403, no provider call.
- Overlapping manual/scheduled run: shared guard blocks duplicate.
- Redis outage: Mongo is authoritative; list/detail falls back without breaking.
- Upstox full save after GMP write: regression test proves GMP/history survive.
- No prior GMP: optional field absent and rest of API valid.

## Corner-case matrix

| Case | Trigger | Expected | Covered by |
|---|---|---|---|
| Missing GMP | Null price or absent provider row | No GMP block/point; IPO API still succeeds | Unit tests; Postman row 4 |
| Zero GMP | price is "0" | Persist/return zero | Provider/domain unit test |
| Ambiguous mapping | Similar names or duplicate IPO candidates | Skip and count unmatched | Mapper unit test |
| Pre-RHP | Dates null, dates_tba true | Match only if remaining keys are unique | Mapper unit test |
| Same provider response retry | Scheduler retry/manual rerun | One observation for the same provider timestamp/run; changed provider value appends a new point | Persistence test |
| Out-of-order result | Older provider timestamp arrives | Do not roll back current/history | Persistence test |
| Rate limit | Provider 429/daily limit | Honor Retry-After/reset; retain prior state | Client test; Postman row 5 |
| Timeout/malformed | Network/schema failure | Failure metadata; no partial clearing | Client/service test |
| Overlap | Trigger while another run active | Shared guard prevents duplicate call/write | Lock test; Postman row 3 |
| Upstox refresh | Full document save after GMP write | GMP/history preserved | Regression test |
| Stale Redis | Cache holds pre-refresh DTO | Relevant list/detail cache invalidated | Cache test; Postman rows 4–6 |
| Non-admin | Authenticated user lacks admin role | 403, no vendor request | Security test; Postman row 2 |
| Empty history | IPO has no captured points | 200 and empty data | Service test; Postman row 7 |

## Out of scope

- Customer UI/chart and admin panel UI in this backend slice; protected APIs are included for the later panel.
- Standard/Pro history endpoints, intraday GMP, subscription, anchors, OFS, pre-IPO.
- Backfill old dates; no Free/Basic provider history.
- Separate NSE ipo_issues flow, portfolio/account data.
- Plan upgrade, key rotation, deploy.

## Open questions

None in backend design. Production capture/display of our own daily history is gated on written IPO Guru confirmation that the chosen paid plan permits it. UI is intentionally later.

## Review roles

| Role | When | Looks for |
|---|---|---|
| Agent architect | Before user review | Code evidence, cache/data preservation, safe matching, scheduler isolation, failure coverage |
| Product/tech owner | This review gate | Same collection, daily granularity, admin controls, backend/frontend boundary, production terms prerequisite |
| Service owner | Before Execute | Auth patterns, API contract, Postman/OpenAPI |
| Optional reviewer | If requested | am-code-review on pack or implementation |

## UI previews

- Consent: not-requested
- Gate: fail for backend-only slice.
- Previews: n/a — not requested

## Services

| Service | Existing/new | Implementation notes | Unit tests |
|---|---|---|---|
| am-market-data | existing | Provider, optional DTO, same-collection daily history, independent scheduler, admin refresh/status, cache invalidation. No new deployable. | Parsing, mapping, idempotency, preserve-on-refresh, auth/lock, cache. |
| am-modern-ui | later | Admin controls and customer GMP display after backend review. | UI flows in later plan. |

## Test Plan (Postman or MCP)

Collection: live **Market Data API**; update it with am-postman during implementation. Use existing Dev environment then. No API key in collection.

| Row | Request/check | Method + path | Expected | Owner | Result |
|---|---|---|---|---|---|
| 1 | List GMP for matched IPO | GET /v1/market-data/ipo?status=open | 200; matched row has GMP; all existing fields present | am-market-data | not verified |
| 2 | Admin permission | POST /v1/admin/sync/ipo-gmp | Non-admin 403; admin accepted; no key in output/log | am-market-data | not verified |
| 3 | Run status/duplicate trigger | GET /v1/admin/sync/ipo-gmp/status; POST same while running | Counts/status visible; duplicate rejected | am-market-data | not verified |
| 4 | IPO without GMP | GET /v1/market-data/ipo/{id} | 200; GMP absent; remaining payload intact | am-market-data | not verified |
| 5 | Provider failure | Manual trigger with stubbed 429/timeout | Failure status; old GMP/history retained | am-market-data | not verified |
| 6 | History ordering/idempotency | GET /v1/market-data/ipo/{id}/gmp-history | 200; oldest-first; one per IST day after rerun | am-market-data | not verified |
| 7 | Empty history | History endpoint for IPO without snapshots | 200; empty named data array | am-market-data | not verified |

Automated regressions must mock the provider; do not consume the user's Free quota in repeated tests.

## Deploy

Preprod after implementation and approval. Production requires paid key and written data-retention confirmation. No deploy in this request.

## Agent scorecard

| Dimension | Score | Evidence |
|---|---:|---|
| Product/features | 9.0 | P0 backend/P1 UI split, optional GMP and manual recovery; refresh dependency now explicit |
| Architecture/design | 9.0 | Seven themed sheets including business flow and failures; provider adapter boundary added |
| Data/identity/contracts | 9.5 | Stable IDs, safe mapping, named DTOs |
| State/consistency | 9.5 | Idempotent daily samples, timestamps, preserve-on-refresh |
| Reliability/failure | 9.0 | Quota, timeout, partial data, overlap, cache, missed-run and stale-IPO cases |
| Security/authz | 9.5 | Server-side key and explicit admin authority |
| Observability | 9.5 | Run metadata and matched/unmatched/updated/skipped counts |
| Testability | 9.5 | Corner matrix mapped to unit/security/Postman rows |
| Operability/rollout | 9.5 | Independent configurable job, preprod first, terms gate |
| TODO/implementability | 9.5 | Ordered backend tasks |
| **Overall** | **9.0** | Refresh dependency, catch-up, stable UI id, and provider-neutral contract added |

## Adversarial review

| Finding | Severity | Resolution |
|---|---|---|
| Full Upstox save might erase GMP history | Major | Preserve enrichment or targeted update; regression test |
| Redis can hide fresh GMP | Major | Invalidate only affected list/detail keys |
| Authenticated does not necessarily mean admin | Major | Enforce explicit admin authority |
| Cross-provider slugs/names differ | Major | Persist Guru slug; multi-field match; skip ambiguity |
| Free/Basic vs provider history rights | Major | Production gated on paid plan and written permission |
| Null, zero, stale and closed rows differ | Minor | Preserve raw semantics/timestamp and filter by internal lifecycle |
| GMP runs before the IPO refresh | Major | Schedule after refresh; require completed refresh marker and block/recover otherwise |
| Server is down at the scheduled time | Major | One startup catch-up for the missing IST date plus admin combined recovery; no unbounded replay |
| New IPO exists at IPO Guru but not locally | Major | Do not create or attach from GMP job; IPO refresh must create it first, then the next GMP run maps it |
| UI couples to IPO Guru identifiers | Minor | UI uses internal IPO id; provider and providerSlug remain optional metadata |

- Blocker/Major count after fixes: 0
- Agent satisfied: yes

## Rating log

| Pass | Delivery | Design | Overall | Gaps patched |
|---|---:|---:|---:|---|
| 1 | 9 | 9 | 9.0 | Replacement writes, cache, auth, matching, rate limits, terms, optional payload |

## User review gate

Review this PLAN, architecture.drawio, and TODO. Confirm or change the same-collection choice, daily sample policy, refresh dependency/catch-up behavior, stable UI id contract, admin trigger/status contract, provider adapter boundary, and backend/frontend boundary. **Do not Execute until explicit approval.**

## REPORT gate

Do not write REPORT.md until implementation tests and Postman rows are verified.
