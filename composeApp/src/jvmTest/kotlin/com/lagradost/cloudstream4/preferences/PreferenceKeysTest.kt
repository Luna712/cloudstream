package com.lagradost.cloudstream4.preferences

import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * This is in jvmTest rather than commonTest so that we can
 * use kotlin-reflect (JVM-only) rather than having to
 * manually maintain two seperate lists for these.
 * Preferences keys should be unique, so this is
 * just to ensure that.
 */
class PreferenceKeysTest {

    @Test
    fun allPreferenceKeysAreUnique() {
        val keys = PreferenceKeys::class.declaredMemberProperties
            .filter { it.returnType.classifier == String::class }
            .map { it.getter.call() }

        assertTrue(keys.isNotEmpty(), "No preference keys found via reflection")
        assertTrue(keys.toSet().size == keys.size, "Preference keys must not collide: $keys")
    }
}
