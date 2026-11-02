import json
from pathlib import Path

import pytest

from osaka_server.invite import is_valid_invite_code, normalize
from osaka_server.schema import validate_response


VECTORS = Path(__file__).resolve().parents[2] / "shared/test-vectors"
CONTRACT = json.loads((VECTORS / "admin-contract.json").read_text(encoding="utf-8"))
INVITES = json.loads((VECTORS / "invite-code.json").read_text(encoding="utf-8"))


@pytest.mark.parametrize("schema,value,expected", [
    (schema, value, group == "valid")
    for schema, cases in CONTRACT.items() if not schema.startswith("_")
    for group, values in cases.items() for value in values
])
def test_contract_vectors(schema, value, expected):
    assert validate_response(schema, value) is expected


@pytest.mark.parametrize("value,expected", [
    (value, group == "valid")
    for group, values in INVITES.items() for value in values
])
def test_invite_vectors(value, expected):
    assert is_valid_invite_code(value) is expected
    assert normalize(value) == (value.strip().upper() if expected else None)


@pytest.mark.parametrize("value", [
    12345678, True, [], {}, b"12345678", "ß234567", "ſ2345678",
    "\u00a012345678", "1234\n5678", "1234567\n",
])
def test_invite_rejects_non_ascii_and_non_strings(value):
    assert normalize(value) is None
    assert not is_valid_invite_code(value)


def test_invite_ascii_whitespace():
    assert normalize("\t abcd1234\r\n") == "ABCD1234"


@pytest.mark.parametrize("schema", ["HealthResponse", "VersionResponse", "ErrorBody"])
@pytest.mark.parametrize("value", [None, [], True, 1, "body"])
def test_schema_rejects_non_objects(schema, value):
    assert not validate_response(schema, value)


@pytest.mark.parametrize("field", ["id", "display_name"])
@pytest.mark.parametrize("value,expected", [
    ("", False), ("x", True), ("界" * 64, True), ("x" * 65, False),
    (1, False), (None, False), ([], False),
])
def test_route_lengths_and_types(field, value, expected):
    route = {"id": "placeholder", "display_name": "Placeholder"}
    route[field] = value
    assert validate_response("HealthResponse", {
        "status": "degraded", "routes": [route],
    }) is expected


@pytest.mark.parametrize("route", [
    None, [], "route", {"id": "x"}, {"display_name": "x"},
    {"id": "x", "display_name": "x", "extra": 1},
])
def test_route_shape(route):
    assert not validate_response("HealthResponse", {"status": "ok", "routes": [route]})


@pytest.mark.parametrize("value,expected", [
    ("", True), ("界" * 200, True), ("x" * 201, False),
    (None, False), (1, False), ([], False),
])
def test_error_message(value, expected):
    assert validate_response("ErrorBody", {"code": "INTERNAL", "message": value}) is expected


@pytest.mark.parametrize("schema,field", [
    ("HealthResponse", "status"), ("VersionResponse", "service"),
    ("VersionResponse", "version"), ("VersionResponse", "protocol"),
    ("ErrorBody", "code"),
])
@pytest.mark.parametrize("value", [None, [], {}, True, 1])
def test_schema_field_types(schema, field, value):
    body = dict(CONTRACT[schema]["valid"][0])
    body[field] = value
    assert not validate_response(schema, body)


@pytest.mark.parametrize("version,expected", [
    ("1.2.3\n", False), ("v1.2.3", False), ("1.2.3-beta", False), ("١.٢.٣", False),
])
def test_version_pattern(version, expected):
    assert validate_response("VersionResponse", {
        "service": "osaka-server", "version": version, "protocol": "admin.v1",
    }) is expected


def test_unknown_schema():
    assert not validate_response("Unknown", {})
