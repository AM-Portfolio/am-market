#!/usr/bin/env bash
# Preprod MF catalog writer cutover (run with Contabo preprod kubeconfig).
set -euo pipefail
NS=am-apps-preprod
ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "== secrets =="
kubectl -n "$NS" get secret am-mutual-fund-preprod-synced-secrets am-parser-preprod-synced-secrets >/dev/null
kubectl -n "$NS" get secret am-mutual-fund-preprod-synced-secrets -o jsonpath='{.data.NSE_API_KEY}' | grep -q . || {
  echo "FAIL: NSE_API_KEY missing"; exit 1; }
kubectl -n "$NS" get secret am-parser-preprod-synced-secrets -o jsonpath='{.data.AM_MARKET_MF_JDBC_PASSWORD}' | grep -q . || {
  echo "FAIL: JDBC password missing"; exit 1; }

echo "== apply =="
kubectl apply -f "$ROOT/mf-catalog-nav-preprod.yaml"
kubectl -n "$NS" patch cronjob am-mf-catalog-nav-daily --type merge -p '{"spec":{"suspend":true}}' 2>/dev/null || true
kubectl -n "$NS" patch cronjob am-mf-catalog-nav-morning --type merge -p '{"spec":{"suspend":true}}' 2>/dev/null || true
kubectl -n "$NS" patch cronjob am-parser-mf-catalog-nav-daily --type merge -p '{"spec":{"suspend":false}}'
kubectl -n "$NS" patch cronjob am-parser-mf-catalog-nav-morning --type merge -p '{"spec":{"suspend":false}}'

echo "== manual Job =="
kubectl -n "$NS" delete job am-parser-mf-catalog-nav-manual --ignore-not-found
kubectl -n "$NS" create job am-parser-mf-catalog-nav-manual --from=cronjob/am-parser-mf-catalog-nav-daily
kubectl -n "$NS" wait --for=condition=complete job/am-parser-mf-catalog-nav-manual --timeout=7200s
kubectl -n "$NS" logs job/am-parser-mf-catalog-nav-manual --tail=40

echo "== smoke =="
curl -fsS "https://am-preprod.asrax.in/parser/v1/mutual-fund/search?q=HDFC&limit=1" | head -c 500
echo
