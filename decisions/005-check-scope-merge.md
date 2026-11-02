# 005 check-scope 合并规则（分支 lead/check-scope-merge）

## 问题
1. 把 main 合入模块分支（server/server-core、android/android-shell、android/android-diagnostics）时 reports/model-issues.md 冲突，解决后提交被 pre-commit 的 scripts/check-scope.sh 拦截：main 带来的 .github/、tests/、decisions/ 等文件被当成本分支越界改动。
2. 无冲突的自动合并不触发 pre-commit，win-shell、win-diagnostics、admin-connect 的合并提交没有经过 scope 检查，存在缺口。

## 方案
1. scripts/check-scope.sh：合并进行中（存在 MERGE_HEAD，或钩子传入 CHECK_SCOPE_MERGE_REF）且检查暂存区时，暂存的 模式+对象 与对方分支同一路径完全相同（含双方都不存在）的文件跳过 scope 检查；受保护配置（.codex/、.claude/）不豁免；其余文件按原规则检查。
2. 新增 .githooks/pre-merge-commit：自动合并前同样跑 check-scope.sh --staged；此时 git 尚未写 MERGE_HEAD，从 GIT_REFLOG_ACTION（"merge <ref>"）取对方分支。
3. 新增 scripts/test-check-scope-merge.sh：临时仓库自测 5 例：原样取自 main 的文件放行；合并时改过的模块外文件拦截；干净自动合并放行；自动合并改出的模块外内容被新钩子拦截（证明钩子执行）；受保护配置原样取自 main 也拦截。
4. CI 的 guards（PR 用 base...HEAD 检查）不变；本 PR 不改 ci.yml，自测暂不进 CI（后续另提）。

## 理由
1. 原样取自 main 的内容已经过 main 的审阅和保护，本分支并未改它，不应算越界。
2. 只要内容与对方分支有一个字节不同就照常检查，合并冲突里夹带的改动仍会被拦截。
3. 补上自动合并的检查缺口，规则对“有冲突/无冲突”一致。

## 风险
1. 放行依据是“与对方分支相同”，若合并的对方分支不是 main（如另一个模块分支），其越界内容会被原样带入；需靠只合 main 的流程约束，PR 时 CI guards 仍按 base 检查。
2. CHECK_SCOPE_MERGE_REF 只在 git 合并流程中（git 目录存在 AUTO_MERGE）才采信，平时手设无效；合并中手设仍可指向任意已存在提交，CI 不读该变量。
3. GIT_REFLOG_ACTION 格式若随 git 版本变化，钩子取不到对方分支时退回原规则（更严，不会更松）。
4. 已经推送的 3 个自动合并提交不追溯。
5. 钩子只在本地设置 core.hooksPath=.githooks 时生效，快进合并不产生合并提交；最终防线仍是 PR 时 CI guards 按 base...HEAD 检查。
6. 自测未接入 CI，后续单独提 ci.yml 变更。

## 第一轮评审（2026-11-02，新会话、互不可见，只喂本稿初版 + diff；日志 logs/check-scope-merge/review-A.log、review-B.log）

### 评审 A：codex exec，gpt-6-astra，high
1. 反对：当前实现扩大豁免范围，与“受保护配置规则不变”的声明不符。
2. 最大风险：仅比较对象哈希，忽略文件模式；提前跳过全部规则，可放行受保护配置的修改、删除。
3. 更好替代方案：先检查受保护路径；验证真实合并及可信 main 提交，同时比较内容和模式；补充绕过、保护路径及钩子触发测试并接入 CI。
4. 有削弱：伪造 CHECK_SCOPE_MERGE_REF 即可在非合并时豁免；任意来源提交均可匹配。自动合并成功测试不能证明钩子执行。
5. 未见直接泄露密钥代码；保护路径绕过会增加敏感文件误提交风险。
6. 未见联网或提权。测试会创建、删除临时仓库并修改其 Git 配置；新增脚本带可执行位。

### 评审 B：ds-claude -p，deepseek-v4-flash，high
1. 反对当前版本。

2. 最大风险：自动合并的检查缺口并未真正补上——钩子只有在本地配置了 `core.hooksPath` 后才会生效，而 `--no-verify`、`--no-commit`、快进合并都会跳过钩子；测试3无论钩子是否触发都会报“通过”，属于空断言，再加上 CI 未改，检查实际上被绕过。

3. 替代：改在 CI 中用 base...HEAD 强制校验合并提交；check-scope 改为用 `git diff --name-only HEAD MERGE_HEAD` 求交集来判断“原样继承”，不使用可被人为设置的环境变量；测试3应断言越界文件确实被拦截。

4. 有削弱：`if f in frommerge: continue` 位于受保护配置删除判断之前，双方都缺失的路径会被判为“相同”而放行，受保护文件可能借此被删除；另外 `CHECK_SCOPE_MERGE_REF` 可被手动设置。

5. 无密钥泄露风险。

6. 无联网；仅在临时目录执行本地 git 操作，无权限提升，但钩子会自动执行仓库内脚本。

### 组长处理
1. 采纳：受保护配置不豁免（A、B）；比较时含文件模式（A）；测试 3 空断言问题，新增例 4 证明钩子确实拦截、例 5 受保护配置（A、B）。
2. 未采纳：钩子依赖本地 hooksPath、快进跳过（B）、自测接 CI（A），记入风险 5、6；CHECK_SCOPE_MERGE_REF 可手设（A、B）记入风险 2。

## 第二轮评审（只喂第一轮修改后的本稿 + diff；日志 review-A-r2.log、review-B-r2.log）

