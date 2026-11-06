package com.osakaconnect.diagnostics

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JvmJsonTest {
    @Test
    fun limitsArrayObjectAndMixedNestingTo64() {
        val wrappers = listOf<Pair<String, (Int) -> Boolean>>(
            "arrays" to { _: Int -> true },
            "objects" to { _: Int -> false },
            "mixed" to { level: Int -> level % 2 == 0 }
        )
        wrappers.forEach { (label, isArray) ->
            fun nested(depth: Int): String = (0 until depth).fold("0") { inner, level ->
                if (isArray(level)) "[$inner]" else """{"a":$inner}"""
            }
            val tokens = JSONTokener(nested(64))
            var value = tokens.nextValue()
            repeat(64) { level ->
                value = if (isArray(63 - level)) (value as JSONArray).get(0)
                    else (value as JSONObject).get("a")
            }
            assertEquals(label, 0L, value)
            assertEquals('\u0000', tokens.nextClean())
            assertThrows(label, JSONException::class.java) { JSONTokener(nested(65)).nextValue() }
        }
        assertThrows(JSONException::class.java) { JSONTokener("[".repeat(10000)).nextValue() }
    }

    @Test
    fun rejectsInvalidEscapes() {
        listOf("\"\\q\"", "\"\\u12\"", "\"\\u00zz\"", "\"\\").forEach { input ->
            assertThrows(JSONException::class.java) { JSONTokener(input).nextValue() }
        }
    }

    @Test
    fun duplicateKeysUseLastValue() {
        val value = JSONTokener("""{"a":1,"a":2}""").nextValue() as JSONObject
        assertEquals(2L, value.get("a"))
    }

    @Test
    fun rejectsMalformedAndOutOfRangeIntegers() {
        listOf(
            "01", "1.", "-", "+1", "1e",
            "99999999999999999999", "9223372036854775808", "-9223372036854775809"
        ).forEach { input ->
            assertThrows(input, JSONException::class.java) { JSONTokener(input).nextValue() }
        }
    }

    @Test
    fun preservesLongBoundariesAndDoubleNumbers() {
        assertEquals(Long.MIN_VALUE, JSONTokener(Long.MIN_VALUE.toString()).nextValue())
        assertEquals(Long.MAX_VALUE, JSONTokener(Long.MAX_VALUE.toString()).nextValue())
        listOf("1.5", "1e2", "1E-2", "99999999999999999999.0", "99999999999999999999e0").forEach { input ->
            assertEquals(input.toDouble(), JSONTokener(input).nextValue())
        }
    }

    @Test
    fun acceptsUnescapedControlCharactersAndLeadingBom() {
        listOf("\u0001", "\n", "\t", "\u0000").forEach { value ->
            assertEquals(value, JSONTokener("\"$value\"").nextValue())
        }
        val value = JSONTokener("\uFEFF{\"a\":1}").nextValue() as JSONObject
        assertEquals(1L, value.get("a"))
    }
}
