from contextlib import contextmanager
import http.client
import io
import json
import os
from pathlib import Path
import shutil
import ssl
import subprocess
import sys
import threading
from http.server import ThreadingHTTPServer
from types import SimpleNamespace

import pytest

from osaka_server import app
from osaka_server.schema import validate_response


FAKE_TOKEN = "obviously-fake-test-token"
SHARED_ERRORS = json.loads(
    (Path(__file__).resolve().parents[2] / "shared/errors.json").read_text(encoding="utf-8")
)


@pytest.fixture(scope="module")
def tls_files(tmp_path_factory):
    if not shutil.which("openssl"):
        yield None
        return
    directory = tmp_path_factory.mktemp("tls")
    cert, key = directory / "cert.pem", directory / "key.pem"
    try:
        subprocess.run([
            "openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
            "-subj", "/CN=localhost", "-addext", "subjectAltName=DNS:localhost",
            "-keyout", str(key), "-out", str(cert),
        ], check=True, capture_output=True)
        yield cert, key
    finally:
        cert.unlink(missing_ok=True)
        key.unlink(missing_ok=True)


@pytest.fixture
def configured_env(monkeypatch, tls_files):
    monkeypatch.setenv("OSAKA_ADMIN_TOKEN", FAKE_TOKEN)
    monkeypatch.setenv("OSAKA_TLS_CERT", str(tls_files[0]) if tls_files else "fake-cert")
    monkeypatch.setenv("OSAKA_TLS_KEY", str(tls_files[1]) if tls_files else "fake-key")
    monkeypatch.delenv("OSAKA_ROUTE_ID", raising=False)
    monkeypatch.delenv("OSAKA_ROUTE_NAME", raising=False)


@contextmanager
def running(server):
    thread = threading.Thread(target=server.serve_forever, kwargs={"poll_interval": 0.01})
    thread.start()
    try:
        yield server
    finally:
        server.shutdown()
        thread.join()
        server.server_close()


@pytest.fixture(params=["handler", "https"])
def server(configured_env, tls_files, request):
    if request.param == "handler":
        yield SimpleNamespace(
            token=FAKE_TOKEN.encode(),
            route={"id": "placeholder-route", "display_name": "Placeholder route"},
        )
        return
    with network_server(tls_files) as instance:
        yield instance


@contextmanager
def network_server(tls_files):
    try:
        instance = make_network_server(tls_files)
    except PermissionError:
        pytest.skip("sandbox denies local sockets; HTTP handler tested separately in memory")
    with running(instance):
        yield instance


def make_network_server(tls_files):
    if tls_files:
        return app.create_server(("127.0.0.1", 0))
    instance = ThreadingHTTPServer(("127.0.0.1", 0), app.Handler)
    instance.token = os.environ["OSAKA_ADMIN_TOKEN"].encode("utf-8")
    instance.route = {"id": "placeholder-route", "display_name": "Placeholder route"}
    return instance


class MemoryRequest:
    def __init__(self, data):
        self.data = io.BytesIO(data)
        self.response = io.BytesIO()

    def makefile(self, *args):
        return self.data

    def sendall(self, data):
        self.response.write(data)


class MemoryConnection:
    """Pass real HTTP request bytes through the handler without a socket."""

    def __init__(self, server):
        self.server = server
        self.data = bytearray()

    def sendall(self, data):
        self.data.extend(data)

    def makefile(self, *args):
        request = MemoryRequest(self.data)
        app.Handler(request, ("127.0.0.1", 0), self.server)
        return io.BytesIO(request.response.getvalue())

    def close(self):
        pass


def connection(server, tls_files, host="localhost", context=None):
    if isinstance(server, SimpleNamespace):
        conn = http.client.HTTPConnection("localhost")
        conn.sock = MemoryConnection(server)
        return conn
    if tls_files:
        context = context or ssl.create_default_context(cafile=str(tls_files[0]))
        return http.client.HTTPSConnection(host, server.server_port, context=context, timeout=3)
    return http.client.HTTPConnection(host, server.server_port, timeout=3)


