# 第 1 期（0.1.0）交付前验收核对（草稿）

- 生成/更新：2026-11-07T04:52:17+10:00（Australia/Brisbane）
- board 文档基于 main：`f7c8abaa04dc7378e3f7588a0af6f65302e94922`（Merge PR#106 INDEX 刷新）
- INDEX/产品证据仍绑：main `@9de89880ea77459b16da87369a23e3285fe7262b`，CI [run 42257087460](https://github.com/Yarrow-he/OsakaConnect/actions/runs/42257087460)（conclusion=success，8/8 job 全绿）
- 说明：`f7c8aba` 的 CI（run 42268478087）撰写时仍 in_progress；验收结论以已绿的 9de8988 / 42257087460 与 `tests/INDEX.md` 为准。
- 本文件是闸口二工作草稿；代码地图与流程图已补；项目图谱本期不交（Yarrow 2026-11-07 09:57 决定）；还差 A/B 评审。

| 编号 | 验收条目 | 结论 | 证据 |
|---|---|---|---|
| A1 | Windows 可构建启动 | 通过 | windows-acceptance @main 9de8988 run 42257087460 success；smoke.ps1；见 tests/INDEX.md |
| A2 | 连接/设置占位导航 | 通过 | windows-acceptance smoke.ps1；同上 |
| A3 | 统一错误展示 | 通过 | WinDiagnostics.Tests；windows-acceptance success run 42257087460 |
| A4 | 日志脱敏 | 通过 | WinDiagnostics.Tests；同上 |
| A5 | Android 可构建启动 | 通过 | android + android-emulator jobs success run 42257087460；diagnostics 目录已在 main |
| A6 | Android 占位导航与生命周期 | 通过 | android-emulator success run 42257087460 |
| A7 | Android 日志脱敏 | 通过 | tests/INDEX.md：android-diagnostics unit + instrumented @main 9de8988 run 42257087460；LogRedactorTest 3/0；产品 `android/.../diagnostics` 已在 |
| A8 | 后台健康/版本 | 通过 | python job pytest tests/server/server-core @9de8988 run 42257087460：**23 passed / 0 skipped** |
| A9 | 管理协议与鉴权 | 通过 | contract + server-core success run 42257087460 |
| A10 | 管理端安全连接四案 | 通过 | a10-docker success run 42257087460 |
| A11 | 跨端不互引 | 通过 | guards-acceptance cross-refs success run 42257087460 |
| A12 | 模块范围与 PR 行数 | 通过 | 策略仓 pr-size 必过；业务侧脚本本地回归见 INDEX |
| A13 | main 全绿 | 通过 | https://github.com/Yarrow-he/OsakaConnect/actions/runs/42257087460 @ 9de8988 conclusion=success |
| A14 | 技术栈锁定 | 通过 | guards-acceptance grep/lang-guard success run 42257087460 |
| A15 | 邀请码 8 位 Crockford | 通过 | tests/INDEX.md：Windows WinDiagnostics.Tests + Android InviteCodeTest 1/0 + server-core；@main 9de8988 run 42257087460 |
| A16 | UI 占位对照 design | 通过 | windows smoke + android-emulator（审阅侧 design inventory 历史已过） |
| A17 | routes[] 可扩展 | 通过 | server-core + lang-guard + windows smoke；run 42257087460 |

## 阻塞/缺口
1. 项目图谱本期不交（Yarrow 2026-11-07 09:57 决定），不算缺口。
2. A/B 评审还没做。

## 下一步
1. 代码地图和流程图送 A/B 各审一轮。
2. 审完附上意见和处理结果，开交付 PR 交 Yarrow。
