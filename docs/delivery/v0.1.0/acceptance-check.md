# 第 1 期（0.1.0）交付前验收核对

- 生成/更新：2026-11-07T10:14:19+10:00（Australia/Brisbane）
- INDEX/产品证据仍绑：main `@9de89880ea77459b16da87369a23e3285fe7262b`，CI [run 42257087460](https://github.com/Yarrow-he/OsakaConnect/actions/runs/42257087460)（conclusion=success，8/8 job 全绿）；见 `tests/INDEX.md`
- 旁注（非 INDEX）：当前 main tip 为 `3cf00f7d82e358de9e4281f6a3f77efc0ec7352a`（Merge PR#108），CI [run 42312486275](https://github.com/Yarrow-he/OsakaConnect/actions/runs/42312486275) 亦全绿（8/8）；INDEX 尚未改记该 tip，故表内「见 INDEX」条目仍以 9de8988 / 42257087460 为准
- Yarrow 2026-11-07 09:57：v0.1.0 本期交付不带项目图谱，只交代码地图和流程图；架构接线（壳接诊断等）放下一期，本期只补文档说明
- 本文件是闸口二交付材料；A/B 审了两轮：第一轮（`/home/box/osaka/logs/delivery-v010/ab-review/`）A 认为可以执行，B 提了 6 条；第二轮（`ab-review-20261107-0958/`）A、B 最终都判否；Yarrow 2026-11-07 10:06 定只补文档说明、架构改动放下一期，补完没有再送 A/B 重评；本 PR #110 交 Yarrow 批准（第 2 类）

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
| A16 | UI 占位对照 design | 未验证 | 只有 windows smoke + android-emulator 冒烟；design inventory 在仓库外 `/home/box/osaka/design/inventory.md`，notes/win-shell.md:16、notes/android-shell.md:16 只把它当只读参考，没有逐项人工对照记录 |
| A17 | routes[] 可扩展 | 通过 | server-core + lang-guard + windows smoke；run 42257087460 |

## 阻塞/缺口
1. 项目图谱本期不交（Yarrow 2026-11-07 09:57 决定），不算缺口。
2. A/B 意见不一致，按 Yarrow 10:06 的决定只补了文档说明（win-shell 依赖澄清、android-shell 测试实路径、flow 边与标签、A16 证据路径、INDEX/证据表述拆开）。架构接线（壳接诊断）下放下一期，本期不改产品代码。
3. A16 缺逐项人工对照记录，目前只算未验证，需要补对照或由 Yarrow 手动验收。
4. 等 Yarrow 对当前 head 批准，以及审阅、测试对当前 head 的结论。

## 下一步
1. 本 PR #110（`lead/delivery-v010`）交 Yarrow 批准（第 2 类）。
2. Yarrow 批准后由组长合并；架构改动留下一期评。