### 评审 A：codex exec，gpt-6-astra，high
1. 反对：模式比较和保护路径豁免问题已修复，但来源校验与测试仍有缺口。
2. 最大风险：可手设 CHECK_SCOPE_MERGE_REF 指向任意提交，在非合并状态放行越界内容；默认回退到名为 none 的引用也不妥。
3. 更好替代方案：校验真实合并及可信 main 来源；测试文件先建于共同祖先，断言钩子拒绝原因，并接入 CI。
4. 有削弱。测试4两边独立新增同名文件，可能因新增冲突失败而假通过；测试5继承该冲突，不能证明保护检查生效。
5. 未见直接泄露密钥代码；任意来源豁免增加敏感文件误提交风险。
6. 无联网或提权操作；测试创建、删除临时仓库并修改其 Git 配置；新增脚本具有执行权限。

### 评审 B：ds-claude -p，deepseek-v4-flash，high
1. 有条件同意：在豁免文件之前仍会判断删除和受保护路径，并比较文件模式与对象，方向正确；但对于自动合并这一缺口，仍未做到强制关闭。
2. 最大风险：pre-merge-commit 钩子只在本地配置了 core.hooksPath 时生效，`--no-verify` 和快进合并都能绕过；`git pull` 时 GIT_REFLOG_ACTION 可能不是 `merge …` 格式，导致取不到 mref（对方分支引用）而退回严格规则，反而会卡住正常拉取；此外 CI 未改动。
3. 替代：把逐文件循环改成用 `git diff --cached --name-only "$mref"` 一次性求差集；删掉 CHECK_SCOPE_MERGE_REF 这个可被伪造的入口，不通过环境变量传入对方分支；在 CI 中对合并提交按 base...HEAD 做强制校验，并入自测。
4. 有削弱：该变量在不进行合并时也能设置，能豁免任意已存在的内容；测试 3 仍是空断言，只靠例 4 间接证明钩子确实执行。
5. 密钥：未见密钥或凭证输出，受保护路径仍会拦截，无泄露风险。
6. 网络/权限：无联网、无提权；自测只在 mktemp 临时仓库中修改本地 git config 后清理；钩子会在本地自动执行仓库内脚本（属既有风险）。

### 组长处理
1. 采纳：例 4、5 原先因双方新增同名文件可能冲突而假通过（A），改为共同祖先先建文件，并断言拒绝原因、断言无 CONFLICT；CHECK_SCOPE_MERGE_REF 改为仅在存在 AUTO_MERGE 时采信，去掉 none 回退（A、B）。
2. 未采纳：钩子依赖本地 hooksPath、快进与 --no-verify（B，后者本就禁止）、自测接 CI（A、B），见风险 5、6。git pull 取不到对方分支时退回原规则（更严）。

## 第三轮评审（只喂第二轮修改后的本稿 + diff；日志 review-A-r3.log、review-B-r3.log）

### 评审 A：codex exec，gpt-6-astra，high
1. 反对当前版本：模式比较、保护路径及测试断言已改进，但豁免来源仍不可信。
2. 最大风险：未限定可信 main；仅检查 AUTO_MERGE 文件存在，不能证明真实合并，配合环境变量可指定任意提交放行越界文件。
3. 更好替代方案：验证真实合并与可信 main 提交；严格校验钩子输入，失败时不豁免；补充伪造状态、非 main 来源测试并接入 CI。
4. 有削弱：与任意来源一致即可跳过 scope；本地钩子仍可跳过。稿称 CI 不变，无法据此确认兜底有效。
5. 未见直接泄露密钥代码；扩大豁免可能增加敏感文件误提交风险。
6. 无联网、提权操作；自测创建并删除临时仓库、修改其 Git 配置；新增脚本带执行位，钩子自动执行脚本。

### 评审 B：ds-claude -p，deepseek-v4-flash，high
1. 反对：合并豁免依赖 AUTO_MERGE 文件，但干净合并时 git 通常不写它；GIT_REFLOG_ACTION 也未必传给钩子。mref 取不到，就退回严格规则，反而会拦下正常带入 main 的 .github/、decisions/ 的合并；decision.md 也没给自测结果，测试 3 近乎空断言。
2. 最大风险：原本要补上的缺口没补上，正常合并却被卡住，开发者会用 --no-verify 绕过，缺口更大。
3. 替代方案：删掉 CHECK_SCOPE_MERGE_REF 与本地 pre-merge-commit，改由 CI 按 base...HEAD 校验，并用 HEAD^2 的 blob 和 mode 求交集来认定“原样继承”，同时把自测接进 CI。
4. 有削弱：.git/AUTO_MERGE 可手工创建，配合该变量仍能按任意提交豁免；不过受保护路径已正确不豁免，删除判断也仍在其前。
5. 无密钥泄露风险；但“任意来源可豁免”会放大敏感文件误提交的可能。
6. 无联网、无提权；自测仅在 mktemp 临时仓库改本地 git config 后清理；钩子会自动执行仓库内脚本，属既有风险。

### 组长备注
1. B 称干净合并不写 AUTO_MERGE：与本机实测不符（git 2.47.3，pre-merge-commit 钩子内 git 目录有 AUTO_MERGE，GIT_REFLOG_ACTION=merge main）；自测 3、4 证明钩子拿到对方分支并按规则放行或拦截。
2. 自测结果（2026-11-02，bash scripts/test-check-scope-merge.sh）：5 例全部通过，退出码 0。
3. 未解决的分歧：豁免来源未限定为可信 main（A、B）；B 建议改由 CI 用 HEAD^2 校验合并提交。三轮 A、B 均反对，按二类交 Hao Yin 决定。
