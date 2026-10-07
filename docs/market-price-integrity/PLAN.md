# Market price, index performance, and Top Movers fix

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
