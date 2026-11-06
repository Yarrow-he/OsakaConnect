package com.osakaconnect.diagnostics

object LogRedactor {
    private val secret = Regex(
        "(\\bAuthorization[ \\t]*:[ \\t]*Bearer[ \\t]+|" +
            "\"?\\b(?:invite|api_key|token|password)\"?[ \\t]*[:=][ \\t]*)(\"(?:\\\\.|[^\"\\\\])*\"|[^\\s,;]+)",
        RegexOption.IGNORE_CASE
    )

    fun redact(line: String): String = secret.replace(line) {
        val mask = if (it.groupValues[2].startsWith('"')) "\"***\"" else "***"
        "${it.groupValues[1]}$mask"
    }
}
