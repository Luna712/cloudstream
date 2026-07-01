package com.lagradost.cloudstream4.preferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PreferenceDefaultsTest {

    @Test
    fun appThemeDefaultsToAmoledLight() {
        assertEquals("AmoledLight", PreferenceDefaults.APP_THEME)
    }

    @Test
    fun primaryColorDefaultsToNormal() {
        assertEquals("Normal", PreferenceDefaults.PRIMARY_COLOR)
    }

    @Test
    fun appLayoutDefaultsToAuto() {
        // -1 is "Auto"
        assertEquals(-1, PreferenceDefaults.APP_LAYOUT)
    }

    @Test
    fun appLayoutDoesNotCollideWithAnyExplicitLayoutValue() {
        // Explicit layout values are 0 (Phone), 1 (TV), 2 (Emulator).
        val explicitLayoutValues = setOf(0, 1, 2)
        assertNotEquals(true, explicitLayoutValues.contains(PreferenceDefaults.APP_LAYOUT))
    }
}
