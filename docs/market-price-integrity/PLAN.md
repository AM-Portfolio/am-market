# Market price, index performance, and Top Movers fix

## Source plan and repository context

**Source plan pointer:** continuing the existing user-reviewed `docs/market-price-integrity/PLAN.md` and the decisions in this conversation; this update narrows the source-verified Top Movers implementation slice without replacing the existing goal or broad-market findings.

**Decisions retained for this slice:** backend only; no UI changes; keep the existing `/analysis/movers` contract and `gainers`/`losers` DTO; retain the already-implemented one-snapshot sorting; remove serial historical-provider fallback from Movers; use existing batched Upstox LTP and Redis stream/cache paths; handle configured NSE indexes from their existing membership source; do not claim complete SENSEX coverage without BSE members; never rank zero, stale, ambiguous, or incomplete inputs.

**Catalog and context:** `am-market` is the Market domain repository; `am-market-data` is its existing backend service, not a new deployable. Analysis source is the local repository. Read the root `am-market/README.md`, nested `am-market-data/README.md`, the workspace-provided `AGENTS.md` production-safety rules, and these source files: `SymbolOrchestratorService`, `NSEIndicesConfig`/`nseindices.yml`, `MarketDataProcessingService`, `StockIndicesService`, `MarketAnalyticsService`, `StockDataEnricher`, `SmartStockService`, `MarketDataService`, `OHLCDataRetriever`, `MarketDataCacheService`, `OHLCQuote`, `UpstoxSymbolResolver`, `UpstoxMarketDataProvider`, `MarketHoursService`, and `MarketCalendarService`. No root or nested `.am.yaml` was present in the inspected repo paths.

**Coverage verdict: partial, now source-grounded.** The existing plan correctly identifies the shared price-data issue, and this review confirms one-snapshot Top Movers is already implemented. Missing coverage was the still-enabled historical provider fallback, all-index subscription scope, invalid base/unchanged-price handling, zero placeholders, receipt-time-only freshness, unsafe existing lock semantics, and missing BSE constituent source. The Top Movers plan below closes those gaps and states the SENSEX prerequisite explicitly.

## Goal

Make the Market UI show one accurate value for each selected timeframe. The top index cards, chart, and Top Movers must use the same Market Data result and must remain responsive after market close.

Scope: `am-market/am-market-data` and the Market UI packages only. Portfolio, Trade, Dashboard outside the Market UI, and Analysis are read-only consumers; they are not changed in this work.

## Current problem seen by users

- A Watchlist stock can show a real price but `0.00` day change and `0.00%` when the market is closed.
- Top Movers can be empty even when index constituents have moved.
- When a user chooses `1M`, `3M`, or another non-daily timeframe, the chart can finish first while the top cards still say `Loading…`.
- On the same screen and timeframe, the chart, top card, and broker can show different percentage returns for the same index. For example, the supplied NIFTY 50 screenshot showed `-5.14%` in the chart while the broker showed `-4.64%` for its `1M` view.
- The problem occurs across multiple indices, so it is a shared calculation/data-flow issue rather than one bad NIFTY 50 record.

## Evidence that leads to this solution

The solution is based on source-code tracing and the observed UI/API behavior, not on an assumption that either the UI or backend is solely at fault.

1. The chart calls `/v1/analysis/historical-charts` and draws the returned candle series.
2. The top cards call `/v1/indices/batch` for a latest value, then make a second historical request and calculate their own return in the UI.
3. The timeframe handler starts chart loading, movers loading, full-card base-price loading, and pinned-card base-price loading around the same time. A background preloader can request further base prices later.
4. The UI keeps more than one cache for timeframe base prices, so requests can finish in a different order and update different state.
5. The backend separately obtains gainers and losers, repeating the same constituent-price enrichment work.
6. Production investigation already showed broken previous-close handling, invalid provider keys, and an unavailable Influx fallback.

Therefore the solution is to make Market Data the single calculator of index performance, then have the Market UI render that one result instead of calculating an independent card percentage.

## What is verified

| Verified fact | Source |
|---|---|
| Watchlist can request `NSE:NSE:IDEA`, which returns no live LTP. | `MarketDataService.java` and production read-only evidence |
| Current price can be used as previous close, producing `0.00` change. | Cache/scheduler flow and production quote evidence |
| Top Movers fetches the same enriched stock data twice: once for gainers and once for losers. | `MarketAnalyticsService.java` `getMoversUnified` |
| Top Movers sends plain tickers to an Upstox operation needing instrument keys; Influx fallback returns 401. | Production read-only logs/API result |
| Timeframe change starts several requests: chart history, movers, full index-card base history, pinned-card base history, then background preloads. | `user_dashboard_page.dart`, `market_provider.dart` |
| Cards calculate their own return from `lastPrice` and a separate historical base price. | `index_card.dart` |
| The chart uses `/v1/analysis/historical-charts`, while cards use `/v1/indices/batch` plus `/v1/market-data/historical-data`. | `api_service.dart` and `MarketAnalyticsService.java` |

## Root cause

The same percentage is calculated in different places from different inputs:

```text
Chart:      historical chart API → chart candles → chart percentage
Top card:   latest-index API + separate historical request → UI percentage
Top Movers: market analytics API → constituent prices → mover percentage
```

The UI’s separate card requests explain the slow `Loading…` state and duplicate network work. The backend’s incorrect previous close explains zero after-market daily change. A mismatch such as `-5.14%` versus a broker value cannot be assigned to one side until the exact timeframe definition, candles, and as-of time are compared; the current design makes such mismatches likely because it has more than one calculation path.

## Required decision before implementation

`1M` must have one written meaning. It cannot be inferred from a screenshot.

Before implementation, record for the broker comparison:

- as-of timestamp and timezone: **Current System Time (IST)**
- whether `1M` means rolling one calendar month, last 30 calendar days, or current calendar month: **Rolling one calendar month (e.g. if today is Oct 7, start date is Sept 7).**
- exact start trading session and closing value: **Close of the trading session from exactly 1 month ago. If that date falls on a holiday/weekend, the nearest preceding trading session's close is used.**
- exact end session/value: **Current LTP (if market is open) or last completed close (if market is closed).**
- treatment of holidays and a still-open market: **If the 1M start date is a holiday, fallback to the latest completed trading session BEFORE that date. If the market is still open, the end price is the real-time live LTP.**

Market Data will implement the approved definition once and expose it to all Market UI surfaces.

## Target design

### One authoritative calculation

Market Data calculates, for every requested index and timeframe:

```text
base price     = close of the approved start trading session
end price      = latest valid price during market hours, otherwise last completed close
change         = end price - base price
change percent = (change / base price) × 100
```

It also returns the selected start/end session dates and the as-of time internally and in the new/additive performance response, so a wrong result can be traced.

The existing historical chart response uses the same canonical start/end selection. The Market UI renders cards from the returned performance result and the chart from the matching candle set. It no longer calculates a second percentage itself.

### One request per purpose

On a timeframe change:

1. One batch history/performance request for the six pinned indices. It supplies both chart data and card returns where symbols overlap.
2. One movers request, only when the movers section is visible/required.
3. Additional history only when the user opens All Indices, and only for symbols not already cached.

There is one shared cache keyed by `symbol + timeframe + exchange/session date`. A newer timeframe selection cancels or ignores older in-flight responses.

### Backward compatibility

- Existing URLs, existing fields, and existing response types stay valid.
- Add performance metadata or an additive endpoint; do not rename or remove existing fields.
- Existing clients continue to consume `/v1/indices/batch` and `/v1/analysis/historical-charts`.
- Older cache records remain readable; new session metadata is optional.
- Market Data accepts existing symbol forms such as `IDEA`, `NSE:IDEA`, and provider keys.

## Files expected to change

### Market Data

- `am-market-data/market-data-service/src/main/java/com/am/marketdata/service/MarketDataService.java` — idempotent symbol qualification and correct live/previous-close meaning.
- `am-market-data/market-data-service/src/main/java/com/am/marketdata/service/MarketDataCacheService.java` — dated cache validation and legacy-record compatibility.
- `am-market-data/market-data-scheduler/src/main/java/com/am/marketdata/scheduler/PreviousCloseScheduler.java` — previous completed-session close.
- `am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/resolver/UpstoxSymbolResolver.java` — true Upstox instrument keys.
- `am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/UpstoxMarketDataProvider.java` — provider requests by resolved key.
- `am-market-data/market-data-analysis/src/main/java/com/am/marketdata/analysis/service/MarketAnalyticsService.java` — one enriched snapshot for both Top Movers sides; shared historical-performance calculation.
- `am-market-data/market-data-analysis/src/main/java/com/am/marketdata/analysis/controller/AnalysisController.java` and the existing historical-response model — additive canonical performance data, if this is chosen after contract review.
- `am-market-data/market-data-api/src/main/java/com/am/marketdata/api/controller/MarketIndexController.java` — preserve valid after-hours index values and expose an additive batch performance route only if controller ownership is preferred after discovery.
- `am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/client/UpStockClient.java` — remove authorization-bearing log output.
- `am-market-data/helm/vault-mappings.yaml` and its existing deployment mapping — repair Influx credentials without committing any secret.

### Market UI only

- `am_market/ui/lib/features/dashboard/presentation/pages/user_dashboard_page.dart` — replace overlapping timeframe loads with one coordinator.
- `am_market/common/lib/providers/market_provider.dart` — keep one cache and one loading state for index timeframe data.
- `am_market/ui/lib/features/market/widgets/index_card.dart` — render server-provided performance; do not independently calculate it.
- `am_market/common/lib/services/api_service.dart` — one shared batch performance/history call and request de-duplication.
- `am_market/common/lib/models/market_data.dart` — add optional parsing for additive performance metadata, preserving existing fields.

No Portfolio, Trade, non-Market Dashboard, or Analysis file changes are in scope.

## Edge cases and solution

| Case | Required behaviour |
|---|---|
| Weekend, holiday, or before market open | Use the latest completed exchange session, never the calendar previous day. |
| After market close | Use the official completed-session close and its correct previous close; do not show fabricated zero. |
| Market open | Use live price only when fresh; otherwise retain last completed close and mark data stale internally. |
| User changes 1D → 1M → 1D quickly | Only the newest request may update the screen. |
| Same symbol requested by chart and card | One network request and one cached result. |
| Missing base candle/new listing | Show unavailable change, not `0.00%`. |
| Invalid/partial provider response | Keep valid symbols, mark unavailable symbols clearly, and never cache placeholders as success. |
| Index key resolves differently across services | Use one canonical identity before calling provider/cache. |
| Index has incomplete/duplicate constituents | Retain last known-good membership; alert and refuse to rank an incomplete invalid set. |
| Provider 401/429/timeout | Do not retry invalid-key/auth failures; use validated fallback only and record the dependency failure. |
| Influx and provider disagree | Use documented source precedence and record disagreement; do not silently overwrite a fresher validated value. |
| Split, bonus, or consolidation | Use provider official adjusted close or mark period return unavailable until reconciled. |
| Global index | Use its own exchange calendar/timezone; do not apply NSE session rules blindly. |
| Old cache / rolling deployment | New fields are optional and old/new pods read each other’s records. |

## Implementation order

1. Discovery: use Postman MCP in preprod to capture existing batch-index, historical-chart, historical-data, and movers contracts; inspect cache schema, exact Influx mapping, provider batch limits, and index-membership owner.
2. Define and approve the `1M` calculation with broker evidence. Add fixture values for NIFTY 50 and NIFTY BANK.
3. Fix Market Data identity, previous close, cache validation, index membership, Influx access, and Top Movers one-snapshot logic.
4. Add canonical batch timeframe performance to Market Data without breaking existing responses.
5. Replace Market UI duplicate loaders/caches with the one canonical response.
6. Test preprod API responses through Postman MCP and verify cards, chart, and movers show the same numbers.
7. Capture performance baseline and compare request count, server calls, latency, error rate, stale data rate, and empty-movers rate.
8. Request separate approval before any production deployment or cache mutation.

