"""
Event Log Persistence Service
Stores processing events in a separate MongoDB database.
"""
import sys
from pathlib import Path
from typing import Optional

sys.path.insert(0, str(Path(__file__).parent.parent))

from am_common.event_models import ProcessingEvent


class EventLogService:
    def __init__(self, mongo_uri: str = "mongodb://localhost:27017", db_name: str = "am_logs"):
        self.mongo_uri = mongo_uri
        self.db_name = db_name
        self._client = None
        self._db = None
        self._collection = None

    def _get_collection(self):
        if self._collection is None:
            try:
                import motor.motor_asyncio
                self._client = motor.motor_asyncio.AsyncIOMotorClient(self.mongo_uri)
                self._db = self._client[self.db_name]
                self._collection = self._db.processing_events
                # Helpful indexes
                self._collection.create_index("timestamp")
                self._collection.create_index([("job_id", 1), ("sheet_id", 1)])
                self._collection.create_index([("event_type", 1), ("status", 1)])
            except ImportError:
                raise ImportError("Event logging requires 'motor' package. Install with: pip install motor")
        return self._collection

    @property
    def database(self):
        if self._db is None:
            self._get_collection()
        return self._db

    async def write_event(self, event: ProcessingEvent):
        collection = self._get_collection()
        await collection.insert_one(event.to_mongo_document())

    async def close(self):
        if self._client:
            self._client.close()


def create_event_log_service(mongo_uri: str = "mongodb://localhost:27017", db_name: str = "am_logs") -> EventLogService:
    return EventLogService(mongo_uri, db_name)
