package com.lagradost.cloudstream4.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudStreamThemeModeTest {

    @Test
    fun containsExpectedNumberOfModes() {
        assertEquals(8, CloudStreamThemeMode.entries.size)
    }

    @Test
    fun containsAllExpectedModesByName() {
        val expected = setOf(
            "Dark", "Amoled", "Light", "Dracula", "Lavender", "SilentBlue", "FollowSystem", "Dynamic",
        )
        val actual = CloudStreamThemeMode.entries.map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun noTwoModesShareAnOrdinal() {
        val ordinals = CloudStreamThemeMode.entries.map { it.ordinal }
        assertTrue(ordinals.toSet().size == ordinals.size)
    }

    @Test
    fun valueOfRoundTripsForEveryMode() {
        for (mode in CloudStreamThemeMode.entries) {
            assertEquals(mode, CloudStreamThemeMode.valueOf(mode.name))
        }
    }
}
