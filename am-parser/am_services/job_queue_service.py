"""
Background Job Queue Service
Manages async processing of long-running tasks
"""

import asyncio
import json
import uuid
import httpx
from datetime import datetime, timedelta
from typing import Optional, Dict, Any, List, Callable
from pathlib import Path
import sys

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_common.job_models import BackgroundJob, JobStatus, JobType, JobProgress, ExcelProcessingJob
from am_persistence.mutual_fund_service import create_mutual_fund_service
from am_services.event_logger import EventLogger
from am_common.event_models import EventType


class JobQueue:
    """Background job queue using MongoDB storage"""
    
    def __init__(self, mongodb_uri: str, database_name: str = "mutual_funds"):
        self.mutual_fund_service = create_mutual_fund_service(mongodb_uri, database_name)
        self.collection_name = "background_jobs"
        self.running_jobs: Dict[str, asyncio.Task] = {}
        self.max_concurrent_jobs = 5
        # Separate DB for logs
        self.event_logger = EventLogger(mongo_uri=mongodb_uri, db_name="am_logs")
        
    async def create_job(
        self, 
        job_type: JobType, 
        input_data: Dict[str, Any], 
        callback_url: Optional[str] = None,
        user_id: Optional[str] = None,
        priority: int = 5
    ) -> str:
        """Create a new background job"""
        job_id = str(uuid.uuid4())
        
        job = BackgroundJob(
            job_id=job_id,
            job_type=job_type,
            input_data=input_data,
            callback_url=callback_url,
            user_id=user_id,
            priority=priority
        )
        
        # Save to database
        collection = self.mutual_fund_service.database[self.collection_name]
        await collection.insert_one(job.to_mongo_document())
        
        print(f"✅ Created background job: {job_id} ({job_type})")
        # Log event
        await self.event_logger.emit(
            EventType.JOB_CREATED,
            "success",
            job_id=job_id,
            message=f"Background job created ({job_type})",
            metadata={"input": input_data, "user_id": user_id}
        )
        return job_id
    
    async def get_job(self, job_id: str) -> Optional[BackgroundJob]:
        """Get job by ID"""
        collection = self.mutual_fund_service.database[self.collection_name]
        doc = await collection.find_one({"_id": job_id})
        
        if doc:
            doc["job_id"] = doc["_id"]
            doc.pop("_id")
            
            # Handle missing or None progress field
            if "progress" not in doc or doc["progress"] is None:
                from am_common.job_models import JobProgress
                doc["progress"] = JobProgress().dict()
            
            return BackgroundJob(**doc)
        return None
    
    async def recover_stuck_jobs(self):
        """Recover jobs that were stuck due to server restart"""
        collection = self.mutual_fund_service.database[self.collection_name]
        
        # Find jobs that are marked as running but not in our running_jobs dict
        stuck_jobs = await collection.find({
            "status": "running",
            "$or": [
                {"started_at": {"$lt": datetime.now() - timedelta(minutes=5)}},  # Running for >5 min
                {"started_at": {"$exists": False}}  # No start time
            ]
        }).to_list(None)
        
        print(f"🔍 Found {len(stuck_jobs)} potentially stuck jobs")
        
        for job_doc in stuck_jobs:
            job_id = job_doc["_id"]
            print(f"🚨 Recovering stuck job: {job_id}")
            
            # Mark as failed with recovery message
            await collection.update_one(
                {"_id": job_id},
                {
                    "$set": {
                        "status": "failed",
                        "completed_at": datetime.now(),
                        "error_message": "Job recovery: Server was restarted while job was running"
                    }
                }
            )
            
            # Log the recovery
            await self.event_logger.emit(
                EventType.JOB_FAILED,
                "warning", 
                job_id=job_id,
                message="Job marked as failed due to server restart"
            )
    
    async def fix_specific_job(self, job_id: str, mark_as_failed: bool = True):
        """Fix a specific stuck job"""
        collection = self.mutual_fund_service.database[self.collection_name]
        
        if mark_as_failed:
            # Mark as failed
            await collection.update_one(
                {"_id": job_id},
                {
                    "$set": {
                        "status": "failed",
                        "completed_at": datetime.now(),
                        "error_message": "Manually fixed: Job was stuck due to server restart"
                    }
                }
            )
            print(f"🔧 Fixed stuck job {job_id} - marked as failed")
        else:
            # Reset to pending to allow retry
            await collection.update_one(
                {"_id": job_id},
                {
                    "$set": {
                        "status": "pending",
                        "error_message": "Reset for retry after server restart"
                    },
                    "$unset": {
                        "started_at": "",
                        "progress": "",
                        "completed_at": ""
                    }
                }
            )
            print(f"🔄 Reset job {job_id} to pending for retry")
    
    async def update_job_status(
        self, 
        job_id: str, 
        status: JobStatus, 
        progress: Optional[JobProgress] = None,
        result: Optional[Dict[str, Any]] = None,
        error_message: Optional[str] = None
    ):
        """Update job status and progress"""
        collection = self.mutual_fund_service.database[self.collection_name]
        
        update_data = {"status": status}
        
        if status == JobStatus.RUNNING and not progress:
            update_data["started_at"] = datetime.now()
        elif status in [JobStatus.COMPLETED, JobStatus.FAILED]:
            update_data["completed_at"] = datetime.now()
            
        if progress:
            update_data["progress"] = progress.dict()
        if result:
            update_data["result"] = result
        if error_message:
            update_data["error_message"] = error_message
            
        await collection.update_one(
            {"_id": job_id},
            {"$set": update_data}
        )
        
        # Log status change
        try:
            await self.event_logger.emit(
                EventType.JOB_STATUS_CHANGED,
                status.value if hasattr(status, 'value') else str(status),
                job_id=job_id,
                metadata={"progress": progress.dict() if progress else None, "error": error_message}
            )
        except Exception:
            pass

        # Send webhook if job is completed
        if status in [JobStatus.COMPLETED, JobStatus.FAILED]:
            await self._send_webhook_notification(job_id)
    
    async def _send_webhook_notification(self, job_id: str):
        """Send webhook notification when job completes"""
        job = await self.get_job(job_id)
        if not job or not job.callback_url:
            return
        
        # Basic validation to avoid exceptions on malformed URLs
        url = job.callback_url.strip()
        if not (url.startswith("http://") or url.startswith("https://")):
            print(f"ℹ️ Skipping webhook for job {job_id}: invalid URL '{url}'. Include http:// or https://")
            try:
                await self.event_logger.emit(
                    EventType.WEBHOOK_SKIPPED,
                    "info",
                    job_id=job_id,
                    message="Invalid webhook URL; missing scheme",
                    metadata={"url": url}
                )
            except Exception:
                pass
            return
            
        try:
            payload = {
                "job_id": job_id,
                "status": job.status,
                "progress": job.progress.dict(),
                "result": job.result,
                "error_message": job.error_message,
                "completed_at": job.completed_at.isoformat() if job.completed_at else None
            }
            
            headers = {"Content-Type": "application/json"}
            if job.callback_headers:
                headers.update(job.callback_headers)
            
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    url,
                    json=payload,
                    headers=headers,
                    timeout=30.0
                )
                print(f"✅ Webhook sent for job {job_id}: {response.status_code}")
                try:
                    await self.event_logger.emit(
                        EventType.WEBHOOK_SENT,
                        "success",
                        job_id=job_id,
                        metadata={"status_code": response.status_code, "url": url}
                    )
                except Exception:
                    pass
                
        except Exception as e:
            print(f"❌ Failed to send webhook for job {job_id}: {e}")
            try:
                await self.event_logger.emit(
                    EventType.WEBHOOK_FAILED,
                    "failed",
                    job_id=job_id,
                    message=str(e),
                    metadata={"url": url}
                )
            except Exception:
                pass
    
    async def start_job_processor(self):
        """Start the background job processor"""
        print("🚀 Starting background job processor...")
        
        while True:
            try:
                # Clean up completed tasks
                completed_tasks = [
                    job_id for job_id, task in self.running_jobs.items() 
                    if task.done()
                ]
                for job_id in completed_tasks:
                    del self.running_jobs[job_id]
                
                # Check if we can start more jobs
                if len(self.running_jobs) < self.max_concurrent_jobs:
                    pending_job = await self._get_next_pending_job()
                    if pending_job:
                        # Start processing the job
                        task = asyncio.create_task(self._process_job(pending_job))
                        self.running_jobs[pending_job.job_id] = task
                        print(f"🔄 Started processing job: {pending_job.job_id}")
                
                # Wait before checking again
                await asyncio.sleep(5)
                
            except Exception as e:
                print(f"❌ Error in job processor: {e}")
                await asyncio.sleep(10)
    
    async def _get_next_pending_job(self) -> Optional[BackgroundJob]:
        """Get the next pending job to process"""
        collection = self.mutual_fund_service.database[self.collection_name]
        
        doc = await collection.find_one(
            {"status": JobStatus.PENDING},
            sort=[("priority", 1), ("created_at", 1)]  # Lower priority number = higher priority
        )
        
        if doc:
            doc["job_id"] = doc["_id"]
            doc.pop("_id")
            return BackgroundJob(**doc)
        return None
    
    async def _process_job(self, job: BackgroundJob):
        """Process a background job"""
        try:
            await self.update_job_status(job.job_id, JobStatus.RUNNING)
            
            if job.job_type == JobType.EXCEL_PROCESSING:
                await self._process_excel_job(job)
            elif job.job_type == JobType.ETF_HOLDINGS_FETCH:
                await self._process_etf_holdings_job(job)
            else:
                raise ValueError(f"Unknown job type: {job.job_type}")
                
        except Exception as e:
            print(f"❌ Job {job.job_id} failed: {e}")
            await self.update_job_status(
                job.job_id, 
                JobStatus.FAILED, 
                error_message=str(e)
            )
    
    async def _process_excel_job(self, job: BackgroundJob):
        """Process an Excel file with multiple sheets"""
        from am_services.file_processing_service import FileProcessingService
        from am_persistence.file_upload_repository import FileUploadRepository
        
        # Initialize services
        file_upload_repo = FileUploadRepository(self.mutual_fund_service.database)
        processing_service = FileProcessingService(file_upload_repo, self.mutual_fund_service)
        
        input_data = job.input_data
        file_id = input_data["file_id"]
        parse_method = input_data.get("parse_method", "together")
        
        try:
            # Get the main file
            main_file = await file_upload_repo.get_file_upload(file_id)
            if not main_file:
                raise ValueError(f"File not found: {file_id}")
            
            # Get all sheet files
            sheet_files = await file_upload_repo.get_files_by_parent_id(file_id)
            total_sheets = len(sheet_files)
            
            progress = JobProgress(total_items=total_sheets, completed_items=0)
            await self.update_job_status(job.job_id, JobStatus.RUNNING, progress=progress)
            
            results = []
            
            for i, sheet_file in enumerate(sheet_files):
                try:
                    progress.current_item = f"Processing {sheet_file.sheet_name}"
                    await self.update_job_status(job.job_id, JobStatus.RUNNING, progress=progress)
                    
                    # Process the sheet
                    result = await processing_service._process_single_sheet(
                        sheet_file, 
                        method=parse_method
                    )
                    
                    if result:
                        progress.completed_items += 1
                        results.append({
                            "sheet_id": sheet_file.file_id,
                            "sheet_name": sheet_file.sheet_name,
                            "portfolio_id": result["portfolio_id"],
                            "status": "success",
                            "deleted": result.get("deleted", {"disk": False, "db": False})
                        })
                        try:
                            await self.event_logger.emit(
                                EventType.SHEET_PARSE_COMPLETED,
                                "success",
                                job_id=job.job_id,
                                file_id=file_id,
                                sheet_id=sheet_file.file_id,
                                portfolio_id=result.get("portfolio_id"),
                                metadata={"deleted": result.get("deleted")}
                            )
                        except Exception:
                            pass
                    else:
                        progress.failed_items += 1
                        results.append({
                            "sheet_id": sheet_file.file_id,
                            "sheet_name": sheet_file.sheet_name,
                            "status": "failed"
                        })
                        try:
                            await self.event_logger.emit(
                                EventType.SHEET_PARSE_COMPLETED,
                                "failed",
                                job_id=job.job_id,
                                file_id=file_id,
                                sheet_id=sheet_file.file_id
                            )
                        except Exception:
                            pass
                    
                    await self.update_job_status(job.job_id, JobStatus.RUNNING, progress=progress)
                    
                except Exception as sheet_error:
                    progress.failed_items += 1
                    results.append({
                        "sheet_id": sheet_file.file_id,
                        "sheet_name": sheet_file.sheet_name,
                        "status": "failed",
                        "error": str(sheet_error)
                    })
            
            # Job completed
            final_result = {
                "total_sheets": total_sheets,
                "successful_sheets": progress.completed_items,
                "failed_sheets": progress.failed_items,
                "results": results,
                "main_file_id": file_id
            }

            # Optional: if all sheets succeeded, delete parent Excel from disk only (keep DB record)
            if progress.failed_items == 0:
                try:
                    if main_file.file_path and Path(main_file.file_path).exists():
                        Path(main_file.file_path).unlink()
                        print(f"🧹 Deleted parent Excel from disk: {main_file.file_path}")
                        try:
                            await self.event_logger.emit(
                                EventType.SHEET_DELETED_DISK,
                                "success",
                                job_id=job.job_id,
                                file_id=file_id,
                                message="Deleted parent Excel from disk"
                            )
                        except Exception:
                            pass
                except Exception as parent_disk_err:
                    print(f"⚠️  Could not delete parent Excel {main_file.file_path}: {parent_disk_err}")
                # Keep DB record for tracking
                final_result["parent_deleted"] = {"disk": True, "db": False}
            
            await self.update_job_status(
                job.job_id, 
                JobStatus.COMPLETED, 
                progress=progress,
                result=final_result
            )
            
            print(f"✅ Excel processing job completed: {job.job_id}")
            
        except Exception as e:
            await self.update_job_status(
                job.job_id, 
                JobStatus.FAILED, 
                error_message=str(e)
            )
            raise

    async def _process_etf_holdings_job(self, job: BackgroundJob):
        """Process ETF holdings fetching job with smart caching"""
        from am_etf.service import ETFService
        from am_etf.smart_holdings_service import SmartETFHoldingsService
        
        # Initialize services
        etf_service = ETFService()
        holdings_service = SmartETFHoldingsService()
        
        input_data = job.input_data
        operation = input_data.get("operation", "fetch_all_holdings")
        force_refresh = input_data.get("force_refresh", False)
        
        # Configure cache policy
        holdings_service.set_cache_policy(expiry_days=1, force_refresh=force_refresh)
        
        try:
            if operation == "fetch_all_holdings":
                # Fetch holdings for all ETFs with ISINs
                limit = input_data.get("limit")
                
                # Get ETFs with ISINs
                all_etfs = await etf_service.list(limit=1000)
                etfs_with_isin = [etf for etf in all_etfs if etf.isin]
                
                if limit:
                    etfs_with_isin = etfs_with_isin[:limit]
                
                total_count = len(etfs_with_isin)
                progress = JobProgress(total_items=total_count, completed_items=0)
                await self.update_job_status(job.job_id, JobStatus.RUNNING, progress=progress)
                
                # Progress callback for smart fetch
                async def progress_callback(current, total, result):
                    progress.current_item = f"{result['symbol'] or result['isin']} ({'cache' if result['cache_hit'] else 'API'})"
                    progress.completed_items = current
                    await self.update_job_status(job.job_id, JobStatus.RUNNING, progress=progress)
                
                # Use smart bulk fetch
                summary = await holdings_service.bulk_smart_fetch(etfs_with_isin, progress_callback)
                
                final_result = {
                    "total_processed": summary["total_processed"],
                    "successful_fetches": summary["successful_fetches"],
                    "failed_fetches": summary["failed_fetches"],
                    "cache_hits": summary["cache_hits"],
                    "api_calls": summary["api_calls"],
                    "cache_hit_rate": summary.get("cache_hit_rate", "0%"),
                    "api_call_savings": summary.get("api_call_savings", "None"),
                    "results": summary["results"],
                    "operation": operation
                }
                
            elif operation == "fetch_single_holdings":
                # Fetch holdings for a single ETF
                symbol = input_data["symbol"]
                isin = input_data["isin"]
                etf_name = input_data.get("etf_name")
                
                progress = JobProgress(total_items=1, completed_items=0)
                progress.current_item = f"{symbol} ({isin})"
                await self.update_job_status(job.job_id, JobStatus.RUNNING, progress=progress)
                
                print(f"🔄 Smart fetching holdings for {symbol}")
                
                result = await holdings_service.smart_fetch_and_store_holdings(
                    isin=isin,
                    symbol=symbol,
                    etf_name=etf_name
                )
                
                progress.completed_items = 1
                if not result["success"]:
                    progress.failed_items = 1
                
                final_result = {
                    "symbol": symbol,
                    "isin": isin,
                    "success": result["success"],
                    "cache_hit": result["cache_hit"],
                    "api_called": result["api_called"],
                    "reason": result["reason"],
                    "holdings_count": result["holdings_count"],
                    "operation": operation
                }
            
            else:
                raise ValueError(f"Unknown ETF operation: {operation}")
            
            await self.update_job_status(
                job.job_id, 
                JobStatus.COMPLETED, 
                progress=progress,
                result=final_result
            )
            
            print(f"✅ ETF holdings job completed: {job.job_id}")
            print(f"📊 Cache performance: {final_result.get('cache_hit_rate', 'N/A')} hit rate, {final_result.get('api_call_savings', 'N/A')}")
            
        except Exception as e:
            await self.update_job_status(
                job.job_id, 
                JobStatus.FAILED, 
                error_message=str(e)
            )
            raise
        finally:
            await etf_service.close()
            await holdings_service.close()


# Global job queue instance
job_queue: Optional[JobQueue] = None


async def get_job_queue() -> JobQueue:
    """Get the global job queue instance"""
    global job_queue
    if job_queue is None:
        import os
        # Initialize with MongoDB connection from environment or default
        mongodb_uri = os.getenv("MONGO_URI", "mongodb://admin:password123@localhost:27017")
        db_name = os.getenv("MONGO_DB", "mutual_funds")
        job_queue = JobQueue(mongodb_uri, db_name)
        
        # Recover any stuck jobs from server restarts
        try:
            await job_queue.recover_stuck_jobs()
        except Exception as e:
            print(f"⚠️  Warning: Could not recover stuck jobs: {e}")
    
    return job_queue