# MF catalog / EOD NAV ownership — am-parser writes + redistributes

**Audience:** market + MF engineers, seniors reviewing the write-path move  
**Status:** target ownership (option A) — MASTER_DOWNLOAD + Postgres dump owned by **am-parser** CronJob; HTTP redistribute stays on parser; MF BFF still proxies.

---

## 1. One-sentence answer

**NSE `MASTER_DOWNLOAD` → `am-parser/mf_catalog_job` CronJob dumps into Postgres `mf_catalog` → `am-parser` HTTP `/v1/mutual-fund/*` redistributes → MF BFF / Explore.**  
Parser API pods do **not** call NSE on request; a **batch job** owns download + DB write.

---

## 2. Current → target

### Before (MF writer)

```text
NSE MASTER_DOWNLOAD
  → am-mutual-fund/catalog CronJob   (WRITE)
  → Postgres mf_catalog
  → am-parser                         (READ HTTP)
  → am-mutual-fund BFF
  → Flutter Explore
```

### After (parser writer — this change)

```text
NSE MASTER_DOWNLOAD
  → am-parser/mf_catalog_job CronJob  (WRITE)
  → Postgres mf_catalog
  → am-parser                         (READ HTTP / redistribute)
  → am-mutual-fund BFF
  → Flutter Explore
```

| Concern | Before | After |
| --- | --- | --- |
| Call NSE `MASTER_DOWNLOAD` | MF `catalog/download_masters.py` | `am-parser/mf_catalog_job/download_masters.py` |
| Upsert Postgres | MF CronJob image `am-mf-catalog-job` | Parser job image `am-parser-mf-catalog-job` |
| Serve Explore search/NAV | am-parser read APIs | **unchanged** |
| BFF proxy | `MF_CATALOG_API_URL` → parser | **unchanged** |
| am-market-data write | never | still **never** |

---

## 3. Why a CronJob (not inside the API process)

Putting `MASTER_DOWNLOAD` inside the long-running parser pod would:

- Couple NSE latency / UAT flakes to Explore latency
- Expand NSE credential blast radius on every API replica
- Spike CPU/memory during SCH+NAV ingest (~thousands of schemes)

Same pattern as the old MF job: **batch ingest → DB → cheap JDBC reads**.

---

## 4. Who owns what

| Piece | Owner | Notes |
| --- | --- | --- |
| NSE broker secrets (`NSE_*`) | Dig: K8s Secret `am-mutual-fund-dev-synced-secrets` (CSI) | Dig has **no** Vault Agent Injector — do not rely on vault.hashicorp.com annotations |
| JDBC user/password | Dig: `am-parser-dev-synced-secrets` (`AM_MARKET_MF_JDBC_*`) | Same keys the parser API already mounts |
| JDBC URL (non-secret) | Helm / CronJob env | `...?currentSchema=mf_catalog` |
| Schema / tables | Shared Postgres `mf_catalog` | `mf_scheme`, `mf_nav_eod`, `mf_scheme_returns`, … |
| Daily / morning schedule | am-parser helm CronJobs | Weekday `14:00` + `04:00` UTC |
| Read APIs | `am_mf_catalog` + `/v1/mutual-fund/*` | Labels `source: nse-master-download` |
| BFF / orders / UCC | am-mutual-fund | Does **not** download masters for Explore |

**Masters downloaded:** `SCH` \| `NAV` \| `SIP` \| `STP` \| `SWP` \| `SET` (full desk set — not NAV-only). Trading eligibility still depends on these rows in Postgres even though Explore mainly shows NAV.

---

## 5. Pros

1. **Single market plane for “fill DB + serve catalog”** — aligns with senior ask that market dumps DB alone.
2. **Matches how Explore already works** — BFF already points at parser; fewer “is market-data or parser the reader?” debates.
3. **Write + read ownership colocated** — lag alerts, schema denorm (`prev_nav`, returns), and API contract live in one repo (`am-market` / `am-parser`).
4. **Clear dual-team boundary** — MF owns broker order plane; market owns scheme universe + EOD NAV store + redistribute APIs.
5. **No change to Flutter contract** — still EOD, still `priceType: EOD_NAV_NOT_LIVE_LTP`.

---

## 6. Cons / risks

1. **NSE credentials leave pure-MF Vault ownership** — desk keys must be injectable by the parser job (shared path or duplicated secret). Ops must not leave plaintext in git.
2. **MF trading still needs master semantics** — if the job only did NAV, SIP/STP eligibility would rot. Job **must** keep full `FILE_TYPES`.
3. **Dual-writer danger during cutover** — old `am-mf-catalog-nav-*` and new `am-parser-mf-catalog-nav-*` must not both run. Suspend MF CronJobs after the new job is green.
4. **Parser gains batch/ops surface** — image publish, CronJob, lag alerts, Vault annotations — not just FastAPI.
5. **Cross-team secret coordination** — until NSE is under a parser Vault path, dig job may temporarily inject MF’s Vault path (document + plan to duplicate).
6. **Stale docs risk** — older MF notes (e.g. handoff “MF CronJob writes”) mislead until updated; treat this file as source of truth for write ownership.

---

## 7. Env matrix

