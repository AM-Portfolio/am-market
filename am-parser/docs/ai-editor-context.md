# AI Editor Context

Purpose: Provide essential repository context and guardrails for any AI-assisted code edits.

## Project overview
- Name: AM Parser (Python)
- Repository: am-parser 
- Python Package: am_parser (underscores required for Python imports)
- Goal: Parse mutual fund Excel/CSV statements and extract normalized portfolio JSON via Manual and LLM methods.
- CLI entry: `python -m am_parser.cli` with commands `parse-manual`, `parse-llm`.

## Code structure
- `am_parser/parsers/`: `manual.py`, `llm.py` (exported in `__init__.py`).
- `am_parser/services/`: `manual_parser.py` (ManualParserService).
- `am_parser/llm/`: `client.py`, `parser.py` (LLM client factory + LLMParserService).
- `am_parser/utils/`: `excel.py` (tabular loader).
- `am_parser/config/`: `header_maps.yaml` (manual parsing header normalization).
- `am_parser/db/`: `models.py` (Pydantic), `repository.py` (repo interfaces + Mongo scaffold).
- `am_parser/cli/`: `main.py` (Click CLI), `__main__.py` to run module.
- `am_parser/api/`: `cli.py` (Click CLI implementation).
- `data/samples/`: CSV and notes.
- `tests/`: pytest tests.
- External modules (top-level):
  - `am-api/`: CLI entry point (`python -m am-api`).
  - `am-persistence/`: Repository exports.
  - `am-services/`: Manual parser service.
  - `am-llm/`: LLM client and parser.
  - `am-common/`: Shared models and utilities.
  - `am-configs/`: Header maps (preferred by manual parser).

## Conventions
- Python 3.11.
- Keep public API stable: `am_parser` exports `ManualParser`, `LLMParser` (legacy aliases for `ManualParserService`, `LLMParserService`).
- Prefer small, focused modules. Avoid side effects at import time.
- I/O:
  - Read Excel/CSV using `utils.excel.load_tabular` or `am-common.load_tabular`.
  - Config from `am_parser/config/header_maps.yaml` or `am-configs/header_maps.yaml` (YAML keys may need quoting, e.g., "%").
- CLI: use `click` (not argparse/typer) to remain consistent.
- Optional Mongo via `requirements-mongo.txt` (do not add mandatory motor dependency to base requirements).

## Quality gates
- Run tests: `python -m pytest -q`.
- For any new CLI option: `python -m am_parser.cli --help` should list it.
- Ensure imports resolve without optional extras installed.

### Code size and readability rules
- Max file length: 500 lines (prefer splitting modules when growing).
- Max function/method length: 50 lines (extract helpers or classes when needed).
- Keep cyclomatic complexity low (prefer early returns, small helpers).
- Write docstrings for public functions and classes.
- Use type hints for public functions and data models.
- Avoid broad exceptions; catch specific errors.
- Log actionable messages; avoid printing in libraries (CLI may print).

### Production readiness
- Input validation and clear error messages at API boundaries.
- No hardcoded secrets/paths; use env/config.
- Deterministic behavior with tests for core flows.
- Feature flags or env toggles for optional providers/integrations.
- Backward-compatible changes to public APIs; bump version when breaking.

## Do / Don’t
- Do: add small tests for new behavior.
- Do: keep CSV and Excel support both working.
- Do: keep LLM path safe when no provider keys are set (dry-run and heuristic fallback).
- Don’t: break `parsers/__init__.py` exports.
- Don’t: hardcode absolute file paths; use `Path` relative to module files where needed.
- Don’t: require Mongo packages unless explicitly installed.

## Security & config
- Use environment variables for secrets (e.g., `OPENAI_API_KEY`, `LLM_PROVIDER`).
- Never commit secrets.

## Future integrations
- LLM providers: OpenAI via JSON schema; others pluggable through `services.llm.get_llm_client`.
- Mongo persistence: `MongoPortfolioRepository` (async, motor).

## Contact points
- Main workflows to update when changing schema: `parsers/manual.py`, `parsers/llm.py`, `services/llm.py`, and relevant tests.
