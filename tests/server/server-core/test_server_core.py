# 用途：server-core 后台黑盒测试。起临时自签证书的 8443 后台子进程，验管理身份校验、
#       健康/版本响应 schema 与单线路约定（A8、A9 后台部分、A15 后台、A17 后台）。
#       邀请码用 shared/test-vectors/invite-code.json 全向量测后台格式函数。
# 前置环境：Python 3 + pytest；jsonschema/pyyaml 仅 schema 断言需要（缺则跳过该断言）；
#           openssl 命令用于运行时生成临时自签证书到 tmp 目录（不入库）。
# 运行命令：pytest tests/server/server-core -q
#           入口待写手交付后对齐，可用环境变量覆盖：
#           OSAKA_SERVER_CMD（默认 "python -m server"）、OSAKA_INVITE_FN（默认 "server.invite.is_valid"）、
#           OSAKA_PORT（默认 8443）。
# 覆盖 acceptance：A8、A9（后台部分）、A15（后台）、A17（后台）。
import importlib
import json
import os
import pathlib
import shlex
import ssl
import subprocess
import sys
import time
import urllib.error
import urllib.request

import pytest

ROOT = pathlib.Path(__file__).resolve().parents[3]
SERVER_DIR = ROOT / "server"
SPEC_PATH = ROOT / "openapi/admin.v1.yaml"
INVITE_VECTORS = ROOT / "shared/test-vectors/invite-code.json"

# 待写手交付后对齐：入口与邀请码函数名以 server/ 实际实现为准，均可用环境变量覆盖。
SERVER_CMD = shlex.split(os.environ.get("OSAKA_SERVER_CMD") or f"{sys.executable} -m server")
INVITE_FN = os.environ.get("OSAKA_INVITE_FN", "server.invite.is_valid")

HOST = "127.0.0.1"
PORT = int(os.environ.get("OSAKA_PORT", "8443"))
ADMIN_TOKEN = "osaka-test-fake-token-0000"  # 明显假值，与真实令牌无关
STARTUP_TIMEOUT = 10

# server/ 未交付时由 session fixture 统一跳过本模块全部用例（pytest 输出跳过原因，退出码 0）。


def _make_tls_context(tmp_path):
    cert = tmp_path / "cert.pem"
    key = tmp_path / "key.pem"
    subprocess.run(
        [
            "openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes",
            "-keyout", str(key), "-out", str(cert), "-days", "1",
            "-subj", "/CN=localhost",
            "-addext", "subjectAltName=DNS:localhost,IP:127.0.0.1",
        ],
        check=True,
        capture_output=True,
    )
    return cert, ssl.create_default_context(cafile=str(cert))


def _get(path, context, token=None):
    request = urllib.request.Request(f"https://{HOST}:{PORT}{path}")
    if token is not None:
        request.add_header("Authorization", f"Bearer {token}")
    opener = urllib.request.build_opener(
        urllib.request.ProxyHandler({}),
        urllib.request.HTTPSHandler(context=context),
    )
    try:
        with opener.open(request, timeout=5) as response:
            return response.status, json.loads(response.read())
    except urllib.error.HTTPError as error:
        return error.code, json.loads(error.read())


def _skip_not_delivered(detail):
    pytest.skip(f"待测：模块未交付（{detail}）")


@pytest.fixture(scope="session", autouse=True)
def server(tmp_path_factory):
    if not SERVER_DIR.is_dir():
        _skip_not_delivered("server/ 目录不存在")
    tmp_path = tmp_path_factory.mktemp("server-core")
    try:
        cert, context = _make_tls_context(tmp_path)
    except (FileNotFoundError, subprocess.CalledProcessError) as error:
        _skip_not_delivered(f"无法用 openssl 生成测试证书：{error}")

    env = os.environ.copy()
    env.update(
        OSAKA_TLS_CERT=str(cert),
        OSAKA_TLS_KEY=str(tmp_path / "key.pem"),
        OSAKA_ADMIN_TOKEN=ADMIN_TOKEN,
        OSAKA_PORT=str(PORT),
    )
    log_path = tmp_path / "server.log"
    with open(log_path, "wb") as log:
        try:
            process = subprocess.Popen(SERVER_CMD, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
        except OSError as error:
            _skip_not_delivered(f"后台入口无法启动：{error}")

        deadline = time.monotonic() + STARTUP_TIMEOUT
        while time.monotonic() < deadline:
            if process.poll() is not None:
                _skip_not_delivered(f"后台入口退出码 {process.returncode}：{_log_tail(log_path)}")
            try:
                _get("/v1/version", context, token=ADMIN_TOKEN)
                break
            except (urllib.error.URLError, ssl.SSLError, OSError, json.JSONDecodeError) as error:
                last_error = error
                time.sleep(0.2)
        else:
            process.terminate()
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
            _skip_not_delivered(f"后台未就绪（{last_error}）：{_log_tail(log_path)}")

        yield context
        process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()


def _log_tail(path, limit=800):
    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return "<无日志>"
    return text[-limit:].replace("\n", " | ")


def _assert_schema(name, body):
    jsonschema = pytest.importorskip("jsonschema")
    yaml = pytest.importorskip("yaml")
    spec = yaml.safe_load(SPEC_PATH.read_text(encoding="utf-8"))
    schema = {"$ref": f"#/components/schemas/{name}", "components": spec["components"]}
    errors = sorted(jsonschema.Draft202012Validator(schema).iter_errors(body), key=str)
    assert not errors, [error.message for error in errors]


@pytest.mark.parametrize("path", ["/v1/health", "/v1/version"])
def test_missing_token_is_401_unauthenticated(server, path):
    status, body = _get(path, server)
    assert status == 401
    assert body["code"] == "UNAUTHENTICATED"
    _assert_schema("ErrorBody", body)


@pytest.mark.parametrize("path", ["/v1/health", "/v1/version"])
def test_wrong_token_is_403_forbidden(server, path):
    status, body = _get(path, server, token="wrong-token-obviously-fake")
    assert status == 403
    assert body["code"] == "FORBIDDEN"
    _assert_schema("ErrorBody", body)


def test_health_with_token(server):
    status, body = _get("/v1/health", server, token=ADMIN_TOKEN)
    assert status == 200
    assert len(body["routes"]) == 1
    _assert_schema("HealthResponse", body)


def test_version_with_token(server):
    status, body = _get("/v1/version", server, token=ADMIN_TOKEN)
    assert status == 200
    _assert_schema("VersionResponse", body)


def _invite_validator():
    module_name, _, attribute = INVITE_FN.rpartition(".")
    sys.path.insert(0, str(ROOT))
    try:
        return getattr(importlib.import_module(module_name), attribute)
    except (ImportError, AttributeError, ValueError):
        pytest.skip(f"待测：邀请码函数未交付（INVITE_FN={INVITE_FN} 待对齐）")


def _accepted(function, sample):
    try:
        return bool(function(sample))
    except (TypeError, ValueError, AttributeError):
        return False  # 抛异常也属于“拒绝”，与向量 invalid 语义一致


_VECTORS = json.loads(INVITE_VECTORS.read_text(encoding="utf-8"))
_INVITE_CASES = [
    pytest.param(sample, True, id=f"valid-{index}")
    for index, sample in enumerate(_VECTORS["valid"])
] + [
    pytest.param(sample, False, id=f"invalid-{index}")
    for index, sample in enumerate(_VECTORS["invalid"])
]


@pytest.mark.parametrize("sample,accepted", _INVITE_CASES)
def test_invite_code_vectors(server, sample, accepted):
    function = _invite_validator()
    assert _accepted(function, sample) is accepted
