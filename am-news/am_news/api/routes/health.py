from fastapi import APIRouter

router = APIRouter(tags=["Health"])


@router.get("/health", operation_id="health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@router.get("/ready", operation_id="ready")
async def ready() -> dict[str, str]:
    return {"status": "ready"}
