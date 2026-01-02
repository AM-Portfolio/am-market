"""
AM App CLI - Unified command-line interface
Single entry point for all parsing operations
"""
import json
import sys
from pathlib import Path
from typing import Optional, List

import click

# Add parent directory to path to find external modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_app.app import AMApp


@click.group()
@click.version_option(version="0.1.0")
def cli():
    """AM App - Unified mutual fund parser with multiple strategies"""
    pass


@cli.command()
@click.option("--input", "-i", "input_file", required=True, 
              type=click.Path(exists=True, dir_okay=False), 
              help="Input CSV/Excel file path")
@click.option("--method", "-m", 
              type=click.Choice(["manual", "llm", "together", "both"]), 
              default="manual",
              help="Parsing method: manual (rule-based), llm (AI-based), together (Together AI), or both (compare)")
@click.option("--output", "-o", "output_file", 
              help="Output JSON file path")
@click.option("--sheet", 
              help="Excel sheet name or index")
@click.option("--header-map", 
              help="Header mapping key for manual parsing")
@click.option("--show-preview", is_flag=True, 
              help="Show debug preview (manual parsing)")
@click.option("--dry-run", is_flag=True, 
              help="Show prompt without LLM call (LLM parsing)")
@click.option("--api-key", 
              help="Together AI API key (for together method)")
def parse(input_file: str, method: str, output_file: Optional[str], 
          sheet: Optional[str], header_map: Optional[str], 
          show_preview: bool, dry_run: bool, api_key: Optional[str]):
    """Parse a single mutual fund file"""
    
    app = AMApp()
    
    try:
        result = app.parse_file(
            input_file,
            method=method,
            output_file=output_file,
            sheet=sheet,
            header_map=header_map,
            show_preview=show_preview,
            dry_run=dry_run,
            api_key=api_key
        )
        
        if not output_file:
            # Print to stdout if no output file specified
            click.echo(json.dumps(result, indent=2, ensure_ascii=False))
            
    except Exception as e:
        click.echo(f"❌ Error: {e}", err=True)
        sys.exit(1)


@cli.command()
@click.option("--input-dir", "-d", 
              type=click.Path(exists=True, file_okay=False), 
              help="Directory containing files to parse")
@click.option("--files", "-f", multiple=True,
              help="Specific file paths to parse (can be used multiple times)")
@click.option("--pattern", "-p", default="*.{csv,xlsx,xls}",
              help="File pattern to match in input directory (default: *.{csv,xlsx,xls})")
@click.option("--method", "-m", 
              type=click.Choice(["manual", "llm", "together", "both"]), 
              default="manual",
              help="Parsing method")
@click.option("--output-dir", "-o", 
              help="Output directory for results")
@click.option("--sheet", 
              help="Excel sheet name or index")
@click.option("--header-map", 
              help="Header mapping key for manual parsing")
@click.option("--api-key", 
              help="Together AI API key (for together method)")
def batch(input_dir: Optional[str], files: tuple, pattern: str,
          method: str, output_dir: Optional[str], 
          sheet: Optional[str], header_map: Optional[str]):
    """Parse multiple files in batch"""
    
    app = AMApp()
    file_paths = []
    
    # Collect files from directory
    if input_dir:
        input_path = Path(input_dir)
        # Handle pattern like "*.{csv,xlsx,xls}"
        if "{" in pattern and "}" in pattern:
            # Extract extensions from pattern like "*.{csv,xlsx,xls}"
            start = pattern.find("{") + 1
            end = pattern.find("}")
            extensions = pattern[start:end].split(",")
            for ext in extensions:
                file_paths.extend(input_path.glob(f"*.{ext.strip()}"))
        else:
            file_paths.extend(input_path.glob(pattern))
    
    # Add specific files
    if files:
        file_paths.extend([Path(f) for f in files])
    
    if not file_paths:
        click.echo("❌ No files found to parse", err=True)
        sys.exit(1)
    
    click.echo(f"📁 Found {len(file_paths)} file(s) to process")
    
    try:
        results = app.batch_parse(
            file_paths,
            method=method,
            output_dir=output_dir,
            sheet=sheet,
            header_map=header_map
        )
        
        # Summary
        success_count = sum(1 for r in results if r["status"] == "success")
        error_count = len(results) - success_count
        
        click.echo(f"\n📊 Batch processing complete:")
        click.echo(f"✅ Success: {success_count}")
        click.echo(f"❌ Errors: {error_count}")
        
        if error_count > 0:
            click.echo("\nErrors:")
            for result in results:
                if result["status"] == "error":
                    click.echo(f"  - {result['file']}: {result['error']}")
                    
    except Exception as e:
        click.echo(f"❌ Batch processing error: {e}", err=True)
        sys.exit(1)


