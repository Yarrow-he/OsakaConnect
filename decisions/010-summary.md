# 决策 010 汇总（组长）

日期：2026-11-06（Brisbane）。评审原文见分支 `lead/shared-protocol-decision-2` 的 decisions/010-shared-protocol-freeze.md「评审记录」。只跑 1 轮。

## 评审结论
1. A（Codex）：选方案 B，建议改为单 PR（测试与冻结记录先备好，并行取得 Yarrow 书面决定，按决定调整后合并冻结）。A 不能宣布冻结；C 结构性问题，不应执行。
2. B（DeepSeek）：选其第 2 步方案 D（A 的收敛版，单 PR）：只加与 A17 无关的检查和反例，2 条线路正例暂缓；Yarrow 书面确认 A17 作为合并门槛；admin.v1 冻结后不可变、语义变更升 admin.v2，哈希只当绊线；先定 `BACKEND_UNREACHABLE` http=0 的语义；合并前跑在途模块兼容性。A、B、C 原样都反对。
3. 一致处：都否定 C；都要求 Yarrow 书面决定 A17（OpenAPI 数组替代 proto；“恰为 1”是服务端行为还是 schema maxItems），notes 解释不能代替；拿不到决定就不宣布冻结。
4. 分歧：所选方案不同（B vs D）；B 方额外提出 http=0 语义、admin.v2 版本规则、2 条线路正例暂缓、兼容性验证。

## 按规则处理
1. 两位评审选择不同，且共识落在 Yarrow 的验收清单 A17 上，按规则上报 Yarrow，不开第 2 轮。
2. 已上报，等待书面决定（见下节）。

## Yarrow 书面决定（2026-11-06 03:01，布里斯班，组长聊天）
原文照录：
1. 「A17 改为 admin.v1.yaml 中 routes 为数组」
2. 「长度为 1 是服务端行为，schema 不设 maxItems」
3. 另：删除作废的含真名记录 `invalid-r1-realname`（本机日志，未入仓）。

## 落地
1. `acceptance.md` A17 的「怎么验证」改为：`openapi/admin.v1.yaml` 中 `routes` 为数组（`type: array`，`minItems: 1`，无 `maxItems`）；后台单测断言返回列表长度 1；客户端接受长度 ≥ 1；CI grep 客户端源码不含线路地址/“Line 01”/“OSA · 01”字面量（仅允许资源文件）。由 PR `lead/acceptance-a17-1` 提交。
2. 冻结执行按评审 A 的方案 B 收敛为单 PR，并采纳 B 方收紧项：补检查与反例、纳入 A17 两条线路正例、冻结 admin.v1（语义变更升 admin.v2）、定义 `BACKEND_UNREACHABLE` 的 http=0、合并前跑在途模块兼容性。任务卡由组长写在本机 backlog-restart 日志目录，不入仓。
