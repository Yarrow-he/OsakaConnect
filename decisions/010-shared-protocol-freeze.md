# 决策 010：shared-protocol 冻结（逾期，原 ETA 2026-11-04）

日期：2026-11-06（Brisbane）。提出：代码组长 · Osaka。本文件只列问题、约束和方案，不含倾向。

## 问题
1. 要评审并冻结 `openapi/`、`shared/`、`tests/contract/`，补反例后开 `shared/shared-protocol` 小 PR。本卡只准改这三个目录和 `notes/shared-protocol.md`。
2. 现有文件（main）：`openapi/admin.v1.yaml`（`/v1/health`、`/v1/version`，bearer 鉴权，`routes` 为 `type: array, minItems: 1`，无 maxItems）；`shared/errors.json`（5 个错误码，其中 `BACKEND_UNREACHABLE` 的 http 为 0）；`shared/invite-code.md`；`shared/test-vectors/` 下 admin-contract、invite-code、redaction 三份向量；`tests/contract/test_contract.py`。
3. 现有契约测试只做两件事：校验 spec 合法；用 admin-contract.json 正反例跑 JSON Schema。它不检查：`errors.json` 的键与 `ErrorBody.code` 枚举是否一致；`invite-code.json` 与 `invite-code.md` 的正则是否一致；redaction 向量的格式。admin-contract 正例里 routes 只有长度 1 的样例，没有 2 条线路的样例。
4. 使用方：server-core 实现协议并出站自检；admin-connect 对响应做边界校验；win-diagnostics、android-diagnostics 只读 JSON 向量与错误码。多个已合并或在途的模块已经按现有文件写了实现和单测，冻结后的改动会波及它们。
5. 前情：决策 004（2026-11-03，分支 `lead/shared-protocol-decision-1`）两位评审都反对当时的三个方案，并指出 A17 写的是“proto 中线路字段为 repeated”，仓库里没有 proto，线路列表只在 OpenAPI 中以数组表示。当时已上报 Yarrow，至今 `acceptance.md` 的 A17 原文未改，仓库里也没有对此的书面决定。
6. 本次要决定：冻结前补哪些反例与检查、“冻结”靠什么机制保证不漂移、在 A17 文字未改的情况下怎么处理。

## 约束（acceptance.md 原文）
1. A9：管理协议固定且有身份校验｜协议唯一事实来源 `openapi/admin.v1.yaml`；CI 契约测试 job 校验 spec 合法且 `shared/test-vectors/admin-contract.json` 正例通过、反例被拒；后台测试：无凭证 401、错误凭证 403、正确凭证可查版本/健康，响应通过 schema 校验
2. A11：跨端共享协议、错误码、测试向量放在共享目录，各端不互相引用内部源码｜CI 脚本检查依赖/引用路径
3. A15：邀请码固定 8 位：字符集 Crockford Base32（`0-9A-Z` 去 I、L、O、U），大小写不敏感，输入归一化为大写；规则只在 `shared/invite-code.md` 与 `shared/test-vectors/invite-code.json` 定义一次，三端共用｜共用测试向量（合法/非法/长度 7 与 9/含 I、L、O、U/小写）在 Windows、Android、后台三处单测中全部通过；本期只交付格式校验，激活流程在 0.2.0；字符集 Crockford Base32 由 Yarrow 2026-11-02 确定
4. A17：一期单线路但可扩展：协议与数据模型用线路列表（`routes[]`，本期长度恰为 1），客户端不得硬编码线路地址或名称｜proto 中线路字段为 repeated；后台单测断言返回列表长度 1；CI grep 客户端源码不含线路地址/“Line 01”/“OSA · 01”字面量（仅允许资源文件）
5. A12（相关）：只建本期所需模块，不铺空框架；单 PR 手写生产逻辑 200–600 行
6. 其他：不新增依赖（除非组长批准，契约测试现有依赖为 pyyaml、jsonschema、openapi-spec-validator，仅 CI）；只准改 `openapi/`、`shared/`、`tests/contract/`、`notes/shared-protocol.md`；改协议属“改架构或协议”，必须过本评审会。

## 方案
1. 方案 A：按现状冻结，只补测试。
   1. 做法：协议字段不改。`tests/contract/` 增加：errors.json 键 = ErrorBody.code 枚举；invite-code.json 正反例按 invite-code.md 正则判定；redaction 向量格式检查；admin-contract 增加 2 条线路的正例和边界反例（超长 id、version 前导字符等）。冻结用 `shared/FROZEN.sha256` 记录各文件哈希，契约测试比对，改协议必须同时改哈希并过评审。A17 的“proto repeated”按“OpenAPI 中 routes 为数组”理解，在 `notes/shared-protocol.md` 写明。
   2. 缺点：A17 字面要求 proto，用解释代替修改验收原文，Yarrow 未书面认可，可能被判为不满足验收；哈希文件只能防误改，防不了“同时改文件和哈希”；三端手写校验的一致性仍只靠向量覆盖。
