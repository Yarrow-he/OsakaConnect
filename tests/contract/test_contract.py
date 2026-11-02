# 契约测试：openapi/admin.v1.yaml 合法，且 shared/test-vectors/admin-contract.json 正反例结果符合预期。
# CI 依赖（仅测试）：pyyaml、jsonschema、openapi-spec-validator。
import json, pathlib, yaml, jsonschema, pytest
from openapi_spec_validator import validate

ROOT = pathlib.Path(__file__).resolve().parents[2]
SPEC = yaml.safe_load((ROOT / "openapi/admin.v1.yaml").read_text(encoding="utf-8"))
VEC = json.loads((ROOT / "shared/test-vectors/admin-contract.json").read_text(encoding="utf-8"))

def test_spec_valid():
    validate(SPEC)

def schema(name):
    return {"$ref": f"#/components/schemas/{name}", "components": SPEC["components"]}

CASES = [(n, s, ok) for n, v in VEC.items() if not n.startswith("_") for ok in (True, False) for s in v["valid" if ok else "invalid"]]

@pytest.mark.parametrize("name,sample,ok", CASES)
def test_vectors(name, sample, ok):
    v = jsonschema.Draft202012Validator(schema(name))
    assert v.is_valid(sample) == ok, (name, sample)
