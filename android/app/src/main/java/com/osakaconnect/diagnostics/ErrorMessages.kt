package com.osakaconnect.diagnostics

object ErrorMessages {
    // Kept in sync with shared/errors.json by ErrorMessagesTest.
    private val messages = mapOf(
        "UNAUTHENTICATED" to "未提供管理凭证",
        "FORBIDDEN" to "管理凭证无效",
        "INVALID_INVITE_CODE" to "邀请码格式不正确",
        "BACKEND_UNREACHABLE" to "无法连接后台",
        "INTERNAL" to "发生内部错误，请稍后重试"
    )

    fun zh(code: String): String = messages[code] ?: messages.getValue("INTERNAL")

    internal fun isKnown(code: String): Boolean = messages.containsKey(code)
}