2. 方案 B：分两步，先补测试与向量但不宣布冻结，A17 文字定了再冻结。
   1. 做法：本 PR 只做方案 A 的测试与向量补充，`notes/shared-protocol.md` 标注“admin.v1 待冻结”；同时把 A17 改文字的问题（“proto repeated”改为“OpenAPI routes 为数组”，以及“恰为 1”是服务端行为还是 schema `maxItems: 1`）作为单独一项交 Yarrow 决定；决定后再开一个小 PR 加冻结哈希并宣布冻结。
   2. 缺点：冻结继续推迟，模块已逾期；依赖协议的模块在冻结前仍可能被改动波及；两个 PR，多一轮审测；需要 Yarrow 再做一次决定，等待时间不可控。
3. 方案 C：新增 `shared/admin.v1.proto` 只描述数据模型，满足 A17 字面，然后冻结。
   1. 做法：proto 中 `repeated Route routes`，不生成代码；契约测试加一步，用纯 Python 解析 proto 文本，与 OpenAPI schema 的字段名、类型、必填逐项比对；其余同方案 A，哈希覆盖 proto。
   2. 缺点：出现两份协议定义，与 A9“唯一事实来源”冲突；一致性检查脚本是手写解析器，本身可能出错；proto 无法表达 `additionalProperties: false`、pattern、const，表达不完整；各端并不使用它，属于为验收文字而加的空文件，可能违背 A12“不铺空框架”。

## 评审记录（第 1 轮，2026-11-06，Brisbane；两位评审各自新会话、空临时目录、互不可见，未提供 010-lead.md）

调用：A `codex exec --skip-git-repo-check --ephemeral -s read-only -m gpt-6-astra -c model_reasoning_effort=high -`（stdin 喂输入）；B `OSAKA_ROLE=tester ds-claude -p --model deepseek-flash --output-format text`（ds-claude 内含 `claude --effort high`；DeepSeek 接口模型 ID `deepseek-flash`，其 /models 名称为 DeepSeek-V4.1-Flash）。

调用故障：B 首次以 `--model deepseek-v4.1-flash` 调用，接口返回 400（只支持 deepseek-flash、deepseek-v4-pro），无评审内容；只把模型 ID 改为接口名 `deepseek-flash`，effort 不变，同一输入重跑成功。另：首轮输入误取了本机旧副本的验收清单（与 main 仅人名写法不同），已作废，本记录为按 main 原文重跑的同一轮，A、B 都重跑。

### 输入全文（A、B 相同）

````text
下面是一份待评审的决策稿。请只找严重问题：会真正影响项目的问题（做不出来、做错方向、卡死进度、误合并或误发布、权限或密钥泄露、违背需求和验收清单）。小概率事件先不提，最多单列 3 条，标明“小概率，可忽略”。请按顺序回答：1. 每个方案的严重问题和后果，以及最小的修法；2. 有没有决策稿漏掉的、更简单或更好的方案，有就写出来；3. 最后才表态：你选哪个方案（可以是你在第 2 步提出的），或者都反对，并给出理由。不需要照顾作者的想法，没有严重问题就明确说可以执行。

===== 决策稿全文：decisions/010-shared-protocol-freeze.md =====
# 决策 010：shared-protocol 冻结（逾期，原 ETA 2026-11-04）

日期：2026-11-06（Brisbane）。提出：代码组长 · Osaka。本文件只列问题、约束和方案，不含倾向。

