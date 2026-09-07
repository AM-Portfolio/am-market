from am_news.application.process_raw import ProcessRaw
from am_news.domain.entities import Instrument, ProcessStatus
from am_news.domain.universe import NIFTY50_SYMBOLS
from am_news.schemas.news import ReplayResponse


class ReplayRaw:
    def __init__(self, container) -> None:
        self._c = container
        self._process = ProcessRaw(
            container.parse, container.articles, container.snapshots, NIFTY50_SYMBOLS
        )

    async def execute(self, raw_id: str | None = None) -> ReplayResponse:
        processed = 0
        failed = 0
        skipped = 0
        if raw_id:
            row = await self._c.raw.get(raw_id)
            if row is None or not row.body:
                return ReplayResponse(processed=0, failed=0, skipped=1)
            ok = await self._run_row(row.raw_id, row.body, row.instrument_keys)
            return ReplayResponse(processed=int(ok), failed=int(not ok), skipped=0)
        while True:
            row = await self._c.raw.claim_next((ProcessStatus.pending, ProcessStatus.failed))
            if row is None:
                break
            if not row.body:
                skipped += 1
                await self._c.raw.mark(row.raw_id, ProcessStatus.dead, "empty_body")
                continue
            if row.attempts >= 5:
                await self._c.raw.mark(row.raw_id, ProcessStatus.dead, row.error or "max_attempts")
                failed += 1
                continue
            ok = await self._run_row(row.raw_id, row.body, row.instrument_keys)
            processed += int(ok)
            failed += int(not ok)
        return ReplayResponse(processed=processed, failed=failed, skipped=skipped)

    async def _run_row(self, raw_id: str, body: dict, keys: tuple[str, ...]) -> bool:
        resolved = await self._c.resolver.resolve_nifty50()
        by_key = {item.instrument_key: item for item in resolved}
        instruments = tuple(by_key[key] for key in keys if key in by_key)
        if not instruments:
            instruments = tuple(Instrument(symbol="", instrument_key=key) for key in keys)
        try:
            await self._process.execute(body, instruments)
            await self._c.raw.mark(raw_id, ProcessStatus.processed)
            return True
        except Exception as exc:
            await self._c.raw.mark(raw_id, ProcessStatus.failed, str(exc))
            return False
