# 派活 brief：win-shell（Windows）

- owner：写手 · Windows　分支：`windows/win-shell`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
.NET 8 WPF 应用入口；侧栏导航与 连接(W1)/设置(W4) 占位页，页标识与设计稿编号一致；--self-test 启动自检退出码 0。

## 只准改的文件
- `windows/`
- `notes/win-shell.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
无网络接口；design/inventory.md 第 2 节（仓库外 /home/box/osaka/design，只读参考）。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A1, A2, A16, A14（见 `acceptance.md`）。完成标准：dotnet build/test 通过；UI 测试切换两页断言页标识 W1/W4；--self-test 退出码 0；源码无线路名字面量。

## 禁止事项
- 改清单外文件（`scripts/check-scope.sh` 会拦）；改 `.codex/`、`.claude/` 或任何规则/权限配置
- 新增依赖（未经组长批准）；跳过证书校验；硬编码线路
- 密钥、令牌、私钥写进代码/配置/日志/测试/提交；示例只用明显假值
- `--no-verify`、git merge/push/rebase/强推/删分支；联网；登录或改动真实服务器

## 交付要求
交付时把看板 state 改为 待审测并追加 history；在本文件末尾追加：
- 编译、lint、测试命令及各自输出文件路径
- 本次 CLI 会话日志路径
- 改动文件清单（`git diff --name-only main...`）与已知限制