## 问题
1. 要评审并冻结 `openapi/`、`shared/`、`tests/contract/`，补反例后开 `shared/shared-protocol` 小 PR。本卡只准改这三个目录和 `notes/shared-protocol.md`。
2. 现有文件（main）：`openapi/admin.v1.yaml`（`/v1/health`、`/v1/version`，bearer 鉴权，`routes` 为 `type: array, minItems: 1`，无 maxItems）；`shared/errors.json`（5 个错误码，其中 `BACKEND_UNREACHABLE` 的 http 为 0）；`shared/invite-code.md`；`shared/test-vectors/` 下 admin-contract、invite-code、redaction 三份向量；`tests/contract/test_contract.py`。
3. 现有契约测试只做两件事：校验 spec 合法；用 admin-contract.json 正反例跑 JSON Schema。它不检查：`errors.json` 的键与 `ErrorBody.code` 枚举是否一致；`invite-code.json` 与 `invite-code.md` 的正则是否一致；redaction 向量的格式。admin-contract 正例里 routes 只有长度 1 的样例，没有 2 条线路的样例。
4. 使用方：server-core 实现协议并出站自检；admin-connect 对响应做边界校验；win-diagnostics、android-diagnostics 只读 JSON 向量与错误码。多个已合并或在途的模块已经按现有文件写了实现和单测，冻结后的改动会波及它们。
5. 前情：决策 004（2026-11-03，分支 `lead/shared-protocol-decision-1`）两位评审都反对当时的三个方案，并指出 A17 写的是“proto 中线路字段为 repeated”，仓库里没有 proto，线路列表只在 OpenAPI 中以数组表示。当时已上报 Yarrow，至今 `acceptance.md` 的 A17 原文未改，仓库里也没有对此的书面决定。
6. 本次要决定：冻结前补哪些反例与检查、“冻结”靠什么机制保证不漂移、在 A17 文字未改的情况下怎么处理。

## 约束（acceptance.md 原文）
1. A9：管理协议固定且有身份校验｜协议唯一事实来源 `openapi/admin.v1.yaml`；CI 契约测试 job 校验 spec 合法且 `shared/test-vectors/admin-contract.json` 正例通过、反例被拒；后台测试：无凭证 401、错误凭证 403、正确凭证可查版本/健康，响应通过 schema 校验
2. A11：跨端共享协议、错误码、测试向量放在共享目录，各端不互相引用内部源码｜CI 脚本检查依赖/引用路径
3. A15：邀请码固定 8 位：字符集 Crockford Base32（`0-9A-Z` 去 I、L、O、U），大小写不敏感，输入归一化为大写；规则只在 `shared/invite-code.md` 与 `shared/test-vectors/invite-code.json` 定义一次，三端共用｜共用测试向量（合法/非法/长度 7 与 9/含 I、L、O、U/小写）在 Windows、Android、后台三处单测中全部通过；本期只交付格式校验，激活流程在 0.2.0；字符集 Crockford Base32 由 Yarrow 2026-11-02 确定
4. A17：一期单线路但可扩展：协议与数据模型用线路列表（`routes[]`，本期长度恰为 1），客户端不得硬编码线路地址或名称｜proto 中线路字段为 repeated；后台单测断言返回列表长度 1；CI grep 客户端源码不含线路地址/“Line 01”/“OSA · 01”字面量（仅允许资源文件）
5. A12（相关）：只建本期所需模块，不铺空框架；单 PR 手写生产逻辑 200–600 行
6. 其他：不新增依赖（除非组长批准，契约测试现有依赖为 pyyaml、jsonschema、openapi-spec-validator，仅 CI）；只准改 `openapi/`、`shared/`、`tests/contract/`、`notes/shared-protocol.md`；改协议属“改架构或协议”，必须过本评审会。

## 方案
1. 方案 A：按现状冻结，只补测试。
   1. 做法：协议字段不改。`tests/contract/` 增加：errors.json 键 = ErrorBody.code 枚举；invite-code.json 正反例按 invite-code.md 正则判定；redaction 向量格式检查；admin-contract 增加 2 条线路的正例和边界反例（超长 id、version 前导字符等）。冻结用 `shared/FROZEN.sha256` 记录各文件哈希，契约测试比对，改协议必须同时改哈希并过评审。A17 的“proto repeated”按“OpenAPI 中 routes 为数组”理解，在 `notes/shared-protocol.md` 写明。
   2. 缺点：A17 字面要求 proto，用解释代替修改验收原文，Yarrow 未书面认可，可能被判为不满足验收；哈希文件只能防误改，防不了“同时改文件和哈希”；三端手写校验的一致性仍只靠向量覆盖。
2. 方案 B：分两步，先补测试与向量但不宣布冻结，A17 文字定了再冻结。
   1. 做法：本 PR 只做方案 A 的测试与向量补充，`notes/shared-protocol.md` 标注“admin.v1 待冻结”；同时把 A17 改文字的问题（“proto repeated”改为“OpenAPI routes 为数组”，以及“恰为 1”是服务端行为还是 schema `maxItems: 1`）作为单独一项交 Yarrow 决定；决定后再开一个小 PR 加冻结哈希并宣布冻结。
   2. 缺点：冻结继续推迟，模块已逾期；依赖协议的模块在冻结前仍可能被改动波及；两个 PR，多一轮审测；需要 Yarrow 再做一次决定，等待时间不可控。
