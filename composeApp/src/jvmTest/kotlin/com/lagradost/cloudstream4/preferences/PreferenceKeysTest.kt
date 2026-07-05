package com.lagradost.cloudstream4.preferences

import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertTrue

class PreferenceKeysTest {

    @Test
    fun allPreferenceKeysAreUnique() {
        val keys = PreferenceKeys::class.memberProperties
            .filter { it.returnType.classifier == String::class }
            .map { it.getter.call(PreferenceKeys) as String }

        assertTrue(keys.isNotEmpty(), "No preference keys found via reflection")
        assertTrue(keys.toSet().size == keys.size, "Preference keys must not collide: $keys")
    }
}
