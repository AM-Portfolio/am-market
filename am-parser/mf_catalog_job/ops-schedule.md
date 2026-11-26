# Parser MF catalog ingest schedule

```bash
# From am-parser/ (env: NSE_* + AM_MARKET_MF_JDBC_*)
python mf_catalog_job/daily_job.py
python mf_catalog_job/daily_job.py --alert-if-nav-lag-days 1 --fail-on-stale
```

Flow: NSE `MASTER_DOWNLOAD` → SQLite → chunked Postgres push → `compute_scheme_returns.py`.

## Dig CronJob

Manifests: [`../helm/cronjobs/mf-catalog-nav-daily.yaml`](../helm/cronjobs/mf-catalog-nav-daily.yaml)

| Job | Schedule (UTC) | IST approx |
| --- | --- | --- |
| `am-parser-mf-catalog-nav-daily` | `0 14 * * 1-5` | 19:30 weekdays |
| `am-parser-mf-catalog-nav-morning` | `0 4 * * 1-5` | 09:30 weekdays |

- `concurrencyPolicy: Forbid`
- Image: `ghcr.io/am-portfolio/am-market/am-parser-mf-catalog-job`
- After cutover: **suspend** MF `am-mf-catalog-nav-*` (dual-writer)

See [../docs/mf-catalog-nav-ownership.md](../docs/mf-catalog-nav-ownership.md).
