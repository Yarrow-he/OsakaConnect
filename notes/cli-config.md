# CLI 配置备注

- 审阅：Claude Code + GLM，项目配置 `.claude/settings.json`；本机已有包装命令 `~/.local/bin/glm`（配置目录 ~/.glm-claude，key 取 GLM_API_KEY）。
- 测试：Claude Code + DeepSeek，项目配置 `.claude/settings.tester.json`（`claude --settings .claude/settings.tester.json`）；本机已有包装 `~/.local/bin/ds-claude`，但其 OSAKA_ROLE 默认值仍为 reviewer、glm 默认 tester，与新分工相反，待组长在本机调整（不在仓库内）。
- 模型名 `glm-5.3`、`deepseek-flash` 取自本机包装脚本，未向接口核实。
