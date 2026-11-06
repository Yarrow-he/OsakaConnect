package com.osakaconnect.diagnostics

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class LogRedactorTest {
    @Test
    fun redactsQuotedJsonSecretsIdempotently() {
        val input = """{"token": "FAKE_TOKEN", "password":"FAKE_PASSWORD"}"""
        val expected = """{"token": "***", "password":"***"}"""
        val redacted = LogRedactor.redact(input)
        assertEquals(expected, redacted)
        assertEquals(expected, LogRedactor.redact(redacted))
    }

    @Test
    fun redactsQuotedValuesWithWhitespaceAndEscapes() {
        val input = """{"token":"FAKE_\"TOKEN, WITH SPACE", "password":""}"""
        val expected = """{"token":"***", "password":"***"}"""
        assertEquals(expected, LogRedactor.redact(input))
        assertEquals(expected, LogRedactor.redact(expected))
    }

    @Test
    fun redactsSharedVectors() {
        val vectors = SharedVectors.read("test-vectors/redaction.json")
        assertEquals("***", vectors.get("mask"))
        vectors.array("cases").forEachIndexed { index, value ->
            val case = value as JSONObject
            assertEquals("redaction[$index]", case.get("out"), LogRedactor.redact(case.get("in") as String))
        }
        assertEquals(
            "TOKEN=***; invite=***, password=***",
            LogRedactor.redact("TOKEN=FAKE_TOKEN; invite=FAKECODE, password=FAKE_PASSWORD")
        )
        assertEquals("token=***", LogRedactor.redact(LogRedactor.redact("token=FAKE_TOKEN")))
    }
}
