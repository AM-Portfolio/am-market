from __future__ import annotations

import ast
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
SRC_DIR = ROOT / "am_parser"
MAX_FILE_LINES = 500
MAX_FUNC_LINES = 50


def count_file_lines(path: Path) -> int:
    try:
        return len(path.read_text(encoding="utf-8").splitlines())
    except UnicodeDecodeError:
        return 0


def iter_py_files(base: Path):
    for p in base.rglob("*.py"):
        yield p


def get_func_length(node: ast.AST) -> int:
    # Requires Python 3.8+: ast nodes have lineno & end_lineno
    lineno = getattr(node, "lineno", None)
    end = getattr(node, "end_lineno", None)
    if lineno is None or end is None:
        return 0
    return int(end) - int(lineno) + 1


def check_file(path: Path) -> list[str]:
    errors: list[str] = []
    # Skip dunder and cache folders
    if any(part == "__pycache__" for part in path.parts):
        return errors

    lines = count_file_lines(path)
    if lines > MAX_FILE_LINES:
        errors.append(f"{path.relative_to(ROOT)}: file too long ({lines} > {MAX_FILE_LINES})")

    code = path.read_text(encoding="utf-8")
    try:
        tree = ast.parse(code)
    except SyntaxError as e:
        errors.append(f"{path.relative_to(ROOT)}: syntax error: {e}")
        return errors

    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            length = get_func_length(node)
            if length > MAX_FUNC_LINES:
                errors.append(
                    f"{path.relative_to(ROOT)}:{node.lineno} function '{node.name}' too long ({length} > {MAX_FUNC_LINES})"
                )
    return errors


def main() -> int:
    if not SRC_DIR.exists():
        print("No source directory 'am_parser' found; skipping.")
        return 0

    all_errors: list[str] = []
    for py in iter_py_files(SRC_DIR):
        all_errors.extend(check_file(py))

    if all_errors:
        print("Convention violations:")
        for e in all_errors:
            print(" -", e)
        return 1
    print("Conventions OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
