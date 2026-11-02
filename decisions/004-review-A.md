# 决策 004 评审 A

调用：codex exec，gpt-6-astra，reasoning high，新会话，-s read-only，空临时目录。原始日志：/home/box/osaka/wt/shared-protocol-decision-1/logs/shared-protocol/review-A-004.log（gitignored）。两位评审互不可见。

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

**1. 每个方案最可能出错的地方，以及后果**

以下基于你贴出的内容评审；工作区没有找到对应文件，因此没有核实完整 spec、现有依赖和 CI，也没有运行测试。

**方案 A：最容易把“样例一致”误当成“协议一致”。**

- **手写实现实际上会成为多份可执行定义。** 共享向量只能证明已列举的输入一致。例如，顶层拒绝未知字段，不代表嵌套的 `Route` 也拒绝；缺字段、`null`、错误类型也可能被混为一谈。后果是后台认为响应合法，admin-connect 却拒绝，或者两边同时放过协议禁止的数据。
- **没有说明谁真正解释 OpenAPI。** 如果契约测试只调用手写校验器，spec 改了而校验器没改，测试仍可能全绿。“YAML 能解析”也不等于“OpenAPI 合法”。后果是 A9 的核心要求没有得到验证。
- **校验职责分配错位。** 架构明确说两个 diagnostics 只读 JSON 向量与错误码，却安排“三端”重复实现管理响应校验。邀请码的三个使用端，与管理协议的校验边界不是同一组。后果是增加无用实现，同时可能漏测真正的 admin-connect 边界。
- **spec 哈希不足以冻结整个 shared-protocol。** 错误码、邀请码规则、脱敏向量、测试代码都能变化；即使只看 spec，若同一 PR 可以修改文件和预期哈希，检查也会通过。后果是“冻结”只有变化检测，没有变更约束。

把 A17 的 `proto repeated` 解释成 array，技术意图可以理解，但不能用解释代替验收条文修订；否则验收争议仍然存在。

**方案 B：最容易把“字段一致”误当成“语义一致”。**

