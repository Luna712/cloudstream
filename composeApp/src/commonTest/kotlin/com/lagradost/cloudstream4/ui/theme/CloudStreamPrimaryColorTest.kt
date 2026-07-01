package com.lagradost.cloudstream4.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudStreamPrimaryColorTest {

    @Test
    fun normalMatchesTheCloudStreamPaletteAccentColor() {
        assertEquals(CloudStreamPalette.Primary, CloudStreamPrimaryColor.NORMAL.color)
    }

    @Test
    fun dynamicAndDynamicTwoFallBackToTheSameDefaultColor() {
        // These entries are placeholders whose real color is resolved dynamically
        // at runtime (see resolveDynamicPrimaryColor / resolveDynamicSecondaryColor);
        // their static enum color should just be a sane, matching fallback.
        assertEquals(CloudStreamPrimaryColor.DYNAMIC.color, CloudStreamPrimaryColor.DYNAMIC_TWO.color)
        assertEquals(CloudStreamPrimaryColor.NORMAL.color, CloudStreamPrimaryColor.DYNAMIC.color)
    }

    @Test
    fun everyEntryHasAFullyOpaqueColor() {
        for (entry in CloudStreamPrimaryColor.entries) {
            assertEquals(1f, entry.color.alpha, "Expected $entry to be fully opaque")
        }
    }

    @Test
    fun containsExpectedNumberOfColorOptions() {
        assertEquals(22, CloudStreamPrimaryColor.entries.size)
    }

    @Test
    fun whiteEntryIsActuallyWhite() {
        val white = CloudStreamPrimaryColor.WHITE.color
        assertEquals(1f, white.red)
        assertEquals(1f, white.green)
        assertEquals(1f, white.blue)
    }

    @Test
    fun noTwoEntriesShareTheSameName() {
        val names = CloudStreamPrimaryColor.entries.map { it.name }
        assertTrue(names.toSet().size == names.size)
    }
}