3. 方案 C：新增 `shared/admin.v1.proto` 只描述数据模型，满足 A17 字面，然后冻结。
   1. 做法：proto 中 `repeated Route routes`，不生成代码；契约测试加一步，用纯 Python 解析 proto 文本，与 OpenAPI schema 的字段名、类型、必填逐项比对；其余同方案 A，哈希覆盖 proto。
   2. 缺点：出现两份协议定义，与 A9“唯一事实来源”冲突；一致性检查脚本是手写解析器，本身可能出错；proto 无法表达 `additionalProperties: false`、pattern、const，表达不完整；各端并不使用它，属于为验收文字而加的空文件，可能违背 A12“不铺空框架”。

===== 必要片段（main 上现有文件全文） =====

----- openapi/admin.v1.yaml -----
openapi: 3.1.0
info:
  title: Osaka 管理协议
  version: admin.v1
  description: 唯一事实来源（Yarrow 2026-11-02）。HTTPS 必须且客户端必须校验证书链与主机名。令牌只从环境变量读取。
servers:
  - url: https://osaka-server:8443
security:
  - bearer: []
paths:
  /v1/health:
    get:
      operationId: getHealth
      responses:
        "200": {description: ok, content: {application/json: {schema: {$ref: "#/components/schemas/HealthResponse"}}}}
        "401": {$ref: "#/components/responses/Unauthenticated"}
        "403": {$ref: "#/components/responses/Forbidden"}
  /v1/version:
    get:
      operationId: getVersion
      responses:
        "200": {description: ok, content: {application/json: {schema: {$ref: "#/components/schemas/VersionResponse"}}}}
        "401": {$ref: "#/components/responses/Unauthenticated"}
        "403": {$ref: "#/components/responses/Forbidden"}
components:
  securitySchemes:
    bearer: {type: http, scheme: bearer}
  responses:
    Unauthenticated: {description: 无凭证, content: {application/json: {schema: {$ref: "#/components/schemas/ErrorBody"}}}}
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
    VersionResponse:
      type: object
      additionalProperties: false
      required: [service, version, protocol]
      properties:
        service: {const: osaka-server}
        version: {type: string, pattern: "^\\d+\\.\\d+\\.\\d+$"}
        protocol: {const: admin.v1}
    ErrorBody:
      type: object
      additionalProperties: false
      required: [code, message]
      properties:
        code: {type: string, enum: [UNAUTHENTICATED, FORBIDDEN, INVALID_INVITE_CODE, BACKEND_UNREACHABLE, INTERNAL]}
        message: {type: string, maxLength: 200}


----- shared/errors.json -----
{
  "UNAUTHENTICATED": {"http": 401, "zh": "未提供管理凭证"},
  "FORBIDDEN": {"http": 403, "zh": "管理凭证无效"},
  "INVALID_INVITE_CODE": {"http": 400, "zh": "邀请码格式不正确"},
  "BACKEND_UNREACHABLE": {"http": 0, "zh": "无法连接后台"},
  "INTERNAL": {"http": 500, "zh": "发生内部错误，请稍后重试"}
}


----- shared/invite-code.md -----
# 邀请码规则（A15）
- 长度固定 8。字符集 Crockford Base32：`0123456789ABCDEFGHJKMNPQRSTVWXYZ`（无 I、L、O、U）。
- 大小写不敏感，校验前转大写；去掉首尾空白；内部横杠不允许（显示时可按 4-4 分组，提交前去掉）。
- 正则（归一化后）：`^[0-9A-HJKMNP-TV-Z]{8}$`
- 测试向量：`test-vectors/invite-code.json`，三端单测必须全部通过。


----- shared/test-vectors/admin-contract.json -----
{"_说明":"契约样例：valid 必须通过、invalid 必须被拒。三端边界校验与 CI 契约测试共用。",
 "HealthResponse":{"valid":[{"status":"ok","routes":[{"id":"r1","display_name":"Osaka 01"}]}],
  "invalid":[{"status":"ok","routes":[]},{"status":"up","routes":[{"id":"r1","display_name":"x"}]},{"routes":[{"id":"r1","display_name":"x"}]},
   {"status":"ok","routes":[{"id":"","display_name":"x"}]},{"status":"ok","routes":[{"id":"r1","display_name":"x"}],"extra":1},{"status":"ok","routes":"r1"}]},
 "VersionResponse":{"valid":[{"service":"osaka-server","version":"0.1.0","protocol":"admin.v1"}],
  "invalid":[{"service":"osaka-server","version":"0.1","protocol":"admin.v1"},{"service":"other","version":"0.1.0","protocol":"admin.v1"},
   {"service":"osaka-server","version":"0.1.0","protocol":"admin.v2"},{"service":"osaka-server","version":"0.1.0"},{"service":"osaka-server","version":"0.1.0","protocol":"admin.v1","debug":true}]},
 "ErrorBody":{"valid":[{"code":"FORBIDDEN","message":"管理凭证无效"}],
  "invalid":[{"code":"NOPE","message":"x"},{"code":"FORBIDDEN"},{"code":"INTERNAL","message":"x","stack":"at Foo()"}]}}