- `repeated Route routes` 允许零个或多个元素，既不保证非空，也不保证一期恰好一个。它没有解决真正的数量约束。[Protobuf 官方说明](https://protobuf.dev/programming-guides/proto3/#specifying-field-rules)
- 检查字段名和类型，无法证明 `required`、长度、枚举、未知字段处理、HTTP 状态码和认证要求一致。要覆盖这些语义，一致性脚本本身就会变成另一套复杂的协议实现。
- 若 proto 不参与运行，它只是为满足一句验收文字而增加的维护负担；若参与运行，就必须解决两套模型之间的语义差异。后果分别是虚假保障，或实际兼容性问题。

需要纠正草稿中的判断：**存在第二种表示，不必然违反唯一事实来源。** 从 OpenAPI 确定性生成、禁止手改的产物，可以是派生物。B 的问题是没有定义单向派生关系，而且没有证明引入 proto 有实际使用价值。

**方案 C：最容易把“生成了模型”误当成“生成了完整校验”。**

- 生成成功、编译成功，都不能证明运行时会拒绝所有非法响应。必须指定生成器版本、目标、配置、序列化库及校验调用位置。例如 Kotlin 生成器就有独立的 `failOnUnknownProperties` 配置，其默认值为 `false`；不能仅凭生成器名称认定未知字段一定被拒绝。[生成器官方文档](https://openapi-generator.tech/docs/generators/kotlin/)
- OpenAPI 生成物不能自动解决邀请码归一化、错误码目录和脱敏规则。后果是管理模型看似统一，其他冻结对象仍然漂移。
- 只在 CI 生成，不保证各端构建使用同一批产物。没有固定工具版本、配置和产物来源，就可能出现相同 spec 生成不同代码，或各端继续使用旧代码。
- 依赖成本不止生成工具，还可能包括生成代码要求的运行库，批准范围需要明确。

生成物放在共享模块，并不天然违反 A11；是否违规取决于实际引用关系。“200 行 PR 限制”也没有出现在提供的约束原文里，应核实适用范围，不能直接当作否决依据。

**三案共同遗漏的关键问题：**

- **`routes` 的数量约束尚未作出决策。** 当前 `minItems: 1` 接受两条线路。若“一期恰好一条”属于协议约束，就缺少 `maxItems: 1`；若它只是一期服务端行为，则应保留可扩展 schema，并单独测试服务端只返回一条。A17 的后台单测措辞支持后一种理解，但需要明确写下来，不能宣称当前 schema 已保证长度为 1。
- **结构校验覆盖不了认证。** A9 的无凭证 401、错误凭证 403、正确凭证可访问两个接口，需要调用真实处理链路；几个响应 JSON 正反例不能替代。
- **没有定义冻结后的兼容性规则。** 严格拒绝未知字段意味着以后新增响应字段也可能破坏旧客户端。“字段可选”不自动等于兼容，需要版本或升级策略。
- **邀请码必须按给定字符规则实现。** 应明确只允许 ASCII 字符及其小写形式，不能因套用通用 Crockford 解码器而接受被明确禁止的 I、L、O，也不能让 Unicode 大写转换把原本非法的输入变成合法值。向量应断言归一化结果，而不只是通过或失败。
- **错误码和脱敏没有完整验收安排。** 要检查五个错误码与响应定义是否一致、未知错误码如何处理，以及脱敏向量是否由实际脱敏实现执行。仅把文件纳入仓库不够。

**2. 漏掉的更好方案**

可以增加**方案 D：按使用边界实施校验，分别建立契约验证和冻结机制**。它是对 A 的收缩与补强，不需要新增 proto，也不预设全面代码生成。

具体做法：

1. **明确事实来源和使用方。** 管理协议仍由 OpenAPI 定义；server-core 校验实际出站响应，admin-connect 校验原始响应边界。两个 diagnostics 按架构只消费其需要的共享 JSON。邀请码在 Windows、Android、后台三个实际实现中运行同一份向量，并检查归一化结果。

2. **建立独立的协议判定依据。** CI 除验证 OpenAPI 文档合法，还要按其中实际接口、状态码和媒体类型选择 schema，验证向量及真实响应。运行时手写校验器也执行对应向量，与 schema 判定比较；向量的预期结果独立审查，不能由被测实现自行生成。特别检查嵌套未知字段、缺失与 `null`、类型、长度边界、枚举、空列表和版本格式。

   先盘点已有依赖。如果已有合适工具就复用；如果没有，就提交明确的 CI 校验依赖批准请求。**不能为了“不新增依赖”自制一个不完整校验器，再把它称为 OpenAPI 合法性验证。**

3. **修订 A17，而不是补一个无用的 proto。** 建议明确为：协议与模型使用线路列表；一期服务端返回长度恰为 1；消费方按列表处理。保留 `minItems: 1`，用后台行为测试约束一期数量。若业务实际要求所有一期消费者都拒绝多线路，则另行明确并加上上限。

4. **冻结完整发布集合。** 发布清单记录 OpenAPI、错误码、邀请码规则及三份向量的版本与哈希；各使用方固定到同一份发布集合。测试代码和 CI 配置同样受变更审查，防止通过削弱检查绕过冻结。

5. **区分检测和授权。** 哈希负责发现变化；受保护的基线、必需检查和指定责任人的变更审查负责控制变化。更新基线必须说明兼容性影响，不能仅靠重新计算哈希过关。具体保护方式按现有托管平台落实。

D 仍有代价：手写运行时校验会保留重复逻辑，有限向量无法证明所有输入都一致。它的改进在于加入独立的 schema 判定、真实边界测试和明确的发布约束，使这些风险可检测、可追责。

**3. 最后表态**

**我选 D，但反对按当前草稿立即宣布冻结。**

B 为修补验收措辞引入了没有实际需求的第二种模型；C 尚未证明生成物具备所需的运行时校验能力；A 的方向最接近当前规模，但现有测试与哈希安排不足以支撑“冻结”。

冻结前必须完成四件事：修订 A17 并明确线路数量语义；落实独立的 OpenAPI/schema 校验；验证真实认证链路和各使用边界；建立覆盖全部共享文件的受保护发布基线。缺任何一项，都只能叫“暂定版本”，不能叫“已冻结”。
