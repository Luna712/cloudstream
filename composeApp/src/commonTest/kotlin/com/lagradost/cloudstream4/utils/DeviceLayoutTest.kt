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
        // since the actual result is platform/environment dependent,
        // which we might add tests for eventually but not for now.
        DeviceLayout.isLandscape()
    }

    @Test
    fun isLayoutStateMatchesSameFlagAsIsLayout() {
        DeviceLayout.update()
        val anyKnownLayout =
            DeviceLayout.PHONE or DeviceLayout.TV or DeviceLayout.EMULATOR or DeviceLayout.COMPUTER
        assertTrue(DeviceLayout.isLayoutState(anyKnownLayout).value)
        assertTrue(DeviceLayout.isLayoutState(anyKnownLayout).value == DeviceLayout.isLayout(anyKnownLayout))
    }

    @Test
    fun isLayoutStateDoesNotMatchUnrelatedFlagCombo() {
        DeviceLayout.update()
        // Whatever the resolved layout is, PHONE and TV are mutually
        // exclusive single flags, so at most one of them can be active.
        val phoneMatches = DeviceLayout.isLayoutState(DeviceLayout.PHONE).value
        val tvMatches = DeviceLayout.isLayoutState(DeviceLayout.TV).value
        assertFalse(phoneMatches && tvMatches)
    }

    @Test
    fun isLayoutStateReflectsUpdatesToLayoutState() {
        // update() re-resolves and writes through to both layoutId and
        // layoutState, so isLayoutState should always agree with isLayout
        // immediately after any call to update().
        DeviceLayout.update()
        val everything =
            DeviceLayout.PHONE or DeviceLayout.TV or DeviceLayout.EMULATOR or DeviceLayout.COMPUTER
        assertTrue(DeviceLayout.isLayoutState(everything).value == DeviceLayout.isLayout(everything))

        DeviceLayout.update()
        assertTrue(DeviceLayout.isLayoutState(everything).value == DeviceLayout.isLayout(everything))
    }
}
