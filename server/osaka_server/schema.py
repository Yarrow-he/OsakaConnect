"""Handwritten checks for openapi/admin.v1.yaml response schemas."""

import re


def _fields(value, names):
    return isinstance(value, dict) and value.keys() == set(names)


def _route(value):
    return _fields(value, ("id", "display_name")) and all(
        isinstance(item, str) and 1 <= len(item) <= 64
        for item in value.values()
    )


def validate_response(schema, value):
    """Return False for unknown schemas or any invalid response field."""
    if schema == "HealthResponse":
        return (
            _fields(value, ("status", "routes"))
            and value["status"] in ("ok", "degraded")
            and isinstance(value["routes"], list)
            and len(value["routes"]) >= 1
            and all(_route(route) for route in value["routes"])
        )
    if schema == "VersionResponse":
        return (
            _fields(value, ("service", "version", "protocol"))
            and value["service"] == "osaka-server"
            and value["protocol"] == "admin.v1"
            and isinstance(value["version"], str)
            and re.search(r"^\d+\.\d+\.\d+$", value["version"]) is not None
        )
    if schema == "ErrorBody":
        return (
            _fields(value, ("code", "message"))
            and value["code"] in (
                "UNAUTHENTICATED", "FORBIDDEN", "INVALID_INVITE_CODE",
                "BACKEND_UNREACHABLE", "INTERNAL",
            )
            and isinstance(value["message"], str)
            and len(value["message"]) <= 200
        )
    return False
