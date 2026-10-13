from fastapi import APIRouter, Depends, Response

from am_news.api.deps import get_container, require_user
from am_news.application.get_current_affairs import GetCurrentAffairs
from am_news.domain.entities import Principal
from am_news.domain.universe import NIFTY50_SYMBOLS
from am_news.schemas.news import CurrentAffairsResponse, ErrorEnvelope

router = APIRouter(tags=["Current affairs"])

_ERRORS = {
    401: {"model": ErrorEnvelope, "description": "JWT required"},
    403: {"model": ErrorEnvelope, "description": "Forbidden"},
}


@router.get(
    "/v1/current-affairs",
    response_model=CurrentAffairsResponse,
    operation_id="getCurrentAffairs",
    summary="Current affairs",
    description="Last 10 NIFTY 50 articles from Redis then Mongo. Never calls the vendor.",
    responses=_ERRORS,
)
async def get_current_affairs(
    response: Response,
    _: Principal = Depends(require_user),
    container=Depends(get_container),
) -> CurrentAffairsResponse:
    response.headers["Cache-Control"] = "private, max-age=30"
    return await GetCurrentAffairs(container.snapshots, container.articles, NIFTY50_SYMBOLS).execute()
