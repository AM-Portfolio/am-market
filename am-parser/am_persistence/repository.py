from typing import Iterable, Optional

import sys
from pathlib import Path

# Add parent directory to path to find other external modules  
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_services import Portfolio


class PortfolioRepository:
    async def upsert(self, doc: Portfolio) -> str:
        raise NotImplementedError

    async def get(self, doc_id: str) -> Optional[Portfolio]:
        raise NotImplementedError

    async def list(self, limit: int = 100) -> Iterable[Portfolio]:
        raise NotImplementedError


class MongoPortfolioRepository(PortfolioRepository):
    def __init__(self, uri: str, db_name: str = "am_parser", collection: str = "portfolios"):
        try:
            import motor.motor_asyncio as motor  # type: ignore
        except Exception as e:  # pragma: no cover
            raise RuntimeError(
                "motor (MongoDB async driver) is required. Install with `pip install motor`"
            ) from e

        self._client = motor.AsyncIOMotorClient(uri)
        self._col = self._client[db_name][collection]

    async def upsert(self, doc: Portfolio) -> str:
        payload = doc.model_dump(exclude_none=True)
        _id = payload.get("meta", {}).get("_id")
        if _id:
            await self._col.update_one({"_id": _id}, {"$set": payload}, upsert=True)
            return str(_id)
        res = await self._col.insert_one(payload)
        return str(res.inserted_id)

    async def get(self, doc_id: str) -> Optional[Portfolio]:
        data = await self._col.find_one({"_id": doc_id})
        if not data:
            return None
        return Portfolio.model_validate(data)

    async def list(self, limit: int = 100):
        cursor = self._col.find({}).limit(limit)
        async for d in cursor:
            yield Portfolio.model_validate(d)
