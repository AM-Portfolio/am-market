# AM News Phase 1 context

Captured 2026-09-06. No tokens, JWT values, or secret payloads. Path names and key names only.

This file is the MCP gate. FastAPI, Flutter widgets, Helm, ingress, and gitops apps wait until the hub is reviewed in chat.

## Checklist

- [x] four feature branches exist off origin/main
- [x] live GET /v2/news status + article count recorded
- [x] fixture saved (redacted) with real field names
- [x] same article under two keys: unknown (not observed on this page)
- [x] UpstoxAPI Postman News folder + example (fallback collection; original UpstoxAPI is forbidden)
- [x] GrowthBook news-ui-enabled created; env ON/OFF recorded (N/A: MCP cannot create the flag)
- [x] Vault mongo/redis/oidc path names recorded (no values)
- [x] Redis is the market-data instance (yes, same Helm path)
- [x] token source: Redis key `market_data:upstox:access_token` (yes as design; cluster key presence not probed)

## 1. GitHub

Remotes are `AM-Portfolio/am-market`, `AM-Portfolio/am-modern-ui`, `AM-Portfolio/am-infra`, `AM-Portfolio/am-gitops`. Default branch is `main` on each.

| Repo | Feature branch | origin/main SHA used to create it |
| --- | --- | --- |
| AM-Portfolio/am-market | `feature/am-news` | `d40846667ff56de6e59eb8e6416552bf64c9fbbb` |
| AM-Portfolio/am-modern-ui | `feature/dashboard-news` | `85f0254534f0ff0dcff70fb160325478c6956fa8` |
| AM-Portfolio/am-infra | `feature/am-news-ingress` | `7be1342183bcac1a7cbf23c021aee5cc87af6c35` |
| AM-Portfolio/am-gitops | `feature/am-news-app` | `f68125dfd7863d532bbeed7e3aec43187f966c5b` |

At capture time each feature branch still pointed at that same SHA (docs-only commit comes after this file).

## 2. Upstox MCP + live news

Launcher: `C:\Users\ssd26\.asrax\bin\upstox-mcp.cmd` -> `https://mcp.upstox.com/mcp`. Cursor namespace `user-upstox` is ready. REST `UPSTOX_ACCESS_TOKEN` is not MCP OAuth.

MCP tools are trading and profile only: funds, holdings, IPO, orders, positions, profile, trades. There is no Get News tool. Ingest must use REST `GET https://api.upstox.com/v2/news`.

MCP OAuth works (`get-profile` returned `status: success`). Do not log profile fields.

