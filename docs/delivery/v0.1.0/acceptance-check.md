# 第 1 期（0.1.0）交付前验收核对（草稿）

- 生成时间：2026-11-07T03:55:16+10:00（Australia/Brisbane）
- main：`9de89880ea77459b16da87369a23e3285fe7262b`
- 依据 CI：[run 42257087460](https://github.com/Yarrow-he/OsakaConnect/actions/runs/42257087460)（conclusion=success）
- 状态说明：本文件是闸口二①②的工作草稿；标「待核对」的条目要等测试刷新 `tests/INDEX.md` 后才能改成通过或打回。
- 尚未完成：代码地图、mermaid 流程图、项目图谱、A/B 评审。

| 编号 | 验收条目 | 结论 | 证据 |
|---|---|---|---|
| A1 | Windows 可构建启动 | 通过 | windows-acceptance @main 9de8988 run 42257087460 success；smoke.ps1 |
| A2 | 连接/设置占位导航 | 通过 | windows-acceptance smoke.ps1；同上 |
| A3 | 统一错误展示 | 通过 | WinDiagnostics.Tests；windows-acceptance success run 42257087460 |
| A4 | 日志脱敏 | 通过 | WinDiagnostics.Tests；同上 |
| A5 | Android 可构建启动 | 通过 | android + android-emulator jobs success run 42257087460；diagnostics 目录已在 main |
| A6 | Android 占位导航与生命周期 | 通过 | android-emulator success run 42257087460 |
| A7 | Android 日志脱敏 | 待核对 | INDEX 仍写 @674017f 未含 diagnostics；#104 已合入产品目录，需测试刷新 INDEX 并确认单测覆盖 |
| A8 | 后台健康/版本 | 通过 | python job pytest tests/server/server-core success run 42257087460（skip 数仍待 INDEX 核） |
| A9 | 管理协议与鉴权 | 通过 | contract + server-core success run 42257087460 |
| A10 | 管理端安全连接四案 | 通过 | a10-docker success run 42257087460 |
| A11 | 跨端不互引 | 通过 | guards-acceptance cross-refs success run 42257087460 |
| A12 | 模块范围与 PR 行数 | 通过 | 策略仓 pr-size 必过；业务侧脚本本地回归见 INDEX |
| A13 | main 全绿 | 通过 | https://github.com/Yarrow-he/OsakaConnect/actions/runs/42257087460 @ 9de8988 conclusion=success |
| A14 | 技术栈锁定 | 通过 | guards-acceptance grep/lang-guard success run 42257087460 |
| A15 | 邀请码 8 位 Crockford | 待核对 | Windows/服务端有覆盖；Android 侧随 diagnostics 合入后需测试确认 INDEX |
| A16 | UI 占位对照 design | 通过 | windows smoke + android-emulator（审阅侧 design inventory 历史已过） |
| A17 | routes[] 可扩展 | 通过 | server-core + lang-guard + windows smoke；run 42257087460 |

## 阻塞/缺口
1. `tests/INDEX.md` 仍停在 main `@674017f`，未反映 #104 合入后的 android-diagnostics（A7/A15）。
2. 服务端 pytest 历史 skip 数未在 INDEX 核清。
3. 交付三件套与 A/B 评审未开工。

## 下一步
1. 测试刷新 INDEX（绑定当前 main head）。
2. 组长补代码地图与 mermaid；请服务端写手按 ops 有效标签生成项目图谱。
3. 全部通过后送 A/B 审一轮，再交 Yarrow。
