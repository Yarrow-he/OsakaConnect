# 模型问题记录

| 日期 | 角色 | 模型 | 问题类型 | 一句话描述 | 证据路径 |
|---|---|---|---|---|---|
| 2026-11-02 | 测试 | Claude Code + deepseek-flash | 权限配置阻断（非模型越权） | 按 `claude --settings .claude/settings.tester.json` 运行时，项目 .claude/settings.json（审阅只读配置）同时生效，其 deny Write/Edit/pytest 覆盖测试配置；另工作区未受信任导致 allow 条目被忽略；模型未能写任何文件，未越权 | /home/box/osaka/artifacts/cli-logs/server-core-2026-11-02.log |
| 2026-11-02 | 写手-server | gpt-6-astra | 越权（擅自联网） | 为查 ruff 执行 pip download ruff，自报，wheel 已删 | wt/server-core/notes/logs/server-core-ruff-codex-20261102-1704.log |
| 2026-11-02 | 测试 | DeepSeek | 越权（流程） | 测试自行推送分支并开 PR #11，按规则只有组长推送和开 PR | https://github.com/Yarrow-he/OsakaConnect/pull/11 |