def request(server, tls_files, path, authorization=None):
    conn = connection(server, tls_files)
    try:
        headers = {} if authorization is None else {"Authorization": authorization}
        conn.request("GET", path, headers=headers)
        response = conn.getresponse()
        data = response.read()
        assert int(response.getheader("Content-Length")) == len(data)
        if response.status == 401:
            assert response.getheader("WWW-Authenticate") == "Bearer"
        if data:
            assert response.getheader("Content-Type") == "application/json; charset=utf-8"
        return response.status, json.loads(data) if data else None
    finally:
        conn.close()


@pytest.mark.parametrize("path,schema", [
    ("/v1/health", "HealthResponse"), ("/v1/version", "VersionResponse"),
])
@pytest.mark.parametrize("authorization,status,code", [
    (None, 401, "UNAUTHENTICATED"),
    ("Bearer obviously-fake-wrong-token", 403, "FORBIDDEN"),
    ("Basic obviously-fake-test-token", 403, "FORBIDDEN"),
    ("Bearer", 403, "FORBIDDEN"), ("Bearer ", 403, "FORBIDDEN"),
    ("Bearer é", 403, "FORBIDDEN"),
    (f"Bearer {FAKE_TOKEN}", 200, None),
    (f"bearer {FAKE_TOKEN}", 200, None),
])
def test_authentication(server, tls_files, path, schema, authorization, status, code):
    actual, body = request(server, tls_files, path, authorization)
    assert actual == status
    assert validate_response("ErrorBody" if code else schema, body)
    if code:
        assert body == {"code": code, "message": SHARED_ERRORS[code]["zh"]}
    elif schema == "HealthResponse":
        assert body == {"status": "ok", "routes": [server.route]}
        assert len(body["routes"]) == 1
    else:
        assert body["version"] == "0.1.0"


def test_server_header_does_not_expose_python(server, tls_files):
    conn = connection(server, tls_files)
    try:
        conn.request("GET", "/v1/health")
        response = conn.getresponse()
        assert response.getheader("Server") == "osaka-server"
        assert "Python" not in response.getheader("Server")
    finally:
        conn.close()


def test_duplicate_credentials_rejected(server, tls_files):
    conn = connection(server, tls_files)
    try:
        conn.putrequest("GET", "/v1/health")
        conn.putheader("Authorization", f"Bearer {FAKE_TOKEN}")
        conn.putheader("Authorization", "Bearer obviously-fake-wrong-token")
        conn.endheaders()
        response = conn.getresponse()
        assert response.status == 403
        assert json.loads(response.read())["code"] == "FORBIDDEN"
    finally:
        conn.close()


def test_constant_time_comparison(server, tls_files, monkeypatch):
    original = app.hmac.compare_digest
    calls = []

    def compare(left, right):
        calls.append((left, right))
        return original(left, right)

    monkeypatch.setattr(app.hmac, "compare_digest", compare)
    assert request(server, tls_files, "/v1/health", f"Bearer {FAKE_TOKEN}")[0] == 200
    assert calls == [(FAKE_TOKEN.encode(), FAKE_TOKEN.encode())]


@pytest.mark.parametrize("path", ["/missing", "/v1/health/", "/"])
def test_unknown_path(server, tls_files, path):
    assert request(server, tls_files, path) == (404, None)


def test_logs_do_not_contain_credentials(server, tls_files, capfd):
    request(server, tls_files, f"/v1/health?token={FAKE_TOKEN}", f"Bearer {FAKE_TOKEN}")
    request(server, tls_files, f"/missing/{FAKE_TOKEN}")
    captured = capfd.readouterr()
    assert FAKE_TOKEN not in captured.out + captured.err


