# 派活 brief：ci-guards（共用）

- owner：代码组长 · Osaka　分支：`ci/ci-guards`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
完善 CI：契约测试 job（已有初版）；A10 docker 端到端：ci/ 下用 openssl 生成临时 CA 与 SAN=osaka-server 证书、另一张不可信 CA 证书和 SAN 不匹配证书，server 容器以服务名 osaka-server 运行，探测容器经 docker 网络连接，覆盖四项必测；技术栈/互引路径/硬编码线路检查；PR 行数统计。

## 只准改的文件
- `.github/`
- `scripts/`
- `ci/`
- `notes/ci-guards.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
openapi/admin.v1.yaml；acceptance.md A10–A14、A17。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A10, A11, A12, A13, A14, A17（见 `acceptance.md`）。完成标准：PR 上 workflow 全绿；故意构造的 4 种 A10 场景结果符合预期；不使用 localhost/127.0.0.1；私钥只在 CI 运行时生成，不入仓。

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
