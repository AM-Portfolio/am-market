"""
Mutual Fund Persistence Service - Handle MongoDB operations for mutual fund data
"""
import sys
from pathlib import Path
from typing import List, Optional, Dict, Any
from datetime import datetime

# Add parent directory to path to find external modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_common.mutual_fund_models import MutualFundPortfolio, PortfolioSummary, Holding


class MutualFundService:
    """
    Service class for handling mutual fund portfolio operations
    Accepts MutualFundPortfolio model objects and handles persistence
    """
    
    def __init__(self, mongo_uri: str = "mongodb://localhost:27017", db_name: str = "mutual_funds"):
        self.mongo_uri = mongo_uri
        self.db_name = db_name
        self._client = None
        self._db = None
        self._collection = None

    def _get_collection(self):
        """Lazy initialization of MongoDB connection"""
        if self._collection is None:
            try:
                import motor.motor_asyncio
                self._client = motor.motor_asyncio.AsyncIOMotorClient(self.mongo_uri)
                self._db = self._client[self.db_name]
                self._collection = self._db.portfolios
            except ImportError:
                raise ImportError(
                    "MongoDB support requires 'motor' package. "
                    "Install with: pip install motor"
                )
        return self._collection

    @property
    def database(self):
        """Get the database instance"""
        if self._db is None:
            self._get_collection()  # Initialize connection
        return self._db

    async def save_portfolio_with_id(self, portfolio: MutualFundPortfolio, custom_id: str) -> str:
        """
        Save a mutual fund portfolio with a specific custom ID (to match sheet ID)
        
        Args:
            portfolio: MutualFundPortfolio model instance
            custom_id: Custom ID to use (e.g., sheet file ID)
            
        Returns:
            str: The custom ID used for the document
        """
        collection = self._get_collection()
        
        # Convert to MongoDB document
        doc = portfolio.to_mongo_document()
        doc["updated_at"] = datetime.now().isoformat()
        doc["_id"] = custom_id  # Use custom ID instead of auto-generated ObjectId
        doc["sheet_id"] = custom_id  # Also store as separate field for queries
        
        try:
            # Try to insert with custom ID
            await collection.insert_one(doc)
            print(f"✅ Portfolio inserted with custom ID: {custom_id}")
            return custom_id
        except Exception as e:
            # If ID already exists, update the document
            if "duplicate key" in str(e).lower():
                doc.pop("_id")  # Remove _id for update
                result = await collection.replace_one(
                    {"_id": custom_id}, 
                    doc
                )
                print(f"✅ Portfolio updated with custom ID: {custom_id}")
                return custom_id
            else:
                # If other error, fall back to auto-generated ID
                print(f"⚠️ Custom ID failed, using auto-generated: {e}")
                doc.pop("_id", None)
                result = await collection.insert_one(doc)
                return str(result.inserted_id)

    async def get_portfolio_by_id(self, portfolio_id: str) -> Optional[MutualFundPortfolio]:
        """
        Retrieve a portfolio by MongoDB document ID (supports both ObjectId and custom string IDs)
        
        Args:
            portfolio_id: MongoDB document ID (ObjectId string or custom string)
            
        Returns:
            MutualFundPortfolio instance or None if not found
        """
        collection = self._get_collection()
        
        try:
            # First try as string ID (for custom IDs)
            doc = await collection.find_one({"_id": portfolio_id})
            if doc:
                doc.pop("_id", None)
                return MutualFundPortfolio(**doc)
            
            # If not found, try as ObjectId (for auto-generated IDs)
            from bson import ObjectId
            try:
                doc = await collection.find_one({"_id": ObjectId(portfolio_id)})
                if doc:
                    doc.pop("_id", None)
                    return MutualFundPortfolio(**doc)
            except Exception:
                pass
                
        except Exception:
            pass
        return None

    async def close(self):
        """Close MongoDB connection"""
        if self._client:
            self._client.close()


# Factory function for easy instantiation
def create_mutual_fund_service(mongo_uri: str = "mongodb://localhost:27017", 
                              db_name: str = "mutual_funds") -> MutualFundService:
    """
    Factory function to create MutualFundService instance
    
    Args:
        mongo_uri: MongoDB connection URI
        db_name: Database name
        
    Returns:
        MutualFundService instance
    """
    return MutualFundService(mongo_uri, db_name)