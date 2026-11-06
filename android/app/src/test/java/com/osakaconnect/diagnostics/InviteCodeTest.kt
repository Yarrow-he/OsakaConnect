package com.osakaconnect.diagnostics

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodeTest {
    @Test
    fun validatesAndNormalizesSharedVectors() {
        val vectors = SharedVectors.read("test-vectors/invite-code.json")
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            vectors.array("valid").forEachIndexed { index, value ->
                val input = value as String
                assertTrue("valid[$index]", InviteCode.isValid(input))
                val normalized = InviteCode.normalize(input)
                assertEquals(input.trim().uppercase(Locale.ROOT), normalized)
                assertTrue(InviteCode.isValid(normalized))
            }
            vectors.array("invalid").forEachIndexed { index, value ->
                assertFalse("invalid[$index]", InviteCode.isValid(value as? String))
            }
            assertEquals("ABCD1234", InviteCode.normalize(" abcd1234 "))
            assertFalse(InviteCode.isValid("ß000000"))
            assertFalse(InviteCode.isValid("K0000000"))
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
