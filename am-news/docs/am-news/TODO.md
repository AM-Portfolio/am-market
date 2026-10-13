# AM News TODO

Check off in git as work lands. Canonical hub: this directory.

## am-market (`feature/am-news`)

- [x] Phase 0 checkout off origin/main
- [x] Phase 1 MCP until CONTEXT.md is complete (N/A rows recorded)
- [x] Phase 2 docs hub (CONTEXT, PLAN, TODO, Draw.io, README, fixture)
- [x] User approved scaffold. FastAPI service in `am-news/`
- [x] Resource routes `/v1/current-affairs|insight|holdings` (no service name). Thin routers + application use cases + schemas
- [x] Enriched OpenAPI (operationId, named DTOs, examples). Export Postman from `/openapi.json`. `test_openapi_schema.py`
- [x] `UpstoxNewsAdapter` + CountingFetch fixture JSON (browser-like User-Agent; parse `data` as keyed object)
- [x] NIFTY 50 resolver cache from am-market-data (ingest only)
- [x] `news_raw_batches` persist body; process; `news_articles` + `news_article_symbols`
- [x] Replay scheduler (body only) + IST sync + feed lock 409
- [x] Insight/holdings use cases Redis then Mongo; JWT required; Cache-Control private
- [x] Admin use cases + `am_platform_security` roles (`admin` / `super_admin`)
- [x] Helm values + vault-mappings (same Redis as market-data); `market-news.yml` CI; direct-deploy including prod
- [ ] After `am test` green and merge to main: deploy am-apps-prod / am-vps-prod. Admin `startFeed` for all NIFTY 50. Verify status/raw/articles/Redis. User 401/200, user cannot start feed
- [ ] Then full UI integration.

## am-modern-ui (`feature/dashboard-news`)

- [x] Pointer README under `docs/am-news/`
- [x] `FeatureFlagKeys.newsUiEnabled` + `newsUiEnabledProvider` defaultValue true except production flavor false
- [x] GrowthBook `news-ui-enabled` force ON production/dev/preprod (spt off)
- [x] `DashboardWidgetId.news` + default layout + catalog builder
- [x] `EnvDomains.news` = `$apiBase/news` then client paths `/v1/insight` (not `/v1/news/insight`). `NewsApiConfig` + `NewsRepository` JWT, 800ms timeout
- [x] Holdings symbols from portfolio holdings (not movers); parallel kickoff; skeleton/empty copy
- [x] `flutter test` flag on/off, no admin URL, layout merge inserts news. Summary stays on its own provider

## am-infra (`feature/am-news-ingress`)

- [x] Pointer README under `docs/am-news/`
- [x] Add `/news` to `dev-middlewares.yaml`, preprod `ingress-routing.yaml`, and `prod-middlewares.yaml`
- [x] Ingress path `/news` is Helm-owned on the am-news chart (same pattern as am-parser). No extra central Ingress YAML

## am-gitops (`feature/am-news-app`)

- [x] Pointer README under `docs/am-news/`
- [x] `dev/apps/am-news.yaml`, `preprod/apps/am-news.yaml`, `prod/apps/am-news.yaml` (mirror parser, release `am-news` bare on prod, namespace `am-apps-prod`)
- [x] Matching `image-tags/am-news.yaml` in each env (placeholder tag until first publish)

## GrowthBook / Postman / local

- [x] Live GET `/v2/news` + redacted fixture
- [x] Enable Upstox MCP OAuth (no news tool; REST ingest)
- [x] Vendor Postman News folder (fallback collection; original UpstoxAPI is 403)
- [x] Create `news-ui-enabled`; force ON production/dev/preprod, OFF spt
- [x] Vault path names for mongo/redis/oidc. Same Redis as market-data
- [ ] Cluster confirm Redis key `market_data:upstox:access_token` on prod
- [ ] After service: `am test` -> deploy prod VPS `am-apps-prod` -> admin NIFTY 50 feed -> user 401/200 -> then dashboard integration
