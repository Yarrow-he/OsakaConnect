#!/usr/bin/env bash
# 用途：为 A10 端到端在运行时生成临时 CA 与服务器证书，写入调用方给的临时目录：
#       trusted/（可信 CA 签发，SAN=DNS:osaka-server）、untrusted/（另一非受信 CA 签发，
#       SAN=DNS:osaka-server）、wronghost/（可信 CA 签发，SAN=DNS:other-host）。
#       私钥只在运行时生成，不入库；调用方负责清理目录。
# 前置环境：openssl 3.x（GitHub Actions ubuntu-latest 自带）；无需 .NET/JDK。
# 运行命令：bash tests/admin/admin-connect/gen-certs.sh <临时输出目录>（通常由 run-a10.sh 调用）
# 覆盖 acceptance：A10（①可信证书、③非受信 CA、④主机名不匹配）。
set -euo pipefail
umask 077

OUT=${1:?用法: gen-certs.sh <临时输出目录>}
command -v openssl >/dev/null || { echo "需要 openssl" >&2; exit 2; }

die() { echo "gen-certs 失败：$*" >&2; exit 1; }

mkdir -p "$OUT/ca" "$OUT/trusted" "$OUT/untrusted" "$OUT/wronghost"

make_ca() { # $1=输出目录 $2=CA 名称
  openssl req -x509 -newkey rsa:2048 -nodes -days 1 \
    -keyout "$1/ca.key" -out "$1/ca.crt" \
    -subj "/CN=Osaka-A10-$2-$(date +%s%N)" \
    -addext "basicConstraints=critical,CA:TRUE" \
    -addext "keyUsage=critical,keyCertSign,cRLSign" >/dev/null 2>&1
}

issue() { # $1=CA 目录 $2=输出目录 $3=证书 CN $4=SAN
  local ca_dir="$1" out_dir="$2" csr="$2/request.csr" ext="$2/ext.cnf"
  openssl req -new -newkey rsa:2048 -nodes \
    -keyout "$out_dir/server.key" -out "$csr" -subj "/CN=$3" >/dev/null 2>&1
  printf 'subjectAltName=%s\nbasicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\n' \
    "$4" > "$ext"
  # 序列号文件也放在临时目录，避免 openssl 在仓库工作目录留下 ca.srl
  local serial=(-CAserial "$ca_dir/ca.srl")
  [ -f "$ca_dir/ca.srl" ] || serial+=(-CAcreateserial)
  openssl x509 -req -in "$csr" -days 1 \
    -CA "$ca_dir/ca.crt" -CAkey "$ca_dir/ca.key" "${serial[@]}" \
    -out "$out_dir/server.crt" -extfile "$ext" >/dev/null 2>&1
  rm -f "$csr" "$ext"
}

make_ca "$OUT/ca" trusted
make_ca "$OUT/untrusted" untrusted
cp "$OUT/ca/ca.crt" "$OUT/trusted/ca.crt"
cp "$OUT/ca/ca.crt" "$OUT/wronghost/ca.crt"

issue "$OUT/ca" "$OUT/trusted" osaka-server "DNS:osaka-server"
issue "$OUT/untrusted" "$OUT/untrusted" osaka-server "DNS:osaka-server"
issue "$OUT/ca" "$OUT/wronghost" other-host "DNS:other-host"

# 自检：三套证书的性质与 A10 预期一致，否则后续四项必测没有意义。
openssl verify -CAfile "$OUT/trusted/ca.crt" "$OUT/trusted/server.crt" >/dev/null 2>&1 \
  || die "trusted/server.crt 无法被可信 CA 验证"
openssl x509 -in "$OUT/trusted/server.crt" -noout -checkhost osaka-server >/dev/null 2>&1 \
  || die "trusted/server.crt 的 SAN 不含 osaka-server"
if openssl verify -CAfile "$OUT/trusted/ca.crt" "$OUT/untrusted/server.crt" >/dev/null 2>&1; then
  die "untrusted/server.crt 被可信 CA 验证通过"
fi
openssl verify -CAfile "$OUT/wronghost/ca.crt" "$OUT/wronghost/server.crt" >/dev/null 2>&1 \
  || die "wronghost/server.crt 无法被可信 CA 验证（应为可信链）"
openssl x509 -in "$OUT/wronghost/server.crt" -noout -checkhost other-host >/dev/null 2>&1 \
  || die "wronghost/server.crt 的 SAN 不含 other-host"
if openssl x509 -in "$OUT/wronghost/server.crt" -noout -checkhost osaka-server >/dev/null 2>&1; then
  die "wronghost/server.crt 的 SAN 仍匹配 osaka-server"
fi

printf '临时 CA 与证书已生成（运行时，未入库）：%s\n' "$OUT"
printf '  trusted/    可信 CA，SAN=DNS:osaka-server\n'
printf '  untrusted/  非受信 CA，SAN=DNS:osaka-server\n'
printf '  wronghost/  可信 CA，SAN=DNS:other-host\n'
