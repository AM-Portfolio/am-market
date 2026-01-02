"""
Event models for processing logs
"""
from enum import Enum
from typing import Optional, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field


class EventType(str, Enum):
    JOB_CREATED = "job_created"
    JOB_STATUS_CHANGED = "job_status_changed"
    UPLOAD_RECEIVED = "upload_received"
    EXCEL_SPLIT = "excel_split"
    SHEET_PARSE_STARTED = "sheet_parse_started"
    SHEET_PARSE_COMPLETED = "sheet_parse_completed"
    PORTFOLIO_SAVED = "portfolio_saved"
    PORTFOLIO_DUPLICATE = "portfolio_duplicate"
    SHEET_DELETED_DISK = "sheet_deleted_from_disk"
    WEBHOOK_SENT = "webhook_sent"
    WEBHOOK_SKIPPED = "webhook_skipped"
    WEBHOOK_FAILED = "webhook_failed"


class ProcessingEvent(BaseModel):
    event_type: EventType
    status: str = Field(description="success|failed|running|pending|info")
    timestamp: datetime = Field(default_factory=datetime.utcnow)

    # Correlation identifiers
    job_id: Optional[str] = None
    file_id: Optional[str] = None  # parent Excel
    sheet_id: Optional[str] = None
    portfolio_id: Optional[str] = None

    # Context
    message: Optional[str] = None
    metadata: Optional[Dict[str, Any]] = None

    def to_mongo_document(self) -> Dict[str, Any]:
        doc = self.dict()
        # Store ISO string for readability
        doc["timestamp"] = self.timestamp
        return doc
