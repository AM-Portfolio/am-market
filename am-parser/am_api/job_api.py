"""
Async Job API Endpoints
Handles background processing with immediate response
"""

from fastapi import APIRouter, HTTPException, status, UploadFile, File, Form, BackgroundTasks
from fastapi.responses import JSONResponse
from typing import Optional, List
import asyncio
from datetime import datetime, timedelta

from am_common.job_models import (
    JobResponse, JobStatusResponse, BackgroundJob, JobStatus, JobType, ExcelProcessingJob
)
from am_services.job_queue_service import get_job_queue
from am_services.file_upload_service import FileUploadService
from am_persistence.file_upload_repository import FileUploadRepository
from am_persistence import create_mutual_fund_service


router = APIRouter(tags=["Background Jobs"])


@router.post("/upload-excel-async", response_model=JobResponse)
async def upload_excel_async(
    file: UploadFile = File(...),
    parse_method: str = Form(default="together"),
    callback_url: Optional[str] = Form(default=None),
    user_id: Optional[str] = Form(default=None)
):
    """
    Upload Excel file for async background processing
    Returns immediately with job ID, processes in background
    """
    try:
        # Initialize services with proper database connection
        service_instance = create_mutual_fund_service(
            mongo_uri="mongodb://admin:password123@localhost:27017",
            db_name="mutual_funds"
        )
        repo = FileUploadRepository(service_instance.database)
        upload_service = FileUploadService()  # Uses default directories
        job_queue = await get_job_queue()
        
        # Step 1: Upload and split Excel file (quick operation)
        print(f"🚀 Starting async Excel upload: {file.filename}")
        
        # Upload main file
        main_file_upload = await upload_service.save_uploaded_file(file)
        
        # Persist main file to database
        await repo.create_file_upload(main_file_upload)
        print(f"✅ Main file uploaded: {main_file_upload.file_id}")
        
        # Split into sheets (quick operation)
        sheet_files = upload_service.split_excel_into_sheets(main_file_upload)
        
        # Persist sheet files to database
        for sheet_file in sheet_files:
            await repo.create_file_upload(sheet_file)
        
        sheet_count = len(sheet_files)
        print(f"✅ Excel split into {sheet_count} sheets")
        
        # Step 2: Create background job for LLM processing
        # Validate webhook URL if provided
        normalized_callback = None
        callback_note = None
        if callback_url:
            cb = callback_url.strip()
            if cb.startswith("http://") or cb.startswith("https://"):
                normalized_callback = cb
            else:
                callback_note = "Ignoring invalid callback_url (missing http/https)."
        job_input = {
            "file_id": main_file_upload.file_id,
            "file_path": main_file_upload.file_path,
            "sheet_count": sheet_count,
            "parse_method": parse_method
        }
        
        job_id = await job_queue.create_job(
            job_type=JobType.EXCEL_PROCESSING,
            input_data=job_input,
            callback_url=normalized_callback,
            user_id=user_id
        )
        
        # Estimate completion time (1.5 min per sheet average)
        estimated_minutes = sheet_count * 1.5
        estimated_completion = datetime.now() + timedelta(minutes=estimated_minutes)
        
        resp = JobResponse(
            job_id=job_id,
            status=JobStatus.PENDING,
            message=f"Excel file uploaded successfully. Processing {sheet_count} sheets in background.",
            estimated_completion_time=estimated_completion.strftime("%Y-%m-%d %H:%M:%S"),
            status_url=f"/jobs/{job_id}/status",
            webhook_url=normalized_callback
        )
        # If invalid callback provided, include an extra hint field in response
        if callback_note:
            # FastAPI response_model ignores extra keys unless we wrap; use JSONResponse for hint
            return JSONResponse(status_code=200, content={
                **resp.dict(),
                "note": callback_note
            })
        return resp
        
    except Exception as e:
        print(f"❌ Upload error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to upload file: {str(e)}"
        )


@router.get("/{job_id}/status", response_model=JobStatusResponse)
async def get_job_status(job_id: str):
    """Get the current status of a background job"""
    try:
        job_queue = await get_job_queue()
        job = await job_queue.get_job(job_id)
        
        if not job:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Job not found: {job_id}"
            )
        
        # Estimate remaining time
        estimated_remaining = None
        if job.status == JobStatus.RUNNING and job.progress.total_items > 0:
            remaining_items = job.progress.total_items - job.progress.completed_items
            if remaining_items > 0:
                estimated_minutes = remaining_items * 1.5  # 1.5 min per sheet average
                estimated_remaining = f"{estimated_minutes:.1f} minutes"
        
        return JobStatusResponse(
            job_id=job_id,
            status=job.status,
            progress=job.progress,
            result=job.result,
            error_message=job.error_message,
            created_at=job.created_at,
            started_at=job.started_at,
            completed_at=job.completed_at,
            estimated_remaining_time=estimated_remaining
        )
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get job status: {str(e)}"
        )


