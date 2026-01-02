"""
Lightweight event logger to persist processing events.
"""
from typing import Optional, Dict, Any

from am_common.event_models import ProcessingEvent, EventType
from am_persistence.event_log_service import create_event_log_service, EventLogService


class EventLogger:
    def __init__(self, mongo_uri: str = "mongodb://localhost:27017", db_name: str = "am_logs"):
        self._service: EventLogService = create_event_log_service(mongo_uri, db_name)

    async def emit(
        self,
        event_type: EventType,
        status: str,
        *,
        job_id: Optional[str] = None,
        file_id: Optional[str] = None,
        sheet_id: Optional[str] = None,
        portfolio_id: Optional[str] = None,
        message: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
    ):
        evt = ProcessingEvent(
            event_type=event_type,
            status=status,
            job_id=job_id,
            file_id=file_id,
            sheet_id=sheet_id,
            portfolio_id=portfolio_id,
            message=message,
            metadata=metadata,
        )
        try:
            await self._service.write_event(evt)
        except Exception:
            # Logging must never break processing; swallow errors.
            pass