----- shared/test-vectors/invite-code.json -----
{"valid": ["7X2KQ9MZ", "abcd1234", "00000000", " 7X2KQ9MZ "], "invalid": ["7X2KQ9M", "7X2KQ9MZ1", "7X2KQ9MI", "LLLLLLLL", "OOOO0000", "UUUU1111", "7X2K-Q9MZ", "", null, "7X2KQ9M!", "７X2KQ9MZ", "7X2K Q9MZ", "0x2KQ9MZ0"]}

----- shared/test-vectors/redaction.json -----
{
 "mask": "***",
 "cases": [
  {
   "in": "Authorization: Bearer abc.def.ghi",
   "out": "Authorization: Bearer ***"
  },
  {
   "in": "invite=7X2KQ9MZ ok",
   "out": "invite=*** ok"
  },
  {
   "in": "api_key=sk-0123456789abcdef",
   "out": "api_key=***"
  },
  {
   "in": "token: eyJhbGciOiJIUzI1NiJ9.x.y",
   "out": "token: ***"
  },
  {
   "in": "connected route=r1",
   "out": "connected route=r1"
  },
  {
   "in": "INVITE=7x2kq9mz",
   "out": "INVITE=***"
  },
  {
   "in": "password=hunter2",
   "out": "password=***"
  },
  {
   "in": "version=0.1.0 status=ok",
   "out": "version=0.1.0 status=ok"
  }
 ]
}

----- tests/contract/test_contract.py -----
# 契约测试：openapi/admin.v1.yaml 合法，且 shared/test-vectors/admin-contract.json 正反例结果符合预期。
# CI 依赖（仅测试）：pyyaml、jsonschema、openapi-spec-validator。
import json, pathlib, yaml, jsonschema, pytest
from openapi_spec_validator import validate

ROOT = pathlib.Path(__file__).resolve().parents[2]
SPEC = yaml.safe_load((ROOT / "openapi/admin.v1.yaml").read_text(encoding="utf-8"))
VEC = json.loads((ROOT / "shared/test-vectors/admin-contract.json").read_text(encoding="utf-8"))

def test_spec_valid():
    validate(SPEC)

def schema(name):
    return {"$ref": f"#/components/schemas/{name}", "components": SPEC["components"]}

CASES = [(n, s, ok) for n, v in VEC.items() if not n.startswith("_") for ok in (True, False) for s in v["valid" if ok else "invalid"]]

@pytest.mark.parametrize("name,sample,ok", CASES)
def test_vectors(name, sample, ok):
    v = jsonschema.Draft202012Validator(schema(name))
    assert v.is_valid(sample) == ok, (name, sample)

````

### 评审 A 回复全文（Codex，gpt-6-astra，effort high）

````text
1. 各方案的严重问题、后果和最小修法

**方案 A：现在不能宣布冻结。**

严重问题是未经授权就替 A17 作了决定，而且不止一处：

- 用 OpenAPI 数组替代“proto repeated”，`notes` 中的解释不能自行消除验收冲突。
- 冻结现有 schema，并加入两条线路的正例，实际上确定了“协议允许多线路”。但“本期恰为 1”究竟限制服务端输出，还是也限制 schema，尚未决定。

后果是：实现和契约测试全部通过，仍可能验收失败；随后修改已冻结协议，又会波及使用方。

最小修法：取得 Yarrow 的书面决定，明确上述两点，并作为验收依据。若决定“schema 允许多线路、服务端本期只返回一条”，两条线路可以作为 **schema 正例**，同时必须保留后台“实际返回长度为 1”的单测；两种测试不能互相替代。验收原文的修改由另卡处理，不越过本卡目录限制。

**方案 B：没有必须否决的严重问题，可以执行。**

它正确地保留了尚未解决的验收阻塞。两条线路目前确实符合 main 的 schema，因此先补这个正例没有错，但必须说明它验证的是**当前 schema**，不能据此宣布 A17 已满足。若最终决定增加 `maxItems: 1`，就必须同步调整向量，并检查已实现的生产端和消费端。

