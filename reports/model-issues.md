# 模型问题记录

| 日期 | 角色 | 模型 | 问题类型 | 一句话描述 | 证据路径 |
|---|---|---|---|---|---|
| 2026-11-02 | 写手-server | Codex gpt-6-astra | 环境/越权拦截 | worktree 未被信任，codex exec 以 read-only 运行，未写任何文件；按组长批准改用 -s workspace-write 重跑 | notes/logs/server-core-codex-20261102-1453.log |
| 2026-11-02 | 测试 | Claude Code + deepseek-flash | 权限配置阻断（非模型越权） | 按 `claude --settings .claude/settings.tester.json` 运行时，项目 .claude/settings.json（审阅只读配置）同时生效，其 deny Write/Edit/pytest 覆盖测试配置；另工作区未受信任导致 allow 条目被忽略；模型未能写任何文件，未越权 | /home/box/osaka/artifacts/cli-logs/server-core-2026-11-02.log |
