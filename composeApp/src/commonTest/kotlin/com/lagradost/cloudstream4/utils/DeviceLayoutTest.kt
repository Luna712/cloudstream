package com.lagradost.cloudstream4.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceLayoutTest {

    @Test
    fun singleFlagMatchesItself() {
        assertTrue((DeviceLayout.PHONE).and(DeviceLayout.PHONE))
    }

    @Test
    fun singleFlagDoesNotMatchOtherFlag() {
        assertFalse((DeviceLayout.PHONE).and(DeviceLayout.TV))
    }

    @Test
    fun orCombinesTwoFlagsSoEitherMatches() {
        val combined = DeviceLayout.PHONE or DeviceLayout.TV
        assertTrue(combined.and(DeviceLayout.PHONE))
        assertTrue(combined.and(DeviceLayout.TV))
    }

    @Test
    fun orCombinationDoesNotMatchAnUnrelatedFlag() {
        val combined = DeviceLayout.PHONE or DeviceLayout.TV
        assertFalse(combined.and(DeviceLayout.EMULATOR))
        assertFalse(combined.and(DeviceLayout.COMPUTER))
    }

    @Test
    fun orIsCommutative() {
        val a = DeviceLayout.PHONE or DeviceLayout.COMPUTER
        val b = DeviceLayout.COMPUTER or DeviceLayout.PHONE
        assertTrue(a.and(DeviceLayout.PHONE) == b.and(DeviceLayout.PHONE))
        assertTrue(a.and(DeviceLayout.COMPUTER) == b.and(DeviceLayout.COMPUTER))
    }

    @Test
    fun combiningAllFourFlagsMatchesEachOne() {
        val everything =
            DeviceLayout.PHONE or DeviceLayout.TV or DeviceLayout.EMULATOR or DeviceLayout.COMPUTER
        assertTrue(everything.and(DeviceLayout.PHONE))
        assertTrue(everything.and(DeviceLayout.TV))
        assertTrue(everything.and(DeviceLayout.EMULATOR))
        assertTrue(everything.and(DeviceLayout.COMPUTER))
    }

    @Test
    fun isLayoutReflectsResolvedLayoutAfterUpdate() {
        DeviceLayout.update()
        // resolveLayout() always resolves to exactly one of these four flags,
        // so at least one of them must be considered active.
        val anyKnownLayout =
            DeviceLayout.PHONE or DeviceLayout.TV or DeviceLayout.EMULATOR or DeviceLayout.COMPUTER
        assertTrue(DeviceLayout.isLayout(anyKnownLayout))
    }

    @Test
    fun isLandscapeDoesNotThrow() {
        DeviceLayout.update()
        // Just verifying this completes without throwing on every target,
        // since the actual boolean result is platform/environment dependent.
        DeviceLayout.isLandscape()
    }
}
