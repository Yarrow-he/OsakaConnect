# 决策 001：0.1.0 模块拆分与接口（草稿，待评审）

日期：2026-11-02（Brisbane）。提出：代码组长 · Osaka。

## 问题
0.1.0 目标：Windows 客户端、Android 客户端、Windows 管理端、Python 后台四部分可构建、可启动；管理端能凭身份安全查询隔离后台的版本/健康。需要定：模块怎么拆、谁写、跨端接口是什么，且“只建真实所需模块，不铺空框架”，单 PR 手写生产逻辑 200–600 行。已定：C#/.NET（Windows+管理端）、Kotlin、Python；邀请码 8 位（Crockford Base32）；UI 按 Figma；单线路可扩展。

## 方案：8 个模块
| 模块 | 端 | owner | 内容 | 验收 |
|---|---|---|---|---|
| shared-protocol | 共用 | 组长 | `proto/admin/v1/admin.proto`、`shared/errors.json`、邀请码规则与测试向量、脱敏测试向量 | A9 A11 A15 A17 |
| server-core | 服务端 | 写手·服务端 | 标准库 `http.server` 或已批依赖；`GET /v1/health`、`/v1/version`；Bearer 令牌（env `OSAKA_ADMIN_TOKEN`，常量时间比较）；401/403；邀请码格式函数 | A8 A9 A15 A17 |
| win-shell | Windows | 写手·Windows | WPF 入口、连接(W1)/设置(W4) 占位页、侧栏导航、`--self-test` 冒烟 | A1 A2 A16 |
| win-diagnostics | Windows | 写手·Windows | ErrorPresenter、LogRedactor、InviteCode | A3 A4 A7 A15 |
| admin-connect | 管理端 | 写手·Windows | WPF 连接页；令牌只取环境变量；被拒不显示已连接 | A10 |
| android-shell | Android | 写手·Android | 单 Activity、P1/P2 占位、生命周期记录 | A5 A6 A16 |
| android-diagnostics | Android | 写手·Android | LogRedactor、InviteCode、错误展示 | A7 A15 |
| ci-guards | 共用 | 组长 | CI、范围检查、技术栈/引用路径/硬编码线路检查 | A11–A14 A17 |

## 接口
- 管理协议：HTTPS + JSON，proto3 定义为唯一事实来源（不生成代码，各端手写 DTO，靠共用 JSON 样例测试对齐）。
  - `GET /v1/health` → `{status, routes:[{id, display_name}]}`，routes 本期长度 1。
  - `GET /v1/version` → `{service, version, protocol:"admin.v1"}`。
  - 无凭证 401 `UNAUTHENTICATED`，错凭证 403 `FORBIDDEN`，错误体 `{code, message}`。
- 错误码：`shared/errors.json`（码→HTTP→中文文案）。
- 测试向量：`shared/test-vectors/{invite-code,redaction}.json`，三端单测读同一文件。
- 本期后台只监听 127.0.0.1（CI 内自测），TLS 用测试自签证书由 CI 生成，不接真实服务器。

## 依赖顺序
shared-protocol → (server-core ∥ win-diagnostics ∥ android-diagnostics) → (win-shell ∥ android-shell ∥ admin-connect) → ci-guards 收口。Android 与管理端先用协议样例夹具开发，最后接真实后台验收。

## 理由
- 按“端 × 真实需求”拆，每模块对应验收条目，无空框架。
- 诊断（脱敏/错误/邀请码）单独成模块，因为它是安全相关、需小测试、且三端共用向量。
- 不生成 proto 代码：本期只有 2 个接口，避免引入 protoc/gRPC 依赖；接口增多时（0.2.0）再评估。
- 管理端归 Windows 写手：同为 C#/.NET，复用 DTO。

## 风险
1. Windows 写手负载最重（3 个模块）。
2. 手写 DTO 可能三端漂移；靠共用 JSON 样例测试缓解。
3. CI 中 WPF UI 自动化与 Android 模拟器不稳定、耗时。
4. 自签 TLS 在 CI 中的信任配置可能被偷懒成“跳过校验”，违反计划 1.6。
5. 邀请码字符集来自手机设计稿推断，需 Hao Yin 确认。

## 评审所需验收片段
A1–A17 见 acceptance.md；关键：A9 身份校验、A10 被拒不显示已连接、A11 不互引源码、A12 不铺空框架与行数预算、A15 邀请码 8 位、A17 单线路列表。

## 评审记录（2026-11-02，两者新会话、互不可见，只喂决策稿 + A9/A10/A11/A12/A15/A17）

### 评审 A：codex exec，gpt-6-astra，reasoning high（日志 /home/box/osaka/logs/review-A.log）
1. 反对：模块拆分基本合理，但 proto 与手写 JSON 的映射未明确，TLS 验收也缺少证书校验失败用例，尚不足以证明接口一致且连接安全。

2. 最大风险：管理端跳过证书校验仍可能通过现有验收；401/403 只能证明后台校验令牌，无法证明管理端连接的是可信后台，存在令牌泄露风险。

3. 更好的替代方案：保留职责划分，以 OpenAPI 明确 HTTP/JSON 契约并做契约测试；CI 配置临时可信证书，补测不可信证书、主机名不匹配均拒绝连接。

### 评审 B：ds-claude -p --model deepseek-v4-flash（DeepSeek Anthropic 接口，effort high；日志 /home/box/osaka/logs/review-B.log；Claude Code 提示未识别该模型名，按 200k 窗口处理，接口正常返回）
1. **反对**。proto 被定为唯一事实来源却不生成代码，只留下维护成本；三端手写 DTO 仅靠正例夹具对齐，字段名、可选性、未知字段必然漂移。

2. **最大风险：验收可“自证”**。后台只听 127.0.0.1、用自签证书，A10 在同机 CI 即可全绿；真实隔离部署与证书校验（最易被跳过）从未被验证，安全目标可能落空。

3. **更好替代**：契约改用 OpenAPI/JSON Schema，三端生成或边界校验 DTO，夹具补反例与未知字段；A10 增加一次强制证书校验、非回环的运行。

### 结论：两位均反对现稿，共同点：①契约改 OpenAPI/JSON Schema + 契约测试（含反例）②验收须强制证书校验并补不可信证书/主机名不匹配拒绝用例。待组长修订后复审。

## 拍板（Hao Yin，2026-11-02 14:49 Brisbane）
流程：评审 A、B 均反对 → 组长上报 Hao Yin → Hao Yin 决定：
1. 接口改为 OpenAPI，`openapi/admin.v1.yaml` 为唯一事实来源；删除 `proto/`。三端在边界做 schema 校验，不新增重依赖（Python 标准库手写校验；C# System.Text.Json + required + 禁未知字段；Kotlin org.json 手写校验），理由见 `docs/architecture.md`。`shared/test-vectors` 补反例（新增 `admin-contract.json`，邀请码与脱敏向量加反例），CI 新增契约测试 job（`tests/contract`）。
2. A10 加必测：证书不可信、主机名不匹配必须拒绝连接；测试不走本机回环（CI 临时 CA + docker 网络服务名 `osaka-server`）。
3. A15 字符集定为 Crockford Base32；acceptance 改为“已确认 Hao Yin 2026-11-02”。
其余模块拆分与 owner 维持原方案；各模块派活 brief 见 `notes/<模块>.md`。
