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
