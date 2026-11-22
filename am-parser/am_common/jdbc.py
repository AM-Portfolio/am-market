"""Convert JDBC PostgreSQL URLs to libpq DSNs (env-agnostic; no host defaults)."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional
from urllib.parse import parse_qs, unquote, urlparse


@dataclass(frozen=True)
class PgConnectParams:
    """Parsed connection bits suitable for psycopg.connect(**kwargs) or a DSN string."""

    host: str
    port: int
    dbname: str
    user: str
    password: str
    options: Optional[str] = None  # e.g. -c search_path=mf_catalog

    def as_dsn(self) -> str:
        """libpq DSN without embedding password in logs — callers pass kwargs instead when possible."""
        parts = [
            f"host={self.host}",
            f"port={self.port}",
            f"dbname={self.dbname}",
            f"user={self.user}",
            f"password={self.password}",
        ]
        if self.options:
            parts.append(f"options={self.options}")
        return " ".join(parts)

    def connect_kwargs(self) -> dict:
        kw: dict = {
            "host": self.host,
            "port": self.port,
            "dbname": self.dbname,
            "user": self.user,
            "password": self.password,
        }
        if self.options:
            kw["options"] = self.options
        return kw


def jdbc_to_pg_params(
    jdbc_url: str,
    user: str,
    password: str,
) -> PgConnectParams:
    """
    Parse jdbc:postgresql://host:port/db?currentSchema=mf_catalog into connect params.

    Raises ValueError if the URL is empty or not a postgresql JDBC URL.
    Never logs the password.
    """
    raw = (jdbc_url or "").strip()
    if not raw:
        raise ValueError("JDBC URL is empty")
    if raw.lower().startswith("jdbc:"):
        raw = raw[5:]
    if not raw.lower().startswith("postgresql://") and not raw.lower().startswith(
        "postgres://"
    ):
        raise ValueError("JDBC URL must be jdbc:postgresql://...")

    parsed = urlparse(raw)
    host = parsed.hostname or ""
    if not host:
        raise ValueError("JDBC URL missing host")
    port = parsed.port or 5432
    dbname = unquote((parsed.path or "").lstrip("/").split("/")[0] or "")
    if not dbname:
        raise ValueError("JDBC URL missing database name")

    qs = parse_qs(parsed.query or "")
    schema = None
    for key in ("currentSchema", "currentschema", "schema"):
        if key in qs and qs[key]:
            schema = qs[key][0].strip()
            break
    options = f"-c search_path={schema}" if schema else None

    return PgConnectParams(
        host=host,
        port=int(port),
        dbname=dbname,
        user=(user or "").strip(),
        password=password or "",
        options=options,
    )


def safe_jdbc_label(jdbc_url: str) -> str:
    """Host/db label for logs (no credentials)."""
    try:
        raw = (jdbc_url or "").strip()
        if raw.lower().startswith("jdbc:"):
            raw = raw[5:]
        parsed = urlparse(raw)
        db = (parsed.path or "").lstrip("/")
        return f"{parsed.hostname}:{parsed.port or 5432}/{db}"
    except Exception:
        return "(unparseable)"