## Verification and acceptance criteria

- For the approved `1M` definition, Market Data’s NIFTY 50 result matches the recorded broker fixture at the same as-of time, or the difference is explained by documented source/candle rules.
- The top card and chart display the same start value, end value, and percentage for the same selected index/timeframe.
- A timeframe change produces one shared history/performance load for a symbol, not duplicate full and pinned requests.
- Top Movers makes one enrichment pass and returns valid gainers/losers when source data is available.
- After market close, daily values are non-zero when current and previous close differ.
- Existing API clients still deserialize current endpoint responses.
- No double-prefixed symbol, invalid Upstox key, Influx 401, placeholder success, or secret-bearing log remains.
- Tests cover the edge cases listed above, including rapid UI changes and mixed old/new cache records.

## Precise expected behaviour after implementation

| User action or condition | Expected behaviour |
|---|---|
| Open Market page on a trading day | Cards show the latest valid index value and daily change. The chart and selected card use the same as-of time. |
| Open Market page after market close | Cards show the official completed-session close and the correct change from the prior completed session. They do not show `0.00%` unless the two official closes are actually equal. |
| Select `1M` | The card and chart use the same approved start session, end session, base price, end price, and percentage. They display the same return. |
| Select any other supported timeframe | The same one-source rule applies for `1D`, `1W`, `3M`, `6M`, `1Y`, and `5Y`. |
| Switch timeframes quickly | Only the newest selection updates the page. Older slow responses are ignored. |
| Chart/card symbol overlap | One shared history/performance request supplies both surfaces; the card does not wait for a duplicate request. |
| Open All Indices | Only not-yet-cached indices are requested. Existing selected-timeframe data is reused. |
| Provider or history source is unavailable | The affected value is shown as unavailable or last validated data according to freshness rules; it is never displayed as a fabricated zero. |
| View Top Movers | Market Data fetches the constituent snapshot once, then derives both gainers and losers from it. |
| Compare with a broker | Given the same approved timeframe definition and as-of time, the returned base/end values make any difference explainable and testable. |

## Review of this plan

### Rating before redesign: 5.8/10

The previous plan covered live price and movers failures, but omitted the verified UI request duplication, duplicate caches, chart/card split calculation, canonical timeframe contract, race handling, and exact validation against broker data.

### Rating after redesign: 8.8/10

The plan now covers the confirmed causes, backward compatibility, latency, edge cases, file ownership, and verification. It is intentionally not rated higher because the business definition of `1M` and the exact broker comparison data have not been supplied or verified. Treating either as known would be an assumption. Once that definition and fixture are recorded, the plan can be reviewed again for implementation readiness.

## Safety

Production Kubernetes access is read-only. Any production deployment, cache operation, or other mutation requires a separate explanation and your explicit approval.

## Index membership repair addendum

### Problem confirmed after the original plan

`stock_indices_market_data` is the existing MongoDB source used by Heatmap and Top Movers to identify index members. Its documents are not currently trustworthy:

- NIFTY 50 contains 15 members instead of a complete constituent list.
- NIFTY 100 and NIFTY 200 have documents without a `data` array.
- NIFTY 500 needs normalization and validation because the stored count includes an apparent parent-index row.

This is not a UI list problem. `MarketDataService.getIndexConstituents` reads `data` directly. `AnalysisService.getHeatmap` then asks for history for those symbols. When history is unavailable, it currently writes `0.0` for the symbol, which produces the visible tiles with correct names but false `0.00%` values.

### Verified cause

The existing constituent scraper can save `StockData` lists, but `StockIndicesService` treats any existing Mongo document as valid. Its quote-only repair method can create an index document with price metadata and no constituent `data`. A partial list is never treated as a repair condition.

The already-confirmed Influx 401 and raw-ticker Upstox historical request then prevent the existing members from receiving valid history. These are independent failures: repairing membership alone will not restore price changes; repairing prices alone will not restore missing members.

### Verified Influx production root cause

Production investigation confirmed that InfluxDB is healthy, the intended organisation/bucket contain market history, and an authorised credential can read the data. The Market Data pod receives an empty Influx token instead.

The failure chain is:

```text
Vault mapping uses an unsupported CSI template form
→ Secrets Store CSI injects no Influx environment values
→ Spring resolves the token placeholder to an empty string
→ the empty value overrides a fallback expression
→ Market Data sends an empty authorization token
→ InfluxDB correctly returns HTTP 401
```

The Vault record also contains incorrect URL, organisation, and bucket values. Application defaults currently hide those values, but that is accidental and must not be relied on.

**Required correction:** use the existing supported Vault/CSI value mappings to inject URL, organisation, bucket, and token into the pod. No credential may be embedded in source, Helm values, application defaults, tests, logs, documentation, shell commands, or API responses. Application startup must fail clearly when a required Influx value is absent or blank, rather than silently starting with an empty token.

### Paper Trading live-quote failure

Paper Trading is affected by the same Market Data service, but through the live-LTP path rather than the historical-price path:

```text
Paper Trading loads a valid NIFTY 50 constituent list
→ /market-data/live-ltp returns no stock quotes
→ /market-data/quotes fallback returns cached quotes with lastPrice = 0
→ every Paper Watchlist row remains pending
→ order calculations have no safe execution price
```

This proves that security documents and index membership are available; the failure is in the live ingestion/WebSocket subscription to Redis quote-cache path. Influx 401 explains historical failures but does not by itself explain missing live LTP.

**Required behaviour:**

1. The ingestion worker resolves every subscribed ticker to a valid Upstox instrument key before subscribing.
2. A missing, zero, invalid, or stale LTP is never written as a successful latest-price cache value.
3. Quote APIs return explicit availability/freshness status. `cached: true` must not mean a zero-valued or stale quote is usable.
4. During market hours, Paper Trading accepts only a fresh positive LTP. After close, it may display a verified completed-session close, clearly labelled as such.
5. Paper Trading stops its loading state after a completed quote response. If all quotes are unavailable, it shows retry/unavailable state and blocks an order with no valid price; it must never submit at zero.

### Paper Trading implementation slice

| Phase | Existing file/module | Change |
|---|---|---|
| 1. Diagnose live ingestion | Existing stream worker, `StreamerManager`, symbol orchestration, and Grafana logs | Verify active provider, subscription count, resolved-key count, last successful tick time, and zero-tick rejection count. |
| 2. Correct ingestion | Existing Upstox resolver/streamer/cache writer | Reject unresolved subscriptions; cache only positive, validated quotes with `updatedAt`, source, and trade date. |
| 3. Correct quote contract | `MarketDataController` and quote/cache services | Preserve existing fields, add availability/freshness metadata, and do not label zero quotes as usable cached data. |
| 4. Correct Paper UI state | Existing Paper Trading watchlist/controller quote path | End the spinner on terminal unavailable results; render retry/unavailable state; prohibit order calculation/submission without a validated price. |
| 5. Prove recovery | Existing Market Data and Paper Trading tests | Test fresh quote, zero quote, stale quote, missing subscription, after-hours official close, and attempted zero-price order. |

### Target behaviour

Keep the current collection and scraper. Do not add a new membership service or manually seed live prices.

1. An index document is usable only when its `data` array has valid, unique constituent symbols and passes its configured expected-count rule.
2. Existing constituent ingestion repairs a missing, null, duplicate, or incomplete `data` array. It replaces only `data` after validation.
3. Existing Upstox index-price refresh updates only `metadata`; it must preserve a valid `data` array and must not create a "repaired" document without members.
4. A bad scraper response is rejected. The last known-good `data` array remains in MongoDB.
5. Heatmap and Movers report unavailable constituents as unavailable. They must never translate missing history into a successful `0.00%` value or cache an all-zero result.

### Existing-file implementation plan

| Phase | Existing file/module | Change |
|---|---|---|
| 1. Validate membership | `market-data-api/.../StockIndicesService.java` | Replace document-exists-only logic with a reusable validity check: non-null members, normalized unique tickers, parent index excluded, and configured expected count. |
| 2. Repair membership | Existing `market-data-scraper` `MarketDataProcessingService` and mapper | Reuse the existing constituent scrape/save path when validity fails. Validate before replacing `data`; retain the old valid list on any failed or partial response. |
| 3. Preserve membership | `StockIndicesService.fetchFreshData` | Make Upstox EOD quote refresh metadata-only. It may update an existing document's `metadata`, but it must not create or replace a membership list. |
| 4. Repair price sources | `UpstoxMarketDataProvider`, `UpstoxSymbolResolver`, Vault mapping | Reject unresolved tickers before Upstox calls; repair the Influx credential mapping; return unavailable for unresolved/history-failed symbols. |
| 5. Correct consumers | `AnalysisService`, `MarketAnalyticsService` | Return unavailable/partial-result metadata rather than `0.0` placeholders. Reject all-zero heatmap cache writes for every index, not just `INDICES`. |
| 6. One-time recovery | Existing scheduler/admin-safe index-refresh path | After preprod validation, run the existing constituent refresh once to rehydrate only documents that fail validation. This is a controlled repair, not manual database insertion. |

### Membership validation rules

| Index | Valid stored member count |
|---|---:|
| NIFTY 50 | 50 |
| NIFTY 100 | 100 |
| NIFTY 200 | 200 |
| NIFTY 500 | 500 after excluding the parent-index row |

The code must record observed count, expected count, normalized duplicates, source timestamp, and added/removed members. A count mismatch is a failed refresh, never a successful overwrite.

### Exchange-aware index catalogue and SENSEX

The Market Heatmap includes `SENSEX`, but `stock_indices_market_data` has no SENSEX membership document. The existing constituent scraper is NSE-oriented, so it cannot silently repair a BSE index with an NSE request. That is why the Heatmap has no constituent history for SENSEX and renders a false zero value.

Keep the existing collection and repair flow, but add a small configuration catalogue for every supported constituent index. This is configuration inside the existing service, not a new membership system. Each entry defines:

```text
canonical index symbol, exchange/source path, count policy, parent-row identity, enabled-for-movers/heatmap
```

Examples: SENSEX uses its BSE constituent path and a 30-company policy; NIFTY IT has 10 constituents; NIFTY BANK must use the current configured count from its published index definition rather than an old hardcoded `12` assumption. The catalogue is updated as part of a rebalance release and stores the effective version with the Mongo document.

Rules:

1. No generic “non-empty list means valid” fallback exists for an unconfigured index.
2. A configured exact-count index must match its current configured count after normalization.
3. An index whose methodology states a maximum/range uses an explicit allowed range plus an approved current-count version, not a guessed number.
4. SENSEX is unavailable until its BSE membership path has produced a validated 30-company list. The UI must show unavailable, not `0.00%`.
5. Only indexes with a valid membership document are eligible for constituent Heatmap and Top Movers. Index-level performance may remain available independently.

### Rollout and proof

1. Reproduce in preprod with incomplete NIFTY 50, null NIFTY 100 data, and an intentionally partial scraper response.
2. Verify that partial input does not overwrite a good stored list.
3. Verify that valid input restores the correct list and that Upstox metadata refresh preserves it.
4. Verify each restored member resolves to an Upstox key and receives either a fresh price/history result or explicit unavailable status.
5. Verify NIFTY 50 Heatmap has 50 tiles with valid returns, or clear unavailable states; it must never show false all-zero tiles.
6. Verify Top Movers receives the validated constituent list and has valid partial-results behaviour.
7. Only after preprod proof, request explicit approval for the one-time production repair refresh. Production MongoDB, cache, and scheduler mutation remain approval-gated.

