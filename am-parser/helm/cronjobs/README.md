# MF catalog NAV CronJobs (am-parser writer)

Same image + `mf_catalog_job/daily_job.py` for **dev / preprod / prod**.  
Only namespace, JDBC URL, and synced-secret names change. **Never cross-env** (preprod must not write `user_platform_dev`).

| File | Namespace | DB | NSE secret | JDBC secret | Default suspend |
| --- | --- | --- | --- | --- | --- |
| [`mf-catalog-nav-daily.yaml`](mf-catalog-nav-daily.yaml) (dev/dig) | `am-apps-dev` | `user_platform_dev` | `am-mutual-fund-dev-synced-secrets` | `am-parser-dev-synced-secrets` | `false` (live) |
| [`mf-catalog-nav-preprod.yaml`](mf-catalog-nav-preprod.yaml) | `am-apps-preprod` | `user_platform_preprod` | `am-mutual-fund-preprod-synced-secrets` | `am-parser-preprod-synced-secrets` | `true` until cutover |
| [`mf-catalog-nav-prod.yaml`](mf-catalog-nav-prod.yaml) | `am-apps-prod` | `user_platform` | `am-mutual-fund-synced-secrets` | `am-parser-synced-secrets` | `true` until cutover |

Image pin is identical across files — repin all three after GHCR publish of `am-parser-mf-catalog-job`.

## Cutover per env

```bash
# 0) Confirm secrets exist (keys NSE_* + AM_MARKET_MF_JDBC_*)
kubectl -n <ns> get secret <mf-synced> <parser-synced> -o jsonpath="{.data}" | head

# 1) Apply
kubectl apply -f helm/cronjobs/mf-catalog-nav-<env>.yaml

# 2) Suspend any legacy MF writer in that namespace
kubectl -n <ns> patch cronjob am-mf-catalog-nav-daily --type merge -p '{"spec":{"suspend":true}}' || true
kubectl -n <ns> patch cronjob am-mf-catalog-nav-morning --type merge -p '{"spec":{"suspend":true}}' || true

# 3) Unsuspend parser jobs (preprod/prod start suspended)
kubectl -n <ns> patch cronjob am-parser-mf-catalog-nav-daily --type merge -p '{"spec":{"suspend":false}}'
kubectl -n <ns> patch cronjob am-parser-mf-catalog-nav-morning --type merge -p '{"spec":{"suspend":false}}'

# 4) Manual once
kubectl -n <ns> create job am-parser-mf-catalog-nav-manual --from=cronjob/am-parser-mf-catalog-nav-daily
```

Read path is already env-agnostic: parser Helm `AM_MARKET_MF_JDBC_URL` in `values.{dev,preprod,prod}.yaml` + BFF `MF_CATALOG_API_URL`.

Canonical doc: [`../../docs/mf-catalog-nav-ownership.md`](../../docs/mf-catalog-nav-ownership.md).
