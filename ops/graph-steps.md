# 项目图谱：下载、生成、交回步骤

定版位置：业务仓库 `ops/graph-steps.md`，由 Yarrow 打 `ops-v<N>` tag 后生效；读的时候用 `git show ops-v<N>:ops/graph-steps.md`。安全底线（下载前双重通过、不装不联网、清密钥断网、只放交付目录）同时留在组长规则正文里，不搬进本文件。

1. 下载：上游 Understand-Anything（https://github.com/Egonex-AI/Understand-Anything）先由测试做安全扫描、审阅审一遍，对固定提交号 T 都通过并把结论发给组长后，才让服务端写手 bot（不是 Codex）下载；第一次安装和以后每次升级都这样，没通过不下载、不安装。只用 git 按 T 下载源码和它锁定的依赖到 `skills/understand-anything/`，并在 `skills/INDEX.md` 记一行；不跑上游安装脚本、不写用户目录、不建链接、不自动更新；运行时缺依赖或要联网就停下报组长，不现装。
2. 生成：Codex 照旧不联网，服务端写手用他的 Codex CLI 加载，在 main 的提交号 S 的独立检出目录里，用清掉 OSAKA_*_APP_PRIVATE_KEY 等全部密钥环境变量的进程、断网跑 `$understand --language zh`；若工具把分析目录切到别处，判失败。
3. 交回：`knowledge-graph.json`、T、S、会话日志路径；图谱只放进组长的 `docs/delivery/v<期号>/`，由组长随交付 PR 提交，不推到别的分支。

组长规则引用占位：`ops-v<N>`（N 由 Yarrow 打 tag 时填入；规则里写明 tag，读用 `git show ops-v<N>:ops/graph-steps.md`）。