### Adversarial review: gaps found and closed

| Gap or edge case | Why the earlier plan was insufficient | Required solution |
|---|---|---|
| A list has the expected count but the wrong members | Count-only validation would accept a stale or malformed NIFTY 50 list. | Persist the existing `docVersion` as a membership version/hash and log the normalized added/removed symbols. A repair may publish only a complete, normalized list. Review an unexpectedly large membership delta before production publication. |
| A source includes a row for the parent index | Blindly subtracting one from NIFTY 500 can remove a real member. | Exclude only a normalized ticker exactly equal to the requested index identity, then validate the final unique count. |
| Many users hit an invalid document together | Request-time scraping can create a scraper stampede and slow the Market UI. | One coalesced, asynchronous repair per index. Reads return the last valid membership; if none exists, return explicit unavailable membership. Do not make a user request wait for a scraper run. |
| Scraper and quote refresh write the same document | Repository-wide saves can race: a stale object can erase newly repaired `data`, or a repair can overwrite fresher metadata. | Use an existing-service atomic partial update or document-version compare-and-set: membership repair updates `data`/membership version only; quote refresh updates `metadata` only. Retry on a version conflict by rereading. |
| A successful repair leaves old analysis cache | Heatmap or Movers can still render the old 15 members or old all-zero result. | Include membership version in new Heatmap/Mover cache keys. On a successful repair, invalidate only cache entries for that index. Old keys remain readable during rolling deployment but are not selected by new code. |
| Influx remains unavailable after membership repair | Full membership without history still yields broken returns. | Gate the production repair acceptance test on Influx authenticated reads and strict Upstox key resolution for a representative sample plus all displayed Heatmap members. Membership repair must not be marked successful because Mongo alone was fixed. |
| One member has no price/history | Dropping the whole index hides useful valid data; writing `0.00%` lies. | Return valid members plus explicit unavailable symbols/count. Movers ranks only fresh, valid quotes and states the excluded count. The UI displays `—` for unavailable tiles. |
| Existing Heatmap API is `Map<String, Double>` | Replacing a value with a status would break existing clients. | Keep the current endpoint and fields compatible. Add an opt-in/additive detailed response for Market UI with `values`, `unavailableSymbols`, `membershipVersion`, `asOf`, and `sourceStatus`; migrate only Market UI. |
| Cache age crosses a market boundary | A Friday or holiday cache may be valid after close but stale when the next session opens. | Validate both `updatedAt` and exchange trade date. During market hours reject stale live values; after close use only the latest completed-session close with its trade date. |
| Existing bad documents have no last-known-good copy | A failed refresh cannot safely retain a list that was already partial. | For those documents, do not expose the partial list as valid. Mark membership unavailable until the existing scraper produces a complete validated result. |
| Production recovery is interrupted | A half-repaired set can make the UI inconsistent. | Repair per index atomically; record outcome per index; make the operation idempotent and resumable. Do not clear good documents before a validated replacement is ready. |
| BSE SENSEX is requested through NSE-only constituent logic | SENSEX has no Mongo membership document, so its Heatmap has no inputs and becomes false zero. | Add exchange/source-path catalogue entry; repair SENSEX through its BSE constituent path; show unavailable until validated. |
| Sector index count changes after rebalance | An old fixed count such as NIFTY BANK 12 can reject a valid current list or accept wrong assumptions. | Version the expected count policy per configured index; update it with the membership/rebalance release and retain the effective date. |
| Sensitive database access was pasted into chat | Credentials can be exposed and cannot be treated as durable configuration. | Rotate the exposed database credential; use Vault mappings only. Do not place a connection string or token in code, docs, logs, tests, or commands. |

### Revised implementation order

1. Add contract tests around the current Mongo document, constituent scraper, quote updater, Heatmap, and Movers paths.
2. Add membership validity checks and atomic field-level write behaviour in the existing index service/persistence path.
3. Reuse the existing scraper for coalesced repair only; no request-time blocking scrape and no quote-only document creation.
4. Repair Influx authentication and strict ticker-to-instrument-key resolution. Add explicit dependency failure outcomes.
5. Add additive detailed Heatmap/Mover availability metadata and version-aware cache handling while preserving existing response contracts.
6. Update only Market UI to render unavailable values as `—` and to show partial-data state where applicable.
7. Run the preprod recovery drill, including retry/resume and cache invalidation.
8. Review the full validation output, then request explicit approval for the production repair run.

### Expanded acceptance criteria

- An index with 15 NIFTY 50 members is not considered valid or served as a complete NIFTY 50 membership.
- An index with no `data` is not repaired by a quote-only Upstox request.
- A valid membership repair cannot erase price metadata, and a price update cannot erase a valid membership list.
- An old Heatmap/Mover cache cannot survive a successful membership version change.
- A member with missing history is unavailable, never `0.00%`; other valid members remain visible.
- A 401 from Influx or unresolved Upstox key produces an observable dependency failure and no fabricated value.
- The existing simple Heatmap endpoint remains consumable by older clients; the Market UI uses the additive detailed contract.
- SENSEX and every configured sector index use the correct exchange-specific membership source and current versioned count policy.

### Rating after adversarial review

The earlier plan was **8.8/10**. It identified the primary data failures, but lacked membership-versioned cache invalidation, concurrency protection, a no-stampede repair policy, a backward-compatible unavailable-data contract, and a safe recovery rule for documents with no known-good list.

The revised plan is **9.5/10**. It closes the identified production, cache, API-contract, and recovery gaps using the existing collection, scraper, and service boundaries. It is ready for user review, but not implementation: exact preprod fixture data and the production repair command/runbook must be prepared and reviewed before any mutation.

## Final safeguards added after full-plan review

### Safe membership publication

The recovery flow must never overwrite a known-good member list with an incomplete source response. For every configured index, the existing scraper must first fetch the complete exchange-specific list, normalize symbols, remove duplicates, validate the versioned count policy, and compare it with the currently published list. Only a valid complete list may be atomically published with a new membership version/hash. Then invalidate only the Heatmap and Movers cache entries tied to the previous membership version.

If validation fails, keep the last known-good list and mark the refresh as failed. If there is no known-good list, return explicit membership-unavailable state. Never publish a partial list.

### Unified quote freshness contract

Every quote-derived result — live LTP, Watchlist, Paper Trading, Heatmap, Movers, and index cards — must use the same internal fields: `asOf`, `source`, `tradeDate`, and availability state. A live quote is usable only when it is positive and inside the configured market/session freshness limit. A completed-session close is explicitly marked historical; it is never silently presented as a live price.

When there is no usable value, the API returns unavailable or stale status. The UI renders `—`, and Paper Trading blocks valuation and order submission. It must not display fabricated `0.00%`, retain an endless spinner, or use an old LTP as a current price.

### Bounded provider-failure behaviour

For a repeated instrument-resolution, Upstox, Influx, or quote-cache failure, coalesce requests for the same symbol/timeframe and temporarily stop repeated remote attempts. Return the availability state quickly, retain only still-valid cached data, and retry normally after the bounded interval. This prevents one failed dependency from causing slow pages or a request storm.

### Release proof and alerts

Preproduction verification must cover NIFTY 50, NIFTY 100, NIFTY 200, NIFTY 500, NIFTY BANK, NIFTY IT, and SENSEX. For each, prove a valid exchange-specific member list, resolvable instrument keys, positive fresh quotes for sample members, valid base prices across supported timeframes, and correct Heatmap/Mover/Paper Trading unavailable handling.

Add alerts for incomplete membership, all-zero constituent quotes, excessive quote age, Influx authorization failures, unresolved provider keys, and failed membership refreshes. These identify the failed stage before users see empty or misleading market data.

### Final review rating

The plan is **9.6/10**. The implementation design now handles correctness, backwards compatibility, stale-data prevention, safe membership recovery, provider incidents, and verification across NSE and BSE index families. The remaining work before implementation is deliberately operational: capture actual preproduction fixtures and prepare the approval-gated production recovery command/runbook. No assumption or mutation is needed to complete that preparation.

## Quote-quality contract and Paper Trading safety

### Problem this closes

The current APIs can return a number, an empty result, or `0.0`, but do not give every caller a reliable explanation of whether that number is usable. The UI therefore cannot consistently distinguish a fresh live quote, an official market-close value, a delayed quote, and a failed quote path. This produces false `0.00%` Heatmap tiles, ambiguous empty Movers, and unsafe Paper Trading valuation when the quote cache is degraded.

### Canonical quote states

Use one additive per-symbol quote-quality contract across live LTP, quotes, Watchlist, Paper Trading, Heatmap, Movers, and index cards:

| State | Required meaning | May calculate/display | May immediately simulate a market-order fill |
|---|---|---|---|
| `LIVE` | Positive quote received within the active-session freshness limit | Yes | Yes |
| `CLOSED` | Verified official close for the most recently completed exchange session | Yes, labelled market closed | No |
| `DELAYED` | Positive quote from a declared delayed feed with known delay | Yes, visibly labelled with delay | No by default |
| `UNAVAILABLE` | No valid quote because it is missing, zero, unresolved, invalid, stale, or dependency retrieval failed | No; show `—` and reason | No |

Every additive quote result carries `price`, `asOf`, `tradeDate`, `source`, `status`, and, where applicable, `delaySeconds` and a machine-readable reason. Existing numeric fields and response shapes remain unchanged for old clients.

### Calculation rule

Calculate and display a value whenever the required inputs are valid:

```text
fresh live LTP + valid base/previous close       => calculate as LIVE
official completed-session close + valid base    => calculate as CLOSED
declared delayed quote + valid base              => calculate as DELAYED
missing, zero, unresolved, or stale input        => UNAVAILABLE; do not calculate
```

`UNAVAILABLE` is not a default state and must not hide a valid official close. It is only used when there is no trustworthy current or completed-session value.

### Paper Trading fill policy

1. The UI may display `LIVE`, `CLOSED`, or `DELAYED` values with their status label.
2. During an active session, an immediate simulated market fill requires a `LIVE` quote. `UNAVAILABLE`, stale, zero, or unresolved quotes block the valuation and submission.
3. A `DELAYED` quote can be viewed but cannot silently create an immediate fill. Its use requires an explicit product decision; the default is to block immediate simulation.
4. After close, a `CLOSED` value is a reference price only. A user may create a clearly labelled next-session simulated order, but it remains queued and cannot be recorded as a fill at the close.
5. At the next valid session, queued paper orders are repriced and evaluated against the first eligible `LIVE` quote. Cancelled, expired, rejected, and filled outcomes are recorded distinctly.
6. The final order/fill service, not only the UI, validates the quote status and captures the immutable price snapshot: price, quote `asOf`, `tradeDate`, source, status, order time, and fill time. This prevents bypass through a direct API request.

### Edge cases found in the review and their solution

