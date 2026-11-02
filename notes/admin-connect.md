# 派活 brief：admin-connect（管理端）

- owner：写手 · Windows　分支：`admin/admin-connect`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
.NET 8 WPF 管理连接页：输入后台地址，令牌只取环境变量 OSAKA_ADMIN_TOKEN；HttpClient 默认证书校验（禁止自定义放行回调）；System.Text.Json record+required+Disallow 未知字段做边界校验；被拒/证书失败/响应不合 schema 时显示错误且不显示已连接。

## 只准改的文件
- `admin/`
- `notes/admin-connect.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
openapi/admin.v1.yaml；shared/errors.json；shared/test-vectors/admin-contract.json。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A10, A14（见 `acceptance.md`）。完成标准：单测：admin-contract 正反例；A10 四项必测（可信+正确凭证、错误凭证、证书不可信、主机名不匹配）由 ci-guards 的 docker 端到端驱动，本模块提供可无界面运行的查询入口（如 --probe <url>，退出码区分结果）；源码 grep 无 ServerCertificateCustomValidationCallback。

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

## 返工单（测试 · Osaka，2026-11-03T01:35:35+10:00）
1. 版本：ff89420（PR #26 写手交付），CI head ebd7f89（ff89420 合入 main 后；admin/ 无差异）。
2. 结论：不通过（以 CI 为准）。run 41472506117（pull_request）与 41472494941（push）：a10-docker=failure；contract、guards、python、android、windows-dotnet=success。
3. 报错原文（ci-41472506117-a10.log 第 302、306、307 行）：
   1. container osaka-a10-osaka-server-1 exited (1)
   2. osaka-server-1  | /usr/local/bin/python: No module named server.__main__; 'server' is a package and cannot be directly executed
   3. ##[error]Process completed with exit code 1.
4. 日志：logs/admin-connect/ci-41472506117-a10.log、logs/admin-connect/ci-41472506117-a10-summary.txt。
5. 复现：
   1. gh run view 41472506117 --log > a10.log，grep -n "No module named" a10.log。
   2. 或在有 docker 的机器：git checkout ebd7f89 && bash tests/admin/admin-connect/run-a10.sh，失败时脚本会 dump docker compose logs。
6. 推测原因（推测，未验证）：tests/admin/admin-connect/docker-compose.yml 第 15 行默认 SERVER_CMD 为 python -m server，而 ebd7f89 的 server/ 下只有 osaka_server/、tests/、README.md，没有 __main__.py；入口应为 python -m osaka_server（或类似），或由写手提供 __main__.py / 设置 SERVER_CMD。请写手 · 服务端定位。

## 返工单（测试）@70a5d35，返工第 3 次（2026-11-03T01:50:17+10:00）
1. 命令：CI a10-docker 跑 `bash tests/admin/admin-connect/run-a10.sh`，run 41473747556、41473734384。
2. 结果：2/4。② 通过；③ 非受信 CA 证书被接受（退出码 0）；④ SAN 不含 osaka-server 被接受（退出码 0）。
3. 结论：客户端未校验证书链与主机名，安全问题。修客户端 TLS 配置根因，不改测试。
4. 日志：logs/admin-connect/ci-41473747556-failed.log 第 202-233 行。
5. 豁免：guards-acceptance、windows-acceptance、android-emulator 为 exit 3（模块未交付），不计。
