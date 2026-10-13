"""
Background Job Models for Async Processing
Handles long-running LLM processing tasks
"""

from datetime import datetime
from enum import Enum
from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field, ConfigDict


class JobStatus(str, Enum):
    """Job processing status"""
    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


class JobType(str, Enum):
    """Type of background job"""
    EXCEL_PROCESSING = "excel_processing"
    SHEET_PARSING = "sheet_parsing"
    BATCH_PROCESSING = "batch_processing"
    ETF_HOLDINGS_FETCH = "etf_holdings_fetch"


class JobProgress(BaseModel):
    """Job progress tracking"""
    total_items: int = Field(default=0, description="Total items to process", examples=[10])
    completed_items: int = Field(default=0, description="Successfully processed items", examples=[5])
    failed_items: int = Field(default=0, description="Failed items count", examples=[0])
    current_item: Optional[str] = Field(default=None, description="Currently processing item name", examples=["Sheet_1"])

    @property
    def percentage(self) -> float:
        if self.total_items == 0:
            return 0.0
        return (self.completed_items / self.total_items) * 100.0


class BackgroundJob(BaseModel):
    """Background job model"""
    job_id: str = Field(..., description="Unique job identifier", examples=["job_9b1deb4d3a7c"])
    job_type: JobType = Field(..., description="Type of job", examples=[JobType.EXCEL_PROCESSING])
    status: JobStatus = Field(default=JobStatus.PENDING, description="Current job status", examples=[JobStatus.PENDING])

    # Input data
    input_data: Dict[str, Any] = Field(default_factory=dict, description="Job input parameters")

    # Progress tracking
    progress: JobProgress = Field(default_factory=JobProgress, description="Job progress tracking")

    # Results
    result: Optional[Dict[str, Any]] = Field(default=None, description="Job execution results")
    error_message: Optional[str] = Field(default=None, description="Error message if processing failed")

    # Timestamps
    created_at: datetime = Field(default_factory=datetime.now, description="Job creation timestamp")
    started_at: Optional[datetime] = Field(default=None, description="Job start timestamp")
    completed_at: Optional[datetime] = Field(default=None, description="Job completion timestamp")

    # Callback configuration
    callback_url: Optional[str] = Field(default=None, description="Webhook callback URL")
    callback_headers: Optional[Dict[str, str]] = Field(default=None, description="HTTP headers for webhook callback")

    # Metadata
    user_id: Optional[str] = Field(default=None, description="User ID associated with the job")
    priority: int = Field(default=5, description="Job priority (1=highest, 10=lowest)", examples=[5])

    def to_mongo_document(self) -> Dict[str, Any]:
        """Convert to MongoDB document"""
        doc = self.dict()
        doc["_id"] = self.job_id
        return doc


class JobResponse(BaseModel):
    """API response for job creation"""
    job_id: str = Field(..., description="Unique job identifier", examples=["job_9b1deb4d3a7c"])
    status: JobStatus = Field(..., description="Initial job status", examples=[JobStatus.PENDING])
    message: str = Field(..., description="Human readable status message", examples=["Job accepted and queued for processing"])
    estimated_completion_time: Optional[str] = Field(default=None, description="Estimated completion duration", examples=["30 seconds"])
    status_url: str = Field(..., description="URL endpoint to poll for job status", examples=["/v1/jobs/job_9b1deb4d3a7c"])
    webhook_url: Optional[str] = Field(default=None, description="Webhook callback URL")

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "job_id": "job_9b1deb4d3a7c",
                "status": "pending",
                "message": "Job accepted and queued for processing",
                "estimated_completion_time": "30 seconds",
                "status_url": "/v1/jobs/job_9b1deb4d3a7c",
                "webhook_url": "https://am-dev.asrax.in/api/v1/webhooks/job-complete"
            }
        }
    )


class JobStatusResponse(BaseModel):
    """API response for job status check"""
    job_id: str = Field(..., description="Unique job identifier", examples=["job_9b1deb4d3a7c"])
    status: JobStatus = Field(..., description="Current job status", examples=[JobStatus.RUNNING])
    progress: JobProgress = Field(..., description="Current progress metric")
    result: Optional[Dict[str, Any]] = Field(default=None, description="Final job result payload when completed")
    error_message: Optional[str] = Field(default=None, description="Error message if job failed")
    created_at: datetime = Field(..., description="Job creation timestamp")
    started_at: Optional[datetime] = Field(default=None, description="Job start timestamp")
    completed_at: Optional[datetime] = Field(default=None, description="Job completion timestamp")
    estimated_remaining_time: Optional[str] = Field(default=None, description="Estimated time remaining", examples=["15 seconds"])

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "job_id": "job_9b1deb4d3a7c",
                "status": "running",
                "progress": {
                    "total_items": 10,
                    "completed_items": 4,
                    "failed_items": 0,
                    "current_item": "HDFC_Mutual_Fund_Q3.xlsx"
                },
                "result": None,
                "error_message": None,
                "created_at": "2026-07-25T01:25:00Z",
                "started_at": "2026-07-25T01:25:02Z",
                "completed_at": None,
                "estimated_remaining_time": "15 seconds"
            }
        }
    )


class ExcelProcessingJob(BaseModel):
    """Specific job type for Excel processing"""
    file_id: str = Field(..., description="Uploaded file identifier")
    file_path: str = Field(..., description="Storage file path")
    sheet_count: int = Field(..., description="Number of sheets in the workbook")
    parse_method: str = Field(default="together", description="LLM parsing method")
    callback_url: Optional[str] = Field(default=None, description="Webhook callback URL")
    process_sheets_parallel: bool = Field(default=False, description="Enable parallel sheet parsing")
    max_parallel_sheets: int = Field(default=3, description="Max concurrent sheets to process")