| Edge case | Risk | Required solution |
|---|---|---|
| Market closes, weekend, or exchange holiday | Friday live LTP can be mistaken for Monday live data. | Use exchange calendar and `tradeDate`; transition valid close data to `CLOSED` at session end and reject it as `LIVE` on the next session. |
| Pre-open, auction, halt, or circuit-breaker period | A received tick may not represent executable continuous-market price. | Add an exchange-session phase to status evaluation. Show the quote if valid, but block immediate simulated market fills unless the phase permits them. |
| Feed timestamp differs from server clock | Clock skew can mark a valid quote stale or an old quote fresh. | Prefer provider/exchange timestamp when present; retain server-receipt timestamp separately; validate allowed skew and record both for diagnosis. |
| Delayed and live sources are mixed | The UI can compare or rank incomparable values. | Source precedence is `LIVE` > `CLOSED` for after-hours reference > `DELAYED`; never replace fresh live data with delayed data. Movers and Heatmap must expose excluded delayed/unavailable counts. |
| Partial quote batch | One bad symbol can make the entire page look failed, or silently rank incomplete Movers. | Return each symbol with its own state. Keep valid rows. Include requested, valid, delayed, closed, and unavailable counts in detailed responses. |
| A valid quote turns invalid while an order is submitted | UI validation can pass but the price can expire before server receipt. | Revalidate quote status in the order/fill service at submission and capture the exact accepted snapshot atomically with the simulated order. |
| Zero, negative, NaN, or malformed provider value | A bad value becomes a false price or percentage. | Reject it before cache write and response construction; return `UNAVAILABLE` with a non-sensitive reason code. |
| Corporate action, symbol migration, or instrument remap | A valid old price can be compared with an incompatible new base. | Include resolved instrument identity and trade date in cache identity; invalidate/rebuild the affected historical base and quote mapping before calculation. |
| Fresh price but missing base/previous close | A percentage can be fabricated as zero. | Preserve valid LTP but mark the derived change/performance unavailable until a validated base exists. |
| Provider outage or rate limit | Repeated callers cause slow pages and hide the true reason. | Use the bounded, coalesced failure behaviour already defined in this plan, return dependency-unavailable status quickly, and alert operators. |
| Rolling deployment with older clients | New status fields could break callers or old data may lack timestamps. | Add fields only. Legacy records without required timestamps/status are treated conservatively as unavailable for immediate fills, while ordinary display falls back only to a validated completed-session close. |

### Existing-file implementation boundary

Market Data changes remain limited to the existing quote/cache/controller/provider paths described in this plan. The Paper Trading UI consumes the additive fields in its existing market client and watchlist controller. Before changing an order path, discover and document its current owner; the server-side final validation must be placed in that existing owner rather than duplicated in the UI or introduced as a new service.

### Acceptance criteria for quote quality

- A valid official close continues to display and calculate after close, labelled `CLOSED`; it is never reported as a live tick.
- A zero or stale market-hours quote cannot calculate Heatmap/Mover returns or create an immediate paper fill.
- A partial quote response preserves valid rows and reports unavailable rows/counts without substituting `0.00%`.
- An immediate paper fill has a persisted, auditable quote snapshot and cannot be created through a UI or direct API bypass when the quote is not `LIVE`.
- A delayed source is visibly identified and cannot silently replace a fresh source or create a default immediate fill.
- At the next exchange session, an old close cannot pass the `LIVE` freshness rule.
- Existing consumers continue parsing the current numeric response fields.

## Correction: cache-zero recovery using the existing market calendar

`lastPrice = 0.0` in Redis is a cache failure signal, not a conclusion that the user has no usable market value. The prior wording that immediately mapped every cache-zero to `UNAVAILABLE` is replaced by this recovery policy.

### Existing components to reuse

Use the existing `MarketCalendarService`, `MarketCalendarSyncService`, `MarketHoursService`, and `OfficialClosePolicy`. They already provide exchange calendar data, NSE session timing, weekends/holidays, exchange-local timestamps, scheduled and lazy calendar sync, calendar-health metadata, and an official-close selection rule. Do not create a second market clock or a separate holiday table.

`MarketHoursService` currently asks only for NSE. Quote evaluation must instead pass the quote's resolved exchange into the existing calendar service so BSE/SENSEX and future exchange-specific instruments are evaluated against their own calendar.

### Correct recovery sequence

For every symbol, resolve its canonical instrument identity and exchange first. Then use the calendar state before choosing a price source:

| Calendar state | Recovery sequence | Result when a valid value is found |
|---|---|---|
| Exchange is in its continuous open session | 1. Fresh positive Redis streaming quote. 2. Coalesced provider LTP snapshot using the resolved instrument key. 3. Valid current-session provider OHLC/quote fallback. | Return `LIVE`; asynchronously repair the Redis latest-price cache. |
| Exchange is closed, weekend, holiday, or before session | Do not request a live tick. 1. Use the last completed session close via `OfficialClosePolicy`. 2. Read validated historical storage. 3. Use provider end-of-day history with the resolved key only if storage has no valid close. | Return `CLOSED` and calculate from the last completed trading session. |
| Calendar is unavailable/stale or a provider call fails | Continue only with a previously validated value whose trade date and source satisfy the relevant rule. Do not guess today’s status or manufacture a price. | Return the valid `LIVE`/`CLOSED` value when evidence supports it; otherwise `NO_DATA` with the dependency reason. |

Only after all applicable recovery sources fail should the response have no displayable value. A zero Redis cache entry must therefore trigger cache recovery, not an immediate blank UI state.

### Refined response semantics

Keep the additive contract, but distinguish **displayability** from **immediate fill eligibility**:

| Result | Display and calculate | Immediate Paper Trading market fill |
|---|---|---|
| `LIVE` | Yes | Yes, within freshness limit |
| `CLOSED` | Yes, as last official close | No |
| `DELAYED` | Yes, with delay label | No by default |
| `NO_DATA` | No price; explain recovery failed | No |

`NO_DATA` replaces the ambiguous user-facing use of `UNAVAILABLE`. Internally, it carries the failed stage (`CACHE_MISS`, `KEY_UNRESOLVED`, `PROVIDER_FAILURE`, `HISTORY_MISSING`, or `CALENDAR_UNCERTAIN`) without exposing infrastructure detail to end users. The UI message is simply “Price temporarily unavailable”; it is never used while a validated completed close exists.

### Calendar-specific edge cases and fixes

| Gap found | Fix |
|---|---|
| Current `MarketHoursService` hardcodes NSE | Add exchange-aware use of the existing calendar service at quote evaluation boundaries; retain the existing NSE helper only for legacy callers. |
| Calendar metadata can report stale/fallback data | Include calendar source/staleness in quote evaluation. A stale calendar may support display of an already validated official close but may not authorise an immediate simulated fill. |
| Calendar currently models open/closed timing, not auction, halt, or circuit-breaker state | Do not invent these states from time alone. Until a provider status source is integrated, immediate Paper Trading market fills are allowed only in the normal configured continuous session; any explicit provider halt/auction flag blocks fills. |
| BSE and NSE may have different holiday/session records | Resolve exchange from the instrument, then call the existing calendar by exchange. The same rule covers SENSEX and NSE indices without separate UI logic. |
| A request-time cache outage could fan out into provider calls | Coalesce cache-miss recovery per `instrument + session`, use a short negative-result interval, and warm Redis only after validating the recovered quote. |

### Revised acceptance criteria

- A Redis `0.0` or absent value during an open session makes one bounded, resolved-key provider recovery attempt; a recovered positive value is returned as `LIVE` and repairs the cache.
- A Redis `0.0` or absent value after close still returns the last validated completed-session close when historical data exists; it is `CLOSED`, not `NO_DATA`.
- SENSEX/BSE and NSE symbols use their resolved exchange calendar, not the NSE-only legacy helper.
- A stale or fallback calendar cannot permit an immediate paper-market fill, but does not hide a previously validated official close.
- `NO_DATA` occurs only when cache, valid provider recovery, and valid completed-session history cannot provide a trustworthy value.

## Recover first, diagnose clearly, return typed contracts

### Recovery is required before `NO_DATA`

`NO_DATA` is a terminal outcome, never the first response to a Redis miss, `0.0`, or incomplete quote. The quote coordinator must make the bounded recovery sequence defined above for each affected symbol: normalize request identity, resolve the supported instrument key, consult the resolved exchange calendar, use cache where valid, request the provider only when the calendar permits it, then use a verified completed-session close where appropriate.

Recovery is coalesced by `resolvedInstrument + exchange + session/tradeDate`, so a cache incident creates one recovery operation shared by concurrent API calls. A short negative-result interval prevents repeated provider calls only after the complete recovery path has failed. A successful recovered quote updates the cache asynchronously after validation. The response must not wait for unrelated symbols in the same batch.

### Meaningful, structured logging and metrics

Every quote request and recovery must carry a generated/request correlation ID. Log the flow in structured, searchable fields so a developer can trace one symbol without reconstructing the path from free text.

| Stage | Required log fields | Level |
|---|---|---|
| Request accepted | correlationId, endpoint, requested symbol, requested exchange, timeframe, batch size | `DEBUG` / one aggregate `INFO` per batch |
| Identity normalized | correlationId, requested symbol, normalized symbol, resolved exchange, resolution outcome | `DEBUG` |
| Instrument resolution failed | correlationId, normalized symbol, exchange, resolver source, reason code | `WARN` |
| Cache read | correlationId, canonical instrument identity, cache key type, positive/zero/missing result, quote age, trade date | `DEBUG` |
| Recovery started/completed | correlationId, canonical instrument identity, calendar state, attempted source, outcome, latencyMs | `INFO` |
| Provider rejection/failure | correlationId, canonical instrument identity, provider operation, sanitized error code, HTTP status, latencyMs | `WARN` / `ERROR` only for unexpected failures |
| Final quote decision | correlationId, status, source, asOf, tradeDate, price-present flag, reason code | `INFO` |

Never log authorization headers, tokens, connection strings, full provider payloads, or customer/order details. Avoid per-tick `INFO` noise: batch summaries are `INFO`; normal successful cache hits are `DEBUG`. Add counters/timers for resolver failures, zero-cache entries, recovery attempts, recovery success by source, final `NO_DATA` reason, quote age, and provider latency. These metrics and correlation IDs must be visible in existing logs/Grafana.

### Named DTOs and OpenAPI-ready endpoints

Do not add or keep anonymous `Map<String, Object>` responses for the new quote-quality API. Create explicit, verbose DTOs in the existing API model package:

| DTO / enum | Purpose |
|---|---|
| `MarketQuoteBatchResponseDto` | Batch response with request correlation ID, server timestamp, exchange/session context, and aggregate counts. |
| `MarketQuoteItemResponseDto` | One requested symbol’s canonical identity, numeric values, calculation fields, quote-quality state, source, timestamps, and safe reason code. |
| `MarketQuoteQualityStatus` | Closed enum: `LIVE`, `CLOSED`, `DELAYED`, `NO_DATA`. |
| `MarketQuoteRecoveryReason` | Closed safe reason enum: cache miss, cache zero, stale cache, unresolved instrument, provider failure, history missing, calendar uncertain. |
| `MarketQuoteBatchSummaryDto` | Requested, live, closed, delayed, and no-data counts; used by Heatmap and Movers to explain partial results. |

Expose a typed additive endpoint, for example `GET /v2/market-data/quotes`, with a stable `operationId`, schema examples, and the DTOs above as OpenAPI component schemas. Preserve current `/live-ltp` and `/quotes` JSON contracts while existing clients migrate; mark the legacy map-based endpoints deprecated in OpenAPI after the typed endpoint is verified. Market UI and Paper Trading use only the typed endpoint once available.

Add API contract tests and Postman regression requests for: a fresh cache hit, Redis zero followed by provider recovery, after-close official close recovery, unresolved instrument, provider error, stale calendar, partial batch, and an all-failed batch. The generated OpenAPI document must show named schemas for every new successful response, never a free-form object/map.

## Existing `unresolved_symbols` ledger and logging compatibility

### What the existing collection can and cannot do

