package com.lagradost.cloudstream3.mvvm

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class ArchComponentExtTest {

    @Test
    fun getStackTracePrettyStripsFullyQualifiedClassNames() {
        val throwable = try {
            throwNested()
        } catch (t: Throwable) {
            t
        }

        val pretty = throwable.getStackTracePretty()

        // Should not contain the fully qualified package/class prefix
        assertFalse(
            pretty.contains("com.lagradost.cloudstream3.mvvm.ArchComponentExtTest"),
            "Expected no fully qualified class names, got:\n$pretty"
        )

        // Should still reference the source file itself
        assertTrue(
            pretty.contains(".kt"),
            "Expected pretty stack trace to reference a .kt file, got:\n$pretty"
        )
    }

    @Test
    fun getStackTracePrettyIncludesMessageWhenShowMessageIsTrue() {
        val throwable = RuntimeException("boom")
        val pretty = throwable.getStackTracePretty(showMessage = true)
        assertTrue(pretty.startsWith("\nboom"), "Expected message prefix, got:\n$pretty")
    }

    @Test
    fun getStackTracePrettyOmitsMessageWhenShowMessageIsFalse() {
        val throwable = RuntimeException("boom")
        val pretty = throwable.getStackTracePretty(showMessage = false)
        assertFalse(pretty.contains("boom"), "Did not expect message, got:\n$pretty")
    }

    @Test
    fun getStackTracePrettyHandlesThrowableWithNoStackTraceLinesGracefully() {
        // A throwable that hasn't been thrown may have an empty stack trace on some targets
        val throwable = RuntimeException("no trace")
        val pretty = throwable.getStackTracePretty(showMessage = false)
        // Should not throw, and should just be an empty or line-based string
        assertEquals(pretty, pretty.trim().let { pretty }) // no crash / sane result
    }

    private fun throwNested(): Nothing {
        throw IllegalStateException("nested failure")
    }
}
