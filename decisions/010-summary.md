# 决策 010 汇总（组长）

日期：2026-11-06（Brisbane）。评审原文见 decisions/010-shared-protocol-freeze.md「评审记录」。只跑 1 轮。

## 评审结论
1. A（Codex）：选方案 B，建议改为单 PR（测试与冻结记录先备好，并行取得 Yarrow 书面决定，按决定调整后合并冻结）。A 不能宣布冻结；C 结构性问题，不应执行。
2. B（DeepSeek）：选其第 2 步方案 D（A 的收敛版，单 PR）：只加与 A17 无关的检查和反例，2 条线路正例暂缓；Yarrow 书面确认 A17 作为合并门槛；admin.v1 冻结后不可变、语义变更升 admin.v2，哈希只当绊线；先定 `BACKEND_UNREACHABLE` http=0 的语义；合并前跑在途模块兼容性。A、B、C 原样都反对。
3. 一致处：都否定 C；都要求 Yarrow 书面决定 A17（OpenAPI 数组替代 proto；“恰为 1”是服务端行为还是 schema maxItems），notes 解释不能代替；拿不到决定就不宣布冻结。
4. 分歧：所选方案不同（B vs D）；B 方额外提出 http=0 语义、admin.v2 版本规则、2 条线路正例暂缓、兼容性验证。

## 按规则处理
1. 两位评审选择不同，且共识落在 Yarrow 的验收清单 A17 上，按规则上报 Yarrow，不开第 2 轮。
2. 待 Yarrow 决定：
   1. A17 的“proto 中线路字段为 repeated”是否改为“OpenAPI admin.v1.yaml 中 routes 为数组”。
   2. “本期长度恰为 1”是服务端行为（schema 保持 minItems: 1、无 maxItems，后台单测断言 1，客户端接受 ≥1），还是 schema 约束（加 maxItems: 1）。
3. 决定前：不宣布冻结、不开 shared-protocol PR。