`unresolved_symbols` is useful for this work, but it is not a market-price source. Today `InstrumentUtils` records a symbol only after the local instrument-table lookup cannot resolve it. The document records a unique symbol, first/last requested times, request count, and a `resolved` flag. It is currently written asynchronously, is not read by the quote path, has no automated retry worker, and its `resolved` flag is not updated. Therefore it can identify repeated bad input, missing instrument-master records, or unsupported naming, but it cannot itself recover a quote or prove that a price is unavailable.

### Required use in the recovery flow

1. Do not write an `unresolved_symbols` entry on a Redis miss, zero cache value, provider timeout, or temporary Influx failure. Those are quote-source failures, not unresolved identities.
2. Attempt canonicalization, prefix stripping, ISIN lookup, exchange-aware instrument lookup, and the existing provider resolver first.
3. Only after every identity-resolution path fails, upsert the ledger entry asynchronously and return `NO_DATA` with `KEY_UNRESOLVED`.
4. A successful later resolution marks the corresponding ledger entry resolved and records its latest resolved instrument identity. It does not delete the history.
5. Use the existing ledger as an operations queue: rank unresolved identities by request count and recency after an instrument-master refresh, then run a bounded reconciliation through the existing resolver. Do not retry it for every user request.

Extend the existing document additively with optional diagnostic fields: requested identifier, normalized identifier, requested/resolved exchange, identifier type (`TICKER`, `ISIN`, provider key, or unknown), last safe reason code, resolver source, last correlation/trace reference, last attempted time, and resolved instrument identity. Keep the current `symbol`, timestamps, request count, and existing records readable. Before introducing an exchange-aware unique key, inspect the current Mongo index and prepare a backwards-compatible migration; do not silently change or drop the existing unique index.

### Preserve the existing logging system

The service already uses `FlowLogger`/`FlowSpan` for request flow, SLF4J for component logs, MDC helpers in `LoggingUtil`, JSON logging from `am-logback-include.xml`, OpenTelemetry `traceId`/`spanId`, and Micrometer metrics. The quote-recovery implementation must extend this stack only:

- Start or continue the existing `FlowSpan` at the controller/service boundary. Reuse its trace/span correlation in all recovery logs and DTO diagnostics; do not create a competing correlation-ID generator.
- Use existing SLF4J parameterized messages and the existing MDC keys (`component`, `operation`, `symbol`, `interval`) through `LoggingUtil` where applicable. Add quote-specific fields only through the established structured logging/MDC mechanism after confirming they are included by the shared logger.
- Preserve `logback-spring.xml`, the shared `am-logback-include.xml` transport/format, existing log levels, trace export, and existing FlowLogger operation names. Do not add a second logger, appender, log format, or synchronous remote logging call.
- Keep normal cache hits at `DEBUG`, one FlowSpan completion plus aggregate summary at `INFO`, expected unresolved/provider outcomes at `WARN`, and only unexpected exceptions at `ERROR` with the existing exception handling pattern.
- Add Micrometer counters/timers beside existing `market.data.*` metrics, tagged with bounded values such as recovery source and reason code. Never tag metrics with raw symbols, ISINs, instrument keys, user IDs, or correlation IDs.

### Logging and ledger acceptance criteria

- One quote request can be traced in Grafana through existing `traceId`/`spanId`, FlowSpan operation, and structured symbol-resolution/recovery fields.
- An unresolved symbol record means identity resolution exhausted, never merely that Redis or a provider was temporarily unavailable.
- A repaired instrument is marked resolved without losing the request-count history that identified it.
- Existing dashboards, JSON parsing, log transport, trace export, and FlowLogger spans continue working unchanged.
- No credentials, provider authorization data, full provider payload, user identifier, or raw order data enters application logs, metrics, or the unresolved-symbol ledger.

## Final gap closure after implementation-plan review

### Terminology and response compatibility

The earlier uses of `UNAVAILABLE` in this document mean the final `NO_DATA` state defined above. Implementation must use only the final enum vocabulary: `LIVE`, `CLOSED`, `DELAYED`, and `NO_DATA`. A valid LTP with a missing performance base is not `NO_DATA`: return the quote normally and make only the change/performance fields unavailable.

The current controller is rooted at `/v1/market-data`. The illustrative `/v2/market-data/quotes` path must be confirmed against the API gateway/versioning conventions during discovery before code is written. The requirement is a new typed, versioned, additive route; it must not be created at an un-routable path. The legacy `/v1` endpoints adapt from the one central typed quote result and retain their present JSON shape until consumers migrate.

### Calendar and close-data proof

`UpstoxMarketCalendarSource` accepts an exchange parameter and maps exchange-specific holiday/timing records, so it is suitable for BSE only when the provider response actually contains valid BSE data. Preproduction must prove current-year NSE and BSE calendar rows, source metadata, and special-session timing before SENSEX is allowed to use calendar-based decisions. If BSE calendar coverage is absent or stale, SENSEX can display a validated completed close but cannot receive an immediate simulated market-fill permission.

`OfficialClosePolicy` selects the latest positive candle; quote recovery must additionally prove that its candle belongs to a completed exchange session, has the expected trade date, and comes from an approved historical source. A current-day partial candle, an unfinalized tick, or a candle from a different resolved instrument cannot become `CLOSED`.

### One coordinator and complete adoption

Create one internal typed quote-recovery coordinator. It is the sole owner of identity resolution, exchange calendar decision, cache recovery, provider fallback, official-close validation, quote status, and cache repair. The typed API, legacy `/live-prices`, legacy `/live-ltp`, legacy `/quotes`, Heatmap, Movers, Watchlist, and Paper Trading must delegate to it rather than reimplementing a different fallback order. This prevents a UI surface from silently restoring the current zero/stale behaviour.

Add a consumer migration table to the implementation PR and prove each consumer’s old/new response mapping with contract tests. No client is switched to the typed endpoint until the endpoint is present in generated OpenAPI and its Postman regression collection has passed.

### Safe asynchronous diagnostics

The current unresolved-symbol write uses a generic asynchronous task, which can lose MDC and trace context. New recovery diagnostics must use an existing application-managed executor with observability context propagation, or explicitly capture only the existing `traceId`/`spanId` as data before dispatch. Do not create a new executor or tracing system. The background write is best-effort and must never delay a quote response.

Normalize, bound, and validate identifiers before logging or storing them: trim, canonicalize case/prefix, cap length, and record identifier type. This prevents malformed external input from creating excessive cardinality, confusing logs, or polluted unresolved-symbol entries.

### Final acceptance additions

- The generated OpenAPI route is reachable through the deployed gateway and has named request/error/success schemas before any UI migration.
- A BSE/SENSEX calendar decision is enabled only after a preproduction proof of BSE calendar records; NSE success is not accepted as BSE proof.
- `CLOSED` is backed by a verified completed-session candle for the same resolved instrument and trade date.
- Every quote consumer delegates to the central recovery coordinator; no independent cache/provider fallback remains.
- Asynchronous unresolved-symbol persistence preserves an existing trace reference where supported and never adds latency to the request path.

### Final review rating

**9.7/10.** The plan now has a consistent quote-state vocabulary, calendar-aware multi-exchange recovery, validated official-close behaviour, one recovery owner, typed backwards-compatible APIs, existing-observability compatibility, and a safe unresolved-symbol feedback loop. The remaining 0.3 is intentionally reserved for source verification that cannot be assumed from code alone: preproduction BSE calendar coverage, provider instrument-key coverage, gateway route confirmation, and real fixture results.
# Top Movers shared-quote recovery plan

## Problem

Top Movers can return empty gainers and losers when required stock prices are missing from Redis or the historical-data cache. The request path can then fall back to provider history lookups for individual symbols. That makes a dashboard request slow, repeats calls across indexes and concurrent users, and can time out before ranking is produced. A provider lookup can also fail if a bare ticker is sent where Upstox requires its instrument key. Returning a zero or old price would make the ranking misleading.

## Evidence and reasoning

The observed request path is: index membership → quote enrichment → cache/history lookup → provider fallback → filter and rank. Diagnostics showed the request spending time in per-symbol historical provider fallback, while valid cached quotes were available for some liquid symbols. Existing code has bulk Upstox quote/OHLC methods and a streaming-to-Redis path; this plan reuses those mechanisms rather than adding another provider integration. Index membership changes less often than prices, so membership should define the subscription universe while prices continue to update independently.

## Intended behavior

- Top Movers keeps its existing `gainers` and `losers` arrays, DTO fields, and JSON shape.
- Rankings use a valid current price and the correct previous close for the trading session.
- During an open session, rankings use a fresh live quote. After close or on a holiday, they use the completed session’s cached close/reference values, with the existing market calendar determining the session.
- A missing or invalid quote triggers a bounded batched recovery attempt; it is never converted to a fake zero or silently presented as current.
- If recovery fails, that constituent is omitted from ranking and the reason is logged. Missing data is not represented as a flat market.
- Successfully recovered data is stored in shared cache for later requests and other indexes to reuse.

## Source-level verification against the current branch

The following findings were checked in the local `am-market` tree and replace the earlier “verify during implementation” assumptions:

| Area | Verified current behavior | Required plan consequence |
|---|---|---|
| Unified Top Movers | `MarketAnalyticsService.getMoversUnified` calls `fetchEnrichedData` once and sorts that same snapshot for gainers and losers. | Keep this optimization. Fix the quote source and completeness checks underneath it; do not rebuild the endpoint. |
| Slow fallback | `SmartStockService` calls the 9-argument `getHistoricalDataBatch` overload. `MarketDataService` delegates it with `allowProviderFallback=true`; a missing member can enter provider history retrieval on the request path. | Make Movers history reads cache/database-only. Recover current-price misses through one bounded batch LTP call, not per-symbol historical calls. |
| Batch capability | `UpstoxMarketDataProvider.getLTP` resolves a list through `UpstoxSymbolResolver` and chunks it with `BATCH_SIZE=500`. The resolver performs batched instrument lookups. 500 is current code configuration, not yet independently verified as the vendor limit. | Reuse the batch path, verify the vendor limit before release, pass exchange-qualified identities, and reject ambiguous/unresolved mappings. |
| Quote validity | `StockDataEnricher` filters only on positive `lastPrice`; missing `previousClose` becomes sortable/displayable `0.0%`. `SmartStockService` can reject a valid unchanged quote when OHLC open is absent and `lastPrice == previousClose`. | Require positive current price and positive session-valid base; equal current/base values are valid unchanged data. Missing base is incomplete, never `0.0%`. |
| Zero placeholders | `OHLCDataRetriever.retrieveFromProvider` creates zero-valued `OHLCQuote` entries for missing provider results and empty provider responses. The latest-price Redis writer separately rejects `lastPrice <= 0`, but the retriever still returns placeholders as results. | Remove placeholder-as-success behavior and ensure downstream persistence cannot store a zero placeholder as a successful quote. |
| Quote timestamps | `OHLCQuote` has no timestamp/source fields. `MarketDataCacheService.cacheLatestPrices` stores server-side `updatedAt` and `source=UPSTOX_WS`, without retaining exchange event time or comparing timestamps before overwriting. | Keep public DTOs unchanged. Track internal receipt time/source and provider event time when available; use an atomic newer-write check. Without event time, freshness only means recent receipt while feed health is good. |
| Existing coalescing | `StockIndicesService.activeSymbolFetches` coalesces per-symbol calls within one JVM. The Redis option-chain lock fails open and releases by key without an owner token. | Do not reuse either unchanged. Use a token-owned, fail-closed quote-refresh lease across pods; if Redis coordination is down, do not fan out to provider. |
| NSE membership registry | `nseindices.yml`/`NSEIndicesConfig` enumerate broad and sector NSE indices. `StockIndicesMarketDataService` supports batch `findByIndexSymbols`, but no list-all method. `SymbolOrchestratorService` subscribes defaults, NIFTY 500, ETFs, active portfolio symbols, and global indexes—not all configured NSE members. | Use configured index names plus the existing batch lookup to expand the stream universe. No new list-all API is needed for NSE membership. |
| Membership validation/repair | The scraper rejects empty lists and applies existing 45/95/190/490 minimums to numeric 50/100/200/500 indexes, plus market-status and trade-date checks. `NSEStockInsidicesData` has no source-reported total. `StockIndicesService` treats any existing Mongo document as found, even if `data` is empty/incomplete. Sector indexes have no count rule. | Keep existing broad-index checks; validate blank/duplicate members and existing invalid documents; retain last-known-good membership. For an unexpected sector-roster shrink, require a second matching scrape before replacement and alarm on unresolved coverage. Since there is no source total, do not claim certainty against a stable truncated upstream response. |
| SENSEX membership | NSE config does not list SENSEX. The provider has a BSE index-key mapping, but the inspected code has no BSE constituent scraper/config path; the reported Mongo collection has no SENSEX document. | Apply all-member streaming to configured NSE indexes. Keep SENSEX out of complete movers until a validated BSE membership source populates the existing membership store; never infer members from the index key. |
| Calendar | `MarketCalendarService` supports exchange-specific sessions. `MarketHoursService.isMarketOpen()` asks only for NSE and can fall back when calendar data is missing/stale. | Use the calendar for each member’s exchange. A fallback/stale calendar cannot certify a LIVE value; use a validated completed-session close or mark the index incomplete. |