等待时间不可控是实际进度风险，但根源是验收决定缺失，不能通过自行宣布冻结消除。应明确 Yarrow 的决定是冻结的唯一待决事项，避免使用方把“待冻结”误解为完全不能继续开发。

**方案 C：存在结构性严重问题，不应执行。**

新增手写 proto，再要求它与 OpenAPI 逐项一致，会引入第二份需要同步维护的模型定义。稿中没有明确它只是可再生成的派生物，与 A9“唯一事实来源”产生直接冲突。

更关键的是，它仍然没有解决 A17 的核心歧义：`repeated` 并不表达“恰为 1”，各端也不使用该文件。即使文本比对通过，也不能证明实际协议和数据模型满足验收。结果是增加维护工作，却继续承担验收失败和返工风险。

最小修法：删除新增 proto 和解析器，回到 B，先解决验收文字。若 Yarrow 真正要求引入 protobuf，则需要另行评审协议范围及使用方改造，不能靠一个不用的文件完成验收。

**哈希机制本身不构成否决理由。** A 已同时要求评审；哈希用于发现意外变化是合理的。不能把“文件与哈希一起更新后测试通过”当作评审通过，但也不必要求哈希承担权限控制的职责。

2. 漏掉的更简单方案

可以采用 **B 的单 PR 版本**：

- 立即把测试、向量和冻结记录准备成可评审的小 PR。
- 并行取得 Yarrow 的书面决定，明确 proto 替代关系及线路数量约束。
- 按决定调整最终内容，确认受影响使用方的相关测试通过，再合并并冻结。

这样无需预先安排“补测试 PR”和“冻结 PR”两轮。如果书面决定迟迟不到，而测试补强需要先落地，再拆成 B 原来的两个 PR。

这不会消除验收阻塞，但能减少机械性的第二轮工作，也不需要新增协议文件、依赖或代码生成体系。

3. 最后选择

**我选 B，优先按上述单 PR 方式执行。**

当前真正阻止冻结的是缺少有权决定者的书面验收结论。A 绕过了这个阻塞，C 增加了工作却没有解决它；B 能推进已有工作，并避免把未决解释固化成协议。
````

### 评审 B 回复全文（Claude Code + DeepSeek-V4.1-Flash，effort high）

````text
# 评审意见：决策 010（shared-protocol 冻结）

## 1. 各方案的严重问题、后果与最小修法

### 先列跨方案的硬问题（A/B/C 都中，不解决就不算冻结完成）

**1.1 A17 验收原文未改，冻结没有验收依据（阻断级）**
- **后果**：A17 仍写“proto 中线路字段为 repeated”，仓库没有 proto；004 已因同一问题被两位评审否掉，Yarrow 至今没有书面决定。A 用 `notes` 解释、B 推迟、C 造 proto，都没有消除“验收时可判 A17 不满足”的风险。一旦验收被拒，已按冻结契约实现的 server-core、admin-connect、win/android 都要返工，004 的死锁会原样重复。
- **最小修法**：冻结前拿到 Yarrow 的一句话书面修订/豁免，例如：“A17 的 proto repeated 改为 OpenAPI `routes` 为数组；本期服务端返回恰 1 条，schema 不设 `maxItems`；客户端接受 ≥1 条。”记录在 `decisions/010` 或 PR 描述里。拿不到就不要宣布冻结；`notes/shared-protocol.md` 里的解释不能替代 `acceptance.md`。

**1.2 “冻结”机制挡不住漂移，且没有版本升级规则（误合并/误发布）**
- **后果**：`FROZEN.sha256` 和协议文件在同一个 PR 里，改文件的人可以顺手改哈希；如果清单只覆盖 `openapi/`、`shared/` 而不覆盖 `tests/contract/` 和向量，改测试即可绕过。更关键的是，改完后 `protocol` 仍是 `admin.v1`，客户端只校验 `protocol == admin.v1`，发现不了语义变化。下游以为还是 admin.v1，实际契约已变。
- **最小修法**：把冻结写成硬规则：`admin.v1` 冻结后不可变，任何语义变更开 `admin.v2`（或加 `protocol_revision` 并在 `VersionResponse` 暴露）；哈希清单覆盖 `openapi/`、`shared/` 下所有协议/向量文件、`tests/contract/test_contract.py`；对这三个路径要求指定评审人/CODEOWNERS 批准，哈希只当绊线。不想维护哈希就用 tag/commit SHA 记录冻结点，更简单。

