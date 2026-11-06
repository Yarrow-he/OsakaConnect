# Understand-Anything PLUGIN_ROOT（外置预构建，delivery-v010）

本文件仅作路径说明，**不要**把 `node_modules` / 上游整树提交进业务仓。

- T = `98d6dca712407b8cbf0bca21b539c197e5a7825c`
- PLUGIN_ROOT = `/home/box/osaka/tools/understand-anything-98d6dca/understand-anything-plugin`
- 已预构建: `$PLUGIN_ROOT/packages/core/dist/index.js`（组长授权一次性 `pnpm install --frozen-lockfile` + `pnpm --filter @understand-anything/core build`）
- 生成用业务检出 S = `f7c8abaa04dc7378e3f7588a0af6f65302e94922` → `/home/box/osaka/tools/osaka-S-f7c8aba`
- 摘要: `/home/box/osaka/logs/delivery-v010/project-graph/prebuild-20261107.md`
- 约束: 不跑 upstream `install.sh`；运行时缺 dist/依赖即停，不现装；图谱产物只交组长 `docs/delivery/v0.1.0/`。
