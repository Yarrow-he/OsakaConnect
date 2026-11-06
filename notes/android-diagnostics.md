# 派活 brief：android-diagnostics（Android）

- owner：写手 · Android　分支：`android/android-diagnostics`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
LogRedactor、InviteCode.isValid、错误展示（errors.json 文案）；为 0.2.0 预留的响应校验用 org.json 手写，本期只做 admin-contract 解析校验函数及单测。

## 只准改的文件
- `android/`
- `notes/android-diagnostics.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
shared/errors.json；shared/test-vectors/{redaction,invite-code,admin-contract}.json。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A7, A15（见 `acceptance.md`）。完成标准：JVM 单测读同一份向量全部通过；不新增 Gradle 依赖（JUnit 除外）。

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

## 看板收口（pipeline-watchdog 2026-11-07T01:41:33+1000）

1. PR #104 已合入 main（merge `44f3f70`，head `d999a2c`）。
2. 过时重开 PR #103 已用 `as_lead.sh close` 关闭（被 #104 取代）。
3. ErrorMessages 反向断言建议留后续小 PR。
