# 派活 brief：server-core（服务端）

- owner：写手 · 服务端　分支：`server/server-core`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
Python 后台：标准库 http.server + ssl 监听 0.0.0.0:8443（证书/私钥路径与令牌均取环境变量 OSAKA_TLS_CERT、OSAKA_TLS_KEY、OSAKA_ADMIN_TOKEN）；GET /v1/health、/v1/version；无凭证 401、错凭证 403（hmac.compare_digest）；响应出站前手写 schema 自检；邀请码格式函数。

## 只准改的文件
- `server/`
- `notes/server-core.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
openapi/admin.v1.yaml 全部；shared/errors.json；shared/test-vectors/{admin-contract,invite-code}.json。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A8, A9, A15, A17（见 `acceptance.md`）。完成标准：pytest：401/403/200；响应用 admin-contract.json 的正反例验证自检函数；invite-code 全向量；routes 长度 1；不新增运行时依赖（无 requirements 或只含测试依赖）。

## 禁止事项
- 改清单外文件（`scripts/check-scope.sh` 会拦）；改 `.codex/`、`.claude/` 或任何规则/权限配置
- 新增依赖（未经组长批准）；跳过证书校验；硬编码线路
- 密钥、令牌、私钥写进代码/配置/日志/测试/提交；示例只用明显假值
- `--no-verify`、git merge/push/rebase/强推/删分支；联网；登录或改动真实服务器

## 交付要求
交付时把看板 state 改为 待审测并追加 history；在本文件末尾追加：
- 编译、lint、测试命令及各自输出文件路径
- 本次 CLI 会话日志路径
- 改动文件清单（`git diff --name-only main...`）与已知限制

## 交付（写手 · 服务端，2026-11-02）
- 模块：server-core，分支 `server/server-core`
- 改动文件：server/{.gitignore,README.md,osaka_server/{__init__,app,schema,invite}.py,tests/{test_app,test_contract}.py,notes/server-core.md,reports/*.txt}（后两者为 Codex 沙箱内自测记录）；notes/server-core.md、notes/server-core-taskcard.md、notes/logs/*；reports/model-issues.md
- 运行：`OSAKA_TLS_CERT=... OSAKA_TLS_KEY=... OSAKA_ADMIN_TOKEN=... python3 -m osaka_server.app`（在 server/ 下，见 server/README.md）
- 编译：`python3 -m compileall -q server/osaka_server server/tests` → notes/logs/server-core-compile.log（EXIT 0）
- lint：ruff/pyflakes 未安装，仅 AST 语法检查 → notes/logs/server-core-lint.log（EXIT 0）
- 测试：`cd server && python3 -m pytest -q -rs -p no:cacheprovider` → notes/logs/server-core-pytest.log（181 passed，0 skipped，含真实本地 TLS 连接 401/403/200）
- Codex 会话日志：notes/logs/server-core-codex-20261102-1455.log（成功）；1453（read-only 失败）、1454（中断）
- 已知限制：无正式 lint 工具；Codex 沙箱禁 socket，其自测跳过 30 项，已在沙箱外重跑全过；routes 来自环境变量占位默认值；不含部署脚本（本期未要求）。

## 审阅（审阅 · Osaka，2026-11-02，提交 94c35f3）
- 结论：必须改 1 条，其余通过。CLI：Claude Code + GLM，日志 /home/box/osaka/logs/review-server-core-20261102-1501.log
- 通过：无硬编码密钥/IP/账号/token（0.0.0.0:8443 为任务卡指定，测试只用假值）；Bearer 鉴权 401/403 用 hmac.compare_digest（app.py:45）；invite.py 正则与 shared 规范一致；无新增依赖；未改 .claude/.codex；未提交 board.json；与 docs/architecture.md 一致。
- 必须改：
  1. server/osaka_server/schema.py:33 version 校验用 `\d` 与 `$`，会放过 Unicode 数字和尾部换行，与 openapi/admin.v1.yaml:52 不同源；server/tests/test_contract.py:94 把这两例断言为 True，固化了偏差。改为 fullmatch + [0-9]，测试两例改 False。能否机器化：能（契约向量 admin-contract.json 补这两条 invalid，CI 契约 job 覆盖）。
- 建议改：
  1. 删 server/notes/server-core.md 与 server/reports/*.txt：与根 notes 及 notes/logs 重复且过期（pytest.txt 记 151 passed/30 skipped；server/notes/server-core.md:5 与事实矛盾），删后核对 server/README.md:40 链接。
  2. server/osaka_server/invite.py:10 `strip()` 剥离范围与 .NET/Android trim 可能不一致（A15 三端一致），建议限定字符集或补边界向量。
  3. app.py 未覆写 version_string()，响应带 Python 版本 Server 头。
  4. server/tests/test_app.py:98-137 内存传输层仅为沙箱服务，可简化，非阻塞。

## 返工1（写手 · 服务端，2026-11-02）
- 必须改已修：schema.py version 改 re.fullmatch + [0-9]；test_contract.py 两例改 False。
- 建议已做：app.py 覆写 version_string()（Server: osaka-server，加测试）；删 server/notes/server-core.md、server/reports/*.txt，README 链接改 ../notes/server-core.md。
- 未做：admin-contract.json 反例（组长定归 ci-guards）；invite.py strip（涉三端，待 shared-protocol 补向量）；test_app 简化（非必须）；rebase（禁改历史，组长定不 rebase）。
- 编译：notes/logs/server-core-rework1-compile.log；测试：`cd server && python3 -m pytest -q -rs -p no:cacheprovider` → notes/logs/server-core-rework1-pytest.log
- Codex：notes/logs/server-core-rework1-codex-20261102-1509.log；任务卡 notes/server-core-rework1-taskcard.md

### 审阅 · 返工1（83ec2c7，2026-11-02 15:14 AEST）
结论：通过。
- 证明：compile EXIT 0；pytest 183 passed（沙箱 152+31 skipped，数目自洽）；Codex 日志在。
- schema.py:33 fullmatch [0-9]，与 openapi/admin.v1.yaml:52（ECMA）一致，尾换行与非 ASCII 数字拒绝；test_contract.py:94 两反例断言 False。
- app.py:26-27 version_string 返回 osaka-server，不泄露版本；删除文件与 README.md:40 链接一致。
- 无硬编码密钥/IP/账号；未动 .claude/.codex；无范围外文件；无新依赖。
- 建议改：test_app.py:188 "Python" 断言被 :187 覆盖，冗余，可留。
- 日志：/home/box/osaka/logs/review-server-core-rework1-20261102-1511.log；diff：/home/box/osaka/logs/review-server-core-rework1-diff.txt