### Resolved behavior when completeness cannot be proven

The existing response has only `gainers` and `losers`, with no coverage field. Keep that shape unchanged. Attempt batch recovery first. If any required member still lacks verified identity, a fresh positive current price, or a valid comparison base, return the existing empty arrays for that index and log one structured coverage summary. This can resemble a flat market in older clients, but it avoids a wrong “top” or “bottom” ranking; logs and metrics distinguish the cause. Adding a public status field requires a separate contract review.

## Implementation plan

1. **Use the verified NSE index registry.** Read broad/sector names from `NSEIndicesConfig`, load membership with one `findByIndexSymbols` call, and union distinct members with existing defaults, ETFs, and active symbols. Keep SENSEX out of complete BSE rankings until a validated BSE membership source exists.
2. **Repair invalid memberships safely.** Treat existing empty, null, duplicate, or invalid arrays as repair candidates, not valid documents. Preserve the last-known-good list unless a scrape passes trade-date, market-status, identity, uniqueness, and existing minimum-count checks. For sector lists without a source total, require a second identical scrape after an unexpected shrink and alert on unresolved coverage.
3. **Keep identity exchange-aware.** Carry index exchange into each member identity; resolve Upstox keys in bulk and map results back to canonical Redis symbols. Use ISIN/security identity where available; reject ambiguous results rather than selecting the first ticker match.
4. **Keep the existing one-snapshot Movers calculation.** `getMoversUnified` already enriches once and sorts the same data for both sides. Preserve it. Require positive LTP and a positive, session-correct comparison base; equal prices are valid unchanged data; missing bases must not become `0.0%`.
5. **Remove the slow request fallback.** Make Movers history reads cache/database-only by explicitly disabling provider fallback. For current-price misses, use the existing batched LTP path once per owned refresh set, with a validated base price. Never issue serial historical candle calls for missing Movers symbols in the interactive request.
6. **Coalesce and bound recovery.** Use owner-token Redis leases per normalized symbol, group owned symbols into the current configured provider batches (500 in code; verify the live vendor limit before release), and apply bounded timeouts/cooldown. Waiters re-read cache briefly and do not launch duplicate work. Redis coordination failure is fail-closed.
7. **Protect quote cache integrity.** Remove zero placeholders from provider results before they can be treated as successful data. Store only positive prices with valid bases; retain receipt time, source, trade date, and provider event time when supplied. Atomic writes reject older events. Treat legacy cache entries without reliable freshness metadata as misses.
8. **Apply exchange session policy.** Use `MarketCalendarService` for the relevant exchange. A stale/fallback calendar cannot certify a LIVE quote. After close, use a completed-session close matching the same instrument and trade date; otherwise the index is incomplete.
9. **Preserve public contracts and logging.** Keep `/analysis/movers`, its parameters, `gainers`/`losers` DTO fields, and JSON shape unchanged. Make no UI edits. Follow existing AppLogger/SLF4J/FlowLogger/Micrometer conventions; log a batch summary and bounded symbol diagnostics without credentials or raw-symbol metric labels.
10. **Verify safely.** Cover membership union/deduplication, stale-document repair, SENSEX exclusion until BSE members exist, ambiguous identity, LTP-only recovery, missing base, valid unchanged quote, zero/stale/out-of-order ticks, lease expiry/owner-safe release, Redis outage, provider partial response/timeout, calendar fallback, and unchanged Movers JSON. Use isolated/mock-backed tests; do not write through production DB/cache port-forwards. Keep Kubernetes production access read-only.

## Edge cases and handling

| Case | Handling |
|---|---|
| A stock belongs to several indexes | Deduplicate provider work and share its validated cached quote; rank it within each index from the same snapshot. |
| Some members resolve and others do not | Attempt batch recovery; if a required member remains unresolved, return existing empty arrays for that index and log coverage rather than presenting a partial ranking as complete. |
| Provider returns partial batch data or times out | Cache only valid returned prices. If required coverage remains incomplete, return empty arrays for that index, apply cooldown, and avoid per-symbol request-path fallback. |
| Cache has zero, malformed, or stale quote | Treat it as invalid; do not rank it or let an older response overwrite a newer valid quote. |
| Concurrent requests miss the same quote | Coalesce across pods with owner-token Redis leases per normalized symbol; if Redis is unavailable, fail closed rather than fan out. |
| Market is closed, weekend, or holiday | Use the market calendar and last completed session; do not label a prior-session close as live. |
| Previous close is missing or invalid | Exclude that member until bounded cache/database recovery succeeds and log the missing field. |
| Membership data is incomplete or absent | Repair through the configured NSE scraper and retain the last-good roster. SENSEX stays incomplete until a validated BSE membership source exists. |
| Duplicate ticker names or changed provider mapping | Qualify with the index exchange and use ISIN/security identity; reject ambiguous resolver results rather than choosing the first. |
| Feed and batch recovery both fail | Do not serve stale/zero values or partial rankings as complete; return existing empty arrays for that index and log outage/coverage counts. |
| Current price is valid but comparison base is missing | Exclude the ranking; do not let null percentage change become `0.0%`. |
| Current price equals previous close and OHLC open is absent | Keep it as valid unchanged data; equality alone is not evidence of a bad quote. |
| Feed provides only server receipt time | Use receipt age plus stream-health heartbeat and disclose this freshness limit in diagnostics; do not claim exchange-event freshness. |

## Expected file areas

Limit changes to market-data backend components already in this request path: `SymbolOrchestratorService` for the membership subscription universe, `SmartStockService` to prevent request-time per-symbol history fallback, `OHLCDataRetriever` or the existing cache writer to reject invalid quote placeholders, and existing analytics/enrichment code only if needed to consume one shared snapshot. Add focused tests beside these classes. Confirm exact files against the current branch before editing. No UI, unrelated services, credential changes, or production data changes are in scope.

## Acceptance checks

- All supported indexes use membership from the existing source; shared members are streamed/fetched once.
- A Movers request does not perform serial historical-provider calls per missing constituent.
- A cache miss uses bounded batch recovery; successful quotes are reused by later requests.
- Upstox receives resolved instrument keys; database/cache lookups keep the backend’s accepted canonical format.
- Zero, stale, malformed, or older-over-newer quotes never become ranked prices or overwrite newer values.
- Previous-close calculations follow the market calendar and last completed trading session.
- Partial provider failures cache only valid returned quotes; the affected index is omitted from rankings until full quote/base coverage is valid, with the reason visible in logs. Other indexes continue independently.
- Existing Top Movers JSON and other endpoint contracts remain unchanged.
- Verification does not write to production-connected stores.

## Top Movers test plan and latency behavior

The cache-hit path is one Redis snapshot plus membership/base reads. A cache miss is allowed one shared, bounded LTP batch for symbols owned by the refresh lease; no serial historical-provider retries occur on the request path. Set the batch timeout below the shortest configured API/gateway request timeout with enough remaining budget to validate, write cache, and serialize the existing response. Establish the numeric timeout and latency target from current configuration and a pre-change baseline; do not invent a p99 target. A refresh timeout or failed lease wait produces the existing empty arrays for an index whose full coverage is unavailable, while other indexes continue.

| ID | Case / trigger | Expected result | Verification |
|---|---|---|---|
| TM-01 | Complete Redis snapshot; all prices and bases valid | Same current `gainers`/`losers` JSON; one enrichment snapshot | Unit test plus existing Postman response contract |
| TM-02 | Redis miss for multiple symbols | One batched Upstox LTP lookup after resolving keys; successful results reused from cache | Provider/service unit test; assert call count and cache write |
| TM-03 | Several pods/requests miss the same symbols | One owner refresh; waiters reread cache and do not duplicate provider work | Distributed-lock integration test with shared test Redis |
| TM-04 | Redis lease store unavailable | No provider fan-out; incomplete index returns existing empty arrays and logs/metrics the coordination failure | Unit test with Redis exception |
| TM-05 | Provider returns partial data, zero, malformed key, or times out | Cache only valid returned prices; no placeholder success; index is not ranked as complete | Provider/enricher tests |
| TM-06 | `lastPrice == previousClose`, OHLC open absent | Keep the unchanged quote; rank at true 0.0% only when the base is valid | SmartStockService/enricher regression test |
| TM-07 | Positive LTP but missing, zero, wrong-session, or wrong-exchange base | Do not rank as 0.0%; return existing empty arrays if index coverage is incomplete | Enricher/analytics regression test |
| TM-08 | Older tick arrives after newer tick | Older event cannot replace the newer cache value | Cache compare-and-set integration test |
| TM-09 | Membership doc exists but is empty, duplicate, or below existing validity rules | Trigger existing scraper repair; keep last-known-good membership if new snapshot fails validation | Membership service tests |
| TM-10 | Sector membership shrinks unexpectedly | Repeat source fetch; publish only if matching valid roster; otherwise keep prior roster and alert coverage | Scraper validation test with two responses |
| TM-11 | NIFTY members overlap with another configured index | One shared refresh for a symbol; completeness still assessed separately per index | Universe/enrichment test |
| TM-12 | SENSEX requested without validated BSE roster/calendar | Do not claim complete movers; preserve endpoint shape and log missing BSE membership/calendar | Contract test and operational checklist |
| TM-13 | Weekend, holiday, special session, stale calendar | Select a verified completed-session base or mark the ranking incomplete; never use yesterday by calendar arithmetic | Calendar boundary tests |
| TM-14 | Legacy Redis entry has no reliable freshness metadata | Treat as a miss and repopulate; do not label it live | Cache compatibility test |
| TM-15 | Existing API consumers request Top Movers | JSON keys, DTO fields, route, and parameters remain unchanged | Postman contract comparison against baseline |

**Test status:** planned only; no tests or Postman requests were run during this source review. **Operational proof before release:** record cache-hit/miss latency, provider batch duration, duplicate-batch count, quote coverage, stale/invalid rejection count, incomplete-index count, and per-index membership freshness in preprod. Avoid logging raw secrets or symbol values as metric labels.

## Hardening decisions for edge cases and tradeoffs

