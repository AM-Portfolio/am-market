from __future__ import annotations

import asyncio
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.openapi.utils import get_openapi
from fastapi.responses import JSONResponse

from am_news import __version__
from am_news.api.routes.admin_feed import router as admin_feed_router
from am_news.api.routes.admin_replay import router as admin_replay_router
from am_news.api.routes.current_affairs import router as affairs_router
from am_news.api.routes.health import router as health_router
from am_news.api.routes.holdings import router as holdings_router
from am_news.api.routes.insight import router as insight_router
from am_news.application.replay_raw import ReplayRaw
from am_news.application.sync_scheduler import SyncScheduler
from am_news.container import build_container
from am_news.schemas.news import ErrorEnvelope
from am_news.settings import settings


@asynccontextmanager
async def lifespan(app: FastAPI):
    if getattr(app.state, "container", None) is None:
        app.state.container = build_container()
    mongo = getattr(app.state.container.articles, "ensure_indexes", None)
    if callable(mongo):
        await mongo()
    stop = asyncio.Event()

    async def _loops() -> None:
        scheduler = SyncScheduler(app.state.container)
        replay = ReplayRaw(app.state.container)
        while not stop.is_set():
            try:
                await scheduler.tick()
                await replay.execute()
            except Exception:
                pass
            try:
                await asyncio.wait_for(stop.wait(), timeout=60)
            except asyncio.TimeoutError:
                continue

    task = asyncio.create_task(_loops()) if settings.news_sync_enabled else None
    yield
    stop.set()
    if task is not None:
        task.cancel()


docs_url = "/docs" if settings.news_docs_enabled else None
redoc_url = "/redoc" if settings.news_docs_enabled else None
openapi_url = "/openapi.json" if settings.news_docs_enabled else None

app = FastAPI(
    title="AM News",
    version=__version__,
    lifespan=lifespan,
    docs_url=docs_url,
    redoc_url=redoc_url,
    openapi_url=openapi_url,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health_router)
app.include_router(affairs_router)
app.include_router(insight_router)
app.include_router(holdings_router)
app.include_router(admin_feed_router)
app.include_router(admin_replay_router)


@app.exception_handler(HTTPException)
async def http_error(_: Request, exc: HTTPException) -> JSONResponse:
    detail = exc.detail
    if isinstance(detail, dict) and "error_code" in detail:
        return JSONResponse(status_code=exc.status_code, content=detail)
    return JSONResponse(
        status_code=exc.status_code,
        content=ErrorEnvelope(error_code="HTTP_ERROR", message=str(detail)).model_dump(),
    )


@app.exception_handler(Exception)
async def unhandled(_: Request, exc: Exception) -> JSONResponse:
    return JSONResponse(
        status_code=500,
        content=ErrorEnvelope(error_code="INTERNAL_SERVER_ERROR", message="unexpected").model_dump(),
    )


def custom_openapi():
    if app.openapi_schema:
        return app.openapi_schema
    schema = get_openapi(
        title=app.title,
        version=app.version,
        routes=app.routes,
        description="AM News: current affairs and holdings. User path is Redis then Mongo only.",
    )
    schema["components"] = schema.get("components") or {}
    schema["components"]["securitySchemes"] = {
        "BearerAuth": {"type": "http", "scheme": "bearer", "bearerFormat": "JWT"}
    }
    schema["security"] = [{"BearerAuth": []}]
    app.openapi_schema = schema
    return schema


app.openapi = custom_openapi
