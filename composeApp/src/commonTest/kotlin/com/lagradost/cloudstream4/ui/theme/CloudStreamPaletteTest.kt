package com.lagradost.cloudstream4.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CloudStreamPaletteTest {

    @Test
    fun primaryMatchesExpectedHexValue() {
        assertEquals(Color(0xFF3D50FA), CloudStreamPalette.Primary)
    }

    @Test
    fun amoledBlackIsPureBlack() {
        assertEquals(Color(0xFF000000), CloudStreamPalette.AmoledBlack)
    }

    @Test
    fun lightBlackBgIsPureWhite() {
        assertEquals(Color(0xFFFFFFFF), CloudStreamPalette.LightBlackBg)
    }

    @Test
    fun amoledBlackAndAmoledNearBlackAreDistinct() {
        assertNotEquals(CloudStreamPalette.AmoledBlack, CloudStreamPalette.AmoledNearBlack)
    }

    @Test
    fun darkBackgroundColorsAreDarkerThanLightBackgroundColors() {
        // A rough sanity check that "dark" theme colors are actually darker
        // (lower luminance) than their "light" theme counterparts.
        fun luminance(color: Color) = (color.red + color.green + color.blue) / 3f

        assertTrue(luminance(CloudStreamPalette.DarkBlackBg) < luminance(CloudStreamPalette.LightBlackBg))
        assertTrue(luminance(CloudStreamPalette.DarkText) > luminance(CloudStreamPalette.LightText))
    }

    @Test
    fun eachThemeDefinesADistinctBackgroundColor() {
        val backgrounds = listOf(
            CloudStreamPalette.DarkBlackBg,
            CloudStreamPalette.AmoledBlack,
            CloudStreamPalette.LightBlackBg,
            CloudStreamPalette.DraculaBlackBg,
            CloudStreamPalette.LavenderBlackBg,
            CloudStreamPalette.SilentBlueBlackBg,
        )
        assertEquals(
            backgrounds.size,
            backgrounds.toSet().size,
            "Theme backgrounds should not collide: $backgrounds",
        )
    }
}
