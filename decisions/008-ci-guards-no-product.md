# 008 guards 在无产品代码时为何 exit 3，以及拆分方案

## 根因
1. grep-guard.sh（第 17-21 行）与 lang-guard.sh（第 61 行）在 windows/ admin/ android/ server/ 都不存在时 exit 3。
2. 这不是缺陷，是 PR #8（决策 004，Hao Yin 21:31 批准「没东西测就亮红灯」）的有意设计：未交付即不绿，防止假绿。
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
1. 二类：改动工作流结构，guards 这个检查名的内容变少（两个守卫移出到新任务），视为可能放宽，需 Hao Yin 批准。
2. 合并后新任务名 guards-acceptance 需另行经 Hao Yin 批准才加入必过检查（第二阶段）。
