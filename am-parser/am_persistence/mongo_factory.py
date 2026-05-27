"""Shared async MongoDB client factory (one client per URI)."""
from typing import Dict, Optional

import motor.motor_asyncio

_clients: Dict[str, motor.motor_asyncio.AsyncIOMotorClient] = {}


def get_async_mongo_client(
    mongo_uri: str,
    *,
    direct_connection: bool = True,
    server_selection_timeout_ms: int = 20_000,
) -> motor.motor_asyncio.AsyncIOMotorClient:
    if mongo_uri not in _clients:
        _clients[mongo_uri] = motor.motor_asyncio.AsyncIOMotorClient(
            mongo_uri,
            directConnection=direct_connection,
            serverSelectionTimeoutMS=server_selection_timeout_ms,
        )
    return _clients[mongo_uri]


def get_database(
    mongo_uri: str,
    db_name: str,
    *,
    direct_connection: bool = True,
) -> motor.motor_asyncio.AsyncIOMotorDatabase:
    return get_async_mongo_client(
        mongo_uri, direct_connection=direct_connection
    )[db_name]


def close_mongo_client(mongo_uri: Optional[str] = None) -> None:
    """Close shared client(s). Call from app shutdown, not per HTTP request."""
    if mongo_uri:
        client = _clients.pop(mongo_uri, None)
        if client:
            client.close()
        return
    for client in _clients.values():
        client.close()
    _clients.clear()


def release_service_mongo_refs(service) -> None:
    """Drop cached collection/db refs without closing the shared pool."""
    service._client = None
    service._db = None
    if hasattr(service, "_collection"):
        service._collection = None
    if hasattr(service, "_holdings_collection"):
        service._holdings_collection = None
