# Discover UI — full layout, gaps, TODO

**Mocks (SoT):** [basket-design-web.png](./basket-design-web.png) · [basket-mobile-screen.png](./basket-mobile-screen.png)  
**Companion:** [PLAN](./smart-baskets-discover-plan.md) · [TODO.md](./TODO.md)  
**Code:** `am-modern-ui/am_portfolio_ui/.../basket_explorer.dart`, `discover_view_state.dart`

**Rule:** App shell (sidebar, global ⌘K, New Trade, mobile bottom nav) is **out of scope**. Discover **content** only. Use `am_design_system` / `ModuleColors.portfolio`. Hide bookmark + row ⋮ in v1 (mock shows them; no API).

---

## Status (2026-09-06)

| Layer | Status |
|-------|--------|
| Parser `/v2/funds` + performance | PROD live (`funds-v2-mdurl-20260906`) |
| Portfolio opportunities enrich | PROD live (`basket-discover-cachefix-20260906`) |
| Postman PROD Newman | 11 req / 47 asserts / 0 fail |
| Flutter Discover UI | **Density + split shipped** — await manual DoD vs reference ([refinement plan](./discover-ui-refinement-plan.md)) |

---

## Desktop layout (web mock)

```text
SMART BASKETS CONTENT
  Title: Smart Baskets
  Subtitle: Discover pre-built ETF portfolios and invest in market themes with one click.
  Toggle: [Discover] [My Baskets]
  Search: Search ETFs, baskets or themes...
  Theme chips: Top picks | Nifty 50 | Bank | IT | … (from GET /catalog)
  Period: [1Y] [3Y] [5Y] [All]
  Sort: Sort by Recommended ▾
  Clear all

  TOP PICKS
    "Top picks — Top performers for this segment"          [View all →]
    3 cards = top 3 by period return in current segment (theme/query)
    CTA: Create basket →

  ALL BASKETS
    "All baskets (N) — Explore the complete list of ETF baskets"
    DataTable:
      Basket/ETF | Category | Constituents | Match | {period} return | Required | Create basket →
```

### Top-picks card fields

| UI | Data |
|----|------|
| Category chip | `categoryLabel` |
| Initials avatar | No logo API — DS placeholder |
| Name / ticker | `etfName` / `etfSymbol` |
| Sparkline | `sparklineCloses` (hide if null/empty; ≤24) |
| Match ring + held/missing | `matchScore`, `heldCount`, `missingCount` |
| Period return | `return1Y\|3Y\|5Y` by period; `—` if null; never fake `0%` |
| Constituents | `totalItems` |
| Required | `minimumInvestmentAmount` → `₹X.XL` |
| CTA | `Create basket →` → `openPreview` + telemetry |

### Table columns

Same fields; small match ring; **period column header** syncs with period control (`1Y return` / `3Y` / `5Y` / All→prefer 5Y); Preview button; **no ⋮**.

---

## Mobile layout (mobile mock)

```text
  Title + subtitle
  Sticky [Discover] [My Baskets]
  Search
  Theme chips (horizontal scroll)
  Performance: 1Y | 3Y | 5Y | All
  Sort by Recommended
  Meta: "{N} baskets · As of {returnsAsOf}"
  Vertical dense cards for full displayList — NO DataTable
```

Card: category · name · ticker · sparkline (if space) · match + held/missing · period return · constituents · required · Preview →. Bookmark hidden.

Breakpoint: `maxWidth < AmBreakpoints.mobile` (600).

---

## Functionality locks

| Control | Behavior |
|---------|----------|
| Top picks chip | `defaultQuery`; refetch |
| Theme chip | Theme `query`; refetch |
| Period | Client-only; no refetch |
| Sort | Client-only: Recommended (= matchDesc), Return desc, Required asc |
| Clear all | Reset theme→default, period→1Y, sort→Recommended, clear search |
| View all | Scroll to All baskets (desktop) |
| Search | Single ETF → preview; multi → query (existing) |
| My Baskets | Existing toggle |
| Fail-open | Missing perf still shows match + required + Preview |

---

## Gap vs current code

| Mock element | Now | Need |
|--------------|-----|------|
| Subtitle | Missing | Add |
| Labeled Sort by Recommended | Icon popup only | Labeled control |
| Clear all | Missing | Add |
| Top picks subtitle + View all | Title only | Add + scroll |
| Sparkline | Missing | Wire `sparklineCloses` |
| Strong +X% return | Muted inline | Emphasize + color |
| ₹L formatting | Uses K under 1L | Prefer Lakh style for Discover |
| Category chip | Plain / long text | Chip from `categoryLabel` |
| All baskets DataTable | **Second card grid** | Replace with table |
| Period column sync | N/A | With table |
| Mobile dense + As of | Partial | Verify vs mock |
| Bookmark / ⋮ | Mock only | Keep hidden |

Primary file: `am_portfolio_ui/lib/features/basket/presentation/widgets/basket_explorer.dart`.

---

## Implementation order

1. Filter band: subtitle, labeled sort, Clear all, View all scroll key  
2. Card: sparkline, return emphasis, category chip, Preview →, ₹L  
3. Desktop DataTable for All baskets  
4. Mobile dense list + As of meta  
5. Flutter run vs PROD; accept against both PNGs  

---

## TODO (executable — also mirrored in TODO.md Module 3)

### Docs

- [x] This file + mocks linked
- [x] PLAN Phase 5–8 / Remaining UI updated
- [x] TODO.md Module 3 rewritten
- [x] README index

### Desktop Phase 6

- [x] Subtitle under Smart Baskets
- [x] Labeled Sort by Recommended (+ Match / Return / Required)
- [x] Clear all
- [x] Top picks header copy + View all → scroll
- [x] Card: category chip, sparkline, strong return, ₹L, Preview →
- [x] All baskets DataTable (7 columns; period header sync)
- [x] No bookmark / no ⋮

### Mobile Phase 7

- [x] Sticky Discover / My Baskets
- [x] Performance row + Sort
- [x] Meta `{N} baskets · As of …`
- [x] Dense vertical cards only (no DataTable)
- [x] Same fields as desktop (sparkline if space)

### Verify

- [ ] Local Flutter against PROD ← **ready for you now**
- [ ] Desktop checklist vs `basket-design-web.png`
- [ ] Mobile checklist vs `basket-mobile-screen.png`
- [ ] Mark Module 3 done in TODO.md
