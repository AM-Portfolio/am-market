# am-parser MF catalog job (writer)

NSE `MASTER_DOWNLOAD` (SCH/NAV/SIP/STP/SWP/SET) → SQLite → Postgres `mf_catalog`.

**Ownership / pros-cons / cutover:** [../docs/mf-catalog-nav-ownership.md](../docs/mf-catalog-nav-ownership.md)

This replaces `am-mutual-fund/catalog` as the **writer**. The long-running parser API only **reads** JDBC and serves `/v1/mutual-fund/*`.

## Local

```bash
# From am-parser/
pip install -r mf_catalog_job/requirements-job.txt
# Needs NSE_* + AM_MARKET_MF_JDBC_*
python mf_catalog_job/daily_job.py --fail-on-stale
python mf_catalog_job/daily_job.py --skip-download --skip-push   # ingest-only smoke
```

```bash
pytest mf_catalog_job/tests -q
```

## Image

```bash
docker build -f mf_catalog_job/Dockerfile -t am-parser-mf-catalog-job .
# Published as ghcr.io/am-portfolio/am-market/am-parser-mf-catalog-job
```

## CronJobs

See [`../helm/cronjobs/mf-catalog-nav-daily.yaml`](../helm/cronjobs/mf-catalog-nav-daily.yaml).
