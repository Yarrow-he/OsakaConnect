# 0.1.0 交付前核对（草稿，pipeline-watchdog 2026-11-07 00:00 Australia/Brisbane）

对照 `acceptance.md` 与 main `@f5b740ec62963123fdff4b845a1cca74270be201`、`tests/INDEX.md`。

| 编号 | 结果 | 证据 |
|---|---|---|
| A1 | 暂记通过 | tests/INDEX：windows-acceptance @674017f run 42202140900；main 后续 CI 亦绿（run 42226665517 @f5b740e） |
| A2 | 暂记通过 | 同 A1 smoke/导航 |
| A3 | 暂记通过 | WinDiagnostics.Tests / windows-dotnet |
| A4 | 暂记通过 | 同 A3 |
| A5 | 暂记通过 | android + android-emulator success |
| A6 | 暂记通过 | 同 A5 |
| A7 | **不通过** | 产品目录 `android/app/src/main/java/com/osakaconnect/diagnostics` 在 main 不存在；INDEX 写明未执行；MainActivity 仍注释未接 LogRedactor |
| A8 | 待核 skip | server-core INDEX「部分」：pytest skip 未核清 |
| A9 | 暂记通过 | contract + server 身份校验（skip 清完前 A8/A9 后台项保留待核） |
| A10 | 暂记通过 | a10-docker success |
| A11 | 暂记通过 | check-cross-refs / guards-acceptance |
| A12 | 暂记通过 | 策略仓 pr-size；业务仓 scripts 本地回归 |
| A13 | 暂记通过 | main CI success run 42226665517 |
| A14 | 暂记通过 | lang-guard / guards-acceptance |
| A15 | **不通过（Android）** | 同 A7；Windows/后台向量另有覆盖 |
| A16 | 暂记通过 | win-shell / android-shell 页面标识 |
| A17 | 暂记通过 | openapi routes[]；grep-guard |

处置：派写手 · Android 从历史 tip `f66b7da` 产品树重开 `android/android-diagnostics-2`，过审过测后再继续交付包。
