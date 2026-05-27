# am-parser Helm configuration

Flat `values*.yaml` files are merged by the universal chart at deploy time.

## Configuration matrix

| Variable | Local `.env` | Vault | Helm `env` | Notes |
|----------|--------------|-------|------------|-------|
| `ENVIRONMENT` | `local` | — | `dev` / `preprod` / `prod` | `local` enables `.env` file override for Mongo |
| `MONGO_URI` | yes | `mongodb.url` → `MONGO_URI` | — | Never commit real URIs |
| `TOGETHER_API_KEY` | yes | `together.TOGETHER_API_KEY` | — | LLM parsing |
| `MONGO_DB` | yes | optional | yes | Default `mutual_funds` |
| `MARKET_DATA_URL` | yes | — | yes | Holdings ISIN enrichment (`am-market-data`) |
| `LOG_LEVEL` | yes | — | yes | `DEBUG`, `INFO`, … |
| `ETF_LIST_CACHE_MINUTES` | yes | — | yes | In-memory ETF list cache for search/holdings |
| `ETF_HOLDINGS_CACHE_DAYS` | yes | — | yes | Smart holdings refresh window |
| `DEFAULT_PARSE_METHOD` | yes | — | optional | `together` or `manual` |

## Files

- `values.yaml` — base (port, image, security)
- `values.dev.yaml` / `values.preprod.yaml` / `values.prod.yaml` — environment overrides
- `vault-mappings.yaml` — secret key mapping for the deploy pipeline

## Local deploy

```bash
cd am-parser
./scripts/deploy-local.sh
```
