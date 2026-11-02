# 决策 004 汇总（组长）

日期：2026-11-03T07:47:22+10:00（Brisbane）。评审原文：decisions/004-review-A.md、decisions/004-review-B.md（各一轮，互不可见）。

## 评审结论
1. A（Codex）：反对按当前草稿冻结，选其第 2 步提出的方案 D（OpenAPI 为源，按使用边界校验，独立 schema 判定，真实认证链路测试，受保护的发布基线）。见 004-review-A.md「3. 最后表态」。
2. B（DeepSeek）：反对 A、B、C 原样执行，选方案 D（D0 先澄清验收，再分层冻结）；若必须三选一则选 A 并附四个条件。见 004-review-B.md 第 153 行起。
3. 两份评审都反对决策稿里的三个方案，且共同指出必须先修订或澄清验收清单 A17（“proto repeated”与 A9“唯一事实来源”冲突）。

## 按规则处理
1. 规则：两个评审都反对，或分歧涉及 Hao Yin 的需求和验收清单，就上报 Hao Yin 决定，不开第二轮。本次两条都命中。
2. 上报 Hao Yin 待决定的问题：
   1. A17 的“proto 中线路字段为 repeated”是否改成“协议（OpenAPI admin.v1.yaml）中线路字段 routes 为数组，本期服务端返回长度恰为 1，客户端按列表处理”。
   2. 本期“恰为 1 条线路”是服务端行为约束（schema 保留 minItems: 1，后台单测断言长度 1，加 2 线路向量验证客户端不写死 routes[0]），还是 schema 约束（加 maxItems: 1）。
   3. 是否接受“冻结”按方案 D 落地：先标 v1 为 draft，补规则表与边界向量、真实端点 401/403 测试、发布清单（规范化哈希），再冻结；以及 CI 若要用成熟 OpenAPI/JSON Schema 校验库属新增依赖，需单独批准。
3. 在 Hao Yin 决定前：shared-protocol 不冻结、不开实现；不依赖该决定的 ci-guards 照原计划排队。
