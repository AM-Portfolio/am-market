import json
import sys
from pathlib import Path

import click

# Add parent directory to path to find other external modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_services import ManualParserService
from am_llm import LLMParserService


@click.group()
def cli():
    """AM Parser CLI"""


def _write_output(data: dict, out: str | None):
    text = json.dumps(data, indent=2, ensure_ascii=False)
    if out:
        Path(out).write_text(text, encoding="utf-8")
        click.echo(f"Wrote {out}")
    else:
        click.echo(text)


@cli.command(name="parse-manual")
@click.option("--input", "input_path", type=click.Path(exists=True, dir_okay=False), required=True, help="Path to Excel file")
@click.option("--sheet", default=None, help="Worksheet name or index")
@click.option("--out", "out_path", default=None, help="Output JSON file path")
@click.option("--header-map", default=None, help="Header map key in YAML config")
@click.option("--show-preview", is_flag=True, help="Print detected columns and first rows")
def parse_manual(input_path: str, sheet: str | int | None, out_path: str | None, header_map: str | None, show_preview: bool):
    parser = ManualParserService(header_map_key=header_map)
    result = parser.parse(input_path, sheet=sheet, show_preview=show_preview)
    _write_output(result, out_path)


@cli.command(name="parse-llm")
@click.option("--input", "input_path", type=click.Path(exists=True, dir_okay=False), required=True, help="Path to Excel file")
@click.option("--sheet", default=None, help="Worksheet name or index")
@click.option("--out", "out_path", default=None, help="Output JSON file path")
@click.option("--provider", default=None, help="LLM provider (e.g., openai)")
@click.option("--model", default=None, help="Model name for the provider")
@click.option("--dry-run", is_flag=True, help="Show the prompt and extracted table without calling LLM")
def parse_llm(input_path: str, sheet: str | int | None, out_path: str | None, provider: str | None, model: str | None, dry_run: bool):
    parser = LLMParserService(provider=provider, model=model)
    result = parser.parse(input_path, sheet=sheet, dry_run=dry_run)
    _write_output(result, out_path)
