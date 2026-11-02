package com.osakaconnect.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class PageTest {
    @Test
    fun restoresKnownPagesAndDefaultsMissingOrInvalidStateToP1() {
        assertEquals(Page.P1, Page.restore("P1"))
        assertEquals(Page.P2, Page.restore("P2"))
        assertEquals(Page.P1, Page.restore(null))
        assertEquals(Page.P1, Page.restore("unknown"))
    }
}