**1.3 `errors.json` 的 `BACKEND_UNREACHABLE.http = 0` 被原样冻结**
- **后果**：唯一事实来源里出现非法 HTTP 状态；server-core 若按 `errors.json` 的 `http` 映射响应，会发出状态 0；win/android 诊断端对 0 的含义没有定义。提议的测试只查“键 = `ErrorBody.code` 枚举”，查不出这个问题。更根本的是，`errors.json` 可能混了服务端错误和客户端本地错误；用一条“键=枚举”的测试会把客户端本地码也塞进服务端响应枚举。
- **最小修法**：先定 `BACKEND_UNREACHABLE` 是服务端错误还是客户端本地错误：服务端就应有合法状态（如 502/503）并留在 `ErrorBody`；客户端本地就不应进 `ErrorBody`，“键=枚举”的检查要排除它。无论哪种，都加测试 `http ∈ {0} ∪ [100,599]`，并在 `notes` 写清 0 的语义；最好直接改 `null` 或删除 `http` 并同步三端。

**1.4 新向量没有和已在途模块做兼容性验证（卡进度）**
- **后果**：决策稿自己说多个已合并/在途模块按现有文件写了实现和单测；本卡又不准改它们。新增 2-route 正例、trim、边界反例等，可能让它们的单测变红。冻结 PR 合并后主分支/下游 CI 变红，发布卡住；或者为了过 CI 临时放宽向量，冻结失真。
- **最小修法**：冻结 PR 前把新向量对 server-core、admin-connect、win-diagnostics、android-diagnostics 各跑一遍；失败的先在各自卡修完，或把该向量拆到后续 PR；PR 描述列出兼容性结果。不要只看 `tests/contract` 这一个 job。

### 方案 A
- **A-1（阻断）**：核心就是 1.1。A 把 A17 的处理写成 `notes` 里的解释，没有 Yarrow 书面认可；这是用文档解释代替验收修订，风险没有被消除，只是被隐藏。最小修法同 1.1。
- **A-2**：A 说“协议字段不改，只补测试”，但 `shared/test-vectors` 本身就是契约的一部分（A9/A11 明确 CI 和三端用它）；加“2 条线路正例”等于改契约行为，不是纯补测试。它本身可以解释为“客户端要接受 ≥1 条，后台单测仍断言 1 条”，与 A17 的“后台单测断言长度 1”不必然冲突；但如果 Yarrow 把“恰为 1”解释成 schema `maxItems: 1`，这个正例和后续实现都要回滚。**最小修法**：在 Yarrow 裁决前不加 2-route 正例；或加但在向量里明确标注“future-compat，不代表本期验收”，并写明“长度 1 是服务端断言，不是 schema `maxItems`”。
- **A-3**：1.2 的哈希/版本问题在 A 里最明显；A 自己也承认哈希防不了“同时改文件和哈希”，但没有补上版本升级和路径评审。最小修法同 1.2。

### 方案 B
- **B-1（自相矛盾）**：B 说 A17 未定、冻结推迟，但第一个 PR 仍做“方案 A 的测试与向量补充”，包括 2-route 正例——这等于先把 A17 的一个解释落地，再等 Yarrow 决定。若 Yarrow 否掉，已合并的向量要回滚。**最小修法**：第一个 PR 去掉所有依赖 A17 语义的向量（2-route、`maxItems` 相关），只做与 routes 无关的检查：errors 键=enum、invite-code 正则、redaction 形状、超长 id/version 前导字符反例。
- **B-2（卡进度）**：冻结继续延期，卡已逾期；依赖模块在冻结前继续暴露在漂移风险里；Yarrow 决策没有 owner 和截止时间，等待不可控；两个 PR 多一轮审测。**最小修法**：给 Yarrow 决策设硬截止（例如 2 个工作日）和 owner；到期无回复，评审会记录解释并执行/升级；或者把 Yarrow 确认并入本次评审（见第 2 步 D），不要单独等一轮。
- B 的优点是保守、不会错误冻结；如果 Yarrow 确实短期不能回复，B（去掉 A17 相关向量后）是可接受的降级，但不要把它叫“完成冻结”。

