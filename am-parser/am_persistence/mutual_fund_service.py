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
    
    def __init__(self, mongo_uri: str = None, db_name: str = None):
        """Initialize MutualFundService
        
        Args:
            mongo_uri: MongoDB connection URI (defaults to settings.mongo_uri)
            db_name: Database name (defaults to settings.mongo_db)
        """
        from am_configs.settings import settings
        
        self.mongo_uri = mongo_uri or settings.mongo_uri
        self.db_name = db_name or settings.mongo_db
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

    async def save_portfolio(self, portfolio: MutualFundPortfolio) -> str:
        """
        Save a mutual fund portfolio (auto-generated ID)
        
        Args:
            portfolio: MutualFundPortfolio model instance
            
        Returns:
            str: The database ID of the saved document
        """
        collection = self._get_collection()
        doc = portfolio.to_mongo_document()
        doc["updated_at"] = datetime.now().isoformat()
        
        result = await collection.insert_one(doc)
        return str(result.inserted_id)

    async def list_portfolios(self, fund_name: Optional[str] = None, limit: int = 50) -> List[PortfolioSummary]:
        """
        List portfolios with optional filtering
        
        Args:
            fund_name: Optional fund name to filter by
            limit: Max results
            
        Returns:
            List of PortfolioSummary objects
        """
        collection = self._get_collection()
        query = {}
        if fund_name:
            # Case-insensitive partial match
            query["mutual_fund_name"] = {"$regex": fund_name, "$options": "i"}
            
        cursor = collection.find(query).sort("portfolio_date", -1).limit(limit)
        
        results = []
        async for doc in cursor:
            try:
                doc.pop("_id", None)
                portfolio = MutualFundPortfolio(**doc)
                results.append(PortfolioSummary.from_portfolio(portfolio))
            except Exception as e:
                print(f"Error parsing portfolio for summary: {e}")
                continue
                
        return results

    async def get_all_portfolios(self, limit: int = 100) -> List[MutualFundPortfolio]:
        """Get all portfolios with full details"""
        collection = self._get_collection()
        cursor = collection.find({}).limit(limit)
        results = []
        async for doc in cursor:
            try:
                doc.pop("_id", None)
                results.append(MutualFundPortfolio(**doc))
            except Exception:
                continue
        return results

    async def get_holdings_by_isin(self, isin_code: str) -> List[Dict[str, Any]]:
        """
        Find all holdings across all portfolios matching an ISIN
        
        Args:
            isin_code: ISIN code to search for
            
        Returns:
            List of holdings with fund context
        """
        collection = self._get_collection()
        
        pipeline = [
            {"$unwind": "$portfolio_holdings"},
            {"$match": {"portfolio_holdings.isin_code": isin_code}},
            {"$project": {
                "fund_name": "$mutual_fund_name",
                "portfolio_date": "$portfolio_date",
                "holding": "$portfolio_holdings",
                "_id": 0
            }}
        ]
        
        results = []
        async for doc in collection.aggregate(pipeline):
            results.append({
                "fund_name": doc.get("fund_name"),
                "portfolio_date": doc.get("portfolio_date"),
                "name_of_instrument": doc.get("holding", {}).get("name_of_instrument"),
                "percentage_to_nav": doc.get("holding", {}).get("percentage_to_nav"),
                "isin_code": doc.get("holding", {}).get("isin_code")
            })
            
        return results

    async def get_fund_statistics(self, fund_name: str) -> Optional[Dict[str, Any]]:
        """
        Get statistics for a specific fund name
        """
        # Finds the latest portfolio for this fund
        collection = self._get_collection()
        
        doc = await collection.find_one(
            {"mutual_fund_name": fund_name},
            sort=[("portfolio_date", -1)]
        )
        
        if not doc:
            return None
            
        doc.pop("_id", None)
        portfolio = MutualFundPortfolio(**doc)
        summary = PortfolioSummary.from_portfolio(portfolio, top_n=20)
        
        return {
            "fund_name": portfolio.mutual_fund_name,
            "latest_portfolio_date": portfolio.portfolio_date,
            "total_holdings_count": portfolio.total_holdings,
            "top_10_concentration": sum(
                float(h.percentage_to_nav.rstrip('%')) 
                for h in summary.top_holdings
                if h.percentage_to_nav and '%' in h.percentage_to_nav
            )
        }

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
def create_mutual_fund_service(mongo_uri: str = None, 
                              db_name: str = None) -> MutualFundService:
    """
    Factory function to create MutualFundService instance
    
    Args:
        mongo_uri: MongoDB connection URI (defaults to settings.mongo_uri)
        db_name: Database name (defaults to settings.mongo_db)
        
    Returns:
        MutualFundService instance
    """
    return MutualFundService(mongo_uri, db_name)