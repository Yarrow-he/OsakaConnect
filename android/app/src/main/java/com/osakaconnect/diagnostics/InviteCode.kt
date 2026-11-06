package com.osakaconnect.diagnostics

import java.util.Locale

object InviteCode {
    private val format = Regex("[0-9A-HJKMNP-TV-Za-hjkmnp-tv-z]{8}")

    fun normalize(input: String): String = input.trim().uppercase(Locale.ROOT)

    // Nullable at the boundary because the shared invalid vectors include null.
    fun isValid(input: String?): Boolean = input != null && format.matches(input.trim())
}
