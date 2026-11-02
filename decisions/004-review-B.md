# 决策 004 评审 B

调用：ds-claude -p --model deepseek-v4-flash，effort high，新会话，空临时目录，禁用 Bash/Edit/Write/Web 工具（第 1 次以 --tools "" 运行时输出畸形工具调用、无回答，日志 review-B-004-attempt1-malformed.log）。原始日志：/home/box/osaka/wt/shared-protocol-decision-1/logs/shared-protocol/review-B-004.log（gitignored）。两位评审互不可见。

## 输入（原文）

````
下面是一份待评审的决策稿。假设其中每个方案都有错。请按顺序回答：1. 每个方案最可能出错的地方，以及出错后的后果；2. 有没有决策稿漏掉的、更好的方案，有就写出来；3. 最后才表态：你选哪个方案（可以是你在第 2 步提出的），或者都反对，并给出理由。不需要照顾作者的想法，找不出问题时要说明你检查了哪些方面。

--- decisions/004-shared-protocol-freeze.md ---
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

--- acceptance.md 第 17、19、28、30 行 ---
| A9 | 管理协议固定且有身份校验 | 协议唯一事实来源 `openapi/admin.v1.yaml`；CI 契约测试 job 校验 spec 合法且 `shared/test-vectors/admin-contract.json` 正例通过、反例被拒；后台测试：无凭证 401、错误凭证 403、正确凭证可查版本/健康，响应通过 schema 校验 |
| A11 | 跨端共享协议、错误码、测试向量放在共享目录，各端不互相引用内部源码 | CI 脚本检查依赖/引用路径 |
| A15 | 邀请码固定 8 位：字符集 Crockford Base32（`0-9A-Z` 去 I、L、O、U），大小写不敏感，输入归一化为大写；规则只在 `shared/invite-code.md` 与 `shared/test-vectors/invite-code.json` 定义一次，三端共用 | 共用测试向量（合法/非法/长度 7 与 9/含 I、L、O、U/小写）在 Windows、Android、后台三处单测中全部通过；本期只交付格式校验，激活流程在 0.2.0；字符集 Crockford Base32 由 Hao Yin 2026-11-02 确定 |
| A17 | 一期单线路但可扩展：协议与数据模型用线路列表（`routes[]`，本期长度恰为 1），客户端不得硬编码线路地址或名称 | proto 中线路字段为 repeated；后台单测断言返回列表长度 1；CI grep 客户端源码不含线路地址/“Line 01”/“OSA · 01”字面量（仅允许资源文件） |

--- openapi/admin.v1.yaml 第 30-45 行 ---
    Forbidden: {description: 凭证错误, content: {application/json: {schema: {$ref: "#/components/schemas/ErrorBody"}}}}
  schemas:
    HealthResponse:
      type: object
      additionalProperties: false
      required: [status, routes]
      properties:
        status: {type: string, enum: [ok, degraded]}
        routes: {type: array, minItems: 1, items: {$ref: "#/components/schemas/Route"}}
    Route:
      type: object
      additionalProperties: false
      required: [id, display_name]
      properties:
        id: {type: string, minLength: 1, maxLength: 64}
        display_name: {type: string, minLength: 1, maxLength: 64}
````

## 回复（原文）

检查范围：决策稿全文、`acceptance.md` 第 17/19/28/30 行、`openapi/admin.v1.yaml` 第 30–45 行、`docs/architecture.md` 第 12–17 行的使用方分工、`notes/shared-protocol.md` 的完成标准。当前工作目录里没有这些文件，所以完整 60 行 spec、三份向量、`errors.json`、`invite-code.md`、`test_contract.py` 和 CI 配置只能按你贴出的片段判断；涉及未展示部分的，我标为“待核实”。

## 1. 每个方案最可能出错的地方

### 方案 A：OpenAPI 唯一源 + 手写校验 + 共享向量

**最可能出错：把“跑同一批向量”当成“得到同一套校验逻辑”。**

