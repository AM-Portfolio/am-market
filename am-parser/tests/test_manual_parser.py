import sys
from pathlib import Path

# Add parent directory to path to find am_* modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from am_services import ManualParserService as ManualParser


def test_manual_parser_csv_sample(tmp_path: Path):
    sample = Path(__file__).resolve().parents[1] / "data" / "samples" / "mutualfund_sample.csv"
    assert sample.exists()

    parser = ManualParser()
    result = parser.parse(sample)

    assert "holdings" in result and isinstance(result["holdings"], list)
    assert len(result["holdings"]) >= 3
    totals = result["totals"]
    assert totals["mkt_value"] > 0
    # weights should sum roughly to 100
    assert 99.0 <= totals["weight"] <= 101.0
