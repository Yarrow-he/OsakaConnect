# 任务卡：server-core 返工1（按 ponytail full 执行）
## 目标
1. server/osaka_server/schema.py:33 version 校验改为 `re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", v)`（ECMA 语义，拒 Unicode 数字和尾部换行），与 openapi/admin.v1.yaml:52 一致。
2. server/tests/test_contract.py:94 中 "1.2.3\n" 与 "١.٢.٣" 期望改为 False。
3. server/osaka_server/app.py 的 handler 覆写 `version_string()` 返回 "osaka-server"（不带 Python 版本）；在 server/tests/test_app.py 加一条最小测试断言 Server 头不含 "Python"（若现有测试结构可方便取头，否则只直接调用 handler 的 version_string）。
4. 删除 server/notes/server-core.md 与 server/reports/*.txt；server/README.md:40 链接改为 `../notes/server-core.md`。
## 只准改的文件
server/osaka_server/schema.py, server/osaka_server/app.py, server/tests/test_contract.py, server/tests/test_app.py（仅新增一条测试）, server/README.md, 删除 server/notes/server-core.md、server/reports/*.txt
## 验收
`python3 -m compileall -q server/osaka_server server/tests` 通过；`cd server && python3 -m pytest -q -p no:cacheprovider` 通过（沙箱 socket 跳过可接受）。
## 禁止
改清单外文件（尤其 shared/、admin-contract.json、invite.py、board.json）；改 .codex/.claude/~/.codex/config.toml；git 任何提交/rebase/push；联网；新增依赖；真实密钥/IP。
