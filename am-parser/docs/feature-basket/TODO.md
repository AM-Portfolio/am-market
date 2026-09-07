# Feature-basket — TODO only

Companion: [PLAN](./smart-baskets-discover-plan.md) · [discover-ui-layout-and-todo.md](./discover-ui-layout-and-todo.md) · [architecture.drawio](./architecture.drawio) · [fixtures/](./fixtures/) · mocks: [web](./basket-design-web.png) · [mobile](./basket-mobile-screen.png)

**Legend:** `[ ]` pending · `[~]` in progress · `[x]` done  
**Rule:** Update after every phase / one-click test. No strategy essays here.  
**UI remaining SoT:** [discover-ui-refinement-plan.md](./discover-ui-refinement-plan.md) · layout notes: [discover-ui-layout-and-todo.md](./discover-ui-layout-and-todo.md)

**Credentials:** `POSTMAN_API_KEY` in `~/.asrax/credentials.env` — never commit.

---

## Workspace prep

- [x] Separate PLAN vs TODO in `docs/feature-basket`
- [x] PLAN scorecard **10/10** (spec)
- [x] `POSTMAN_API_KEY` verified (Postman `/me` OK)
- [ ] Confirm Postman MCP tools visible in Cursor after MCP reload (user)
- [x] Postman PROD collection (asrax workspace):
  - [x] `AM Smart Baskets Discover — PROD` UID: `51581661-16287613-bf51-4313-b99f-0d50ebd717cf`
  - [x] Environment `AM PROD` UID: `51581661-7e2a3f8b-5e3c-4644-b57d-bc0e776af19d`
  - [x] Newman PROD: **11 requests / 47 asserts / 0 failed** (2026-09-06)
- [x] Layout/TODO doc: `discover-ui-layout-and-todo.md`
- [~] Fixtures (partial live samples exist under `fixtures/`)

---

## Module 1 — am-parser

- [x] Branch `hotfix/funds-v2-performance`
- [x] `/v2/funds` + performance SoT + MF stub; v1 frozen
- [x] Unit tests (math + openapi) **6 passed**
- [x] **PROD image:** `ghcr.io/am-portfolio/am-parser:funds-v2-mdurl-20260906` (kubectl roll; prefer Vault `MARKET_DATA_API_URL`)
- [x] Preprod/dev tags previously: `funds-v2-20260906` / mdurl; preprod MARKET_DATA overlay in gitops
- [~] Dev pods: Mongo / infra flaky — not Discover blocker
- [x] Postman/Newman PROD accuracy (v1 freeze, v2 holdings+returns, MF, search, bulk, perf)

---

## Module 2 — am-portfolio

- [x] Branch `hotfix/basket-discover-enrich`
- [x] `EtfApiClient` → `/v2/funds/...`; `etfSymbol` + `categoryLabel` + returns/sparkline on opportunities
- [x] Cache round-trip preserves Discover fields (`CachedEtfData` + `EnrichedEtfService`)
- [x] Unit tests (basket + EnrichedEtfService discover fields)
- [x] **PROD image:** `ghcr.io/am-portfolio/am-portfolio:basket-discover-cachefix-20260906`
- [x] Postman/Newman PROD: catalog + opportunities with returns/sparkline
- [~] Preprod cluster etcd/startup-probe flaky — prefer PROD for Discover UI verify
- [x] Mark module 2 **API** done (UI still open)

---

## Module 3 — am-modern-ui (REMAINING FOCUS)

### Done (baseline)

- [x] Branch `hotfix/basket-discover-ui`
- [x] Models + `DiscoverViewState` (period/sort client-only)
- [x] Theme chips (catalog) + period chips + sort popup
- [x] Top picks ×3 **cards**
- [x] Preserve search → preview / My Baskets
- [x] All baskets **DataTable** on desktop (cards = Top picks only)

### Desktop Phase 6 — vs `basket-design-web.png`

- [x] Subtitle under Smart Baskets
- [x] Labeled **Sort by Recommended** (+ Match / Return / Required)
- [x] **Clear all**
- [x] Top picks header (“Top performers…”) + **View all →** scroll to All baskets
- [x] Card: category chip, **sparkline**, strong period return, ₹L, **Create basket →**
- [x] Top picks = top performers for selected period in segment
- [x] **All baskets DataTable** (Basket/ETF, Category, Constituents, Match, {period} return, Required, Create basket)
- [x] Period column header syncs with period control
- [x] No bookmark / no ⋮

### Mobile Phase 7 — vs `basket-mobile-screen.png`

- [x] Sticky Discover / My Baskets (inline toggle when shown)
- [x] Performance row + Sort
- [x] Meta `{N} baskets · As of …`
- [x] Dense vertical cards only (**no** DataTable)
- [x] Same fields as desktop (sparkline if space)

### Density + split (execute refinement plan)

- [x] Split `basket_explorer` → `widgets/discover/*` (≤600 lines/file)
- [x] Compact filter band + More overflow + Clear all clears search
- [x] Cards ~196px + shared match ring + CAGR labels
- [x] Full-width dense All baskets table
- [x] Unit tests: `DiscoverViewState` labels/sort + layout constants

### One-click / design verify

- [ ] Local Flutter run against **PROD** ← **ready for you**
- [ ] DoD checklist in [discover-ui-refinement-plan.md](./discover-ui-refinement-plan.md)
- [ ] Desktop checklist vs web mock / reference screenshot
- [ ] Mobile checklist vs mobile mock
- [ ] Mark module 3 done

---

## Rollback

- [ ] Prior good image tags restored if needed
- [ ] Newman / Collection re-green after rollback

---

## Later

- [ ] PRs when user asks
- [ ] Freeze PROD tags in gitops (Argo may heal kubectl rolls)
- [ ] MF data fill (Phase 10) — not Discover v1 UI blocker
