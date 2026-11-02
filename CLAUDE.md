# CLAUDE.md — OsakaConnect 0.1.0（与 AGENTS.md 同步）

醒来只读：`board.json`、自己的 `memory/<角色>.md`、`memory/shared.md`、本次任务需要的 `notes/<模块>.md`。动手前查 `docs/architecture.md`。

硬规则：
1. 最小修改：按 ponytail full 执行；只改任务卡列出的文件和 board.json scope 内路径；不顺手重构。
2. 不新增依赖：新增第三方包须组长批准并写进决策文件。
3. 交付带证明：编译、lint、测试命令与输出文件路径写进 `notes/<模块>.md`；没有证明不算交付。
4. 禁止 `--no-verify`；pre-commit（`scripts/check-scope.sh`）和 CI 不通过就改代码，不改检查。
5. 不改权限配置：`.codex/`、`.claude/` 及任何 bot 规则只由 Hao Yin 决定。
6. 密钥只走环境变量：不写进代码、配置、日志、测试向量、提交或聊天；示例值用明显假值。
7. 禁止：git merge、删除分支、强推、改历史、联网、登录或改动真实服务器。
8. 状态流转与 history 记录按 board.json 规则；返工 3 次后上报。

## 角色与模型（Hao Yin 2026-11-02 批准）
| 角色 | CLI | 后端 | 项目配置 |
|---|---|---|---|
| 写手（Windows/Android/服务端） | Codex | gpt-6-astra，high | `.codex/config.toml` + `.codex/rules/osaka.rules` |
| 审阅 | Claude Code | GLM（智谱 Anthropic 兼容接口），key 取 `GLM_API_KEY` | `.claude/settings.json`（只读 + git diff/log/show） |
| 测试 | Claude Code | DeepSeek（Anthropic 兼容接口），key 取 `DEEPSEEK_API_KEY` | `.claude/settings.tester.json`（放行跑测试、虚拟机/模拟器；禁改宿主机路由、DNS、代理、防火墙） |
| 决策评审会 | A：Codex gpt-6-astra high；B：DeepSeek v4-flash high | — | 新会话、互不可见 |
