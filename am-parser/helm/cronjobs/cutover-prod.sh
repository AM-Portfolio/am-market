#!/usr/bin/env bash
# Prod MF catalog writer cutover (run with prod kubeconfig).
# Requires: kubectl context for am-apps-prod, secrets present, dual-writer off.
set -euo pipefail
NS=am-apps-prod
ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "== secrets (keys only) =="
kubectl -n "$NS" get secret am-mutual-fund-synced-secrets -o jsonpath='{.data}' | tr ',' '\n' | head
kubectl -n "$NS" get secret am-parser-synced-secrets -o jsonpath='{.data}' | tr ',' '\n' | head
kubectl -n "$NS" get secret am-mutual-fund-synced-secrets -o jsonpath='{.data.NSE_API_KEY}' | grep -q . || {
  echo "FAIL: NSE_API_KEY missing on am-mutual-fund-synced-secrets"; exit 1; }
kubectl -n "$NS" get secret am-parser-synced-secrets -o jsonpath='{.data.AM_MARKET_MF_JDBC_PASSWORD}' | grep -q . || {
  echo "FAIL: AM_MARKET_MF_JDBC_PASSWORD missing on am-parser-synced-secrets"; exit 1; }

echo "== apply CronJobs (start suspended in YAML; we unsuspend next) =="
kubectl apply -f "$ROOT/mf-catalog-nav-prod.yaml"

echo "== suspend legacy MF writers if present =="
kubectl -n "$NS" patch cronjob am-mf-catalog-nav-daily --type merge -p '{"spec":{"suspend":true}}' 2>/dev/null || true
kubectl -n "$NS" patch cronjob am-mf-catalog-nav-morning --type merge -p '{"spec":{"suspend":true}}' 2>/dev/null || true

echo "== unsuspend parser writers =="
kubectl -n "$NS" patch cronjob am-parser-mf-catalog-nav-daily --type merge -p '{"spec":{"suspend":false}}'
kubectl -n "$NS" patch cronjob am-parser-mf-catalog-nav-morning --type merge -p '{"spec":{"suspend":false}}'

echo "== manual Job =="
kubectl -n "$NS" delete job am-parser-mf-catalog-nav-manual --ignore-not-found
kubectl -n "$NS" create job am-parser-mf-catalog-nav-manual --from=cronjob/am-parser-mf-catalog-nav-daily
kubectl -n "$NS" wait --for=condition=complete job/am-parser-mf-catalog-nav-manual --timeout=7200s
kubectl -n "$NS" logs job/am-parser-mf-catalog-nav-manual --tail=40

echo "== smoke parser (ingress) =="
curl -fsS "https://am.asrax.in/parser/v1/mutual-fund/search?q=HDFC&limit=1" | head -c 500
echo
echo "OK cutover script finished — confirm source=nse-master-download + nav in JSON above"