@router.get("/{job_id}/result")
async def get_job_result(job_id: str):
    """Get the result of a completed job"""
    try:
        job_queue = await get_job_queue()
        job = await job_queue.get_job(job_id)
        
        if not job:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Job not found: {job_id}"
            )
        
        if job.status == JobStatus.COMPLETED:
            return {
                "job_id": job_id,
                "status": "completed",
                "result": job.result,
                "completed_at": job.completed_at
            }
        elif job.status == JobStatus.FAILED:
            return {
                "job_id": job_id,
                "status": "failed",
                "error_message": job.error_message,
                "completed_at": job.completed_at
            }
        else:
            return {
                "job_id": job_id,
                "status": job.status,
                "message": "Job not yet completed",
                "progress": job.progress.dict()
            }
            
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get job result: {str(e)}"
        )


@router.delete("/{job_id}")
async def cancel_job(job_id: str):
    """Cancel a pending or running job"""
    try:
        job_queue = await get_job_queue()
        job = await job_queue.get_job(job_id)
        
        if not job:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Job not found: {job_id}"
            )
        
        if job.status in [JobStatus.COMPLETED, JobStatus.FAILED]:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Cannot cancel job in status: {job.status}"
            )
        
        await job_queue.update_job_status(job_id, JobStatus.CANCELLED)
        
        return {
            "job_id": job_id,
            "status": "cancelled",
            "message": "Job cancelled successfully"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to cancel job: {str(e)}"
        )


@router.get("/")
async def list_jobs(
    job_status: Optional[JobStatus] = None,
    user_id: Optional[str] = None,
    limit: int = 50
):
    """List background jobs with optional filtering"""
    try:
        job_queue = await get_job_queue()
        
        # Build query filter
        query_filter = {}
        if job_status:
            query_filter["status"] = job_status
        if user_id:
            query_filter["user_id"] = user_id
        
        # Get jobs from database
        collection = job_queue.mutual_fund_service.database[job_queue.collection_name]
        cursor = collection.find(query_filter).sort("created_at", -1).limit(limit)
        
        jobs = []
        async for doc in cursor:
            doc["job_id"] = doc["_id"]
            doc.pop("_id")
            job = BackgroundJob(**doc)
            jobs.append({
                "job_id": job.job_id,
                "job_type": job.job_type,
                "status": job.status,
                "progress": job.progress.dict(),
                "created_at": job.created_at,
                "started_at": job.started_at,
                "completed_at": job.completed_at
            })
        
        return {
            "jobs": jobs,
            "total_count": len(jobs),
            "filter": query_filter
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to list jobs: {str(e)}"
        )


@router.post("/admin/fix-stuck-job/{job_id}")
async def fix_stuck_job(job_id: str, mark_as_failed: bool = True):
    """
    Admin endpoint to fix stuck jobs
    Used when jobs get stuck due to server restarts
    """
    try:
        job_queue = await get_job_queue()
        
        # Check if job exists
        job = await job_queue.get_job(job_id)
        if not job:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Job {job_id} not found"
            )
        
        # Fix the job
        await job_queue.fix_specific_job(job_id, mark_as_failed)
        
        # Get updated job
        updated_job = await job_queue.get_job(job_id)
        
        action = "marked as failed" if mark_as_failed else "reset to pending"
        
        return {
            "message": f"Job {job_id} has been {action}",
            "job_id": job_id,
            "old_status": job.status,
            "new_status": updated_job.status,
            "error_message": updated_job.error_message,
            "action_taken": action
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to fix job: {str(e)}"
        )


@router.post("/admin/recover-stuck-jobs") 
async def recover_all_stuck_jobs():
    """
    Admin endpoint to recover all stuck jobs
    Useful after server restarts
    """
    try:
        job_queue = await get_job_queue()
        
        # This will run the recovery process
        await job_queue.recover_stuck_jobs()
        
        return {
            "message": "Stuck job recovery process completed",
            "timestamp": datetime.now(),
            "action": "All stuck jobs have been recovered"
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to recover stuck jobs: {str(e)}"
        )