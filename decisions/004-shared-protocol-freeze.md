# 决策 004：shared-protocol 定稿与冻结方式（草稿，待评审）

日期：2026-11-03（Brisbane）。提出：代码组长 · Osaka。本文件只列问题、约束和方案，不含倾向。

## 问题
1. shared-protocol 要定稿并冻结：`openapi/admin.v1.yaml`（管理协议，现 60 行，含 `/v1/health`、`/v1/version`）、`shared/errors.json`（5 个错误码）、`shared/invite-code.md`、`shared/test-vectors/` 下三份向量（admin-contract、invite-code、redaction），以及 `tests/contract/test_contract.py`。
2. 使用方（`docs/architecture.md` 第 12–17 行）：server-core 实现该协议并出站自检；admin-connect 边界校验响应；win-diagnostics、android-diagnostics 只读 JSON 向量与错误码。
3. 待定的是：各端如何从同一份定义得到校验逻辑，以及“冻结”靠什么机制保证不漂移。
4. 现状矛盾：A17 写“proto 中线路字段为 repeated”，但仓库里没有 proto 文件，线路列表在 `openapi/admin.v1.yaml` 第 38 行以 `routes: {type: array, minItems: 1}` 表达。

## 约束（acceptance.md 原文）
1. A9（第 17 行）：管理协议固定且有身份校验｜协议唯一事实来源 `openapi/admin.v1.yaml`；CI 契约测试 job 校验 spec 合法且 `shared/test-vectors/admin-contract.json` 正例通过、反例被拒；后台测试：无凭证 401、错误凭证 403、正确凭证可查版本/健康，响应通过 schema 校验
2. A11（第 19 行）：跨端共享协议、错误码、测试向量放在共享目录，各端不互相引用内部源码｜CI 脚本检查依赖/引用路径
3. A15（第 28 行）：邀请码固定 8 位：字符集 Crockford Base32（`0-9A-Z` 去 I、L、O、U），大小写不敏感，输入归一化为大写；规则只在 `shared/invite-code.md` 与 `shared/test-vectors/invite-code.json` 定义一次，三端共用｜共用测试向量（合法/非法/长度 7 与 9/含 I、L、O、U/小写）在 Windows、Android、后台三处单测中全部通过；本期只交付格式校验，激活流程在 0.2.0；字符集 Crockford Base32 由 Hao Yin 2026-11-02 确定
4. A17（第 30 行）：一期单线路但可扩展：协议与数据模型用线路列表（`routes[]`，本期长度恰为 1），客户端不得硬编码线路地址或名称｜proto 中线路字段为 repeated；后台单测断言返回列表长度 1；CI grep 客户端源码不含线路地址/“Line 01”/“OSA · 01”字面量（仅允许资源文件）
5. brief（`notes/shared-protocol.md`）：不新增依赖（未经组长批准）；完成标准为 `pytest -q tests/contract` 全过，反例覆盖缺字段、未知字段、枚举外值、空列表、版本格式。

## 方案
1. 方案 A：OpenAPI 唯一源 + 手写校验 + 共享向量。
   1. 做法：只维护 `openapi/admin.v1.yaml`；各端用标准库手写校验；三端单测与 CI 契约测试都跑 `shared/test-vectors/` 同一批正反例；冻结靠契约测试加 spec 文件哈希检查。A17 的“proto repeated”解释为 OpenAPI 的 array。
   2. 缺点：三端各写一遍校验，靠向量覆盖面防漂移，向量漏掉的情况会不一致；A17 字面写的是 proto，需 Hao Yin 认可这种解释或改文字。
2. 方案 B：OpenAPI 为准，另加 `shared/admin.v1.proto` 只描述数据模型。
   1. 做法：proto 中 `repeated Route routes`，满足 A17 字面；CI 加一步检查 proto 与 OpenAPI 字段一致。
   2. 缺点：两份定义，违背 A9“唯一事实来源”的精神，需要额外一致性检查脚本；若用 protoc 生成代码则是新依赖，需批准。
3. 方案 C：OpenAPI 生成各端代码（openapi-generator 等）。
   1. 做法：CI 从 spec 生成 C#/Kotlin/Python 模型和校验器，各端引用生成产物。
   2. 缺点：新增构建工具依赖，需批准；生成物体积大，可能冲击 200 行 PR 限制和 A11 引用路径检查；生成器对 `additionalProperties: false`、pattern 的支持不一，仍需向量兜底。
