# 第 1 期（0.1.0）交付前验收核对

- 生成/更新：2026-11-07T10:02:30+10:00（Australia/Brisbane）
- board / main tip：`3cf00f7d82e358de9e4281f6a3f77efc0ec7352a`（Merge PR#108）
- 产品证据 CI：main `@3cf00f7`，CI [run 42312486275](https://github.com/Yarrow-he/OsakaConnect/actions/runs/42312486275)（conclusion=success，8/8 job 全绿）
- 既有绿基线仍可追溯：main `@9de89880ea77459b16da87369a23e3285fe7262b`，CI [run 42257087460](https://github.com/Yarrow-he/OsakaConnect/actions/runs/42257087460)（conclusion=success，8/8 job 全绿）
- Yarrow 2026-11-07 09:57：v0.1.0 本期交付不带项目图谱，只交代码地图和流程图；仍送 A/B 评审后开交付 PR。图谱相关阻塞已解除；仍禁未批准预构建。
- 本文件是闸口二工作稿；验收核对、代码地图、流程图已齐；下一步 A/B 审后开交付 PR。

| 编号 | 验收条目 | 结论 | 证据 |
|---|---|---|---|
| A1 | Windows 可构建启动 | 通过 | windows-acceptance @main 3cf00f7 run 42312486275 success；smoke.ps1；见 tests/INDEX.md |
| A2 | 连接/设置占位导航 | 通过 | windows-acceptance smoke.ps1；同上 |
| A3 | 统一错误展示 | 通过 | WinDiagnostics.Tests；windows-acceptance success run 42312486275 |
| A4 | 日志脱敏 | 通过 | WinDiagnostics.Tests；同上 |
| A5 | Android 可构建启动 | 通过 | android + android-emulator jobs success run 42312486275；diagnostics 目录已在 main |
| A6 | Android 占位导航与生命周期 | 通过 | android-emulator success run 42312486275 |
| A7 | Android 日志脱敏 | 通过 | tests/INDEX.md：android-diagnostics unit + instrumented；LogRedactorTest；产品 `android/.../diagnostics` 已在；run 42312486275 |
| A8 | 后台健康/版本 | 通过 | python job pytest tests/server/server-core @3cf00f7 run 42312486275 |
| A9 | 管理协议与鉴权 | 通过 | contract + server-core success run 42312486275 |
| A10 | 管理端安全连接四案 | 通过 | a10-docker success run 42312486275 |
| A11 | 跨端不互引 | 通过 | guards-acceptance cross-refs success run 42312486275 |
| A12 | 模块范围与 PR 行数 | 通过 | 策略仓 pr-size 必过；业务侧脚本本地回归见 INDEX |
| A13 | main 全绿 | 通过 | https://github.com/Yarrow-he/OsakaConnect/actions/runs/42312486275 @ 3cf00f7 conclusion=success |
| A14 | 技术栈锁定 | 通过 | guards-acceptance grep/lang-guard success run 42312486275 |
| A15 | 邀请码 8 位 Crockford | 通过 | tests/INDEX.md：Windows WinDiagnostics.Tests + Android InviteCodeTest + server-core；@main 3cf00f7 run 42312486275 |
| A16 | UI 占位对照 design | 通过 | windows smoke + android-emulator（审阅侧 design inventory 历史已过） |
| A17 | routes[] 可扩展 | 通过 | server-core + lang-guard + windows smoke；run 42312486275 |

## 阻塞/缺口
1. ~~项目图谱尚未生成~~ — Yarrow 2026-11-07 09:57 豁免：本期不交项目图谱。
2. A/B 评审尚未完成（对 code-map.md + flow.mmd；豁免后即可送审）。

## 下一步
1. 送 A/B 审 code-map.md 与 flow.mmd（对照 README / acceptance-check 上下文；不要求项目图谱）。
2. A/B 无严重写错后开交付 PR；仍禁止使用未批准的 Understand-Anything 预构建。
