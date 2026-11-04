# 008 guards 在无产品代码时为何 exit 3，以及拆分方案

## 根因
1. grep-guard.sh（第 17-21 行）与 lang-guard.sh（第 61 行）在 windows/ admin/ android/ server/ 都不存在时 exit 3。
2. 这不是缺陷，是 PR #8（决策 004，Yarrow 21:31 批准「没东西测就亮红灯」）的有意设计：未交付即不绿，防止假绿。
3. 真正的问题是：guards 任务把两类检查混在一起：
   - 流程检查（scope 等）：对任何 PR 都有意义，应能变绿并设为必过；
   - 产品验收守卫（grep-guard、lang-guard）：没有产品时只能是“待测”。
   因此 guards 在产品上 main 之前永远红，无法列为必过检查，scope 检查也被连带失去拦截作用。
4. 把“无产品”当作通过会推翻决策 004，不采用。

## 方案（最小改动）
1. ci.yml：grep-guard、lang-guard 两步原样移到新任务 guards-acceptance；无产品时照旧 exit 3（红）。
2. guards 只留 scope 检查，并加自测 scripts/test-ci-guards-split.py：两个脚本必须在 guards-acceptance 里、不在 guards 里、该任务无 continue-on-error/if、两个脚本仍含 exit 3。
3. 脚本本身一行不改。

## 档位
1. 二类：改动工作流结构，guards 这个检查名的内容变少（两个守卫移出到新任务），视为可能放宽，需 Yarrow 批准。
2. 合并后新任务名 guards-acceptance 需另行经 Yarrow 批准才加入必过检查（第二阶段）。

## 评审（仅一轮）
### 评审 A（codex gpt-6-astra high）
有条件同意

1. **存在合并门槛削弱及自测绕过空间。** 两个守卫未改，仍保留无产品时红灯；但移出 `guards` 后，仅要求 `guards` 必过不能阻止验收失败的 PR 合并。应明确批准这一过渡状态，并在需要验收阻断时将 `guards-acceptance` 设为必过。结构自测仅匹配文本，脚本路径放在注释中、命令加 `|| true`、仅在注释或不可达代码中保留 `exit 3`，均可能通过。另，首个守卫失败后，第二个默认跳过，不能保证两者均执行。

2. **此 diff 未见新增密钥泄露风险。** 没有新增密钥引用、凭据输出或外传命令；但仅凭 diff 无法审计既有守卫脚本的全部行为。

3. **本地没有联网或权限操作。** 本次仅只读查看 `diff.txt`，未运行脚本、修改文件或联网。新增自测也只读文件；新增 CI 任务的 `actions/checkout@v4` 会在 CI 环境获取仓库，diff 未新增提权或权限配置。
tokens used
8,012
有条件同意

1. **存在合并门槛削弱及自测绕过空间。** 两个守卫未改，仍保留无产品时红灯；但移出 `guards` 后，仅要求 `guards` 必过不能阻止验收失败的 PR 合并。应明确批准这一过渡状态，并在需要验收阻断时将 `guards-acceptance` 设为必过。结构自测仅匹配文本，脚本路径放在注释中、命令加 `|| true`、仅在注释或不可达代码中保留 `exit 3`，均可能通过。另，首个守卫失败后，第二个默认跳过，不能保证两者均执行。

2. **此 diff 未见新增密钥泄露风险。** 没有新增密钥引用、凭据输出或外传命令；但仅凭 diff 无法审计既有守卫脚本的全部行为。

3. **本地没有联网或权限操作。** 本次仅只读查看 `diff.txt`，未运行脚本、修改文件或联网。新增自测也只读文件；新增 CI 任务的 `actions/checkout@v4` 会在 CI 环境获取仓库，diff 未新增提权或权限配置。

### 评审 B（DeepSeek v4-flash high，ds-claude -p）
**结论：有条件同意**

1. **是否削弱或可绕过检查？**
   - 短期确有削弱：两个产品验收守卫移出 `guards` 后，原本必过的 `guards` 可在 grep-guard/lang-guard 为红时变绿；而 `guards-acceptance` 按决策 008 要第二阶段才加入必过检查。这中间存在一个窗口——PR 可在产品验收守卫为红的情况下被合并。必须由阶段二的分支保护配置闭环，或明确在首个含产品代码的 PR 之前完成 `guards-acceptance` 必过设置；否则门禁等效性不成立。合并后 main 整体仍会因 `guards-acceptance` 为红（符合决策 004），但 `guards` 单项为绿，可能误导看板/审批。
   - 自测可被绕过：`test-ci-guards-split.py` 只做子串匹配，`run: echo tests/.../grep-guard.sh` 或 `bash ... || true` 都能通过；它只拦 `continue-on-error` 和任意 `if:`，不拦 `|| true`、`set +e`、`|| exit 0`。它也没有检查 `guards` 任务自身被加 `continue-on-error`/`if:` 或 scope 步骤被弱化的可能。且自测与被检查的 ci.yml 在同一 PR 内可被一并修改，本质是回归冒烟测试，不是防篡改控制。
   - 结构上符合决策 004：守卫脚本未改、仍保留 exit 3，拆分方向正确。但 diff 未展示 `guards` 任务原有 checkout 及 lang-guard 的实际 `run:` 行，无法确认两任务均有 checkout，也无法确认自测硬编码的 `tests/android/android-shell/lang-guard.sh` 与 ci.yml 路径一致（不一致会报错而非放行，属可用性而非放宽风险）。另：决策 008 自述为二类改动需 Yarrow 批准，diff 中未见批准记录，同意应以该批准与阶段二必过设置为条件。

2. **是否有密钥泄露风险？**
   - 无。新增 Python 仅读取本地 ci.yml 与两个守卫脚本并打印路径/失败信息，不读环境变量、不引用 `secrets.*`、不输出 token；工作流未新增 secret 或权限声明。新增 job 的 `actions/checkout@v4` 属既有模式（fork PR 下 token 只读），未扩大泄露面。

3. **是否有本地联网或权限操作？**
   - 无。脚本只有 `pathlib` 读文件与正则匹配，无 subprocess、无网络请求、无权限提升、无写文件。工作流层唯一网络行为是标准 checkout，与既有 job 一致；未新增 `permissions:`、`sudo`、凭证或外部调用。

### 组长备注
1. A、B 均为有条件同意，共同点：guards 变绿后、guards-acceptance 加入必过前，存在可在产品守卫为红时合并的窗口；自测只做文本匹配，挡不住 `|| true` 等改法，只是回归冒烟，不是防篡改。
2. A 指出：guards-acceptance 内 grep-guard 失败会跳过 lang-guard（移动前在 guards 内也是如此，非本 PR 引入）。
3. 按规则只评一轮，本稿未据评审修改；交 Yarrow 决定。