OpenAPI 是规则，向量是样例；三端用标准库手写，等于把同一份规则转写三遍。brief 的反例只覆盖缺字段、未知字段、枚举外值、空列表、版本格式，没有覆盖：`Route` 内层的未知字段、`maxLength` 65 的边界、类型错误、`null` 与缺字段的区别、`routes` 长度为 2、重复 JSON key、空 body、错误的 `Content-Type`。一个端漏掉嵌套 `additionalProperties` 或 `maxLength`，仍然可以通过全部现有向量。

**后果：** CI 全绿，但三端行为分叉；同一响应 server-core 自检通过、admin-connect 边界校验拒绝；线上只在未覆盖输入上暴露。这正是“冻结”要防的漂移，却被向量覆盖率掩盖了。

**其他会出错的地方：**

- A17 的“proto 中线路字段为 repeated”是验收文字，不是注释。方案 A 自行解释为 OpenAPI array，决策稿无权改验收标准。后果是验收被驳回，或留下书面违规。
- A 说“只维护 OpenAPI”，但 `shared/errors.json` 仍是第二份错误码定义；spec 里的 `ErrorBody.code` 与它如何同步，没有机制。`invite-code.md` 与向量也是两份工件。A 的“唯一源”只对 admin 路径成立，不是对整个 shared-protocol 成立。
- “三端都跑同一批向量”范围过宽：admin 协议的使用方是 server-core 和 admin-connect；win/android 是只读消费方。强制它们实现 admin 全量校验，是额外耦合，不是 A9 的要求。
- “spec 文件哈希”与 spec 修改在同一个 PR 里更新，是自证，不是冻结；hash 也不验证 spec 合法性（A9 要求“spec 合法”）。如果环境里没有现成的 YAML/OpenAPI 校验依赖，A 要么违反“不新增依赖”，要么写一个很弱的合法性检查。

### 方案 B：OpenAPI 为准，另加 `shared/admin.v1.proto` 描述数据模型

**最可能出错：proto3 表达不了这份契约真正依赖的约束。**

`additionalProperties: false`、`minItems: 1`、`minLength/maxLength`、`pattern` 在 proto3 里都没有对应语义。proto JSON 默认忽略未知字段，字段名默认 lowerCamelCase（`displayName` vs OpenAPI 的 `display_name`），scalar 无 presence，枚举和 int64 的 JSON 映射也不同。所谓“proto 与 OpenAPI 字段一致”的检查，只能比名字和类型，比不了这些语义。

**后果：** 两套定义在 CI 里“一致”，运行时一个接受空 `routes`、未知字段，另一个拒绝；反例测试要么失败，要么被改成更弱的版本；A9 的严格 schema 校验名存实亡。若用 protoc 生成代码，还会引入新的依赖和 protobuf JSON 与 OpenAPI JSON 的映射差异。

**其他会出错的地方：**

- 直接违反 A9“唯一事实来源”。B 的唯一理由，是满足 A17 里“proto”这个字面词；这是在为一个可能过时或误用的词增加第二套定义。
- 如果不用 protoc 生成，proto 就是一份会腐烂的文档；如果用 protoc，就新增依赖，且两端 wire format 未必一致。
- “字段一致性脚本”是自研的部分检查器，会给出虚假的安全感；它漏掉一个映射时，比没有它更危险。

### 方案 C：OpenAPI 生成各端代码

**最可能出错：代码生成器生成的是模型/序列化器，不是统一的校验器。**

`additionalProperties: false` 在多数语言的默认反序列化里被忽略；`minItems`、`pattern`、required/null 的支持随语言、模板、生成器版本而变。同一份 spec 会生成三种校验强度。

**后果：** 三端都“用了生成代码”，但只有一端真正拒绝未知字段；反例在别的端失败，或为了过测试关掉校验；CI 只跑了其中一种生成物，就宣布契约通过。这种“看起来统一”比明说三份手写更危险，因为它掩盖差异。

**其他会出错的地方：**

- openapi-generator 是新的 Java 工具链 + 各端 runtime，涉及版本锁定、离线构建、供应链和审批；对 60 行、2 个 endpoint 的 spec，成本远大于收益。
- 生成物若提交，diff 体积冲击 200 行 PR 限制且不可 review；若构建时生成，每端构建都依赖生成器和模板，A11 的引用路径检查也可能被触发。
- C 完全没有解决 A17 的 proto 字面问题，和 A 一样。

## 三者共同的、更根本的错误

