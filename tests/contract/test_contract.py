import hashlib
import json
import pathlib
import re

import jsonschema
import pytest
import yaml
from openapi_spec_validator import validate

ROOT = pathlib.Path(__file__).resolve().parents[2]
SPEC = yaml.safe_load((ROOT / "openapi/admin.v1.yaml").read_text(encoding="utf-8"))
VEC = json.loads((ROOT / "shared/test-vectors/admin-contract.json").read_text(encoding="utf-8"))


def test_spec_valid():
    validate(SPEC)


def schema(name):
    return {"$ref": f"#/components/schemas/{name}", "components": SPEC["components"]}


CASES = [
    (n, s, ok)
    for n, v in VEC.items() if not n.startswith("_")
    for ok in (True, False)
    for s in v["valid" if ok else "invalid"]
]


@pytest.mark.parametrize("name,sample,ok", CASES)
def test_vectors(name, sample, ok):
    v = jsonschema.Draft202012Validator(schema(name))
    assert v.is_valid(sample) == ok, (name, sample)


def test_error_codes_match_spec():
    errors = json.loads((ROOT / "shared/errors.json").read_text(encoding="utf-8"))
    codes = SPEC["components"]["schemas"]["ErrorBody"]["properties"]["code"]["enum"]
    assert set(errors) == set(codes)


INVITES = json.loads((ROOT / "shared/test-vectors/invite-code.json").read_text(encoding="utf-8"))


@pytest.mark.parametrize("sample,ok", [
    (sample, ok)
    for ok in (True, False)
    for sample in INVITES["valid" if ok else "invalid"]
])
def test_invite_vectors(sample, ok):
    rules = (ROOT / "shared/invite-code.md").read_text(encoding="utf-8")
    patterns = re.findall(r"^- 正则（归一化后）：`([^`]+)`$", rules, re.MULTILINE)
    assert len(patterns) == 1
    valid = isinstance(sample, str) and re.fullmatch(patterns[0], sample.strip().upper()) is not None
    assert valid == ok, sample


def test_redaction_shape():
    vectors = json.loads((ROOT / "shared/test-vectors/redaction.json").read_text(encoding="utf-8"))
    jsonschema.Draft202012Validator({
        "type": "object",
        "additionalProperties": False,
        "required": ["mask", "cases"],
        "properties": {
            "mask": {"type": "string", "minLength": 1},
            "cases": {
                "type": "array",
                "minItems": 1,
                "items": {
                    "type": "object",
                    "additionalProperties": False,
                    "required": ["in", "out"],
                    "properties": {
                        "in": {"type": "string"},
                        "out": {"type": "string"},
                    },
                },
            },
        },
    }).validate(vectors)


def test_routes_future_compatibility():
    routes = SPEC["components"]["schemas"]["HealthResponse"]["properties"]["routes"]
    assert routes["type"] == "array"
    assert routes["minItems"] == 1
    assert "maxItems" not in routes
    assert any(len(sample["routes"]) == 2 for sample in VEC["HealthResponse"]["valid"])


def test_frozen_manifest():
    manifest = ROOT / "shared/FROZEN.sha256"
    paths = sorted(
        path for directory in ("openapi", "shared")
        for path in (ROOT / directory).rglob("*")
        if path.is_file() and path != manifest
    ) + [ROOT / "tests/contract/test_contract.py"]
    expected = "".join(
        f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.relative_to(ROOT).as_posix()}\n"
        for path in paths
    )
    assert manifest.read_text(encoding="utf-8") == expected
