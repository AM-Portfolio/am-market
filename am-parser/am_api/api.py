"""
FastAPI REST API for Mutual Fund Portfolio Service
Provides HTTP endpoints for saving and retrieving mutual fund portfolio data
"""

from fastapi import FastAPI, HTTPException, status, Depends, UploadFile, File, Form, BackgroundTasks, APIRouter
from fastapi.responses import JSONResponse
from typing import List, Optional
import sys
from pathlib import Path
import asyncio
from contextlib import asynccontextmanager
import os

# Add parent directory to path to find external modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_persistence import create_mutual_fund_service, MutualFundService
from am_common.mutual_fund_models import MutualFundPortfolio, PortfolioSummary
from am_common.upload_models import (
    FileUpload, FileUploadResponse, FileListResponse, 
    FileProcessingRequest, ProcessingStatus
)
from am_services.file_upload_service import FileUploadService
from am_services.file_processing_service import FileProcessingService
from am_persistence.file_upload_repository import FileUploadRepository

# Import job API
from am_api.job_api import router as job_router
from am_api.etf_api import router as etf_router
from am_services.job_queue_service import get_job_queue


# Global service instances
service_instance: Optional[MutualFundService] = None
file_upload_service: Optional[FileUploadService] = None
background_processor_task: Optional[asyncio.Task] = None
file_processing_service: Optional[FileProcessingService] = None
file_upload_repo: Optional[FileUploadRepository] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Manage application lifecycle - startup and shutdown"""
    global service_instance, file_upload_service, file_processing_service, file_upload_repo, background_processor_task
    
    # Startup: Initialize services
    try:
        # Get MongoDB connection details from environment variables
        mongo_uri = os.getenv("MONGO_URI", "mongodb://admin:password123@localhost:27017")
        mongo_db = os.getenv("MONGO_DB", "mutual_funds")
        
        print(f"🔌 Connecting to MongoDB: {mongo_uri}")
        
        service_instance = create_mutual_fund_service(
            mongo_uri=mongo_uri,
            db_name=mongo_db
        )
        
        # Initialize file upload services
        file_upload_service = FileUploadService()
        file_upload_repo = FileUploadRepository(service_instance.database)
        file_processing_service = FileProcessingService(
            file_upload_repo, 
            service_instance
        )
        
        # Start background job processor
        job_queue = await get_job_queue()
        background_processor_task = asyncio.create_task(job_queue.start_job_processor())
        print("✅ Started background job processor")
        
        print("✅ Connected to MongoDB")
        print("✅ Initialized file upload services")
        yield
    except Exception as e:
        print(f"❌ Failed to initialize services: {e}")
        raise
    finally:
        # Shutdown: Close services
        if background_processor_task:
            background_processor_task.cancel()
            print("🔐 Background job processor stopped")
        if service_instance:
            await service_instance.close()
            print("🔐 MongoDB connection closed")


# Create FastAPI app with lifecycle management
app = FastAPI(
    title="Mutual Fund Portfolio API",
    description="REST API for managing mutual fund portfolio data",
    version="1.0.0",
    lifespan=lifespan
)

from fastapi.middleware.cors import CORSMiddleware
app.add_middleware(
    CORSMiddleware,
    allow_origin_regex="https?://.*", # Flexible for dev/prod while allowing credentials
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(job_router, prefix="/v1")
app.include_router(etf_router, prefix="/v1")





def get_service() -> MutualFundService:
    """Dependency to get the service instance"""
    if service_instance is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Database service not available"
        )
    return service_instance


def get_file_upload_service() -> FileUploadService:
    """Dependency to get the file upload service instance"""
    if file_upload_service is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="File upload service not available"
        )
    return file_upload_service


def get_file_upload_repo() -> FileUploadRepository:
    """Dependency to get the file upload repository instance"""
    if file_upload_repo is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="File upload repository not available"
        )
    return file_upload_repo


def get_file_processing_service() -> FileProcessingService:
    """Dependency to get the file processing service instance"""
    if file_processing_service is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="File processing service not available"
        )
    return file_processing_service


# Create Router for Mutual Fund endpoints
mutual_fund_router = APIRouter(tags=["Mutual Funds"])


@app.get("/", response_model=dict)
async def root():
    """Root endpoint with API information"""
    return {
        "message": "Mutual Fund Portfolio API",
        "version": "1.0.0",
        "endpoints": {
            "save_portfolio": "POST /portfolios",
            "get_portfolio": "GET /portfolios/{portfolio_id}",
            "list_portfolios": "GET /portfolios",
            "search_portfolios": "GET /portfolios/search?fund_name=...",
            "get_holdings_by_isin": "GET /holdings/{isin_code}",
            "get_fund_statistics": "GET /funds/{fund_name}/statistics"
        }
    }


@mutual_fund_router.post("/portfolios", response_model=dict, status_code=status.HTTP_201_CREATED)
async def save_portfolio(
    portfolio_data: dict,
    service: MutualFundService = Depends(get_service)
):
    """
    Save a mutual fund portfolio to the database
    
    Args:
        portfolio_data: JSON data containing mutual fund portfolio information
        
    Returns:
        Saved portfolio data with database ID
    """
    try:
        # Validate and create portfolio model
        portfolio = MutualFundPortfolio(**portfolio_data)
        
        # Save to database via service
        portfolio_id = await service.save_portfolio(portfolio)
        
        # Retrieve the saved portfolio to return complete data
        saved_portfolio = await service.get_portfolio_by_id(portfolio_id)
        
        if not saved_portfolio:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to retrieve saved portfolio"
            )
        
        return {
            "status": "success",
            "message": "Portfolio saved successfully",
            "data": {
                "id": portfolio_id,
                "mutual_fund_name": saved_portfolio.mutual_fund_name,
                "portfolio_date": saved_portfolio.portfolio_date,
                "total_holdings": saved_portfolio.total_holdings,
                "created_at": saved_portfolio.created_at.isoformat() if saved_portfolio.created_at else None,
                "updated_at": saved_portfolio.updated_at.isoformat() if saved_portfolio.updated_at else None
            },
            "portfolio": saved_portfolio.model_dump()
        }
        
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Invalid portfolio data: {str(e)}"
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to save portfolio: {str(e)}"
        )


@mutual_fund_router.get("/portfolios/{portfolio_id}", response_model=dict)
async def get_portfolio(
    portfolio_id: str,
    service: MutualFundService = Depends(get_service)
):
    """
    Get a specific portfolio by ID
    
    Args:
        portfolio_id: MongoDB ObjectId of the portfolio
        
    Returns:
        Portfolio data if found
    """
    try:
        portfolio = await service.get_portfolio_by_id(portfolio_id)
        
        if not portfolio:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Portfolio with ID {portfolio_id} not found"
            )
        
        return {
            "status": "success",
            "data": {
                "id": portfolio_id,
                "mutual_fund_name": portfolio.mutual_fund_name,
                "portfolio_date": portfolio.portfolio_date,
                "total_holdings": portfolio.total_holdings
            },
            "portfolio": portfolio.model_dump()
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to retrieve portfolio: {str(e)}"
        )


@mutual_fund_router.get("/portfolios", response_model=dict)
async def list_portfolios(
    fund_name: Optional[str] = None,
    limit: int = 50,
    service: MutualFundService = Depends(get_service)
):
    """
    List all portfolios or filter by fund name
    
    Args:
        fund_name: Optional fund name to filter by
        limit: Maximum number of portfolios to return (default: 50)
        
    Returns:
        List of portfolio summaries
    """
    try:
        portfolios = await service.list_portfolios(fund_name=fund_name, limit=limit)
        
        return {
            "status": "success",
            "count": len(portfolios),
            "data": [
                {
                    "fund_name": p.fund_name,
                    "portfolio_date": p.portfolio_date,
                    "total_holdings": p.total_holdings,
                    "total_percentage": p.total_percentage,
                    "top_holdings_count": len(p.top_holdings)
                } for p in portfolios
            ]
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to list portfolios: {str(e)}"
        )


@mutual_fund_router.get("/portfolios/search", response_model=dict)
async def search_portfolios(
    fund_name: str,
    service: MutualFundService = Depends(get_service)
):
    """
    Search portfolios by fund name
    
    Args:
        fund_name: Fund name to search for
        
    Returns:
        List of matching portfolio summaries
    """
    try:
        portfolios = await service.list_portfolios(fund_name=fund_name)
        
        return {
            "status": "success",
            "query": fund_name,
            "count": len(portfolios),
            "data": [
                {
                    "fund_name": p.fund_name,
                    "portfolio_date": p.portfolio_date,
                    "total_holdings": p.total_holdings,
                    "total_percentage": p.total_percentage
                } for p in portfolios
            ]
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to search portfolios: {str(e)}"
        )


@mutual_fund_router.get("/holdings/{isin_code}", response_model=dict)
async def get_holdings_by_isin(
    isin_code: str,
    service: MutualFundService = Depends(get_service)
):
    """
    Get all holdings with specific ISIN code
    
    Args:
        isin_code: ISIN code to search for
        
    Returns:
        List of holdings with the specified ISIN
    """
    try:
        holdings = await service.get_holdings_by_isin(isin_code)
        
        return {
            "status": "success",
            "isin_code": isin_code,
            "count": len(holdings),
            "data": holdings
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get holdings: {str(e)}"
        )


@mutual_fund_router.get("/funds/{fund_name}/statistics", response_model=dict)
async def get_fund_statistics(
    fund_name: str,
    service: MutualFundService = Depends(get_service)
):
    """
    Get statistics for a specific fund
    
    Args:
        fund_name: Name of the mutual fund
        
    Returns:
        Fund statistics
    """
    try:
        stats = await service.get_fund_statistics(fund_name)
        
        if not stats:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"No statistics found for fund: {fund_name}"
            )
        
        return {
            "status": "success",
            "fund_name": fund_name,
            "statistics": stats
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get fund statistics: {str(e)}"
        )


@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    """Global exception handler"""
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "status": "error",
            "message": "Internal server error",
            "detail": str(exc) if app.debug else "An unexpected error occurred"
        }
    )


# Health check endpoint
@app.get("/health", response_model=dict)
async def health_check(service: MutualFundService = Depends(get_service)):
    """Health check endpoint"""
    try:
        # Test database connection
        collection = service._get_collection()
        count = await collection.count_documents({})
        
        return {
            "status": "healthy",
            "database": "connected",
            "total_portfolios": count
        }
    except Exception as e:
        return JSONResponse(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            content={
                "status": "unhealthy",
                "database": "disconnected",
                "error": str(e)
            }
        )


# ================================
# FILE UPLOAD ENDPOINTS
# ================================

@mutual_fund_router.post("/upload", response_model=dict)
async def upload_file(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    parse_method: str = Form(default="together")
):
    """
    Upload an Excel file and do ALL the work automatically:
    1. Upload and persist main file to database
    2. Split Excel into individual sheet files  
    3. Persist all sheet files to database
    4. Parse each sheet and save portfolios to database
    
    - **file**: Excel file to upload (.xlsx, .xls)
    - **parse_method**: Parsing method ("manual" or "together") - defaults to "together"
    
    Returns complete processing results with all parsed portfolios
    """
    try:
        # Validate file type
        if not file.filename.lower().endswith(('.xlsx', '.xls')):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Only Excel files (.xlsx, .xls) are supported"
            )
        
        print(f"🔧 Using parse method: {parse_method}")
        
        # 1. Save uploaded file
        file_upload = await file_upload_service.save_uploaded_file(file)
        
        # 2. Save main file to database
        await file_upload_repo.create_file_upload(file_upload)
        print(f"✅ Main file persisted: {file_upload.file_id}")
        
        # 3. Get sheet information before processing
        sheet_infos = []
        if file_upload.file_type.value == "excel":
            try:
                sheet_infos = file_upload_service.get_excel_sheet_info(file_upload.file_path)
            except Exception as e:
                print(f"Warning: Could not read sheet info: {e}")
        
        # 4. Process Excel file (split into sheets and persist all)
        await file_processing_service.process_excel_file(file_upload.file_id)
        print(f"✅ Excel processed and sheets split")
        
        # 5. Get all sheet files created
        sheet_files = await file_upload_repo.get_files_by_parent_id(file_upload.file_id)
        print(f"✅ Found {len(sheet_files)} sheet files")
        
        # 6. Parse each sheet and save portfolios
        parsed_portfolios = []
        parsing_errors = []
        
        for sheet_file in sheet_files:
            try:
                # Parse the sheet file (no API key needed - service uses environment)
                result = await file_processing_service.process_sheet_file(
                    sheet_file.file_id, 
                    parse_method
                )
                
                if result and "portfolio_id" in result:
                    # Get the saved portfolio
                    portfolio = await service_instance.get_portfolio_by_id(result["portfolio_id"])
                    if portfolio:
                        parsed_portfolios.append({
                            "sheet_name": sheet_file.original_filename.replace('.xlsx', ''),
                            "portfolio_id": result["portfolio_id"],
                            "mutual_fund_name": portfolio.mutual_fund_name,
                            "total_holdings": portfolio.total_holdings,
                            "portfolio_date": portfolio.portfolio_date
                        })
                        print(f"✅ Parsed and saved: {sheet_file.original_filename}")
                    else:
                        parsing_errors.append({
                            "sheet_name": sheet_file.original_filename,
                            "error": "Portfolio saved but could not retrieve"
                        })
                else:
                    parsing_errors.append({
                        "sheet_name": sheet_file.original_filename,
                        "error": "Failed to parse portfolio data"
                    })
                    
            except Exception as e:
                parsing_errors.append({
                    "sheet_name": sheet_file.original_filename,
                    "error": str(e)
                })
                print(f"❌ Failed to parse {sheet_file.original_filename}: {e}")
        
        # 7. Return comprehensive results
        return {
            "status": "success",
            "message": "Complete upload and processing workflow finished",
            "main_file": {
                "file_id": file_upload.file_id,
                "filename": file_upload.original_filename,
                "status": "COMPLETED"
            },
            "sheets_processed": len(sheet_files),
            "sheets_info": [
                {
                    "sheet_name": si.sheet_name,
                    "rows": si.row_count,
                    "columns": si.column_count
                }
                for si in sheet_infos
            ],
            "portfolios_parsed": len(parsed_portfolios),
            "parsed_portfolios": parsed_portfolios,
            "parsing_errors": parsing_errors,
            "parse_method": parse_method,
            "workflow_steps": [
                "✅ File uploaded and saved to database",
                "✅ Excel file split into individual sheet files", 
                "✅ All sheet files saved to database",
                f"✅ {len(parsed_portfolios)} portfolios parsed and saved",
                f"⚠️ {len(parsing_errors)} parsing errors" if parsing_errors else "✅ All sheets parsed successfully"
            ]
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to complete upload workflow: {str(e)}"
        )


@mutual_fund_router.post("/upload/excel", response_model=dict)
async def upload_excel_complete(
    file: UploadFile = File(...),
    parse_method: str = Form(default="together"),
    upload_service: FileUploadService = Depends(get_file_upload_service),
    upload_repo: FileUploadRepository = Depends(get_file_upload_repo),
    processing_service: FileProcessingService = Depends(get_file_processing_service),
    mutual_fund_service: MutualFundService = Depends(get_service)
):
    """
    🚀 Complete Excel Upload Workflow - Does EVERYTHING automatically!
    
    This endpoint handles the complete workflow:
    1. ✅ Upload Excel file
    2. ✅ Persist main file to database  
    3. ✅ Split Excel into individual sheet files
    4. ✅ Persist all sheet files to database
    5. ✅ Parse each sheet using manual or LLM parsing
    6. ✅ Save all parsed portfolios to database
    
    - **file**: Excel file to upload (.xlsx, .xls)
    - **parse_method**: "together" (default) or "manual"
    
    Returns: Complete results with all parsed portfolios
    """
    try:
        # Validate file type
        if not file.filename.lower().endswith(('.xlsx', '.xls')):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Only Excel files (.xlsx, .xls) are supported"
            )
        
        print(f"🚀 Starting complete Excel workflow for: {file.filename}")
        print(f"� Using parse method: {parse_method}")
        
        # Step 1: Upload and persist main file
        file_upload = await upload_service.save_uploaded_file(file)
        await upload_repo.create_file_upload(file_upload)
        print(f"✅ Step 1: Main file uploaded and persisted ({file_upload.file_id})")
        
        # Step 2: Get sheet information
        sheet_infos = []
        if file_upload.file_type.value == "excel":
            try:
                sheet_infos = upload_service.get_excel_sheet_info(file_upload.file_path)
                print(f"✅ Step 2: Found {len(sheet_infos)} sheets in Excel file")
            except Exception as e:
                print(f"⚠️ Warning: Could not read sheet info: {e}")
        
        # Step 3: Process Excel (split and persist sheets)
        await processing_service.process_excel_file(file_upload.file_id)
        print(f"✅ Step 3: Excel split into individual sheet files")
        
        # Step 4: Get all created sheet files
        sheet_files = await upload_repo.get_files_by_parent_id(file_upload.file_id)
        print(f"✅ Step 4: {len(sheet_files)} sheet files persisted to database")
        
        # Step 5: Parse each sheet and save portfolios
        parsed_portfolios = []
        parsing_errors = []
        
        for i, sheet_file in enumerate(sheet_files, 1):
            sheet_name = sheet_file.original_filename.replace('.xlsx', '')
            try:
                print(f"🔄 Step 5.{i}: Parsing sheet '{sheet_name}' using {parse_method} method...")
                
                result = await processing_service.process_sheet_file(
                    sheet_file.file_id, 
                    parse_method
                )
                
                if result and "portfolio_id" in result:
                    # Get the saved portfolio
                    portfolio = await mutual_fund_service.get_portfolio_by_id(result["portfolio_id"])
                    if portfolio:
                        parsed_portfolios.append({
                            "sheet_name": sheet_name,
                            "portfolio_id": result["portfolio_id"],
                            "mutual_fund_name": portfolio.mutual_fund_name,
                            "total_holdings": portfolio.total_holdings,
                            "portfolio_date": portfolio.portfolio_date,
                            "parse_method": parse_method
                        })
                        print(f"✅ Step 5.{i}: Successfully parsed and saved '{sheet_name}'")
                    else:
                        parsing_errors.append({
                            "sheet_name": sheet_name,
                            "error": "Portfolio saved but could not retrieve"
                        })
                        print(f"❌ Step 5.{i}: Failed to retrieve saved portfolio for '{sheet_name}'")
                else:
                    parsing_errors.append({
                        "sheet_name": sheet_name,
                        "error": "Failed to parse portfolio data"
                    })
                    print(f"❌ Step 5.{i}: Failed to parse '{sheet_name}'")
                    
            except Exception as e:
                parsing_errors.append({
                    "sheet_name": sheet_name,
                    "error": str(e)
                })
                print(f"❌ Step 5.{i}: Error parsing '{sheet_name}': {e}")
        
        # Final results
        success_count = len(parsed_portfolios)
        error_count = len(parsing_errors)
        total_sheets = len(sheet_files)
        
        print(f"🎉 Workflow complete! {success_count}/{total_sheets} sheets parsed successfully")
        
        return {
            "success": True,
            "message": f"Complete Excel processing finished! {success_count}/{total_sheets} sheets parsed successfully",
            "summary": {
                "main_file_id": file_upload.file_id,
                "original_filename": file_upload.original_filename,
                "total_sheets": total_sheets,
                "successfully_parsed": success_count,
                "parsing_errors": error_count,
                "parse_method": parse_method
            },
            "sheets_info": [
                {
                    "sheet_name": si.sheet_name,
                    "rows": si.row_count,
                    "columns": si.column_count
                }
                for si in sheet_infos
            ],
            "parsed_portfolios": parsed_portfolios,
            "parsing_errors": parsing_errors if parsing_errors else None,
            "workflow_steps": [
                "✅ Excel file uploaded and saved to database",
                "✅ Excel file split into individual sheet files", 
                "✅ All sheet files saved to database",
                f"✅ {success_count} portfolios successfully parsed and saved",
                f"⚠️ {error_count} parsing errors" if error_count > 0 else "✅ All sheets parsed successfully"
            ]
        }
        
    except Exception as e:
        print(f"❌ Complete workflow failed: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Complete Excel workflow failed: {str(e)}"
        )


@mutual_fund_router.post("/process/{file_id}")
async def process_file(
    file_id: str,
    background_tasks: BackgroundTasks
):
    """
    Process an uploaded Excel file by splitting it into individual sheet files
    
    - **file_id**: ID of the uploaded Excel file
    
    This endpoint splits the Excel file into individual sheet files and stores them
    """
    try:
        # Check if file exists
        file_upload = await file_upload_repo.get_file_upload(file_id)
        if not file_upload:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"File not found: {file_id}"
            )
        
        # Schedule background processing
        background_tasks.add_task(
            file_processing_service.process_excel_file,
            file_id
        )
        
        return {
            "message": "File processing started",
            "file_id": file_id,
            "status": "processing"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to process file: {str(e)}"
        )


@mutual_fund_router.post("/parse/{sheet_id}")
async def parse_sheet(
    sheet_id: str,
    background_tasks: BackgroundTasks,
    method: str = Form(default="manual"),
    api_key: Optional[str] = Form(default=None)
):
    """
    Parse an individual sheet file to extract portfolio data
    
    - **sheet_id**: ID of the sheet file to parse
    - **method**: Parsing method (manual, llm, together)
    - **api_key**: API key for LLM parsing (required for 'together' method)
    """
    try:
        # Validate method
        if method not in ["manual", "llm", "together"]:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Method must be one of: manual, llm, together"
            )
        
        # Validate API key for together method
        if method == "together" and not api_key:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="API key is required for 'together' method"
            )
        
        # Check if sheet exists
        sheet_file = await file_upload_repo.get_file_upload(sheet_id)
        if not sheet_file:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Sheet file not found: {sheet_id}"
            )
        
        # Schedule background parsing
        background_tasks.add_task(
            file_processing_service.process_sheet_file,
            sheet_id,
            method,
            api_key
        )
        
        return {
            "message": "Sheet parsing started",
            "sheet_id": sheet_id,
            "method": method,
            "status": "processing"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to parse sheet: {str(e)}"
        )


@mutual_fund_router.get("/files", response_model=FileListResponse)
async def list_files(
    skip: int = 0,
    limit: int = 100,
    status_filter: Optional[str] = None
):
    """
    List uploaded files with optional filtering
    
    - **skip**: Number of records to skip (for pagination)
    - **limit**: Maximum number of records to return
    - **status_filter**: Filter by processing status
    """
    try:
        # Validate status filter
        status_enum = None
        if status_filter:
            try:
                status_enum = ProcessingStatus(status_filter)
            except ValueError:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Invalid status filter: {status_filter}"
                )
        
        files = await file_upload_repo.get_all_files(skip, limit, status_enum)
        total_count = await file_upload_repo.count_files(status_enum)
        
        return FileListResponse(
            files=files,
            total_count=total_count
        )
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to list files: {str(e)}"
        )


@mutual_fund_router.get("/files/{file_id}")
async def get_file_status(file_id: str):
    """
    Get detailed status information for a file and its sheets
    
    - **file_id**: ID of the file to check
    """
    try:
        file_status = await file_processing_service.get_file_status(file_id)
        if not file_status:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"File not found: {file_id}"
            )
        
        return file_status
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get file status: {str(e)}"
        )


@mutual_fund_router.post("/parse-all/{file_id}")
async def parse_all_sheets(
    file_id: str,
    background_tasks: BackgroundTasks,
    method: str = Form(default="manual"),
    api_key: Optional[str] = Form(default=None)
):
    """
    Parse all sheets for a given Excel file
    
    - **file_id**: ID of the Excel file
    - **method**: Parsing method (manual, llm, together)
    - **api_key**: API key for LLM parsing
    """
    try:
        # Validate method
        if method not in ["manual", "llm", "together"]:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Method must be one of: manual, llm, together"
            )
        
        # Validate API key for together method
        if method == "together" and not api_key:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="API key is required for 'together' method"
            )
        
        # Check if file exists
        file_upload = await file_upload_repo.get_file_upload(file_id)
        if not file_upload:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"File not found: {file_id}"
            )
        
        # Schedule background processing
        background_tasks.add_task(
            file_processing_service.process_all_sheets_for_file,
            file_id,
            method,
            api_key
        )
        
        return {
            "message": "Processing all sheets started",
            "file_id": file_id,
            "method": method,
            "status": "processing"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to process all sheets: {str(e)}"
        )



# Include the mutual fund router last, after all endpoints are defined
app.include_router(mutual_fund_router, prefix="/v1")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "api:app",
        host="127.0.0.1",
        port=8000,
        reload=True,
        log_level="info"
    )