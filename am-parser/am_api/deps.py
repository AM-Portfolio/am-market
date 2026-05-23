"""FastAPI dependencies for am-parser services."""
from typing import AsyncGenerator

from am_etf.holdings_service import ETFHoldingsService, create_etf_holdings_service
from am_etf.lookup_service import EtfLookupService
from am_etf.service import ETFService, create_etf_service
from am_etf.smart_holdings_service import (
    SmartETFHoldingsService,
    create_smart_etf_holdings_service,
)


async def get_etf_service() -> AsyncGenerator[ETFService, None]:
    service = create_etf_service()
    try:
        yield service
    finally:
        await service.close()


async def get_holdings_service() -> AsyncGenerator[ETFHoldingsService, None]:
    service = create_etf_holdings_service()
    try:
        yield service
    finally:
        await service.close()


async def get_smart_holdings_service() -> AsyncGenerator[SmartETFHoldingsService, None]:
    service = create_smart_etf_holdings_service()
    try:
        yield service
    finally:
        await service.close()


async def get_etf_lookup_service() -> AsyncGenerator[EtfLookupService, None]:
    etf_service = create_etf_service()
    holdings_service = create_etf_holdings_service()
    lookup = EtfLookupService(etf_service, holdings_service)
    try:
        yield lookup
    finally:
        await holdings_service.close()
        await etf_service.close()