@pytest.mark.parametrize("route", [
    {"id": "", "display_name": "Placeholder"},
    {"id": "x", "display_name": "x" * 65},
    {"id": "x", "display_name": "Placeholder", "extra": True},
])
def test_bad_outbound_response_returns_internal(server, tls_files, route):
    server.route = route
    assert request(server, tls_files, "/v1/health", f"Bearer {FAKE_TOKEN}") == (
        500, {"code": "INTERNAL", "message": SHARED_ERRORS["INTERNAL"]["zh"]},
    )


def test_invalid_outbound_error_is_replaced(server, tls_files, monkeypatch):
    monkeypatch.setitem(app.ERRORS, "UNAUTHENTICATED", {"zh": "x" * 201})
    assert request(server, tls_files, "/v1/version") == (
        500, {"code": "INTERNAL", "message": SHARED_ERRORS["INTERNAL"]["zh"]},
    )


def test_invalid_internal_body_is_not_sent(server, tls_files, monkeypatch):
    server.route = {}
    monkeypatch.setitem(app.ERRORS, "INTERNAL", {"zh": "x" * 201})
    assert request(server, tls_files, "/v1/health", f"Bearer {FAKE_TOKEN}") == (500, None)


@pytest.mark.parametrize("name", ["OSAKA_TLS_CERT", "OSAKA_TLS_KEY", "OSAKA_ADMIN_TOKEN"])
@pytest.mark.parametrize("value", [None, ""])
def test_missing_environment_fails_safely(configured_env, monkeypatch, capsys, name, value):
    if value is None:
        monkeypatch.delenv(name)
    else:
        monkeypatch.setenv(name, value)
    assert app.main() == 1
    error = capsys.readouterr().err
    assert error.strip() == f"Missing required environment variable: {name}"
    assert FAKE_TOKEN not in error


def test_bad_certificate_fails_safely(configured_env, monkeypatch, capsys):
    monkeypatch.setenv("OSAKA_TLS_CERT", "/obviously-fake-private-path/no-cert")
    assert app.main() == 1
    assert capsys.readouterr().err.strip() == "Unable to load OSAKA_TLS_CERT / OSAKA_TLS_KEY"


def test_module_entrypoint():
    environment = dict(os.environ)
    environment.pop("OSAKA_TLS_CERT", None)
    result = subprocess.run(
        [sys.executable, "-m", "osaka_server.app"],
        cwd=Path(__file__).resolve().parents[1], env=environment,
        capture_output=True, text=True,
    )
    assert result.returncode == 1
    assert result.stdout == ""
    assert result.stderr.strip() == "Missing required environment variable: OSAKA_TLS_CERT"


def test_environment_route(configured_env, tls_files, monkeypatch):
    if not tls_files:
        pytest.skip("openssl unavailable; handler tests use HTTP fallback")
    monkeypatch.setenv("OSAKA_ROUTE_ID", "fake-configured-route")
    monkeypatch.setenv("OSAKA_ROUTE_NAME", "测试占位线路")
    with network_server(tls_files) as instance:
        status, body = request(instance, tls_files, "/v1/health", f"Bearer {FAKE_TOKEN}")
    assert status == 200
    assert body["routes"] == [{"id": "fake-configured-route", "display_name": "测试占位线路"}]


@pytest.mark.parametrize("host,trust_cert", [("localhost", False), ("127.0.0.1", True)])
@pytest.mark.parametrize("server", ["https"], indirect=True)
def test_tls_rejects_untrusted_cert_or_wrong_hostname(server, tls_files, host, trust_cert):
    if not tls_files:
        pytest.skip("openssl unavailable; handler tests use HTTP fallback")
    context = ssl.create_default_context(cafile=str(tls_files[0]) if trust_cert else None)
    conn = connection(server, tls_files, host=host, context=context)
    try:
        with pytest.raises(ssl.SSLCertVerificationError):
            conn.request("GET", "/v1/health")
    finally:
        conn.close()
