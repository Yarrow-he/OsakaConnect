# Osaka server 0.1.0

运行时仅使用 Python 3 标准库。在仓库中保留 `shared/errors.json`，然后从
`server/` 启动：

```sh
python3 -m osaka_server.app
```

启动前通过环境变量提供 `OSAKA_TLS_CERT`（证书链文件路径）、
`OSAKA_TLS_KEY`（未加密私钥文件路径）和 `OSAKA_ADMIN_TOKEN`。
缺失或空值会导致启动失败；错误仅包含变量名，不包含环境变量的值。
服务监听 `0.0.0.0:8443`，仅接受 HTTPS。

可选变量 `OSAKA_ROUTE_ID`、`OSAKA_ROUTE_NAME` 分别配置唯一线路的标识和名称，
各为 1–64 个字符，默认值为 `placeholder-route` 和 `Placeholder route`。
不合协议的线路配置会使 health 返回 `500 INTERNAL`。

`GET /v1/health` 和 `GET /v1/version` 要求 `Authorization: Bearer <token>`。
无 Authorization 返回 401，错误或格式不正确的凭证返回 403。
错误文案来自 `shared/errors.json`。未知路径返回无响应体的 404。
响应发送前按 OpenAPI 手写校验，不合格则返回 `500 INTERNAL`。
不记录访问日志，避免请求路径或头部泄露凭证。

`osaka_server.invite.normalize(value)` 对有效 ASCII 邀请码去首尾空白、转大写；
无效值返回 `None`。`is_valid_invite_code(value)` 返回布尔值。

测试使用已有的 pytest，不需要新增运行时依赖：

```sh
python3 -m pytest -q
```

测试直接读取仓库中的共享向量。HTTP handler 同时有内存和真实连接测试；
真实 HTTPS 测试用 OpenSSL 生成临时自签证书，客户端显式信任该证书并校验主机名，
测试后清除证书和私钥。没有 OpenSSL 时，接口测试退回本地明文 HTTPServer，
证书测试跳过。沙箱禁止本地 socket 时，真实连接测试明确跳过，内存 handler
测试仍运行；此时不能据此认定真实 HTTPS 验收完成。

验证命令、输出路径和环境限制见 [交付记录](notes/server-core.md)。
