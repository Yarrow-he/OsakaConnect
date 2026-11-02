# server-core 实现及验证记录

2026-11-02，按 `notes/server-core-taskcard.md` 和 ponytail full 实现。
任务卡禁止修改 `server/` 以外文件，故记录放在此处；未更新根目录的
`notes/server-core.md`、`board.json` 或已有用户改动。

实现：标准库 HTTPS 入口、health/version、Bearer 鉴权、共享错误文案、
手写出站 schema 校验、ASCII 邀请码归一化。测试直接读取共享 JSON 向量，
覆盖字段类型、长度、未知字段、枚举、版本格式、凭证、日志及启动配置错误。
未增加依赖或执行 Git 写操作。

从 `server/` 执行的验证命令：

| 检查 | 命令 | 输出路径（仓库根相对） | 结果 |
| --- | --- | --- | --- |
| 编译 | `python3 -m compileall -f osaka_server tests` | `server/reports/compile.txt` | 退出码 0 |
| 缩进静态检查 | `python3 -m tabnanny -v osaka_server tests` | `server/reports/lint.txt` | 退出码 0；仅检查缩进 |
| 测试 | `python3 -m pytest -q` | `server/reports/pytest.txt` | 151 passed, 30 skipped；退出码 0 |

本环境没有 ruff、flake8、pyflakes 或 pylint；未联网安装。
首轮真实连接测试因创建本地 socket 返回 `PermissionError: [Errno 1] Operation not permitted`
而失败。已保留真实连接测试并明确跳过受限项，同时补充内存 HTTP 传输，使用真正的
HTTP 请求解析及 handler 覆盖功能。30 个跳过项包括真实连接的鉴权、响应、线路配置
和 TLS 信任链/主机名验证；需在允许本地 socket 的环境重跑同一命令完成 HTTPS 验收。
未连接或修改真实服务器。

新增文件范围：`server/osaka_server/{__init__,app,schema,invite}.py`、
`server/tests/test_{app,contract}.py`、`server/README.md`、`server/.gitignore`、
本文件及 `server/reports/` 验证输出。CLI 会话未另行导出，工具输出保留在本次会话中。
