package com.lagradost.cloudstream4.preferences

import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.Test
import kotlin.test.assertTrue

class PreferenceKeysTest {

    @Test
    fun allPreferenceKeysAreUnique() {
        val keys = PreferenceKeys::class.declaredMemberProperties
            .filter { it.returnType.classifier == String::class }
            .map { it.get(PreferenceKeys) }

        assertTrue(keys.isNotEmpty(), "No preference keys found via reflection")
        assertTrue(keys.toSet().size == keys.size, "Preference keys must not collide: $keys")
    }
}
