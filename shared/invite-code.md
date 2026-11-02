# 邀请码规则（A15）
- 长度固定 8。字符集 Crockford Base32：`0123456789ABCDEFGHJKMNPQRSTVWXYZ`（无 I、L、O、U）。
- 大小写不敏感，校验前转大写；去掉首尾空白；内部横杠不允许（显示时可按 4-4 分组，提交前去掉）。
- 正则（归一化后）：`^[0-9A-HJKMNP-TV-Z]{8}$`
- 测试向量：`test-vectors/invite-code.json`，三端单测必须全部通过。
