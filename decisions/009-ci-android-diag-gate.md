# 009 android-emulator：diagnostics 验收按产品目录是否存在执行

## 根因
1. android-emulator 在每个带 android/ 的分支上都跑 tests/android/android-diagnostics/run.sh 与 run-instrumented.sh。
2. 这两个脚本把 JvmJsonDivergenceTest.kt 拷进 app 测试源集，它依赖 JvmJson.kt；JvmJson.kt 只在 android/android-diagnostics 分支上。
3. 所以 android-shell @1b48848 的 android-emulator 红，来自别的模块未交付，不是 android-shell 自身问题（JvmJsonDivergenceTest.kt:61）。

## 方案
1. 只在 android/app/src/main/java/com/osakaconnect/diagnostics 存在时跑这两个脚本；不存在时打 ::warning:: 待测，不算失败。
2. 目录存在时照跑，任一失败任务仍红；android-shell 的 ci-run.sh 与 connectedDebugAndroidTest 不变。

## 档位
1. 二类：按文件是否存在决定是否执行已有测试，属于有条件放宽（产品目录被改名/删掉时 diagnostics 验收会静默跳过，只留 warning）。需 Yarrow 批准，组长不自行合并。

## 评审（仅一轮，全文 logs/ci-android-diag-gate/review-A.log、review-B.log）
1. A（codex gpt-6-astra high）：有条件同意。删除或改名 diagnostics 目录即可跳过两项验收、只留 warning；无密钥风险；无联网/权限操作。
2. B（DeepSeek v4-flash high）：有条件同意。同样指出按路径 fail-open；建议 android-diagnostics 分支自身不豁免（缺目录即 error）、固定 $GITHUB_WORKSPACE 路径、两脚本独立执行不短路；无密钥风险；无联网/权限操作。
3. 组长：确认二类，不自行合并；按规则只评一轮，未据评审修改，B 的补强建议交 Yarrow 决定是否采纳。

## 补强（7cd7654，按 Yarrow 指示采纳 B 的三条）
1. android/android-diagnostics 分支（push 用 ref_name，PR 用 head_ref）缺目录即 ::error:: 并失败。
2. 路径改为 $GITHUB_WORKSPACE 绝对路径。
3. 两个脚本都跑，任一失败即红（不再 && 短路）。本机用 sh 模拟 4 种情况（无目录/其他分支 0、无目录/诊断分支 1、有目录且 run.sh 失败 1 且第二个照跑、都过 0）均符合。

## 第二轮评审（新 diff，全文 logs/ci-android-diag-gate/review-A-r2.log、review-B-r2.log）
1. A：有条件同意。仍可绕过：非诊断分支删/改名目录只得 warning（含以诊断分支为目标的 PR）；无密钥风险；无联网/权限操作。
2. B：有条件同意。shell 逻辑无新漏洞；残余 fail-open 同上，分支改名会让保护失效；提醒确认不是 pull_request_target（已核：ci.yml 只用 pull_request 与 push，checkout 为被测代码）。无密钥风险；无联网/权限操作。
3. 组长：仍为二类，交 Yarrow 批准，不自行合并。