@cli.command()
@click.option("--input", "-i", "input_file", required=True,
              type=click.Path(exists=True, dir_okay=False),
              help="Input file to analyze")
@click.option("--sheet", help="Excel sheet name or index")
def analyze(input_file: str, sheet: Optional[str]):
    """Analyze file structure and preview data"""
    
    try:
        from am_services import load_tabular
        
        click.echo(f"🔍 Analyzing file: {input_file}")
        
        # Load and preview data
        df = load_tabular(input_file, sheet=sheet)
        
        click.echo(f"\n📋 File info:")
        click.echo(f"  - Rows: {len(df)}")
        click.echo(f"  - Columns: {len(df.columns)}")
        
        click.echo(f"\n🏷️ Column names:")
        for i, col in enumerate(df.columns):
            click.echo(f"  {i+1:2d}. {col}")
        
        click.echo(f"\n👀 First 3 rows:")
        preview = df.head(3).to_dict('records')
        click.echo(json.dumps(preview, indent=2, ensure_ascii=False, default=str))
        
    except Exception as e:
        click.echo(f"❌ Analysis error: {e}", err=True)
        sys.exit(1)


@cli.command()
@click.option("--input", "-i", "input_file", required=True,
              type=click.Path(exists=True, dir_okay=False),
              help="Input JSON file with mutual fund data")
@click.option("--mongo-uri", default="mongodb://localhost:27017",
              help="MongoDB connection URI")
@click.option("--db-name", default="mutual_funds", 
              help="MongoDB database name")
@click.option("--dry-run", is_flag=True,
              help="Validate model without saving to MongoDB")
def save_portfolio(input_file: str, mongo_uri: str, db_name: str, dry_run: bool):
    """Save mutual fund portfolio JSON to MongoDB"""
    
    try:
        import json
        from am_common import MutualFundPortfolio
        from am_persistence import create_mutual_fund_service
        import asyncio
        
        click.echo(f"📁 Loading portfolio from: {input_file}")
        
        with open(input_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # Convert to Pydantic model
        portfolio = MutualFundPortfolio(**data)
        
        click.echo(f"✅ Loaded: {portfolio.mutual_fund_name}")
        click.echo(f"📅 Date: {portfolio.portfolio_date}")
        click.echo(f"📊 Holdings: {len(portfolio.portfolio_holdings)}")
        
        if dry_run:
            click.echo("🔍 Dry run - model validation successful!")
            mongo_doc = portfolio.to_mongo_document()
            click.echo(f"📄 Would create MongoDB document with {len(mongo_doc)} fields")
            return
        
        async def save_async():
            service = create_mutual_fund_service(mongo_uri, db_name)
            try:
                click.echo(f"🔌 Connecting to MongoDB: {mongo_uri}")
                portfolio_id = await service.save_portfolio(portfolio)
                click.echo(f"✅ Saved to MongoDB with ID: {portfolio_id}")
                
                # Show some stats
                stats = await service.get_fund_statistics(portfolio.mutual_fund_name)
                if stats:
                    click.echo(f"📊 Fund now has {stats['portfolio_count']} portfolio version(s)")
                
            except ImportError:
                click.echo("❌ MongoDB support requires 'motor' package")
                click.echo("💡 Install with: pip install motor")
            except Exception as e:
                click.echo(f"❌ Error saving to MongoDB: {e}")
            finally:
                await service.close()
        
        asyncio.run(save_async())
        
    except Exception as e:
        click.echo(f"❌ Error: {e}", err=True)
        sys.exit(1)


if __name__ == "__main__":
    cli()
