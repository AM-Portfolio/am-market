from fastapi import APIRouter, Depends, Response

from am_news.api.deps import get_container, require_user
from am_news.application.get_insight import GetInsight
from am_news.domain.entities import Principal
from am_news.domain.universe import NIFTY50_SYMBOLS
from am_news.schemas.news import ErrorEnvelope, InsightRequest, InsightResponse

router = APIRouter(tags=["Insight"])

_ERRORS = {
    401: {"model": ErrorEnvelope, "description": "JWT required"},
}


@router.post(
    "/v1/insight",
    response_model=InsightResponse,
    operation_id="postInsight",
    summary="Insight",
    description="Current affairs plus holdings news for the posted symbols. Redis then Mongo only.",
    responses=_ERRORS,
)
async def post_insight(
    body: InsightRequest,
    response: Response,
    _: Principal = Depends(require_user),
    container=Depends(get_container),
) -> InsightResponse:
    response.headers["Cache-Control"] = "private, max-age=30"
    return await GetInsight(container.snapshots, container.articles, NIFTY50_SYMBOLS).execute(
        body.symbols, include_affairs=True
    )