| Gap or tradeoff | Decision and handling |
|---|---|
| Multiple backend pods can miss the same Redis quotes together | Coalesce refreshes across pods with Redis `SET NX` leases per normalized symbol, then group the symbols owned by that request into provider-sized batches. Give each lease a short TTL longer than the provider timeout; release it only when its owner token matches. Other pods briefly re-read cache, then skip duplicate provider work if the lease is still held. If Redis is unavailable, do not fan out to Upstox from every pod; rely on the stream, fail closed for missing quotes, and log the cache/coordination failure. Prefer an existing distributed-lock utility if the codebase already has one. |
| Provider timeout or repeated failure creates a request storm | One bounded batch attempt per refresh cycle; no immediate retry loop. Apply per-symbol cooldown with bounded exponential backoff and jitter, and use an existing provider circuit breaker if present. Do not cooldown permanent identity errors as if they were transient: flag unresolved/invalid instrument keys for mapping correction and suppress repeated calls until the mapping changes or its refresh window expires. |
| Late or out-of-order ticks overwrite a newer price | Store/update a quote only when its event timestamp is newer than the cached quote timestamp. Compare-and-set the value atomically where the existing Redis write path supports it. A response timestamp generated by the server is not a substitute for the feed event time. |
| Existing quote cache lacks reliable freshness/source metadata | First reuse the metadata already present. If it is absent, add internal versioned/sidecar metadata with feed event time, server receipt time, source, and trade date; dual-read existing cache entries during rollout and treat entries without trustworthy timestamps as expired. Do not alter public DTOs or interpret an old value as live. |
| Feed cadence differs by instrument or session | Make the live freshness threshold configuration-driven from the observed stream cadence plus a bounded grace period; expose age and stale-count metrics. Do not hardcode one universal age until stream cadence is confirmed. Closed-session values are valid only for the latest completed trading session determined by the calendar. |
| NSE membership refresh is partial, empty, delayed, or fails | Validate the new membership snapshot before replacing the saved one. Never replace a known-good snapshot with an empty or clearly partial result. Keep the last-known-good membership set, record its refresh time/source, and report coverage/age. For a new index with no verified snapshot, do not invent constituents from the index name or expected count. |
| A quote is missing for one or more members, making the top/bottom rank uncertain | Attempt the bounded batch recovery. Since the unchanged response has no completeness field, do not present rankings as complete when any constituent lacks a valid price or previous close: return the existing empty `gainers`/`losers` arrays for that index and emit a structured completeness diagnostic. This favors trustworthy rankings over a plausible but potentially incorrect partial ranking. |
| One bad member should not stop other indexes from updating | Process each index independently from its verified membership snapshot. A failure for one index does not block other index responses or shared valid quote updates. Deduplicate shared symbol fetches, but keep membership and completeness decisions index-specific. |
| Previous close is from the wrong date/exchange or affected by a corporate action | Require positive previous-close value, matching instrument/exchange, and session date consistent with the market calendar. Prefer the provider’s official session reference or the existing validated daily close. If the source marks an adjustment or date mismatch, exclude the affected ranking input and log it rather than calculate a misleading percentage. |
| Weekend, holiday, special session, or early close | Use the existing market calendar in the exchange timezone, including special sessions/early close data if supported. “Closed” means the last completed trading session, not simply yesterday. If the calendar has no answer for a date, fail closed and log a calendar-data issue. |
| Symbol aliases, duplicate tickers, or stale instrument mappings | Normalize exchange and ticker at ingestion; resolve via exchange plus instrument identity/ISIN when available. Never resolve by ticker alone if it is ambiguous. Keep the unresolved-symbol diagnostics and avoid repeating provider calls for a mapping already known invalid. |
| Batch response is partial, duplicated, malformed, or contains zero prices | Validate every returned instrument independently, map results back to requested canonical symbols, ignore duplicates/unknown keys and invalid values, and cache only valid newer quotes. Mark the affected index incomplete if a required member remains unresolved. |
| Cache/Redis outage | Do not turn every Top Movers HTTP request into a provider fallback. Let the existing feed/provider recovery mechanism recover when coordination and cache are healthy; return the unchanged empty arrays for incomplete data and log/metric the Redis outage. |
| Rollout mixes old and new backend pods | Keep API JSON unchanged; make internal cache metadata dual-read compatible. Use additive internal keys or versioned values, avoid destructive key migrations, and allow old pods to ignore new sidecar metadata. Confirm that mixed-version writes cannot replace a newer quote with an older one before rollout. |

### Explicit tradeoffs

- **Freshness vs. availability:** A stale cached quote may make the page appear populated, but it can rank stocks incorrectly. The plan chooses to omit incomplete rankings rather than serve stale prices as current.
- **Completeness vs. partial results:** Returning top movers from only the symbols that happened to load is faster, but an omitted member could actually be the top mover. The plan therefore requires full valid coverage for a ranking and uses the existing empty-array response when that requirement is not met.
- **Provider load vs. recovery speed:** A single bounded batch and cooldown may leave a failed symbol unavailable until the next refresh cycle. This is preferable to repeated timeouts and rate-limit pressure. The stream remains the primary continuous source.
- **Compatibility vs. diagnostics:** Public response shape stays fixed. Operational reasons are exposed through structured logs/metrics, so a future UI/API status field would require a separately reviewed contract change.
- **Stream breadth vs. subscription capacity:** Subscribe to every verified supported index member only within the provider’s documented subscription capacity. If the full set exceeds capacity, prioritize the configured index universe and record explicit coverage metrics; do not silently claim full coverage. Confirm current provider limits before choosing the prioritization policy.

## Review

Source verification is complete for the local branch paths cited above. The design reuses the existing NSE index registry, batched membership lookup, Upstox LTP resolver/batch path, market calendar, one-snapshot Movers calculation, and logging stack. It avoids reusing the unsafe option-chain lock unchanged and closes missing-base, unchanged-quote, zero-placeholder, out-of-order-tick, incomplete-membership, and cross-pod duplication gaps. **Top Movers plan rating: 9.5/10.** Remaining external verification is limited to current Upstox batch/subscription limits and actual SENSEX BSE membership/calendar availability. The plan handles both conservatively and does not claim complete SENSEX coverage without validated member data. This is a plan update only, not code implementation or production authorization.

# Verified all-index baseline redesign

## Correction from the live trace

The prior Top Movers section described the history lookup as batched. The production-equivalent local trace disproved that claim and supersedes it for this slice.

- `GET /v1/analysis/movers?type=all&limit=10&indexSymbol=NIFTY%2050&timeFrame=1D` took **29,237 ms**.
- The `service.market.historical.batch` span consumed **28,653 ms** and returned `resultCount=0`, `totalPoints=0` for **50 symbols**.
- The request created 50 enriched quote objects, then rejected all 50 because none had a valid comparison base.
- The NIFTY 50 roster was complete in this trace: `members=50`, `minimumMembers=45`. Membership count was not the cause of this request's empty result.
- `HistoricalDataRetriever.retrieveFromDatabase` currently loops over symbols and calls `persistenceService.getHistoricalData(...)` once per symbol. This is an N+1 Influx read, not a batch read.

The trace proves a missing-baseline failure. It does not by itself prove whether the zero Influx results are caused by an unseeded recent date range, a canonical-symbol mismatch, or both. The plan below makes that distinction a mandatory discovery gate before altering stored data; it does not assume one cause.

## Scope and non-goals

This is a backend-only fix inside `am-market` and its existing `am-common-investment-data` module. It applies to every index with a verified roster: NIFTY broad-market indexes, sector indexes such as NIFTY BANK and NIFTY IT, and later SENSEX after its BSE roster and calendar are verified. No Market UI change and no public Top Movers response change are part of this slice.

The plan does not create a new scheduler, database, API, or provider. It reuses the existing stream, previous-close repository, market calendar, historical sync, Influx repository, and one-snapshot Movers path.

## First proposal review — 6.4/10

The earlier plan had the right safety goal: never rank partial or stale data. It was not ready to implement because it missed the real source-level query shape exposed by the trace.

| Gap | Why it matters | Resolution in this redesign |
|---|---|---|
| “Batch history” was only a method name | The implementation still issued 50 sequential Influx reads, causing the 28.65-second span. | Add a date-bounded, multi-symbol historical query and route baseline recovery through it. |
| One stock identity was not explicit across stream, cache, Influx, and resolver | A quote can exist under ISIN/provider key while Movers asks for a ticker. | Use one internal exchange-qualified identity and preserve ticker/ISIN/provider-key mapping at every boundary. |
| Previous-close date meaning was ambiguous | A base close is for the session before the quote session, not simply “today”. | Store `quoteSessionDate` and `baseSessionDate` with the source and receipt/event times. |
| Equality was treated as suspicious | `lastPrice == previousClose` is a valid 0.00% day. | Accept equality whenever the source, identity, and session dates are valid. |
| NIFTY 50 rules were too name-based | NIFTY BANK, IT, and future rebalanced indexes do not have stable numeric names. | Assess completeness against each verified roster snapshot, not an index-name heuristic. |
| Chunk policy was implied, not executable | NIFTY 500 can become another unbounded or serial workload. | Reuse the existing Influx 100-symbol chunk limit and apply bounded concurrency. |
| The request path was asked to repair a data pipeline outage | It made the UI wait 29 seconds and still returned no data. | Populate bases during existing session/EOD work; request-time recovery is bounded and database-only. |

## Revised target design

### 1. One stock record serves every index

Index membership is separate from price data. A stock that appears in NIFTY 50, NIFTY 100, and NIFTY 500 has one canonical internal identity and one validated quote/base record. Each index only decides whether its own roster is complete and how to rank those shared records.

The canonical internal identity is exchange + trading symbol, with ISIN and Upstox instrument key retained as resolver attributes. For example, the logical identity is `NSE:RELIANCE`; existing Redis reads may retain the backward-compatible bare NSE alias (`RELIANCE`), but both forms must resolve to one identity. The resolver may use `NSE_EQ|INE002A01018`; no path may silently substitute an ISIN-only cache key for the ticker identity.

### 2. Session-correct base snapshots are primary

Reuse the existing `PreviousCloseDocument`, calendar, and historical sync path. A valid baseline must contain:

- canonical identity and exchange;
- positive official close;
- `quoteSessionDate` for the quote being compared;
- `baseSessionDate`, the immediately preceding completed exchange session;
- source, provider event time when available, receipt time, and write time.

The market calendar determines both dates. On weekends, holidays, and special sessions it selects completed sessions rather than subtracting one calendar day. A correct unchanged stock keeps `lastPrice == previousClose` and calculates `0.00%`; it is not sent for repair merely because the values match.

The existing ingestion/EOD flow prepares snapshots for the union of verified constituents across all configured indexes. It deduplicates shared stocks before reading or writing. This is the normal path. A Movers request reads the prepared snapshot; it must not depend on a provider historical call to become usable.

### 3. Replace N+1 history reads with date-bounded chunks

The current common Influx repository already uses 100-symbol chunks for `findByTradingSymbolIn`, proving the safe existing chunk size. That method retrieves latest values without the exact session range needed here, so it must not be reused as a previous-close source.

Add a date-bounded multi-symbol query in the existing common persistence/service path. It must:

1. accept canonical ticker identities, the exchange, `from`, `to`, and daily interval;
2. query only the required date window and return enough data to select the official completed-session close;
3. map each returned row back to the requested canonical identity;
4. distinguish `no rows`, `wrong identity`, `wrong session`, and `invalid close` without turning any into `0.0`;
5. partition requests into at most 100 identities per Flux query, matching the existing Influx compiler limit;
6. execute at most two chunks concurrently through a bounded executor; aggregate successful chunks and report failed chunks independently.

