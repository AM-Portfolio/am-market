from fastapi import APIRouter, Depends, Query, Response

from am_news.api.deps import get_container, require_user
from am_news.application.get_insight import GetInsight
from am_news.domain.entities import Principal
from am_news.domain.universe import NIFTY50_SYMBOLS
from am_news.schemas.news import ErrorEnvelope, HoldingsNewsResponse

router = APIRouter(tags=["Holdings"])

_ERRORS = {
    401: {"model": ErrorEnvelope, "description": "JWT required"},
}


@router.get(
    "/v1/holdings",
    response_model=HoldingsNewsResponse,
    operation_id="getHoldings",
    summary="Holdings news",
    description="Debug/Postman only. Same holdings filter as POST /v1/insight without current affairs.",
    responses=_ERRORS,
)
async def get_holdings(
    response: Response,
    symbols: list[str] = Query(default=[]),
    _: Principal = Depends(require_user),
    container=Depends(get_container),
) -> HoldingsNewsResponse:
    response.headers["Cache-Control"] = "private, max-age=30"
    return await GetInsight(container.snapshots, container.articles, NIFTY50_SYMBOLS).holdings_only(symbols)
