# 007 CI 校验模块分支的合并提交（B 方案，分支 lead/ci-merge-scope）

Hao Yin 2026-11-02 22:13 决定走 B 方案，PR #9（决策 005）关闭不合并。

## 问题
1. main 合入模块分支时，本地 pre-commit 的 check-scope 把 main 带来的文件当成越界，冲突合并无法提交。
2. 无冲突的自动合并不触发 pre-commit，合并提交没有任何 scope 检查（win-shell、win-diagnostics、admin-connect 已有 4 个这样的合并）。

## 方案
1. 新增 scripts/check-merge-scope.sh：对 <base>..<tip> 内每个合并提交 M，取 `git diff M^2 M` 的文件（合并结果与合入侧不同的文件），必须在本模块 paths 及既有附加路径（notes/、board.json、reports/、memory/、tests/INDEX.md）内；受保护配置（.codex/、.claude/）一律拦截。scope 读 M 里的 board.json。非模块分支直接通过。
2. ci.yml guards 任务新增两步：merge-scope（push 和 PR 都跑；PR 时 tip 用 PR 头提交，避开 GitHub 合成的 PR 合并提交）与该脚本自测。
3. scripts/check-scope.sh：仅当检查暂存区且存在 MERGE_HEAD（合并进行中）时，本地只拦受保护配置，其余交给 CI；非合并提交逻辑不变。
4. 新增 scripts/test-check-merge-scope.sh：7 个断言，覆盖原样合并放行、合并夹带模块外改动被 CI 拦、受保护配置本地与 CI 都拦、非合并提交照旧拦、非模块分支通过。

## 理由
1. 判定放在 CI，不依赖本地 hooksPath，也不依赖可伪造的环境变量；自动合并、冲突合并、--no-verify 都逃不过。
2. 与 M^2 比较：原样取自 main 的内容不算本分支改动；合并时夹带的任何改动都会被看到。
3. 本地只在合并进行中放宽非受保护路径，非合并提交不受影响。

## 风险
1. 本地合并提交的模块外夹带要到推送后 CI 才发现（本地已不拦），需在 CI 红后修正再推。
2. M^2 不是 main 时（例如合入另一个模块分支），与 M^2 相同的越界内容会被放行；流程上模块分支只合 main。
3. 模块分支历史上已有越界改动时，之后每个合并提交都会报出这些文件，需要先清理。
4. 本地 MERGE_HEAD 可被手工伪造来跳过本地 scope，但 CI 仍按提交历史检查。
5. 自测接进 CI，每次多几秒。

## 评审记录（2026-11-02，仅一轮；新会话、互不可见，只喂本稿 + diff；日志 logs/ci-merge-scope/）

### 评审 A：codex exec，gpt-6-astra，high
1. 反对：当前实现不足以支撑“合并夹带无法逃过检查”的结论。
2. 最大风险：从被检查提交读取 board.json，且允许修改该文件；可扩大 paths、清空 protected 或删除模块定义，使检查放行。
3. 更好替代方案：从可信基线读取规则，校验合入侧来自 main；补充规则篡改、非 main 合并及脚本异常测试。
4. 有削弱：MERGE_HEAD 存在时，本地跳过非保护路径检查；CI 仅比较第二父提交，会漏掉从该侧原样带入的越界内容。自测还把任意非零退出视为成功拦截。
5. 未见直接泄露密钥的代码，但保护规则可被篡改，降低敏感文件防护。
6. 未见联网或系统权限调整；自测创建并删除临时仓库、设置仓库级 Git 身份，并执行 reset --hard。

### 评审 B：ds-claude -p，deepseek-v4-flash，high
1. 有条件同意：把判定放到 CI 这个方向是对的，但下面提到的漏洞需要先补上。
2. 最大风险：board.json 由被检查的分支自己提供，而它自身的改动又被列入允许范围（extra），分支就能改宽 paths 或删掉 protected 字段，从而绕过 scope 与受保护配置检查；另外，若 checkout 使用默认的浅克隆，就缺少 origin/main 和合并历史，rev-list 容易报错，或者枚举不到合并提交。
3. 更好的替代方案：让 scope 配置从 base 的 board.json 读取，再与分支的配置取交集；在 CI 里显式 fetch base 分支并取足够深度；本地合并时只放行与 M^2 相同的文件，而不是一律放行。
4. 有削弱：本地合并时，只要 MERGE_HEAD 存在，除受保护配置外一律放行，而 MERGE_HEAD 可以伪造；CI 的非合并提交仍只在 PR 上检查，推送时可 --no-verify 绕过；当 M^2 不是 main 时放行越界内容。
5. 无泄露风险：没有引用 secrets，没有 set -x，也没有输出敏感内容。
6. 无：只调用本地 git，并使用 mktemp 创建临时仓库，不联网、不修改权限。

### 组长备注
1. 按规则只评一轮，本稿未据评审修改。
2. B 的浅克隆顾虑不成立：guards 任务 checkout 已设 fetch-depth: 0。
3. A、B 都指出 board.json 取自被检查提交、可被本分支改宽规则，这是本方案最大的未解决风险，交 Hao Yin 决定是否先改为读 base 的 board.json。
4. 档位：改动检查且涉及合并豁免，二类。

## 修订（2026-11-02 22:23 Hao Yin：先改成从 main 读 board.json，补好再交我批）
1. check-merge-scope.sh 的 scope 与受保护清单一律读 origin/main 的 board.json（MAIN_REF，默认 origin/main），不再读被检提交里的副本。
2. 新增：合并提交的合入侧 M^2 必须在 origin/main 上，否则拦截。
3. 自测改为断言具体的拦截原因（不再把任意非零退出当作拦截）；新增用例 6（分支自行放宽 scope 仍拦截）、7（合入侧不在 main 上拦截），共 9 项全部通过。
4. 同一套新用例在旧脚本上跑，6、7 失败，证明新用例确实覆盖了 A、B 共同指出的风险。
5. 这是对评审共同风险的修补，不另开 A/B 评审轮；仍为二类，等 Hao Yin 批准。

### 模型核对补记（2026-11-03，追加）
1. 评审 B 的会话原始记录：~/.ds-claude/projects/-tmp-rvB7/cb99b0f0-7983-43e8-b744-9f6d47975eb9.jsonl（最后写入 2026-11-02 22:15:30 +10:00，内容含 check-merge-scope/MERGE_HEAD，对应本稿评审）。
2. 该记录 7 处 model 字段均为 "deepseek-v4-flash"，与上文“deepseek-v4-flash”一致。
3. ds-claude 包装脚本默认 ANTHROPIC_MODEL=deepseek-flash；同目录另有 2 个 deepseek-flash 会话（-tmp-rvA9*，2026-11-02 23:24/23:28），与本稿无关。
4. 评审 A（codex）的模型未在此补核。