| Universe requested | Maximum query shape |
|---|---|
| NIFTY BANK, IT, or a typical sector roster | One query when at most 100 distinct stocks |
| NIFTY 50 / NIFTY 100 | One query per 100-stock chunk; no per-stock loop |
| NIFTY 200 | Two chunks, maximum two concurrent |
| NIFTY 500 / union of several indexes | Six chunks maximum, two concurrent; shared stocks deduplicated first |

Chunking is required for the broad universe. It is not a substitute for correct data: a chunk returning no valid bases marks only the affected index incomplete and provides a reason in diagnostics.

### 4. Bounded request path

For one Movers request:

1. Load that index's latest verified roster and exchange.
2. Read live quotes and prepared base snapshots using the same canonical identities.
3. For missing bases, run at most one date-bounded cache/Influx batch recovery for the unresolved members. Never call historical Upstox candles on the interactive path.
4. If a current price is missing, use the existing resolved, bounded Upstox LTP batch and cache only valid returned prices. LTP-only data is not enough to rank until a valid base exists.
5. Evaluate coverage against the current roster snapshot. Full coverage ranks gainers and losers from one shared snapshot; incomplete coverage preserves the existing empty arrays and emits one structured summary.

Indexes are independent after the shared stock read: a missing NIFTY IT constituent does not stop NIFTY BANK, but NIFTY IT itself is not presented as a complete ranking.

### 5. Data discovery and repair gate

Before implementation changes the cache or database, run read-only preprod diagnostics for a small set of known members and one full index batch. For each requested identity, record only safe diagnostic fields: canonical ticker, candidate stored identifiers, requested session range, row count, newest row date, and rejection reason. Do not log prices, credentials, tokens, or raw Flux text.

The gate decides the repair action from evidence:

| Evidence | Repair path |
|---|---|
| Rows exist only under another valid provider/ISIN alias | Correct the canonical identity mapping and backfill the ticker-keyed snapshot through the existing sync path. |
| No rows exist for the required completed session | Use the existing resolved Upstox historical sync outside the HTTP request, then persist the validated close. |
| Rows exist but date/session is wrong | Correct calendar/session selection; do not copy the close forward. |
| Rows exist but value is zero/malformed | Reject it, preserve any last valid snapshot, and mark that index incomplete. |
| Influx query fails | Preserve cache state, mark recovery failed, and do not trigger a request-time provider storm. |

### 6. Observability and compatibility

Keep `/v1/analysis/movers` parameters and the `gainers`/`losers` JSON exactly unchanged. Add one FlowLogger/Micrometer-compatible summary per request and per recovery batch with: index, roster size, distinct-stock count, live hits, valid bases, Redis hits, Influx chunk count/duration, rejected-base reason counts, provider LTP recovery count, and coverage decision. Symbol lists belong only in bounded debug logs; metrics use counts, never ticker labels.

Use additive cache metadata and dual-read during rollout. Older quote entries remain readable only when they satisfy the new identity/session checks; otherwise they are treated as a miss. No destructive cache flush or production seed is included in this plan.

## Exact implementation areas

| Area | Existing file area | Change |
|---|---|---|
| Influx batch primitive | `am-common-investment-data/.../EquityPriceMeasurementRepository` and implementation | Add date-bounded multi-symbol daily retrieval using the existing 100-symbol partition rule. |
| Common historical service | `HistoricalDataService`, `HistoricalDataServiceImpl`, `EquityService` / implementation | Expose the typed batch result needed to select per-symbol session closes. No public HTTP endpoint changes. |
| Market retriever | `HistoricalDataRetriever` | Replace the sequential `for (symbol)` database loop with the bounded batch path; preserve per-symbol result/rejection detail. |
| Baseline lifecycle | `SmartStockService`, `PreviousCloseRepository`, existing close/ingestion scheduler | Read/write validated session snapshots; accept valid equality; use asynchronous existing-sync repair outside the request path. |
| Stream identity | `UpstoxMarketDataStreamer`, `UpstoxSymbolResolver`, `MarketDataCacheService` | Seed instrument-key-to-canonical-ticker mapping on subscribe and prevent ISIN-only cache keys from satisfying ticker requests. |
| Ranking | `StockDataEnricher`, `MarketAnalyticsService` | Keep one-snapshot ranking and full roster coverage; replace index-name minimum logic with verified-roster metadata where available. |
| Tests | Existing module test trees | Add focused unit/integration tests listed below. No UI files. |

## Test and release plan

| ID | Scenario | Expected proof |
|---|---|---|
| AB-01 | 50 members, all cache/base data valid | Movers returns ranked gainers/losers; historical span does not enter a 50-call loop. |
| AB-02 | NIFTY 500 with 501 roster members and shared members | Exactly six or fewer 100-item chunks, at most two active at once; no duplicate stock query. |
| AB-03 | NIFTY BANK and NIFTY IT with non-numeric roster sizes | Completeness is based on each stored verified roster, not its name or a hardcoded count. |
| AB-04 | Valid `lastPrice == previousClose` | Stock remains valid at `0.00%` and is excluded from both mover lists, not rejected. |
| AB-05 | Stored history under ISIN/provider key only | Diagnostic identifies identity mismatch; sync writes canonical ticker snapshot before later Movers requests use it. |
| AB-06 | Required session has no valid stored candle | Request remains bounded and returns existing empty arrays for only that index; existing async sync repairs it. |
| AB-07 | One Influx chunk fails in a broad-index request | Successful chunk results remain available internally, affected index is incomplete, no false partial rank is returned. |
| AB-08 | Weekend, holiday, special session, early close | Calendar selects the correct quote/base sessions; no calendar-day arithmetic. |
| AB-09 | Older stream tick arrives after newer tick | It cannot overwrite a newer canonical quote/base record. |
| AB-10 | Existing clients consume Movers | Postman contract comparison confirms the route, parameters, JSON keys, and DTO fields are unchanged. |

Run unit tests first, then isolated Redis/Influx integration tests, then preprod API and trace verification. The required preprod success signal is a trace where `service.market.historical.batch` has one or bounded chunk spans rather than one persistence call per member, and the coverage summary reports a positive valid-base count. Production inspection remains read-only; any data repair, restart, deployment, or seed needs a separate explicit approval.

## Adversarial review and final rating

| Risk found | Final safeguard |
|---|---|
| A batch query could return the latest current close instead of the required prior session close | The new query is date-bounded and calendar-selected; latest-only repository methods are explicitly excluded. |
| Faster partial results could be mistaken for a correct index ranking | Ranking still requires complete valid coverage for that index. |
| Larger indices could overload Influx | Hard partition of 100 and maximum concurrency of two; record per-chunk duration before tuning. |
| An index name may not reveal its constituent count | Coverage is roster-driven. A roster is versioned/validated independently of its display name. |
| Valid unchanged stocks could disappear | Equality is accepted with a verified identity and session-correct baseline. |
| A data gap could trigger Upstox calls for every browser refresh | Interactive history recovery stays cache/Influx-only; sync repair is asynchronous, deduplicated, and provider-key-resolved. |
| Old cache values and new metadata can coexist during rollout | Additive dual-read; invalid legacy entries become misses, never trusted live values. |
| SENSEX lacks a verified BSE roster | It remains ineligible for a complete ranking until its BSE membership and calendar path pass the same checks. |

**Revised rating: 9.5/10.** This rating is conditional on the mandatory discovery gate and test plan above. The plan no longer assumes why the 50 Influx reads returned zero; it deterministically diagnoses and repairs either a data-gap or identity-gap path while keeping every index, response contract, and data-quality safeguard consistent.

# OHLC and intraday-chart integrity addendum

## Confirmed observations

The local request `POST /v1/market-data/ohlc` for `NSE:TCS` proves an after-hours data-corruption bug in the
current backend. Upstox supplied today's values, including `last_price=2156.0` and `ohlc.close=2156.0`, but
AM returned `lastPrice=2036.0` and `ohlc.close=2036.0`. The value `2036.0` was yesterday's `previousClose`.

The source cause is `MarketDataService.applyOfficialDailyCloseWhenMarketClosed`: its Redis fast path treats any
positive `previousClose` as today's official close and overwrites today's quote. `previousClose` is a comparison
base for calculating today's change; it is never a replacement for today's last price or session close.

The chart symptom is independently reproduced with the same read-only request:

| Target | `GET /v1/analysis/historical-charts/TCS?range=1D` | Result |
|---|---|---|
| Local backend | HTTP 200, 75 five-minute candles ending at today's value | Valid data |
| Production endpoint | HTTP 200, `data={}`, `successfulSymbols=0`, `totalDataPoints=0` | Empty data |

This proves that the blank chart is not a Flutter rendering failure. It is an environment/path or data-availability
failure behind the same backend API. It must not be described as a completed “chart fix” until production logs
identify whether the deployed version, cache key, persistence lookup, instrument resolution, or provider fallback
differs from local.

There is also an API ambiguity that makes the failure more likely. The stock-detail caller omits
`isIndexSymbol`; both chart controller methods currently default that parameter to `true`. For `TCS`, the backend
therefore follows an index-oriented path even though the input is a stock. Local happens to return candles, but
that is not a safe contract to rely upon.

## Required correction

1. **Never overlay a quote with `previousClose`.** Remove the closed-market fast path that writes
   `previousClose` into `lastPrice` and `ohlc.close`. Preserve a valid positive quote from the provider/cache.
   Keep `previousClose` unchanged as the day-change baseline.
2. **Use a separate verified session-close snapshot when it exists.** A scheduled/sync path may replace a quote
   only with a positive daily candle close whose identity and completed session date match the requested exchange.
   If that snapshot is not ready, return the current positive provider quote; never regress it to yesterday's
   close and never manufacture `0.00%`.
3. **Resolve omitted chart symbol type on the server.** Keep `isIndexSymbol` as an optional backward-compatible
   query parameter. When supplied, honor it. When omitted, resolve an exact active security as an equity and an
   exact index document as an index. If neither resolves, retain the current public empty-data shape and log the
   typed resolution failure. This fixes stock-detail calls without forcing an immediate UI release.
4. **Make historical reads diagnosable.** Log a correlation ID, requested symbol, resolved identity and kind,
   interval/date range, each cache/database/provider attempt, point count, and selected result source. Empty
   collections are failures for chart purposes and must not be reported in logs as a successful cache result.
5. **Prove the production difference before claiming deployment complete.** Compare the deployed artifact version
   and effective historical-ingestion settings with local, then run the identical TCS request and inspect its
   trace. The expected production path is one resolved equity identity and at least one five-minute point on a
   completed session. This is read-only investigation; no production cache seeding is part of this plan.

## Acceptance tests

| ID | Scenario | Expected result |
|---|---|---|
| OC-01 | Closed-market TCS quote with `lastPrice=2156`, `previousClose=2036` | Return last/close `2156`; retain previous close `2036`. |
| OC-02 | Closed market, valid same-session daily close snapshot | Return that same-session close only; never prior-session close. |
| OC-03 | Closed market, no verified session close but positive provider quote | Preserve provider quote and record the missing snapshot. |
| OC-04 | Direct share chart call without `isIndexSymbol` | Resolver selects equity path and returns its intraday candles. |
| OC-05 | Index chart call without the flag | Resolver selects index path and retains current index-chart behavior. |
| OC-06 | Unknown chart symbol | Existing HTTP/response contract remains stable; logs state unresolved identity and no points. |
| OC-07 | Local and production after deployment | Same TCS 1D request returns a non-empty series with the same resolved identity and valid session date. |

This addendum is part of the price-integrity work and introduces no UI response-schema change. A UI caller may
later send the explicit symbol-type flag as defence in depth, but it is not required for the backend correction.
