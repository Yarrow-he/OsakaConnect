
## 审阅（2584a5a，base bfa016c，2026-11-03）
结论：必须改
日志：/home/box/osaka/logs/pr-line-check/review-2584a5a-20261103T025556.log
1. 必须改 pr_line_check.py:64-69 scaffold_exempt：PR_TITLE 以「脚手架/scaffold」开头即豁免，标题由作者控制，可绕过。改为仅认 label（受权限保护）或组长白名单。可机器化：加用例「仅标题不豁免」。
2. 必须改 pr_line_check.py:19-21 EXCLUDED_DIRS：缺 test-vectors，shared/test-vectors/*.json（测试数据）被计入。可机器化：用例加 shared/test-vectors/x.json。
3. 建议改 :76 加 --no-renames，避免 rename 路径「{a => b}」判定不一致；:44 "Tests" in path 过宽（文件名含 Tests 即免计）。
4. 建议改 :22/:51 锁名单补 pnpm-lock.yaml、go.sum。
5. 建议改 /no-comments：:50「# 锁文件」、:53「# 生成文件」、:87「# 二进制」复述代码，可删。
6. 待组长定：第10条脚手架豁免原文未在 docs/ 或规则文件中找到；本脚本尚未接入 CI（INDEX.md 自述），接入前不能算不可绕过。

## 审阅 返工#1（175d3e6，2026-11-03）
结论：必须改（重复：否，新项来自组长拍板）
证明：logs/pr-line-check/2026-11-03-test-v2.log（命令、SHA 175d3e6、6/6 OK）
日志：/home/box/osaka/logs/pr-line-check/review-175d3e6-20261103T030716.log
1. 上次必须改 1（标题豁免）已修，:66-68 只读 PR_LABELS，有正反用例；上次必须改 2（test-vectors）已修 :20，有用例。
2. 必须改 pr_line_check.py:17-21 EXCLUDED_DIRS 缺 ".github"、"scripts"，与组长拍板（CI/工具不计入）不符。可机器化：两条用例写 300 行断言计入 0。
3. 建议改 :44 排除目录匹配任意层级（server/notes/x.py 也免计），可改为顶级前缀；:46-47 注释复述代码，dirs 与 parts 重复 split。
4. 组长拍板：label scaffold 豁免正确；CI 接入不阻塞。

## 审阅 返工#2（7f197da，2026-11-03）
结论：通过
证明：logs/pr-line-check/2026-11-03-test-v3.log（命令、SHA 7f197da、8/8 OK）
日志：/home/box/osaka/logs/pr-line-check/review-7f197da-20261103T031247.log
1. 上次必须改（.github/scripts）已修 :17/:45，用例 test:117-123；反向用例 server/notes 计入 :125-130。
2. 建议改 :49 任意层级 tests/、*.tests 可藏产品代码（如 windows/tests/x.cs）。改名需在 diff 中可见，审阅可拦，故不阻塞。修法：白名单 parts[0]=="tests" 或 server/tests；可机器化：反向用例 windows/tests/x.cs 断言计入。:47 build/bin/obj/generated 同理。
3. 建议改 :17 补 .githooks（同类工具，现计入）；:51 用小写 p 与 :49 一致。
4. /no-comments 达标。
