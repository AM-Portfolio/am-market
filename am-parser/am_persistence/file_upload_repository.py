"""
File Upload Repository
Handles database operations for file uploads and processing
"""
from typing import List, Optional, Dict, Any
from datetime import datetime
from motor.motor_asyncio import AsyncIOMotorDatabase, AsyncIOMotorCollection

from am_common.upload_models import FileUpload, ProcessingStatus


class FileUploadRepository:
    """Repository for file upload database operations"""
    
    def __init__(self, database: AsyncIOMotorDatabase):
        self.database = database
        self.collection: AsyncIOMotorCollection = database.file_uploads
    
    async def create_file_upload(self, file_upload: FileUpload) -> str:
        """Insert new file upload record"""
        file_data = file_upload.dict()
        file_data['_id'] = file_upload.file_id
        
        result = await self.collection.insert_one(file_data)
        return str(result.inserted_id)
    
    async def get_file_upload(self, file_id: str) -> Optional[FileUpload]:
        """Get file upload by ID"""
        document = await self.collection.find_one({"_id": file_id})
        if document:
            return FileUpload(**document)
        return None
    
    async def update_file_upload(self, file_upload: FileUpload) -> bool:
        """Update file upload record"""
        file_data = file_upload.dict()
        file_data['updated_at'] = datetime.utcnow()
        
        result = await self.collection.update_one(
            {"_id": file_upload.file_id},
            {"$set": file_data}
        )
        return result.modified_count > 0
    
    async def get_files_by_parent_id(self, parent_id: str) -> List[FileUpload]:
        """Get all sheet files for a parent Excel file"""
        cursor = self.collection.find({"parent_id": parent_id})
        documents = await cursor.to_list(length=None)
        return [FileUpload(**doc) for doc in documents]
    
    async def get_all_files(self, skip: int = 0, limit: int = 100, 
                           status_filter: Optional[ProcessingStatus] = None) -> List[FileUpload]:
        """Get all file uploads with optional filtering"""
        query = {}
        if status_filter:
            query["status"] = status_filter
        
        cursor = self.collection.find(query).skip(skip).limit(limit).sort("created_at", -1)
        documents = await cursor.to_list(length=None)
        return [FileUpload(**doc) for doc in documents]
    
    async def count_files(self, status_filter: Optional[ProcessingStatus] = None) -> int:
        """Count total files with optional filtering"""
        query = {}
        if status_filter:
            query["status"] = status_filter
        
        return await self.collection.count_documents(query)
    
    async def update_file_status(self, file_id: str, status: ProcessingStatus, 
                                error_message: Optional[str] = None) -> bool:
        """Update file processing status"""
        update_data = {
            "status": status,
            "updated_at": datetime.utcnow()
        }
        if error_message:
            update_data["error_message"] = error_message
        
        result = await self.collection.update_one(
            {"_id": file_id},
            {"$set": update_data}
        )
        return result.modified_count > 0
    
    async def delete_file_upload(self, file_id: str) -> bool:
        """Delete file upload record"""
        result = await self.collection.delete_one({"_id": file_id})
        return result.deleted_count > 0
    
    async def get_files_by_status(self, status: ProcessingStatus) -> List[FileUpload]:
        """Get all files with specific status"""
        cursor = self.collection.find({"status": status})
        documents = await cursor.to_list(length=None)
        return [FileUpload(**doc) for doc in documents]
    
    async def update_processing_metadata(self, file_id: str, metadata: Dict[str, Any]) -> bool:
        """Update processing metadata for a file"""
        result = await self.collection.update_one(
            {"_id": file_id},
            {"$set": {
                "processing_metadata": metadata,
                "updated_at": datetime.utcnow()
            }}
        )
        return result.modified_count > 0