| Env | Job JDBC URL | Vault postgres | NSE Vault (cutover) | Parser API | BFF `MF_CATALOG_API_URL` |
| --- | --- | --- | --- | --- | --- |
| dig/dev | `jdbc:postgresql://postgres.asrax.in:5432/user_platform_dev?currentSchema=mf_catalog` | `apps/dev/infra/postgres` (job) / `apps/data/dev/infra/postgres` (API CSI — follow helm) | Temporarily `apps/dev/services/am-mutual-fund` | `am-parser-dev:8080` | `http://am-parser-dev:8080` |
| preprod | `.../user_platform_preprod?currentSchema=mf_catalog` | preprod infra postgres | Mirror MF or parser NSE path for that env | `am-parser-preprod:8080` | `http://am-parser-preprod:8080` |
| prod | `.../user_platform?currentSchema=mf_catalog` | prod infra postgres | Same | `am-parser:8080` | `http://am-parser:8080` |

**Never cross-env** (preprod job must not write dig DB).

Job env vars: `NSE_URL`, `NSE_LOGIN`, `NSE_API_KEY`, `NSE_API_SECRET`, `NSE_MEMBER`, `AM_MARKET_MF_JDBC_URL`, `AM_MARKET_MF_JDBC_USER`, `AM_MARKET_MF_JDBC_PASSWORD`.

---

## 8. Code map

| Role | Path |
| --- | --- |
| Download | [`mf_catalog_job/download_masters.py`](../mf_catalog_job/download_masters.py) |
| Ingest + push + returns | [`mf_catalog_job/`](../mf_catalog_job/) (`daily_job.py` orchestrates) |
| Job image | [`mf_catalog_job/Dockerfile`](../mf_catalog_job/Dockerfile) → `ghcr.io/am-portfolio/am-market/am-parser-mf-catalog-job` |
| CronJobs | [`helm/cronjobs/mf-catalog-nav-daily.yaml`](../helm/cronjobs/mf-catalog-nav-daily.yaml) |
| Read APIs | [`am_mf_catalog/service.py`](../am_mf_catalog/service.py), [`am_api/routers/mutual_fund_catalog.py`](../am_api/routers/mutual_fund_catalog.py) |
| Smoke | [`scripts/smoke_mf_catalog.py`](../scripts/smoke_mf_catalog.py) |
| Deprecated MF writer | `am-mutual-fund/catalog/` (shim README only after cutover) |

---

## 9. Cutover checklist (dig first)

**In-repo (done with this change):**

- Writer code + Dockerfile + GHCR workflow under `am-parser/mf_catalog_job`
- Parser CronJob manifests: `helm/cronjobs/mf-catalog-nav-daily.yaml`
- MF CronJob YAML marked `suspend: true`; `am-mutual-fund/catalog/README` deprecated
- Dig read path smoke (ingress): `/parser/v1/mutual-fund/search?q=HDFC` returns `source=nse-master-download` + nav

**Live cluster (ops — needs kube context + image publish):**

```bash
# 1) After GHCR has am-parser-mf-catalog-job:latest
kubectl -n am-apps-dev apply -f am-parser/helm/cronjobs/mf-catalog-nav-daily.yaml

# 2) Suspend legacy MF writers if still active in cluster
kubectl -n am-apps-dev patch cronjob am-mf-catalog-nav-daily --type merge -p '{"spec":{"suspend":true}}'
kubectl -n am-apps-dev patch cronjob am-mf-catalog-nav-morning --type merge -p '{"spec":{"suspend":true}}'

# 3) Kick one run
kubectl -n am-apps-dev create job am-parser-mf-catalog-nav-manual --from=cronjob/am-parser-mf-catalog-nav-daily
kubectl -n am-apps-dev logs -f job/am-parser-mf-catalog-nav-manual
```

4. SQL: `SELECT COUNT(*), MAX(nav_date) FROM mf_nav_eod;` — lag acceptable for EOD.
5. Smoke: `MF_CATALOG_API_URL=https://am-dev.asrax.in/parser python scripts/smoke_mf_catalog.py`
6. BFF: `GET /v1/mf/schemes?q=HDFC` → not `seed-fallback`; `nav` / `navDate` present.
7. Point colleagues at this doc; do not use note 24’s old “MF CronJob writes” wording as current truth.

---

## 10. Explicit non-goals

- No **am-market-data** Java write path for masters
- No live LTP / websocket for MF NAV
- No writing scheme/NAV into Mongo
- No changing Flutter / BFF URL shapes for this move
- No calling `MASTER_DOWNLOAD` from request threads inside the parser API

---

## 11. Verification (local, no cluster)

```bash
cd am-parser
pytest mf_catalog_job/tests tests/test_mf_catalog.py -q
MF_CATALOG_API_URL=https://am-dev.asrax.in/parser python scripts/smoke_mf_catalog.py
```

Known fix from move testing: `_push_core` must use vendored `V1__mf_catalog.sql` (old `ROOT.parents[1]/am-market/...` path broke under `am-parser/mf_catalog_job` and would fail empty-DB migrate). Dig with existing tables skips migrate but empty preprod/prod needs the vendored file in the image.

## 12. Slack one-liner

> EOD MF NAV: NSE `MASTER_DOWNLOAD` is ingested by **am-parser `mf_catalog_job` CronJob** into Postgres `mf_catalog`; **am-parser** serves `/v1/mutual-fund/*`; MF BFF proxies. Not live LTP. Do not run the old MF catalog CronJob in parallel.
