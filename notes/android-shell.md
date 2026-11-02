# 派活 brief：android-shell（Android）

- owner：写手 · Android　分支：`android/android-shell`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
Kotlin 单 Activity；未激活(P1)/已激活(P2) 占位与导航；生命周期回调经 LogRedactor 记录。

## 只准改的文件
- `android/`
- `notes/android-shell.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
design/inventory.md 第 1 节（只读参考）。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A5, A6, A16, A14（见 `acceptance.md`）。完成标准：./gradlew assembleDebug testDebugUnitTest 通过；仪器测试两页切换、旋转与后台回前台不崩溃且生命周期被记录。

## 禁止事项
- 改清单外文件（`scripts/check-scope.sh` 会拦）；改 `.codex/`、`.claude/` 或任何规则/权限配置
- 新增依赖（未经组长批准）；跳过证书校验；硬编码线路
- 密钥、令牌、私钥写进代码/配置/日志/测试/提交；示例只用明显假值
- `--no-verify`、git merge/push/rebase/强推/删分支；联网；登录或改动真实服务器

## 交付要求
交付时把看板 state 改为 待审测并追加 history；在本文件末尾追加：
- 编译、lint、测试命令及各自输出文件路径
- 本次 CLI 会话日志路径
- 改动文件清单（`git diff --name-only main...`）与已知限制

## 交付证明（2026-11-02）
1. CI：https://github.com/Yarrow-he/OsakaConnect/actions/runs/41403200131 ，结论 success，android job 跑 `gradle --no-daemon assembleDebug testDebugUnitTest lint`（Gradle 8.9，JDK 17）。
2. sha：远端 9e47998（组长合入 origin/main 的合并提交），包含本地提交 ae5f609（gh compare：ahead 3，behind 0）。
3. Codex 日志：/home/box/osaka/logs/android-shell-codex-20261102-145542.log ；/home/box/osaka/logs/android-shell-androidx-codex-20261102-165454.log
4. 检查日志： ；CI 核实：/home/box/osaka/logs/android-ci-verify-20261102.log
