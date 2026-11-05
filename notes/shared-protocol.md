# 派活 brief：shared-protocol（共用）

- owner：代码组长 · Osaka　分支：`shared/shared-protocol`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
定稿 openapi/admin.v1.yaml、errors.json、邀请码规则与三份测试向量（含反例）、契约测试。骨架提交已有初版，本模块负责评审后补全与冻结。

## 只准改的文件
- `openapi/`
- `shared/`
- `tests/contract/`
- `notes/shared-protocol.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
openapi/admin.v1.yaml、shared/test-vectors/*.json、shared/errors.json、shared/invite-code.md。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A9, A11, A15, A17（见 `acceptance.md`）。完成标准：/tmp/ct/bin/pytest -q tests/contract 全过；反例覆盖缺字段、未知字段、枚举外值、空列表、版本格式。

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
