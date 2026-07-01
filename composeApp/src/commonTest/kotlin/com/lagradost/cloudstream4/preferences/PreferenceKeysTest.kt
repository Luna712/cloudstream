package com.lagradost.cloudstream4.preferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreferenceKeysTest {

    @Test
    fun appLayoutKeyHasExpectedValue() {
        assertEquals("app_layout_key", PreferenceKeys.APP_LAYOUT)
    }

    @Test
    fun appThemeKeyHasExpectedValue() {
        assertEquals("app_theme_key", PreferenceKeys.APP_THEME)
    }

    @Test
    fun primaryColorKeyHasExpectedValue() {
        assertEquals("primary_color_key", PreferenceKeys.PRIMARY_COLOR)
    }

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
