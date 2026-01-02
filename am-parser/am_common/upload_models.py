"""
Upload Models for file management and processing
"""
from datetime import datetime
from enum import Enum
from typing import Optional, List
from pydantic import BaseModel, Field
from bson import ObjectId


class ProcessingStatus(str, Enum):
    """Status of file processing"""
    UPLOADED = "uploaded"
    PROCESSING = "processing" 
    SPLITTING = "splitting"
    COMPLETED = "completed"
    FAILED = "failed"
    PARSED = "parsed"


class FileType(str, Enum):
    """Type of uploaded file"""
    EXCEL = "excel"
    SHEET = "sheet"
    CSV = "csv"


class FileUpload(BaseModel):
    """Model for uploaded files and their processing status"""
    id: Optional[str] = Field(default=None, alias="_id")
    file_id: str = Field(..., description="Unique identifier for the file")
    original_filename: str = Field(..., description="Original filename uploaded by user")
    stored_filename: str = Field(..., description="Filename as stored in the system")
    file_type: FileType = Field(..., description="Type of file (excel, sheet, csv)")
    file_path: str = Field(..., description="Full path to stored file")
    parent_id: Optional[str] = Field(default=None, description="Parent file ID for sheets")
    sheet_name: Optional[str] = Field(default=None, description="Sheet name if this is a sheet file")
    status: ProcessingStatus = Field(default=ProcessingStatus.UPLOADED, description="Current processing status")
    file_size: int = Field(..., description="File size in bytes")
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    error_message: Optional[str] = Field(default=None, description="Error message if processing failed")
    processing_metadata: Optional[dict] = Field(default=None, description="Additional processing information")

    class Config:
        allow_population_by_field_name = True
        arbitrary_types_allowed = True
        json_encoders = {ObjectId: str}

    def dict(self, **kwargs):
        """Override dict to handle datetime serialization"""
        d = super().dict(**kwargs)
        if 'created_at' in d and isinstance(d['created_at'], datetime):
            d['created_at'] = d['created_at'].isoformat()
        if 'updated_at' in d and isinstance(d['updated_at'], datetime):
            d['updated_at'] = d['updated_at'].isoformat()
        return d


class FileUploadResponse(BaseModel):
    """Response model for file upload"""
    file_id: str
    original_filename: str
    status: ProcessingStatus
    message: str
    sheets_info: Optional[List[dict]] = None


class FileListResponse(BaseModel):
    """Response model for listing files"""
    files: List[FileUpload]
    total_count: int


class FileProcessingRequest(BaseModel):
    """Request model for processing a file"""
    file_id: str
    processing_method: str = Field(default="manual", description="Processing method: manual, llm, together")
    api_key: Optional[str] = Field(default=None, description="API key for LLM processing")
    sheet_names: Optional[List[str]] = Field(default=None, description="Specific sheets to process")


class SheetInfo(BaseModel):
    """Information about an Excel sheet"""
    sheet_name: str
    row_count: int
    column_count: int
    file_id: str
    status: ProcessingStatus = ProcessingStatus.UPLOADED