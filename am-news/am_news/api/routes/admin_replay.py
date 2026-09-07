from fastapi import APIRouter, Depends

from am_news.api.deps import get_container, require_admin
from am_news.application.replay_raw import ReplayRaw
from am_news.domain.entities import Principal
from am_news.schemas.news import ErrorEnvelope, RawBatchSummary, RawListResponse, ReplayResponse

router = APIRouter(prefix="/v1/admin", tags=["Admin"])

_ERRORS = {
    401: {"model": ErrorEnvelope, "description": "JWT required"},
    403: {"model": ErrorEnvelope, "description": "admin or super_admin required"},
}


@router.get(
    "/raw",
    response_model=RawListResponse,
    operation_id="listRaw",
    responses=_ERRORS,
)
async def list_raw(
    _: Principal = Depends(require_admin),
    container=Depends(get_container),
) -> RawListResponse:
    rows = await container.raw.list_recent()
    return RawListResponse(
        items=[
            RawBatchSummary(
                raw_id=row.raw_id,
                process_status=row.process_status,
                instrument_keys=list(row.instrument_keys),
                attempts=row.attempts,
                error=row.error,
                has_body=bool(row.body),
            )
            for row in rows
        ]
    )


@router.post(
    "/replay",
    response_model=ReplayResponse,
    operation_id="replayPending",
    responses=_ERRORS,
)
async def replay_pending(
    _: Principal = Depends(require_admin),
    container=Depends(get_container),
) -> ReplayResponse:
    return await ReplayRaw(container).execute()


@router.post(
    "/replay/{raw_id}",
    response_model=ReplayResponse,
    operation_id="replayRaw",
    responses=_ERRORS,
)
async def replay_one(
    raw_id: str,
    _: Principal = Depends(require_admin),
    container=Depends(get_container),
) -> ReplayResponse:
    return await ReplayRaw(container).execute(raw_id=raw_id)
