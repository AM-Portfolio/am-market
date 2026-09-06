# AM News docs hub

Canonical git-tracked docs for the AM News product. Service will live in this monorepo as `am-market/am-news/` after review. Do not start FastAPI, Flutter widgets, Helm, or gitops apps until CONTEXT + PLAN + TODO + Draw.io are approved in chat.

## Files

- [CONTEXT.md](./CONTEXT.md) - Phase 1 MCP gate (branches, live Upstox, Postman, GrowthBook, Vault path names, Keycloak)
- [PLAN.md](./PLAN.md) - living design
- [TODO.md](./TODO.md) - all-repo checklist
- [architecture.drawio](./architecture.drawio) - in-repo layers (open in diagrams.net or the VS Code Draw.io extension)
- [cross-repo.drawio](./cross-repo.drawio) - who owns what

Mermaid copies of both diagrams live in PLAN.md so GitHub review works without the plugin.

Redacted vendor fixture: [tests/fixtures/upstox_news_page.json](../../tests/fixtures/upstox_news_page.json)

## Pointers in other repos

- am-modern-ui `docs/am-news/README.md` on `feature/dashboard-news`
- am-infra `docs/am-news/README.md` on `feature/am-news-ingress`
- am-gitops `docs/am-news/README.md` on `feature/am-news-app`

Those READMEs link here. They do not duplicate PLAN.

## Public URL shape (locked)

Traefik prefix `/news` (product resource, not the Helm name `am-news`). After strip, FastAPI is `/v1/...`. Example: `https://{host}/news/v1/current-affairs`. Never `/am-news/...`.