1. **A9 与 A17 在验收层直接冲突，没有方案能同时字面满足。** A9 说 OpenAPI 是唯一事实来源，A17 说 proto 中 `repeated`。A/C 违反 A17 字面，B 违反 A9。决策稿把 A 的解释写成“需 Hao Yin 认可”，但这是 blocking prerequisite，不是“缺点”。更关键的是：A17 里的“proto”可能只是 protocol 的缩写，不一定是 Protocol Buffers；先问验收 owner，可能根本不需要 `.proto` 文件。

2. **封闭 schema + 冻结，和 A17 的“可扩展”自相矛盾。** `additionalProperties: false` 意味着任何新增字段都是 breaking change。冻结 v1 后，0.2.0 想加字段，所有冻结客户端都会拒绝。你们没有选：v1 封闭、加法变更走 v2；还是响应容忍未知字段、只对请求严格。不选，第一次扩展就会打穿老客户端。

3. **“单线路、可扩展”没有被测试证明。** spec 只有 `minItems: 1`，后台只断言返回长度 1；如果向量全是 1 条线路，客户端写 `routes[0]` 也能全过。A17 的 CI grep 只抓字面量，而且“仅允许资源文件”是个漏洞：把 `Line 01` 放进资源文件就能绕过。需要至少一条 2 线路的客户端向量，证明客户端按列表处理，而不是只处理第 0 条。

4. **把向量当规范，冻结的是歧义。** `redaction` 只有向量、没有规范文本；`errors.json` 只有 5 个 code，没有返回条件、HTTP 状态映射、未知 code 的兼容策略。`invite-code` 叫“Crockford Base32”，但要求拒绝 I/L/O/U；标准 Crockford 解码器通常把 I/L 映射为 1、O 映射为 0，还可能忽略连字符。实现者若用现成库，会直接接受这些负例。规范必须写明“不做别名映射，直接拒绝”，否则这个名字会误导人。

5. **冻结机制和测试范围不成立。** 把 `tests/contract/test_contract.py` 放进冻结集是范畴错误：测试实现应该能演进，冻结它等于禁止补测试。一个 hash 覆盖所有工件，改邀请码文案也要动 admin 协议。hash 与 spec 同 PR 更新是自证。没有点名 approver、没有客户端版本钉住、没有 Windows/Android 的 CI 矩阵。并且 `pytest -q tests/contract` 如果只跑静态向量，不跑真实 server 的 401/403/health/version，也不跑真实客户端 parser，A9 的后台测试要求并没有被覆盖。

6. **spec 还有语义缺口，冻结前必须补。** 片段里 `Route` 只有 `id` 和 `display_name`，没有 `address`；A17 却要求客户端不得硬编码“线路地址”。如果客户端需要地址才能连接，spec 没给；如果 `id` 就是地址，spec 要写明其语义和格式（现在只是 1–64 字符串）。另外，片段只看到 `Forbidden`，没看到 401/`securitySchemes`；A9 又要求“无凭证 401、错误凭证 403”（这与常见 HTTP 语义相反，错误凭证通常 401），如果是有意设计，spec 必须明确；还有 `status: degraded` 时 `routes` 是否仍必须至少 1 条，也没定义。

## 2. 决策稿漏掉的、更好的方案

有。漏掉的不是“第四个生成器”，而是**先修前提、再按工件分层冻结**。我把它写成方案 D。

### D0：先澄清/修订验收，不急着选工具

- 问 Hao Yin 一个问题：A17 的“proto”是 Protocol Buffers，还是 protocol？如果是 protocol，把 A17 改成“协议/数据模型中的线路字段为数组（OpenAPI `routes`）”，A 就可以不再需要 `.proto`；如果是 protobuf，就必须在“改 A9”和“改 A17”之间做一次明确的验收修订，而不是由决策稿自行解释。
- 明确 A9“唯一事实来源”的范围：只指 admin 协议，不包含 invite-code 和 redaction；`errors.json` 与 spec 的同步机制要单独定义。
- 明确“本期 `routes` 长度恰为 1”是服务端行为约束还是 schema 约束：若是 schema，加 `maxItems: 1`；若是服务端行为，客户端必须接受 `>= 1`，并加 2 线路向量。
- 明确 version 格式是否在 spec 里用 `pattern` 定义。brief 要求“版本格式”反例；如果 spec 没有 pattern，这个反例就无法由 schema 拒绝，只能靠测试代码硬编码，破坏“spec 是唯一源”。
- 明确 401/403 和认证方式写进 spec 的 `securitySchemes`/responses，而不是只写在测试里。
- 明确兼容策略：封闭 v1（加法变更走 v2），还是响应容忍未知字段。二者只能选一个，不能既要 `additionalProperties: false` 又要随时加字段。

