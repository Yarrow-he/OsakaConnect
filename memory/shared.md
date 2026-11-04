# memory/shared.md

全员必读，组长维护，最多 10 条，其他人只读。

1. 密钥、令牌只走环境变量，不进仓库、日志、聊天。
2. 接口以 openapi/admin.v1.yaml 与 shared/ 为准，改接口先找组长。
3. 只改 CI 的 PR 合并标准（Yarrow 2026-11-02 22:57/23:04）：必过检查 + guards 全绿即可合。guards-acceptance（android/ 等四个产品目录）、windows-acceptance（windows/）、android-emulator（android/）、a10-docker（server/ + admin/）在对应产品代码进 main 前豁免可红；进 main 后必须绿并加入必过（board tasks.exempt-checks-required）。
