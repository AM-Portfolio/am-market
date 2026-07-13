"""Shared webhook / callback URL helpers."""
from typing import Optional, Tuple


def normalize_callback_url(callback_url: Optional[str]) -> Tuple[Optional[str], Optional[str]]:
    """
    Validate and normalize a webhook callback URL.

    Returns:
        (normalized_url, note) — note is set when URL was provided but invalid.
    """
    if not callback_url:
        return None, None
    cb = callback_url.strip()
    if cb.startswith("http://") or cb.startswith("https://"):
        return cb, None
    return None, "Ignoring invalid callback_url (missing http/https)."
