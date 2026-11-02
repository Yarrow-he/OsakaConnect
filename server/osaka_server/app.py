"""Run with python3 -m osaka_server.app from server/."""

import hmac
import json
import os
import ssl
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from .schema import validate_response

ERRORS = json.loads(
    (Path(__file__).resolve().parents[2] / "shared/errors.json").read_text(
        encoding="utf-8"
    )
)


def error_body(code):
    return {"code": code, "message": ERRORS[code]["zh"]}


class Handler(BaseHTTPRequestHandler):
    def version_string(self):
        return "osaka-server"

    def log_message(self, format, *args):
        # Request targets and headers may contain credentials; omit access logs.
        pass

    def do_GET(self):
        path = self.path.partition("?")[0]
        if path not in ("/v1/health", "/v1/version"):
            self.send_response(404)
            self.send_header("Content-Length", "0")
            self.end_headers()
            return

        credentials = self.headers.get_all("Authorization", [])
        if not credentials:
            self.respond(401, "ErrorBody", error_body("UNAUTHENTICATED"))
            return
        scheme, separator, token = credentials[0].partition(" ")
        if (
            len(credentials) != 1 or scheme.lower() != "bearer" or not separator
            or not hmac.compare_digest(token.encode("utf-8"), self.server.token)
        ):
            self.respond(403, "ErrorBody", error_body("FORBIDDEN"))
            return

        if path == "/v1/health":
            self.respond(200, "HealthResponse", {
                "status": "ok", "routes": [self.server.route],
            })
        else:
            self.respond(200, "VersionResponse", {
                "service": "osaka-server", "version": "0.1.0", "protocol": "admin.v1",
            })

    def respond(self, status, schema, body):
        if not validate_response(schema, body):
            status, body = 500, error_body("INTERNAL")
            if not validate_response("ErrorBody", body):
                self.send_response(500)
                self.send_header("Content-Length", "0")
                self.end_headers()
                return
        data = json.dumps(body, ensure_ascii=True).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        if status == 401:
            self.send_header("WWW-Authenticate", "Bearer")
        self.end_headers()
        self.wfile.write(data)


class ConfigurationError(ValueError):
    """A configuration error safe to display without environment values."""


def create_server(address=("0.0.0.0", 8443)):
    required = ("OSAKA_TLS_CERT", "OSAKA_TLS_KEY", "OSAKA_ADMIN_TOKEN")
    for name in required:
        if not os.environ.get(name):
            raise ConfigurationError(f"Missing required environment variable: {name}")
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    try:
        context.load_cert_chain(
            os.environ["OSAKA_TLS_CERT"], os.environ["OSAKA_TLS_KEY"],
            password=lambda: "",
        )
    except (OSError, ValueError):
        raise ConfigurationError(
            "Unable to load OSAKA_TLS_CERT / OSAKA_TLS_KEY"
        ) from None
    server = ThreadingHTTPServer(address, Handler)
    server.token = os.environ["OSAKA_ADMIN_TOKEN"].encode("utf-8")
    server.route = {
        "id": os.environ.get("OSAKA_ROUTE_ID", "placeholder-route"),
        "display_name": os.environ.get("OSAKA_ROUTE_NAME", "Placeholder route"),
    }
    try:
        server.socket = context.wrap_socket(server.socket, server_side=True)
    except Exception:
        server.server_close()
        raise
    return server


def main():
    try:
        with create_server() as server:
            server.serve_forever()
    except ConfigurationError as exc:
        print(str(exc), file=sys.stderr)
        return 1
    except OSError:
        print("Unable to start or run HTTPS server", file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        pass
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
