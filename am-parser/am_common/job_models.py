"""
Background Job Models for Async Processing
Handles long-running LLM processing tasks
"""

from datetime import datetime
from enum import Enum
from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field


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
    total_items: int = 0
    completed_items: int = 0
    failed_items: int = 0
    current_item: Optional[str] = None
    
    @property
    def percentage(self) -> float:
        if self.total_items == 0:
            return 0.0
        return (self.completed_items / self.total_items) * 100.0


class BackgroundJob(BaseModel):
    """Background job model"""
    job_id: str = Field(..., description="Unique job identifier")
    job_type: JobType = Field(..., description="Type of job")
    status: JobStatus = Field(default=JobStatus.PENDING, description="Current job status")
    
    # Input data
    input_data: Dict[str, Any] = Field(default_factory=dict, description="Job input parameters")
    
    # Progress tracking
    progress: JobProgress = Field(default_factory=JobProgress, description="Job progress")
    
    # Results
    result: Optional[Dict[str, Any]] = Field(default=None, description="Job results")
    error_message: Optional[str] = Field(default=None, description="Error message if failed")
    
    # Timestamps
    created_at: datetime = Field(default_factory=datetime.now, description="Job creation time")
    started_at: Optional[datetime] = Field(default=None, description="Job start time")
    completed_at: Optional[datetime] = Field(default=None, description="Job completion time")
    
    # Callback configuration
    callback_url: Optional[str] = Field(default=None, description="Webhook URL for completion notification")
    callback_headers: Optional[Dict[str, str]] = Field(default=None, description="Headers for webhook")
    
    # Metadata
    user_id: Optional[str] = Field(default=None, description="User who created the job")
    priority: int = Field(default=5, description="Job priority (1=highest, 10=lowest)")
    
    def to_mongo_document(self) -> Dict[str, Any]:
        """Convert to MongoDB document"""
        doc = self.dict()
        doc["_id"] = self.job_id
        return doc


class JobResponse(BaseModel):
    """API response for job creation"""
    job_id: str
    status: JobStatus
    message: str
    estimated_completion_time: Optional[str] = None
    status_url: str
    webhook_url: Optional[str] = None


class JobStatusResponse(BaseModel):
    """API response for job status check"""
    job_id: str
    status: JobStatus
    progress: JobProgress
    result: Optional[Dict[str, Any]] = None
    error_message: Optional[str] = None
    created_at: datetime
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    estimated_remaining_time: Optional[str] = None


class ExcelProcessingJob(BaseModel):
    """Specific job type for Excel processing"""
    file_id: str
    file_path: str
    sheet_count: int
    parse_method: str = "together"
    callback_url: Optional[str] = None
    process_sheets_parallel: bool = False
    max_parallel_sheets: int = 3