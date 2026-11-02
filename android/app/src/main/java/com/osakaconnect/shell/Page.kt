package com.osakaconnect.shell

internal enum class Page {
    P1, P2;

    companion object {
        fun restore(savedName: String?): Page = entries.firstOrNull { it.name == savedName } ?: P1
    }
}
