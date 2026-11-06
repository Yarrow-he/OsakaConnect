# acceptance.md — 第 1 期：0.1.0 可启动工程（已确认 Yarrow 2026-11-02）

来源：REBUILD-PLAN.md 第 1、2（0.1.0 行）、3、4 节。仓库：Yarrow-he/OsakaConnect。
技术栈（沿用旧工程，计划第 1.2 条“不更换技术栈”）：Windows 客户端与管理端 C#/.NET；Android Kotlin；后台 Python。← 已确认，见 A14
不在本期：激活、账号、VPN 连接、设计稿视觉还原（只做占位导航）。

| 编号 | 验收条目 | 怎么验证 |
|---|---|---|
| A1 | Windows 客户端可构建、可启动 | GitHub Actions windows-latest 上 build + 启动冒烟测试（进程启动后自检退出码 0），日志存档 |
| A2 | Windows 客户端有连接、设置两个占位页与导航 | UI 自动化测试切换两页并断言页面标识 |
| A3 | Windows 客户端统一错误展示 | 单测：注入一个错误，断言显示的是用户可读文案且不含堆栈 |
| A4 | Windows 客户端日志脱敏入口 | 单测：含 token/邀请码/密钥样例的日志经入口输出后被掩码 |
| A5 | Android 客户端可构建、可启动 | Actions Linux + 模拟器：assembleDebug、安装、启动 Activity 无崩溃 |
| A6 | Android 有同等占位页、导航和生命周期入口 | 仪器测试：两页切换；旋转/后台再回前台，生命周期回调被记录且不崩溃 |
| A7 | Android 日志脱敏（与 A4 共用测试向量） | 单测，同一份测试向量文件 |
| A8 | 后台可独立启动并提供版本/健康查询 | Actions：启动后台进程，请求健康和版本接口返回 200 及预期字段 |
| A9 | 管理协议固定且有身份校验 | 协议唯一事实来源 `openapi/admin.v1.yaml`；CI 契约测试 job 校验 spec 合法且 `shared/test-vectors/admin-contract.json` 正例通过、反例被拒；后台测试：无凭证 401、错误凭证 403、正确凭证可查版本/健康，响应通过 schema 校验 |
| A10 | 管理端连接页能安全查询隔离后台 | CI 用临时 CA 签发后台证书，后台以非 localhost 主机名（docker 网络服务名 `osaka-server`）运行，管理端从另一容器连接，不走本机回环。必测：①可信证书+正确凭证→显示版本/健康；②错误凭证→显示拒绝且不显示已连接；③证书不可信（非临时 CA 签发）→拒绝连接；④主机名不匹配（证书 SAN 不含所连主机名）→拒绝连接。③④ 都不得显示已连接，代码中不得出现跳过证书校验的回调 |
| A11 | 跨端共享协议、错误码、测试向量放在共享目录，各端不互相引用内部源码 | CI 脚本检查依赖/引用路径 |
| A12 | 只建本期所需模块，不铺空框架；单 PR 手写生产逻辑 200–600 行 | 审阅逐 PR 核对；CI 统计行数（测试/生成/资源分开） |
| A13 | 四部分构建与测试都在 GitHub Actions 上跑通 | main 上最近一次 workflow 全绿的链接 |

## Yarrow 已定决策（2026-11-02 确认，替代上文“← 已确认，见 A14”）

| 编号 | 验收条目 | 怎么验证 |
|---|---|---|
| A14 | 技术栈锁定：Windows 客户端与管理端为 C#/.NET，Android 为 Kotlin，后台为 Python；不出现其他语言的生产代码 | CI 脚本：`windows/`、`admin/` 下只允许 `*.csproj`/`*.cs`/`*.xaml`；`android/` 下只允许 Kotlin/Gradle(KTS)；`server/` 下只允许 Python；违例即失败。另查 `docs/architecture.md` 技术栈一节与此一致 |
| A15 | 邀请码固定 8 位：字符集 Crockford Base32（`0-9A-Z` 去 I、L、O、U），大小写不敏感，输入归一化为大写；规则只在 `shared/invite-code.md` 与 `shared/test-vectors/invite-code.json` 定义一次，三端共用 | 共用测试向量（合法/非法/长度 7 与 9/含 I、L、O、U/小写）在 Windows、Android、后台三处单测中全部通过；本期只交付格式校验，激活流程在 0.2.0；字符集 Crockford Base32 由 Yarrow 2026-11-02 确定 |
| A16 | UI 按 Yarrow 的 Figma 设计：本期占位页的页面结构、页面名称与导航顺序取自 `design/`（Windows：连接/连接记录/邀请码/设置 中本期只做 连接、设置；Android：未激活/已激活两态入口） | 审阅逐页对照 `design/inventory.md` 页面编号（W1、W4、P1、P2）核对标题与导航顺序；UI 测试断言页面标识与设计稿编号一致。视觉还原不在本期 |
| A17 | 一期单线路但可扩展：协议与数据模型用线路列表（`routes[]`，本期长度恰为 1），客户端不得硬编码线路地址或名称 | `openapi/admin.v1.yaml` 中 `routes` 为数组（`type: array`，`minItems: 1`，无 `maxItems`）；后台单测断言返回列表长度 1；客户端接受长度 ≥ 1；CI grep 客户端源码不含线路地址/“Line 01”/“OSA · 01”字面量（仅允许资源文件）。Yarrow 2026-11-06（布里斯班 03:01）书面决定：「A17 改为 admin.v1.yaml 中 routes 为数组」；「长度为 1 是服务端行为，schema 不设 maxItems」 |
