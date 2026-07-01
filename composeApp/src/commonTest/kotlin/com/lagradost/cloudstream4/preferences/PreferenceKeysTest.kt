package com.lagradost.cloudstream4.preferences

import kotlin.test.Test
import kotlin.test.assertTrue

class PreferenceKeysTest {

    @Test
    fun allPreferenceKeysAreUnique() {
        val keys = listOf(
            PreferenceKeys.APP_LAYOUT,
            PreferenceKeys.APP_THEME,
            PreferenceKeys.PRIMARY_COLOR,
        )
        assertTrue(keys.toSet().size == keys.size, "Preference keys must not collide: $keys")
    }
}
