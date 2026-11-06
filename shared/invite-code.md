# 邀请码规则（A15）
- 长度固定 8。字符集 Crockford Base32：`0123456789ABCDEFGHJKMNPQRSTVWXYZ`（无 I、L、O、U）。
- 大小写不敏感，归一化仅去掉首尾空白（trim）并转大写；不删除横杠，带横杠的输入无效。
- 正则（归一化后）：`^[0-9A-HJKMNP-TV-Z]{8}$`
- 测试向量：`test-vectors/invite-code.json`，三端单测必须全部通过。