Official API (https://upstox.com/developer/api-documentation/get-news):

- Bearer JWT. 7-day window. Max 30 `instrument_keys` per call
- Categories: `instrument_keys` (AM ingest), `positions`, `holdings` (broker book debug only)
- `data` is an object keyed by instrument key, each value an array of articles
- Article fields: `heading`, `summary`, `thumbnail`, `article_link`, `published_time` (unix ms)
- Pagination: `metadata.page` (`page_number`, `page_size`, `total_records`, `total_pages`)
- Asrax speaks `RELIANCE`. Upstox speaks `NSE_EQ|INE…`. Never invent `NSE_EQ|{symbol}`

Live REST (2026-09-06):

- Python default User-Agent: HTTP 403 Cloudflare 1010 (`browser_signature_banned`) on `api.upstox.com`. Do not retry that UA. The adapter must send a normal browser-like User-Agent
- Chrome User-Agent: HTTP 200
- Query: `category=instrument_keys`, keys `NSE_EQ|INE002A01018` (RELIANCE) and `NSE_EQ|INE040H01021` (HDFC Bank), `page_size=100`
- Envelope keys: `status`, `data`, `metadata`
- `data` contained only `NSE_EQ|INE002A01018`. Five articles. HDFC Bank key omitted (empty key not present)
- Article keys match the docs: `heading`, `summary`, `thumbnail`, `article_link`, `published_time`
- Same article under two keys: not observed (only one key in `data`). Still treat it as possible because the vendor shape is per-key arrays. Unique `(provider, article_uid)` plus child symbols stays required
- Fixture: `am-news/tests/fixtures/upstox_news_page.json` (no Authorization header)

## 3. Postman

Instructions resource was fetched first.

Collection **UpstoxAPI** uid `29914436-c8d4cd26-d9ac-4b06-8d6d-a9250fc6f8f5`: GET collection works, create request is 403 (`You are not authorized to access this instance`). Cannot add a News folder there.

Fallback in workspace Asrax (`648a186b-f56c-4a95-b8ff-9a235cbde152`):

- Collection name: `Upstox News (AM ingest)`
- uid: `3526384-8e236c18-bb32-4cf4-bff8-61fe50ab5a4b`
- Folder `News`: instrument_keys, holdings debug, positions debug
- Variable `upstoxAccessToken` (empty in Postman). Not MCP OAuth
- Saved example on instrument_keys: `200 instrument_keys RELIANCE sample (redacted live)`

Product collection **AM News** is later, from FastAPI `/openapi.json`.

## 4. GrowthBook

Environments: `production`, `dev`, `spt`, `preprod`.

Project used: `prj_2Cd9MsEoQ7P8dvVVD4AmvC` (My First Project).

Existing flag keys: `test`, `redis-enabled`, `redis-enabled-by-service`, `subscription-page-enabled`. `news-ui-enabled` is not among them.

`create_feature_flag` and `get_defaults` both fail: `No data source or assignment query found. Experiments require a data source/assignment query.` The MCP create path will not work until GrowthBook has a datasource. `create_force_rule` also has no environment field, so env ON/OFF still needs the GrowthBook UI even after a successful create.

Intended states (not applied):

- `dev` and `preprod`: force ON
- `production` and `spt`: force OFF
- default value `false` so production does not fail open

Create `news-ui-enabled` (boolean, Dart SDK) in the GrowthBook UI after docs approval. Do not flip production ON until Phase 5.

## 5. Vault

KV mount `apps/`. Helm paths include the KV v2 `data/` prefix. MCP `list_secrets` used mount `apps` and path without `data/`.

Leaf secrets list as empty folders; key names come from sibling Helm mappings (values not read).

| Use | Helm path (as in values.prod.yaml) | Mapping keys |
| --- | --- | --- |
| Mongo (parser + market-data) | `apps/data/prod/infra/mongodb` | `url` |
| Redis (market-data) | `apps/data/prod/infra/redis` | `host`, `password`, `port` |
| identity-oidc | `apps/data/prod/services/am-identity` | `OIDC_JWKS_URL`, `OIDC_ISSUER` |
| Upstox API creds (market-data) | `apps/data/prod/services/am-market-data` | `UPSTOX_API_KEY`, `UPSTOX_SECRET_KEY`, `UPSTOX_REDIRECT_URI`, `UPSTOCK_ACCESS_TOKEN` |

am-news Helm must alias the same mongo, redis, and identity-oidc paths. Do not invent a second Redis. The hot ingest token is Redis `market_data:upstox:access_token` (written by am-market-data). Vault `UPSTOCK_ACCESS_TOKEN` is the market-data mapping name (note the spelling) and is not the news read path. Cluster GET of that Redis key was not run in Phase 1.

## 6. Keycloak

Prod MCP realm `am-realm`: roles include `admin`, `user`, `viewer`, `service`. **`super_admin` is not on this realm.**

Preprod realm `am-preprod-realm`: `admin` and `super_admin` both exist.

Kind-nonprod `list_realm_roles` timed out. N/A.

Admin JWT for feed start should accept `admin` and `super_admin`. Prod today only has `admin` unless that role is added later. Do not create users in Phase 1.

## Implications for later code

- User APIs still never call Upstox. Empty store is 200 empty
- Adapter User-Agent cannot be Python-urllib default
- Parser must walk `data` as `dict[instrument_key, list[article]]`, tolerate missing keys, unique articles by vendor id or SHA-256 of `article_link`
- NIFTY 50 resolve still goes through am-market-data on ingest only
- GrowthBook flag is a UI follow-up, not a blocker for writing PLAN/TODO
