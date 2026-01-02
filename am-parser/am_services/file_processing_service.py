"""
File Processing Service
Orchestrates the complete file upload and processing workflow
"""
import asyncio
from typing import List, Optional, Dict, Any
from pathlib import Path
import json
import os

# Load environment variables
try:
    from dotenv import load_dotenv
    load_dotenv(override=True)
    print("✅ Environment variables loaded from .env file")
except ImportError:
    print("⚠️  python-dotenv not installed. Using system environment variables only.")

from am_services.file_upload_service import FileUploadService
from am_persistence.file_upload_repository import FileUploadRepository
from am_app.app import AMApp
from am_common.upload_models import FileUpload, ProcessingStatus, FileType
from am_persistence.mutual_fund_service import MutualFundService
from am_common.mutual_fund_models import MutualFundPortfolio
from am_services.event_logger import EventLogger
from am_common.event_models import EventType

# Import Together AI service
try:
    from am_llm.together_service import TogetherLLMService
    print("✅ TogetherLLMService imported successfully")
except ImportError as e:
    TogetherLLMService = None
    print(f"❌ TogetherLLMService import failed: {e}")


class FileProcessingService:
    """Service for processing uploaded files"""
    
    def __init__(self, file_upload_repo, mutual_fund_service):
        self.file_upload_repo = file_upload_repo
        self.mutual_fund_service = mutual_fund_service
        self.file_upload_service = FileUploadService()
        self.am_app = AMApp()
        # Initialize event logger (separate DB). Reuse main Mongo URI if available.
        try:
            mongo_uri = getattr(mutual_fund_service, 'mongo_uri', "mongodb://localhost:27017")
            self.event_logger = EventLogger(mongo_uri=mongo_uri, db_name="am_logs")
        except Exception:
            self.event_logger = None
    
    async def process_excel_file(self, file_id: str) -> bool:
        """Process an uploaded Excel file by splitting it into sheets"""
        try:
            # Get file upload record
            file_upload = await self.file_upload_repo.get_file_upload(file_id)
            if not file_upload:
                raise ValueError(f"File not found: {file_id}")
            
            if file_upload.file_type != FileType.EXCEL:
                raise ValueError("Can only process Excel files")
            
            # Update status to processing
            await self.file_upload_repo.update_file_status(
                file_id, ProcessingStatus.SPLITTING
            )
            
            # Split Excel into individual sheet files
            sheet_files = self.file_upload_service.split_excel_into_sheets(file_upload)
            
            # Save sheet files to database
            for sheet_file in sheet_files:
                await self.file_upload_repo.create_file_upload(sheet_file)
            # Emit split event
            try:
                if self.event_logger:
                    await self.event_logger.emit(
                        EventType.EXCEL_SPLIT,
                        "success",
                        file_id=file_id,
                        metadata={
                            "sheet_count": len(sheet_files),
                            "sheet_names": [sf.sheet_name for sf in sheet_files],
                            "sheet_ids": [sf.file_id for sf in sheet_files]
                        }
                    )
            except Exception:
                pass
            
            # Update parent file status
            await self.file_upload_repo.update_file_status(
                file_id, ProcessingStatus.COMPLETED
            )
            
            # Update processing metadata
            metadata = {
                "sheets_created": len(sheet_files),
                "sheet_names": [sf.sheet_name for sf in sheet_files],
                "sheet_ids": [sf.file_id for sf in sheet_files]
            }
            await self.file_upload_repo.update_processing_metadata(file_id, metadata)
            
            return True
            
        except Exception as e:
            # Update status to failed
            await self.file_upload_repo.update_file_status(
                file_id, ProcessingStatus.FAILED, str(e)
            )
            return False
    
    async def process_sheet_file(self, sheet_id: str, method: str = None) -> bool:
        """Process an individual sheet file to extract portfolio data"""
        try:
            # Get sheet file record
            sheet_file = await self.file_upload_repo.get_file_upload(sheet_id)
            if not sheet_file:
                raise ValueError(f"Sheet file not found: {sheet_id}")
            
            # Update status to processing
            await self.file_upload_repo.update_file_status(
                sheet_id, ProcessingStatus.PROCESSING
            )
            
            # Parse the sheet file using AMApp
            try:
                if self.event_logger:
                    await self.event_logger.emit(
                        EventType.SHEET_PARSE_STARTED,
                        "running",
                        sheet_id=sheet_id,
                        file_id=getattr(sheet_file, 'parent_file_id', None)
                    )
            except Exception:
                pass
            result = await self._parse_sheet_file(sheet_file, method)
            
            if result:
                # Transform the parser result to MutualFundPortfolio format
                portfolio_data = self._transform_to_mutual_fund_portfolio(result, sheet_file)
                
                # Convert result to MutualFundPortfolio object
                portfolio = MutualFundPortfolio(**portfolio_data)
                
                # 🎯 IMPORTANT: Use sheet_id as portfolio_id for proper tracking
                # This ensures portfolio ID matches sheet ID for easy lookup
                portfolio_id = await self.mutual_fund_service.save_portfolio_with_id(
                    portfolio, 
                    custom_id=sheet_id  # Use sheet ID as portfolio ID
                )
                
                print(f"✅ Portfolio saved with ID: {portfolio_id} (matches sheet ID: {sheet_id})")
                try:
                    if self.event_logger:
                        await self.event_logger.emit(
                            EventType.PORTFOLIO_SAVED,
                            "success",
                            sheet_id=sheet_id,
                            portfolio_id=portfolio_id,
                            metadata={
                                "mutual_fund_name": portfolio_data.get("mutual_fund_name"),
                                "portfolio_date": portfolio_data.get("portfolio_date"),
                                "total_holdings": portfolio_data.get("total_holdings", 0)
                            }
                        )
                except Exception:
                    pass
                
                # Update sheet file status and metadata
                metadata = {
                    "portfolio_id": portfolio_id,
                    "parsing_method": method,
                    "holdings_count": portfolio_data.get("total_holdings", 0),
                    "mutual_fund_name": portfolio_data.get("mutual_fund_name", "Unknown"),
                    "sheet_id_matches_portfolio_id": portfolio_id == sheet_id
                }
                await self.file_upload_repo.update_processing_metadata(sheet_id, metadata)
                await self.file_upload_repo.update_file_status(sheet_id, ProcessingStatus.PARSED)
                
                return {"portfolio_id": portfolio_id, "portfolio_data": portfolio_data}
            else:
                await self.file_upload_repo.update_file_status(
                    sheet_id, ProcessingStatus.FAILED, "Failed to parse sheet data"
                )
                try:
                    if self.event_logger:
                        await self.event_logger.emit(
                            EventType.SHEET_PARSE_COMPLETED,
                            "failed",
                            sheet_id=sheet_id
                        )
                except Exception:
                    pass
                return False
                
        except Exception as e:
            await self.file_upload_repo.update_file_status(
                sheet_id, ProcessingStatus.FAILED, str(e)
            )
            try:
                if self.event_logger:
                    await self.event_logger.emit(
                        EventType.SHEET_PARSE_COMPLETED,
                        "failed",
                        sheet_id=sheet_id,
                        message=str(e)
                    )
            except Exception:
                pass
            return False
    
    async def _parse_sheet_file(self, sheet_file: FileUpload, method: str = None) -> Optional[Dict[str, Any]]:
        """Parse a sheet file using the specified method"""
        try:
            # Get default method from environment or use "together"
            if method is None:
                method = os.getenv("DEFAULT_PARSE_METHOD", "together")
                print(f"🔧 Using default parse method from environment: {method}")
            
            print(f"🔄 Parse method: {method}")
            
            # Use AMApp to parse the file
            result = await asyncio.get_event_loop().run_in_executor(
                None, 
                self._sync_parse_file, 
                sheet_file.file_path, 
                method, 
                sheet_file.sheet_name
            )
            
            return result
            
        except Exception as e:
            print(f"❌ Error in _parse_sheet_file: {e}")
            # Fallback to manual parsing if Together AI fails
            print("🔄 Falling back to manual parsing...")
            try:
                result = await asyncio.get_event_loop().run_in_executor(
                    None, 
                    self._sync_parse_file, 
                    sheet_file.file_path, 
                    "manual", 
                    sheet_file.sheet_name
                )
                return result
            except Exception as fallback_error:
                print(f"❌ Manual fallback also failed: {fallback_error}")
                raise ValueError(f"Error parsing file: {str(e)}")
    
    def _sync_parse_file(self, file_path: str, method: str, sheet_name: Optional[str]) -> Dict[str, Any]:
        """Synchronous wrapper for parsing files"""
        print(f"🔄 Parsing {file_path} using {method} method, sheet: {sheet_name}")
        
        # Debug information
        print(f"🔍 Method: {method}, TogetherLLMService available: {TogetherLLMService is not None}")
        
        if method == "together" and TogetherLLMService:
            # Use Together AI service - it will get API key from environment
            try:
                print(f"🤖 Initializing Together AI service (using environment API key)...")
                together_service = TogetherLLMService()  # No API key needed - uses environment
                print(f"🧠 Calling Together AI extraction for sheet: {sheet_name}")
                print(f"📁 File path: {file_path}")
                print(f"📋 Sheet name: {sheet_name}")
                
                result = together_service.extract_portfolio_from_excel(
                    excel_file=file_path,
                    sheet_name=sheet_name
                )
                print(f"✅ Together AI parsing successful: {result.get('mutual_fund_name', 'Unknown')}")
                print(f"📊 Holdings count: {result.get('total_holdings', 0)}")
                print(f"🎯 Result type: {type(result)}")
                return result
            except Exception as e:
                error_msg = str(e)
                print(f"❌ Together AI parsing failed with error: {type(e).__name__}: {error_msg}")
                if "401" in error_msg or "invalid_api_key" in error_msg or "AuthenticationError" in str(type(e)):
                    print("💡 API Key Error: The Together AI key is invalid.")
                    print("🔗 Get a valid key at: https://api.together.ai/settings/api-keys")
                    print("🔄 Auto-switching to manual parsing...")
                    method = "manual"  # Switch to manual parsing
                else:
                    import traceback
                    print(f"🔍 Full traceback:\n{traceback.format_exc()}")
                    print("🔄 Falling back to AMApp manual parsing...")
                    method = "manual"  # Switch to manual parsing
        elif method == "together":
            print(f"❌ Together AI requirements not met:")
            print(f"   - TogetherLLMService available: {TogetherLLMService is not None}")
            print(f"   - Method is 'together': {method == 'together'}")
            print("🔄 Falling back to manual parsing...")
        else:
            print(f"📝 Using manual parsing method: {method}")
        
        # Use AMApp for manual or fallback parsing
        if sheet_name and file_path.endswith('.xlsx'):
            return self.am_app.parse_file(
                file_path, 
                method=method, 
                sheet=sheet_name
            )
        else:
            return self.am_app.parse_file(file_path, method=method)
    
    async def _process_single_sheet(self, sheet_file: FileUpload, method: str = None) -> Optional[Dict[str, Any]]:
        """Process a single sheet file (used by background jobs)"""
        try:
            # Parse the sheet file using AMApp
            result = await self._parse_sheet_file(sheet_file, method)
            
            if result:
                # Transform the parser result to MutualFundPortfolio format
                portfolio_data = self._transform_to_mutual_fund_portfolio(result, sheet_file)
                
                # Convert result to MutualFundPortfolio object
                portfolio = MutualFundPortfolio(**portfolio_data)
                
                # Use sheet_id as portfolio_id for proper tracking
                portfolio_id = await self.mutual_fund_service.save_portfolio_with_id(
                    portfolio, 
                    custom_id=sheet_file.file_id  # Use sheet ID as portfolio ID
                )
                
                print(f"✅ Portfolio saved with ID: {portfolio_id} (matches sheet ID: {sheet_file.file_id})")
                try:
                    if self.event_logger:
                        await self.event_logger.emit(
                            EventType.PORTFOLIO_SAVED,
                            "success",
                            sheet_id=sheet_file.file_id,
                            portfolio_id=portfolio_id,
                            metadata={
                                "mutual_fund_name": portfolio_data.get("mutual_fund_name"),
                                "portfolio_date": portfolio_data.get("portfolio_date"),
                                "total_holdings": portfolio_data.get("total_holdings", 0)
                            }
                        )
                except Exception:
                    pass
                
                # Update sheet file status and metadata
                metadata = {
                    "portfolio_id": portfolio_id,
                    "parsing_method": method,
                    "holdings_count": portfolio_data.get("total_holdings", 0),
                    "mutual_fund_name": portfolio_data.get("mutual_fund_name", "Unknown")
                }
                await self.file_upload_repo.update_processing_metadata(sheet_file.file_id, metadata)
                await self.file_upload_repo.update_file_status(sheet_file.file_id, ProcessingStatus.PARSED)
                
                # Cleanup: delete the sheet file from disk only (keep DB record for tracking)
                disk_deleted = False
                db_deleted = False
                try:
                    if sheet_file.file_path and os.path.exists(sheet_file.file_path):
                        os.remove(sheet_file.file_path)
                        disk_deleted = True
                        print(f"🧹 Deleted sheet file from disk: {sheet_file.file_path}")
                        try:
                            if self.event_logger:
                                await self.event_logger.emit(
                                    EventType.SHEET_DELETED_DISK,
                                    "success",
                                    sheet_id=sheet_file.file_id,
                                    file_id=getattr(sheet_file, 'parent_file_id', None)
                                )
                        except Exception:
                            pass
                except Exception as disk_err:
                    print(f"⚠️  Could not delete sheet file {sheet_file.file_path}: {disk_err}")

                # Persist deletion flags to metadata for acknowledgement
                try:
                    await self.file_upload_repo.update_processing_metadata(sheet_file.file_id, {
                        **(metadata or {}),
                        "deleted_from_disk": disk_deleted,
                        "deleted_from_db": False
                    })
                except Exception as meta_err:
                    print(f"⚠️  Could not update deletion metadata for {sheet_file.file_id}: {meta_err}")

                return {
                    "portfolio_id": portfolio_id,
                    "portfolio_data": portfolio_data,
                    "deleted": {"disk": disk_deleted, "db": False}
                }
            else:
                await self.file_upload_repo.update_file_status(
                    sheet_file.file_id, ProcessingStatus.FAILED, "Failed to parse sheet data"
                )
                try:
                    if self.event_logger:
                        await self.event_logger.emit(
                            EventType.SHEET_PARSE_COMPLETED,
                            "failed",
                            sheet_id=sheet_file.file_id
                        )
                except Exception:
                    pass
                return None
                
        except Exception as e:
            print(f"❌ Error in _process_single_sheet: {e}")
            await self.file_upload_repo.update_file_status(
                sheet_file.file_id, ProcessingStatus.FAILED, str(e)
            )
            try:
                if self.event_logger:
                    await self.event_logger.emit(
                        EventType.SHEET_PARSE_COMPLETED,
                        "failed",
                        sheet_id=sheet_file.file_id,
                        message=str(e)
                    )
            except Exception:
                pass
            return None
        """Get complete status information for a file and its sheets"""
        file_upload = await self.file_upload_repo.get_file_upload(file_id)
        if not file_upload:
            return None
        
        result = {
            "file_info": file_upload.dict(),
            "sheet_files": []
        }
        
        # If this is an Excel file, get its sheet files
        if file_upload.file_type == FileType.EXCEL:
            sheet_files = await self.file_upload_repo.get_files_by_parent_id(file_id)
            result["sheet_files"] = [sf.dict() for sf in sheet_files]
        
        return result
    
    async def process_all_sheets_for_file(self, file_id: str, method: str = "manual",
                                         api_key: Optional[str] = None) -> Dict[str, Any]:
        """Process all sheets for a given Excel file"""
        result = {
            "success": False,
            "processed_sheets": [],
            "failed_sheets": [],
            "total_sheets": 0
        }
        
        try:
            # Get all sheet files for this parent
            sheet_files = await self.file_upload_repo.get_files_by_parent_id(file_id)
            result["total_sheets"] = len(sheet_files)
            
            # Process each sheet
            for sheet_file in sheet_files:
                success = await self.process_sheet_file(sheet_file.file_id, method, api_key)
                if success:
                    result["processed_sheets"].append({
                        "sheet_id": sheet_file.file_id,
                        "sheet_name": sheet_file.sheet_name
                    })
                else:
                    result["failed_sheets"].append({
                        "sheet_id": sheet_file.file_id,
                        "sheet_name": sheet_file.sheet_name
                    })
            
            result["success"] = len(result["failed_sheets"]) == 0
            return result
            
        except Exception as e:
            result["error"] = str(e)
            return result

    def _transform_to_mutual_fund_portfolio(self, parser_result: Dict[str, Any], 
                                          sheet_file: "FileUpload") -> Dict[str, Any]:
        """
        Transform parser result to MutualFundPortfolio format
        
        Handles both:
        1. Manual parser format: {"fund": {...}, "holdings": [...], "totals": {...}}
        2. Together AI format: {"mutual_fund_name": "...", "portfolio_holdings": [...], ...}
        """
        try:
            # Check if this is already in Together AI format (has mutual_fund_name)
            if "mutual_fund_name" in parser_result and "portfolio_holdings" in parser_result:
                print("✅ Together AI format detected - using directly")
                # Already in correct format, just validate fields
                transformed = {
                    "mutual_fund_name": parser_result.get("mutual_fund_name", "Unknown Mutual Fund"),
                    "portfolio_date": parser_result.get("portfolio_date", "Unknown Date"),
                    "total_holdings": parser_result.get("total_holdings", 0),
                    "portfolio_holdings": parser_result.get("portfolio_holdings", [])
                }
                return transformed
            
            # Otherwise, it's manual parser format - transform it
            print("🔄 Manual parser format detected - transforming...")
            
            # Extract fund information
            fund_info = parser_result.get("fund", {})
            holdings = parser_result.get("holdings", [])
            
            # Transform holdings from parser format to API format
            portfolio_holdings = []
            for holding in holdings:
                # Parser holding: {"isin": "...", "name": "...", "weight": 1.23, ...}
                # API expects: {"name_of_instrument": "...", "isin_code": "...", "percentage_to_nav": "1.23%"}
                
                holding_data = {
                    "name_of_instrument": holding.get("name") or "Unknown",
                    "isin_code": holding.get("isin") or "Unknown", 
                    "percentage_to_nav": f"{holding.get('weight', 0.0):.4f}%" if holding.get('weight') is not None else "0.0000%"
                }
                portfolio_holdings.append(holding_data)
            
            # Generate mutual fund name from sheet name or fund info
            mutual_fund_name = fund_info.get("name")
            if not mutual_fund_name:
                # Use sheet name to generate fund name
                sheet_name = getattr(sheet_file, 'sheet_name', None) or getattr(sheet_file, 'original_filename', 'Unknown')
                if sheet_name and sheet_name != 'Unknown':
                    # Extract base name from filename
                    base_name = sheet_name.replace('.xlsx', '').replace('_', ' ')
                    mutual_fund_name = f"Portfolio {base_name}"
                else:
                    mutual_fund_name = "Unknown Mutual Fund"
            
            # Generate portfolio date
            portfolio_date = fund_info.get("report_date") or "Unknown Date"
            if portfolio_date == "Unknown Date":
                # Try to extract date from filename or use current date
                import datetime
                portfolio_date = datetime.datetime.now().strftime("%B %Y")
            
            # Build the transformed result
            transformed = {
                "mutual_fund_name": mutual_fund_name,
                "portfolio_date": portfolio_date,
                "total_holdings": len(holdings),
                "portfolio_holdings": portfolio_holdings
            }
            
            return transformed
            
        except Exception as e:
            # Return minimal valid structure if transformation fails
            print(f"⚠️ Transformation failed: {e}")
            return {
                "mutual_fund_name": "Unknown Mutual Fund",
                "portfolio_date": "Unknown Date", 
                "total_holdings": 0,
                "portfolio_holdings": []
            }