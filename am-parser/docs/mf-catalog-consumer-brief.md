# MF catalog / EOD NAV — brief for portfolio & trade-management

**Date:** 2026-11-27  
**Owner of dump:** **am-parser** `mf_catalog_job` (not am-mutual-fund)

## What you get

NSE `MASTER_DOWNLOAD` → CronJob → Postgres schema `mf_catalog` → HTTP:

| Env | Parser base | Example |
| --- | --- | --- |
| dig/dev | `http://am-parser-dev:8080` | `GET /v1/mutual-fund/search?q=HDFC&limit=5` |
| preprod | `http://am-parser-preprod:8080` | same path |
| prod | `http://am-parser:8080` | same path |

Ingress: `https://am.asrax.in/parser/v1/mutual-fund/...` (prod), `https://am-preprod.asrax.in/parser/...`, dig `https://am-dev.asrax.in/parser/...`.

Contract: `source=nse-master-download`, `priceType=EOD_NAV_NOT_LIVE_LTP`, fields `nav` / `navDate` (EOD, not live LTP).

Optional BFF (auth + seed-fallback): am-mutual-fund `GET /v1/mf/schemes*` with `MF_CATALOG_API_URL` → parser.

## What you do **not** need

- Do **not** wait for an MF image push to “get NAV into Postgres.” MF only proxies.
- Do **not** call market-data as the primary Explore catalog path for this contract.
- **am-portfolio** today values MF from **stored holding NAV** on portfolio docs — this dump does not auto-refresh those Mongo fields.
- **am-trade-management** today uses **equity** `AM_MARKET_DATA_API_URL` only — no `MF_CATALOG_*` client in-tree.

If your feature needs live scheme search / EOD NAV: wire to **parser** `/v1/mutual-fund/*` (or MF BFF). After prod CronJob Completes + parser API is up, that HTTP is the source of truth.

## Canonical docs

- `am-parser/docs/mf-catalog-nav-ownership.md`
- `am-parser/helm/cronjobs/README.md`