### 方案 C
- **C-1（方向错，违反 A9/A12）**：新增 `shared/admin.v1.proto` 直接违反 A9“协议唯一事实来源 `openapi/admin.v1.yaml`”（spec 自己的 description 也这么写），也违背 A12“不铺空框架”：proto 不生成代码、三端不读，是死文件。**后果**：验收直接冲突；两份定义必然漂移；以后每改一次要同步两处。**最小修法**：不加 proto；把 A17 的文字改掉（同 1.1）。
- **C-2（检查器本身不可靠）**：手写 proto 文本解析器做“字段名/类型/必填”比对，proto 又表达不了 `additionalProperties`、`pattern`、`const`，检查不完整；解析器自身有 bug 时会假通过。**后果**：给出“两份定义一致”的错觉，实际契约仍有差异。**最小修法**：若非要 proto，就用正式 parser 并由 proto 生成 OpenAPI（新增依赖，需组长批准，且是大改）；在“不新增依赖 + 单 PR 200–600 行”的约束下，结论是不做。
- C 的唯一“优点”是满足 A17 字面；但代价是违反 A9，且没有解决真正的问题。反对。

### 小概率，可忽略（单列 3 条）
1. **小概率，可忽略**：`FROZEN.sha256` 对 CRLF、文件重命名、路径大小写敏感，跨平台 checkout 可能误报；CI 在 Linux 单点，概率低。修法：哈希前统一 LF、固定相对路径。
2. **小概率，可忽略**：`invite-code.md` 的“提交前去掉（横杠）”可能被实现成校验器自动去横杠，与 `"7X2K-Q9MZ"` 非法向量冲突；现有向量能兜住，概率低。修法：测试里明确“归一化只做 trim+大写”，并在 md 里把“提交前去掉”限定为显示端行为。
3. **小概率，可忽略**：Unicode 大写归一化差异（ß→SS、土耳其 i 等）在 Python/Kotlin/Java 不一致；现有向量只覆盖全角数字，概率低。修法：测试限定 ASCII 输入，非 ASCII 一律拒绝。

## 2. 决策稿漏掉的更简单/更好方案

**方案 D（推荐）：A 的收敛版 + Yarrow 书面确认作为合并门槛，一个 PR 完成。**
1. 测试/向量只加与 A17 无关的部分：errors 键=enum、`invite-code.json` 按 md 正则（归一化=trim+大写，不删横杠）、redaction 形状、超长 id/version 前导字符反例；2-route 正例暂缓，或在 Yarrow 确认后加并标注 future-compat。
2. 同时拿 Yarrow 对 A17 的一句话书面修订/豁免（“proto repeated”→“OpenAPI routes 数组；本期服务端返回 1 条，schema 不设 `maxItems`；客户端接受 ≥1 条”），记录在 `decisions/010`/PR。这是合并前条件，不是第二个 PR。
3. 冻结规则写清：`admin.v1` 不可变，语义变更升 `admin.v2`；哈希只当绊线，真正门禁是这三个路径的指定评审；哈希清单覆盖测试和向量。
4. 先处理 `http:0` 的语义，并跑一遍在途模块兼容性。

**为什么更简单**：比 B 少一个 PR、少一轮等待；比 C 不引入第二事实来源、不写解析器；比 A 原样多了一步书面确认，但这步本来就必须做，否则冻结没有验收依据。

**如果 Yarrow 在截止前不回复**：退到 D'——只冻结不受 A17 影响的 `errors.json`/`invite-code`/`redaction`，`admin.v1.yaml` 暂不宣布冻结（或只冻结 health/version 部分并在 notes 标注 routes 待定），把 A17 升级为阻塞项。不要用 notes 解释硬冻。

## 3. 表态

**我选方案 D**（第 2 步提出的 A 收敛版）。**反对 B 和 C 原样，A 原样也不能执行。**

理由：
- **A 原样**把验收风险留到冻结之后，用 notes 解释代替 Yarrow 的书面修订；2-route 正例又在 A17 未定时改变了契约行为。风险没有被解决，只是被推迟到验收或下游 CI 爆发。
- **B 原样**延期且自相矛盾：先说 A17 未定，又把依赖 A17 的向量放进第一个 PR；卡已逾期，继续等一个没有 owner/截止时间的决定，不能算完成冻结。
- **C** 用第二份协议定义换 A17 字面满足，直接违反 A9“唯一事实来源”和 A12“不铺空框架”，还引入一个不完整的手写解析器，方向错了。

如果今天必须在原三案里选一个：我选 **B 的降级版**（去掉所有 A17 相关向量，只做无关检查，并设硬截止），作为临时措施；但首选仍是 D。按 D 做完（Yarrow 书面确认 + `http:0` 语义 + 版本规则 + 兼容性验证）可以执行；拿不到 Yarrow 确认，就不要宣布冻结。

权限/密钥方面没有发现阻断问题：spec 要求 token 只从环境变量读取，redaction 向量里是假值，没有真实密钥泄露。
````
