#!/usr/bin/env bash
# 用途：A10 管理端连接四项必测的端到端驱动。生成临时证书后，用 docker compose 让 server 以
#       服务名 osaka-server 运行，管理端 probe 从另一容器执行 --probe https://osaka-server:8443，
#       按 probe 退出码判定：①可信证书+正确令牌→成功显示版本/健康；②错误令牌→拒绝且不显示已连接；
#       ③非受信 CA 签发→拒绝；④SAN 不含 osaka-server→拒绝。③④ 均不得显示已连接。
# 前置环境：GitHub Actions ubuntu-latest + docker compose（待 CI；本机不装 .NET/JDK、不跑构建）。
#           需要 openssl 生成运行时临时证书。
# 运行命令：bash tests/admin/admin-connect/run-a10.sh
# 覆盖 acceptance：A10、A14（probe 为 .NET 无界面入口，镜像内构建）。
set -euo pipefail
umask 077

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT=$(cd "$HERE/../../.." && pwd)
COMPOSE_FILE="$HERE/docker-compose.yml"

# ---------------------------------------------------------------------------
# 待写手交付后对齐：产品入口与 probe 退出码含义集中在这里，产品结构变化只改这一段。
#   probe 入口：${PROBE_BIN} --probe https://osaka-server:8443
#   退出码：0 = 已显示版本/健康；非 0 = 未连接/拒绝（②③④ 的最低要求）。
#           125/126/127 视为容器/入口错误，不算“拒绝”，按测试失败处理。
#   显示文案：CONNECTED_MARKER 为“已连接”标识；OK_MARKER_REGEX 为版本/健康标识。
# ---------------------------------------------------------------------------
SERVER_DIR="$ROOT/server"
ADMIN_DIR="$ROOT/admin"
PROBE_DOCKERFILE="${PROBE_DOCKERFILE:-admin/Dockerfile.probe}"
PROBE_BUILD_CONTEXT="${PROBE_BUILD_CONTEXT:-$ROOT}"
PROBE_BIN="${PROBE_BIN:-/app/probe}"
PROBE_URL="${PROBE_URL:-https://osaka-server:8443}"
SERVER_SERVICE="osaka-server"
PROBE_SERVICE="osaka-probe"
COMPOSE_PROJECT="${COMPOSE_PROJECT:-osaka-a10}"
PROBE_OK=0
PROBE_INFRA_CODES="125 126 127"
CONNECTED_MARKER="${CONNECTED_MARKER:-已连接}"
OK_MARKER_REGEX="${OK_MARKER_REGEX:-(version|health|status|routes|service|protocol|健康|版本|已连接)}"
TEST_ADMIN_TOKEN="${TEST_ADMIN_TOKEN:-osaka-test-fake-token-0000}"  # 明显假值，与真实令牌无关
WRONG_TOKEN="wrong-token-obviously-fake"

missing=""
[ -d "$SERVER_DIR" ] || missing="$missing server/"
[ -d "$ADMIN_DIR" ] || missing="$missing admin/"
if [ -n "$missing" ]; then
  echo "待测：模块未交付（缺少$missing）"
  exit 3
fi

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  echo "待 CI：需要 docker + docker compose（GitHub Actions ubuntu-latest）" >&2
  exit 2
fi

export OSAKA_ROOT="$ROOT"
export OSAKA_ADMIN_TOKEN="$TEST_ADMIN_TOKEN"
export PROBE_TOKEN="$TEST_ADMIN_TOKEN"
export PROBE_DOCKERFILE PROBE_BUILD_CONTEXT PROBE_BIN PROBE_URL
export CERTS_DIR="$(mktemp -d)/certs"
TMP_DIR=$(dirname "$CERTS_DIR")

cleanup() {
  rc=$?
  if [ "$rc" -ne 0 ]; then
    echo "== 容器诊断（退出码 $rc）==" >&2
    OSAKA_CERT_SET=trusted docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" ps -a >&2 || true
    OSAKA_CERT_SET=trusted docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" logs --no-color >&2 || true
  fi
  OSAKA_CERT_SET=trusted docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" \
    down -v --remove-orphans >/dev/null 2>&1 || true
  rm -rf "$TMP_DIR"
  exit "$rc"
}
trap cleanup EXIT

bash "$HERE/gen-certs.sh" "$CERTS_DIR"

FAILED=0
pass() { printf 'PASS  %s\n' "$*"; }
fail() { printf 'FAIL  %s\n' "$*"; FAILED=$((FAILED + 1)); }
is_infra_rc() { case " $PROBE_INFRA_CODES " in *" $1 "*) return 0 ;; *) return 1 ;; esac; }
rejected() { [ "$PROBE_RC" -ne "$PROBE_OK" ] && ! is_infra_rc "$PROBE_RC"; }
not_connected() { ! printf '%s' "$PROBE_OUT" | grep -Fq "$CONNECTED_MARKER"; }

PROBE_OUT=""
PROBE_RC=0
start_server() { # $1 = trusted|untrusted|wronghost
  OSAKA_CERT_SET="$1" docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" \
    up -d --force-recreate --wait "$SERVER_SERVICE"
}
run_probe() { # $1 = probe 使用的令牌
  set +e
  PROBE_OUT=$(PROBE_TOKEN="$1" docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" \
    run --rm -T "$PROBE_SERVICE" 2>&1)
  PROBE_RC=$?
  set -e
  printf '%s\n' "$PROBE_OUT"
}

echo "== 构建 probe 镜像（${PROBE_DOCKERFILE}）=="
docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" build "$PROBE_SERVICE"

echo
echo "== A10-① 可信证书 + 正确令牌 =="
start_server trusted
run_probe "$TEST_ADMIN_TOKEN"
if [ "$PROBE_RC" -eq "$PROBE_OK" ] && printf '%s' "$PROBE_OUT" | grep -Eq "$OK_MARKER_REGEX"; then
  pass "① 退出码 0，输出含版本/健康标识"
else
  fail "① 退出码 $PROBE_RC（期望 $PROBE_OK）或输出缺少版本/健康标识"
fi

echo
echo "== A10-② 可信证书 + 错误令牌 =="
start_server trusted
run_probe "$WRONG_TOKEN"
if rejected && not_connected; then
  pass "② 已拒绝（退出码 $PROBE_RC），未显示已连接"
else
  fail "② 未按预期拒绝：退出码 $PROBE_RC，或显示了“$CONNECTED_MARKER”"
fi

echo
echo "== A10-③ 非受信 CA 签发的证书 =="
start_server untrusted
run_probe "$TEST_ADMIN_TOKEN"
if rejected && not_connected; then
  pass "③ 已拒绝（退出码 $PROBE_RC），未显示已连接"
else
  fail "③ 未按预期拒绝：退出码 $PROBE_RC，或显示了“$CONNECTED_MARKER”"
fi

echo
echo "== A10-④ 证书 SAN 不含 osaka-server =="
start_server wronghost
run_probe "$TEST_ADMIN_TOKEN"
if rejected && not_connected; then
  pass "④ 已拒绝（退出码 $PROBE_RC），未显示已连接"
else
  fail "④ 未按预期拒绝：退出码 $PROBE_RC，或显示了“$CONNECTED_MARKER”"
fi

echo
if [ "$FAILED" -ne 0 ]; then
  echo "A10 端到端失败：$FAILED/4"
  exit 1
fi
echo "A10 端到端通过：4/4"
