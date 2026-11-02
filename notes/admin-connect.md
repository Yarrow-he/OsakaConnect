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
