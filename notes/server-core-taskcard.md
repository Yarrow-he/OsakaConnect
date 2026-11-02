# 任务卡：server-core（按 ponytail full 执行，见 .codex/skills/ponytail/SKILL.md）

## 目标
Python 3 标准库后台（http.server + ssl），监听 0.0.0.0:8443。证书/私钥路径与令牌取环境变量 OSAKA_TLS_CERT、OSAKA_TLS_KEY、OSAKA_ADMIN_TOKEN（缺失则启动失败并给出不含值的错误）。
- GET /v1/health → {"status":"ok","routes":[{"id":..,"display_name":..}]}，routes 本期恰 1 条，取自配置/环境变量（如 OSAKA_ROUTE_ID / OSAKA_ROUTE_NAME，默认明显占位值），不硬编码真实线路
- GET /v1/version → {"service":"osaka-server","version":"0.1.0","protocol":"admin.v1"}
- 鉴权：Authorization: Bearer <token>。缺失→401 UNAUTHENTICATED；错误→403 FORBIDDEN（hmac.compare_digest）。错误体 {code,message}，message 取 shared/errors.json 的 zh。
- 其他路径 404（可用 ErrorBody INTERNAL 以外的简单处理，不扩展协议）。
- 响应出站前用手写校验函数按 openapi/admin.v1.yaml 的 HealthResponse/VersionResponse/ErrorBody 自检（additionalProperties:false、enum、const、pattern、minItems、长度）；自检失败返回 500 INTERNAL。
- 邀请码格式函数 is_valid_invite_code / normalize：8 位 Crockford Base32（0-9A-Z 去 I L O U），大小写不敏感，去首尾空白后归一化为大写；非 str 返回无效；仅 ASCII。
- 日志不打印令牌。

## 只准改的文件
server/ 目录下（建议 server/osaka_server/{__init__,app,schema,invite}.py、server/tests/test_*.py、server/README.md）。不得改其他任何文件。

## 接口定义
openapi/admin.v1.yaml；shared/errors.json；shared/test-vectors/admin-contract.json、invite-code.json（测试从仓库根相对路径读取，不复制）。

## 验收标准（pytest，在 server/ 下 `python3 -m pytest -q`）
1. 无凭证 401、错凭证 403、正确凭证 200（health/version），用测试内生成的临时自签证书（可用 subprocess 调 openssl，若不可用则测试 handler 于明文 HTTPServer），令牌用明显假值。
2. admin-contract.json 全部 valid 通过、invalid 被拒（三个 schema）。
3. invite-code.json 全向量。
4. health routes 长度 == 1。
5. 不新增运行时依赖；不建 requirements 或只放测试依赖。

## 禁止
联网；改 server/ 以外文件；改 .codex/ .claude/；git 任何写操作（commit/push/merge 等由人做）；真实密钥/IP/账号；跳过证书校验；新增依赖。

注：以 `-s workspace-write` 运行（组长批准，2026-11-02）。
