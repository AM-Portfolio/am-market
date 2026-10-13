from fastapi import APIRouter, Depends, status

from am_news.api.deps import get_container, require_admin
from am_news.application.start_feed import StartFeed
from am_news.domain.entities import Principal
from am_news.schemas.news import AcceptedFeedResponse, ErrorEnvelope, FeedStatusResponse

router = APIRouter(prefix="/v1/admin", tags=["Admin"])

_ERRORS = {
    401: {"model": ErrorEnvelope, "description": "JWT required"},
    403: {"model": ErrorEnvelope, "description": "admin or super_admin required"},
    409: {"model": ErrorEnvelope, "description": "Feed already running"},
}


@router.post(
    "/feed/start",
    response_model=AcceptedFeedResponse,
    status_code=status.HTTP_202_ACCEPTED,
    operation_id="startFeed",
    summary="Start NIFTY 50 news feed",
    responses=_ERRORS,
)
async def start_feed(
    _: Principal = Depends(require_admin),
    container=Depends(get_container),
) -> AcceptedFeedResponse:
    return await StartFeed(container).execute(http_start=True)


@router.post(
    "/feed/stop",
    response_model=AcceptedFeedResponse,
    operation_id="stopFeed",
    responses=_ERRORS,
)
async def stop_feed(
    _: Principal = Depends(require_admin),
    container=Depends(get_container),
) -> AcceptedFeedResponse:
    await container.lock.release()
    state = await container.feed_state.get()
    return AcceptedFeedResponse(status=state.status, message="released")


@router.get(
    "/feed",
    response_model=FeedStatusResponse,
    operation_id="getFeed",
    responses=_ERRORS,
)
async def get_feed(
    _: Principal = Depends(require_admin),
    container=Depends(get_container),
) -> FeedStatusResponse:
    state = await container.feed_state.get()
    age = await container.snapshots.affairs_age_seconds()
    dead = await container.raw.dead_count()
    return FeedStatusResponse(
        status=state.status,
        last_sync_at=state.last_sync_at,
        affairs_age_seconds=age,
        dead_count=dead,
        pages_fetched=state.pages_fetched,
        message=state.message,
    )
