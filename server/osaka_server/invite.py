"""ASCII Crockford Base32 invitation codes."""

import re


def normalize(value):
    """Return a valid, trimmed uppercase code, or None for invalid input."""
    if not isinstance(value, str) or not value.isascii():
        return None
    code = value.strip().upper()
    return code if re.fullmatch(r"[0-9A-HJKMNP-TV-Z]{8}", code) else None


def is_valid_invite_code(value):
    return normalize(value) is not None
