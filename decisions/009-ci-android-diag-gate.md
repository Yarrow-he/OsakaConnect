# 009 android-emulator：diagnostics 验收按产品目录是否存在执行

## 根因
1. android-emulator 在每个带 android/ 的分支上都跑 tests/android/android-diagnostics/run.sh 与 run-instrumented.sh。
2. 这两个脚本把 JvmJsonDivergenceTest.kt 拷进 app 测试源集，它依赖 JvmJson.kt；JvmJson.kt 只在 android/android-diagnostics 分支上。
3. 所以 android-shell @1b48848 的 android-emulator 红，来自别的模块未交付，不是 android-shell 自身问题（JvmJsonDivergenceTest.kt:61）。

## 方案
1. 只在 android/app/src/main/java/com/osakaconnect/diagnostics 存在时跑这两个脚本；不存在时打 ::warning:: 待测，不算失败。
2. 目录存在时照跑，任一失败任务仍红；android-shell 的 ci-run.sh 与 connectedDebugAndroidTest 不变。

## 档位
1. 二类：按文件是否存在决定是否执行已有测试，属于有条件放宽（产品目录被改名/删掉时 diagnostics 验收会静默跳过，只留 warning）。需 Hao Yin 批准，组长不自行合并。
