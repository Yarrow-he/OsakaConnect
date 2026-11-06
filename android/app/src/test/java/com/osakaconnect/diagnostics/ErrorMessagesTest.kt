package com.osakaconnect.diagnostics

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMessagesTest {
    @Test
    fun matchesSharedMessagesAndFallsBackWithoutEchoingInput() {
        val messages = SharedVectors.read("errors.json")
        messages.keys().forEach { code ->
            assertEquals(code, (messages.get(code) as JSONObject).get("zh"), ErrorMessages.zh(code))
        }
        val fallback = (messages.get("INTERNAL") as JSONObject).get("zh")
        assertEquals(fallback, ErrorMessages.zh("UNKNOWN\nat Fake.stack(Fake.kt:1)"))
        assertEquals(fallback, ErrorMessages.zh(""))
    }
}
