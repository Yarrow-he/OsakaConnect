# 派活 brief：shared-protocol（共用）

- owner：代码组长 · Osaka　分支：`shared/shared-protocol`（从 main 拉，审阅/测试只针对该分支）
- 执行方式：用指定 CLI，**按 ponytail full 执行**；一个模块一个新会话。

## 目标
定稿 openapi/admin.v1.yaml、errors.json、邀请码规则与三份测试向量（含反例）、契约测试。骨架提交已有初版，本模块负责评审后补全与冻结。

## 只准改的文件
- `openapi/`
- `shared/`
- `tests/contract/`
- `notes/shared-protocol.md`（追加交付证明）、`board.json`（本模块条目）、`reports/model-issues.md`（如有）

## 接口引用
openapi/admin.v1.yaml、shared/test-vectors/*.json、shared/errors.json、shared/invite-code.md。架构见 `docs/architecture.md`（含边界 schema 校验方式）。

## 验收条目
A9, A11, A15, A17（见 `acceptance.md`）。完成标准：/tmp/ct/bin/pytest -q tests/contract 全过；反例覆盖缺字段、未知字段、枚举外值、空列表、版本格式。

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

## 2026-11-06 shared-protocol 冻结交付

本次以任务卡为准：负责人为写手 · 服务端，交付目标分支为 `server/shared-protocol-1`；只改本卡允许的协议、向量、契约测试及本 notes，不改 board。Codex 不做 git 操作；分支、提交与 diff 清单由执行器完成（见下文）。上面的旧 brief 分支与看板更新要求不适用于本次。

本工作树缺少 `decisions/010-summary.md`，`acceptance.md` A17 仍为旧文（包含 proto/repeated）；按任务卡转述的 Yarrow 书面决定实现 A17，不新建 proto。组长开 PR 前需核对 PR #77 已进入基线；本次未联网核实，也未修改这些文件。

### 协议冻结与语义

本次交付将 `admin.v1` 定为冻结基线，待组长审阅和测试后合入。此后任何语义变更（字段、约束、错误码、归一化及响应行为）一律升 `admin.v2`：新增 `openapi/admin.v2.yaml`，`info.version` 与 `VersionResponse.protocol` 为 `admin.v2`，端点使用 `/v2/health`、`/v2/version`；新版本向量使用 `shared/test-vectors/admin-contract.v2.json` 等带版本文件名。保留 v1 文件、路径及向量语义，不直接覆盖 v1。

`shared/FROZEN.sha256` 是绊线，真正门禁仍为审阅和测试。清单覆盖 `openapi/`、`shared/` 下全部文件及 `tests/contract/test_contract.py`，共 7 项；排除清单自身，避免自引用，不覆盖 notes。每行格式为 `sha256  相对路径`，前两目录按路径排序，最后为测试文件。测试重新枚举并逐字比较完整清单，文件增加、删除、内容漂移、清单漏项或重复均失败。只在获准修改后重算清单，不以更新散列代替语义评审。

重算命令（在仓库根执行；交付已执行一次）：

```sh
/tmp/ct/bin/python -B - <<'PY'
import hashlib
from pathlib import Path
root = Path.cwd()
manifest = root / 'shared/FROZEN.sha256'
paths = sorted(p for d in ('openapi', 'shared') for p in (root / d).rglob('*') if p.is_file() and p != manifest)
paths += [root / 'tests/contract/test_contract.py']
manifest.write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.relative_to(root).as_posix()}\n' for p in paths), encoding='utf-8')
PY
```

`BACKEND_UNREACHABLE.http: 0` 表示**未收到 HTTP 响应**，如连接失败、超时、TLS 失败等传输层错误；它不是 HTTP 状态码 0。收到真实 401/403/500 响应时，应分别按 `UNAUTHENTICATED`、`FORBIDDEN`、`INTERNAL` 等响应错误处理，不归为传输失败。管理端遇到该传输错误时按 `BACKEND_UNREACHABLE` 读取 `shared/errors.json` 的中文文案“无法连接后台”；本卡仅定义语义，不改管理端生产代码或现有 http 值。

### 改动与验收覆盖

- A9：保留原 spec 校验及全部 admin-contract 正反例；增加 Route 的 64 字符正例、65 字符 id/display_name 反例、嵌套缺字段/未知字段反例及 `v0.1.0`、`.1.0.0` 版本反例。错误码字典键与 OpenAPI `ErrorBody.code` 枚举必须集合相等，少项、多项均失败。
- A11：新增检查只读 `openapi/` 和 `shared/`，不引用各端内部源码。redaction 形状要求非空字符串 mask、非空 cases 数组、每项仅含字符串 in/out；实际脱敏行为仍由各端测试验收。
- A15：从 `shared/invite-code.md` 提取正则运行所有邀请码正反例；归一化仅 trim + 大写，不删除横杠。修正文档原先“提交前去掉横杠”的矛盾，保留原邀请码向量。
- A17：新增恰好 2 个 Route 的 HealthResponse 正例，顶层 `_说明` 标注 future-compat。`openapi/admin.v1.yaml` 未改；测试明确断言 routes 是 array、minItems 为 1、没有 maxItems，并要求存在双线路正例。实际服务端长度恰为 1 依赖 `server/tests/test_app.py::test_authentication` 的 `len(body["routes"]) == 1`，另见 `tests/server/server-core/test_server_core.py` 同类断言；本次只读核对，未运行需要启动服务的测试，不改 server/。

### 验证证明

全部命令从仓库根执行，无新增依赖、无 git 或联网操作。pytest 使用 `PYTHONDONTWRITEBYTECODE=1`、`PYTEST_ADDOPTS='-p no:cacheprovider'`，避免向其他目录写缓存；带 tee 的命令启用 `set -o pipefail`。

| 检查 | 命令与结果 | 输出文件 |
|---|---|---|
| 契约 | `/tmp/ct/bin/pytest -q tests/contract`：47 passed，0 failed/skip | `/home/box/osaka/logs/shared-protocol/pytest-1.log` |
| 服务端共享向量兼容性 | `PYTHONPATH=server /tmp/ct/bin/pytest -q server/tests/test_contract.py`：124 passed，0 failed/skip | `/home/box/osaka/logs/shared-protocol/server-vectors-1.log` |
| 编译 | `/tmp/ct/bin/python -B -c 'from pathlib import Path; p=Path("tests/contract/test_contract.py"); compile(p.read_text(encoding="utf-8"), str(p), "exec")'` 等价内存编译通过，不写 pyc | `/home/box/osaka/logs/shared-protocol/compile-1.log` |
| 静态检查 | `/tmp/ct/bin/python -B -m tabnanny tests/contract/test_contract.py` 等价 `tabnanny.check` 通过；4 个实现改动文件 UTF-8、末尾换行、无行末空白断言通过 | `/home/box/osaka/logs/shared-protocol/lint-1.log` |
| 拒绝能力 | `runpy.run_path` 加 `unittest.mock.patch` 在内存中损坏错误码、邀请码标记、redaction 形状、A17 限制及 7 个冻结文件内容；27 项检查通过，无落盘突变 | `/home/box/osaka/logs/shared-protocol/mutation-1.log`；完整命令见同目录 `commands-1.log` |
| `/no-comments` | 仅复查本卡改动文件；Python 无注释，JSON 仅保留必需的协议/future-compat 说明；notes 保留交付依据 | `/home/box/osaka/logs/shared-protocol/lint-1.log` |

ruff 在当前环境不可用，未安装或运行；以上静态检查不等同 ruff。未运行 pre-commit/`scripts/check-scope.sh`（该脚本内部使用 git）、远端 CI、Windows/Android 兼容性测试、完整 server-core HTTP 测试。需由组长叫测试跑兼容性卡，A9 身份校验及 A17 实际单线路仍依赖既有服务端断言。

Codex CLI 日志：实现 `/home/box/osaka/logs/shared-protocol/codex-1-20261106-030649.log`（`codex-1-20261106-030645.log` 为参数冲突未运行的首次尝试）；`/no-comments` 独立会话 `/home/box/osaka/logs/shared-protocol/codex-nocomments-20261106-031457.log`（无需删除的注释，冻结哈希 7 项全部匹配）。执行器复跑契约测试 47 passed，覆盖写入 `pytest-1.log`。

### 文件清单

以下清单记录的是原功能 head `98a398c65d879b16706b83021081fc3b48fe5c71` 当时的 `git diff --name-only origin/main...HEAD`（历史记录；不是本 notes 分支当前相对新 main 的差异）：

```text
notes/shared-protocol.md
shared/FROZEN.sha256
shared/invite-code.md
shared/test-vectors/admin-contract.json
tests/contract/test_contract.py
```

已知限制：`shared/invite-code.md` 改了措辞（删“显示时可按 4-4 分组，提交前去掉”），规则本身不变；ruff/CI/兼容性卡未跑；PR #77 需先入基线。