**在 D0 完成前，任何“冻结”都是把未决矛盾写进 hash。**

### D：分层契约 + 可执行冻结

- **按工件拆分冻结单元**：admin 协议、errors、invite-code、redaction 各自有版本和 manifest；不要把 `test_contract.py` 放进冻结集。redaction 在冻结前必须补一份规范文本，向量只能作为样例。
- **每个单元一个规范源，向量带预期结果和规则标签**：每个反例只违反一条规则，避免“因为枚举错而被拒”掩盖“未知字段没被拒”；补充边界语料（64/65、类型错误、`null`、嵌套未知字段、`routes` 0/2、大小写排除字母、空白、非 ASCII、连字符）。
- **校验责任分层，而不是三端各写全量**：server-core 做唯一完整权威校验 + 出站自检；admin-connect 只做文档化的最小边界校验；win/android 只消费各自需要的向量和错误码。若确实要求三端同逻辑，优先从 OpenAPI components 提取一份小体积 JSON Schema、各端用成熟 validator（需单独批准，并核实 C# 库许可和 Android 库成熟度），这比 full codegen 小，语义也更标准。
- **契约测试打真实系统**：真实 server 的 401/403/health/version，真实客户端 boundary parser；CI 矩阵覆盖 backend/Windows/Android。若 Windows/Android 无法进 CI，就明确说“冻结不可执行”，不要用 hash 假装。
- **冻结机制落在治理上**：`shared/contract-manifest.json` 记录 contract version、规范化 hash、指定 approver；版本 bump 单独 PR，不能和 spec 修改同一个自审 PR；每个端记录自己支持的 contract version；CI 验证 manifest 和矩阵。raw YAML 的 hash 会被注释/格式变化打破，应该 hash 规范化内容。
- **加两类验收测试**：一条 id/display_name 都不是 `Line 01`/`OSA · 01` 的 canary 线路，证明客户端不靠字面量；一条 2 线路响应，证明客户端不写死 `routes[0]`。再加未知错误码不崩溃的测试。

D 的代价也要说清楚：它比 A 慢，需要验收 owner 和跨端 CI 的配合；JSON Schema 变体仍可能需要新依赖；如果 D0 拿不到澄清，D 也做不下去。但在当前信息下，这是唯一不把矛盾固化下来的路径。

## 3. 表态

**我反对 A、B、C 按原样执行；我选第 2 步的 D。**

理由不是“三者都不好”，而是三者都接受了同一个错误前提：以为选一个工具就能解决 A9 与 A17 的验收冲突，并以为“共享向量”等于“共享语义”。实际上：

- **A** 最接近正确方向（单一源、无新依赖、改动最小），但它的核心机制——“三份手写校验靠有限向量防漂移”——是不成立的；它也没有冻结治理。A 只能作为最小坏解。
- **B** 是唯一字面满足 A17 的方案，但直接违反 A9，而且 proto3 表达不了契约依赖的约束。它是三者中技术依据最弱的。
- **C** 最贵，且不提供统一校验；对 60 行、2 个 endpoint 的 spec 属于过度工程。

**如果组织强制今天在三者中选一个，我选 A，但附四个条件：**（1）先拿到 A17 的书面澄清/修订；（2）不冻结 `test_contract.py`；（3）加规则表、边界语料、真实端点测试和 CI 矩阵；（4）用独立 approver 的 manifest + 版本，而不是同 PR 更新的 hash。不满足这些条件，A 的 `pytest` 全绿只是假象。

如果 D0 拿不到澄清，我的建议是**明确“暂不冻结”，把 v1 标为 draft**。冻结一个自相矛盾的协议，比不冻结更危险。
