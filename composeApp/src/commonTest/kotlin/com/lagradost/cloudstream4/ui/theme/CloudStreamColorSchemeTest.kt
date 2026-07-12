package com.lagradost.cloudstream4.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudStreamColorSchemeTest {

    @Test
    fun darkSchemeIsNotLight() {
        assertFalse(darkScheme().isLight)
    }

    @Test
    fun lightSchemeIsLight() {
        assertTrue(lightScheme().isLight)
    }

    @Test
    fun amoledSchemeIsDerivedFromDarkSchemeButNotLight() {
        assertFalse(amoledScheme().isLight)
    }

    @Test
    fun amoledSchemeOverridesAllBackgroundLayersToPureBlack() {
        val amoled = amoledScheme()
        assertEquals(CloudStreamPalette.AmoledBlack, amoled.background)
        assertEquals(CloudStreamPalette.AmoledBlack, amoled.surface)
        assertEquals(CloudStreamPalette.AmoledBlack, amoled.surfaceVariant)
        assertEquals(CloudStreamPalette.AmoledBlack, amoled.surfaceContainer)
    }

    @Test
    fun amoledSchemeKeepsDarkSchemeTextAndIconColors() {
        // Only backgrounds are overridden on top of darkScheme(); text/icon/primary
        // should be carried over unchanged.
        val dark = darkScheme()
        val amoled = amoledScheme()
        assertEquals(dark.onBackground, amoled.onBackground)
        assertEquals(dark.onSurfaceVariant, amoled.onSurfaceVariant)
        assertEquals(dark.icon, amoled.icon)
        assertEquals(dark.primary, amoled.primary)
    }

    @Test
    fun draculaSchemeIsDark() {
        assertFalse(draculaScheme().isLight)
    }

    @Test
    fun lavenderSchemeIsLight() {
        assertTrue(lavenderScheme().isLight)
    }

    @Test
    fun silentBlueSchemeIsDark() {
        assertFalse(silentBlueScheme().isLight)
    }

    @Test
    fun allNamedSchemesSharePrimaryAndOngoingColors() {
        // Every preset (aside from copies that explicitly override it) shares the
        // same default primary/ongoing accent colors from the palette.
        val schemes = listOf(
            darkScheme(),
            lightScheme(),
            draculaScheme(),
            lavenderScheme(),
            silentBlueScheme(),
        )

        for (scheme in schemes) {
            assertEquals(CloudStreamPalette.Primary, scheme.primary)
            assertEquals(CloudStreamPalette.Ongoing, scheme.ongoing)
        }
    }

    @Test
    fun copyOverridesOnlyRequestedFieldsAndPreservesTheRest() {
        val original = darkScheme()
        val copy = original.copy(primary = CloudStreamPalette.Ongoing)

        assertEquals(CloudStreamPalette.Ongoing, copy.primary)
        assertEquals(original.background, copy.background)
        assertEquals(original.surface, copy.surface)
        assertEquals(original.onBackground, copy.onBackground)
        assertEquals(original.isLight, copy.isLight)
    }

    @Test
    fun copyWithNoArgumentsProducesAnEquivalentButDistinctInstance() {
        val original = lightScheme()
        val copy = original.copy()

        assertEquals(original.background, copy.background)
        assertEquals(original.surface, copy.surface)
        assertEquals(original.surfaceVariant, copy.surfaceVariant)
        assertEquals(original.surfaceContainer, copy.surfaceContainer)
        assertEquals(original.onBackground, copy.onBackground)
        assertEquals(original.onSurfaceVariant, copy.onSurfaceVariant)
        assertEquals(original.icon, copy.icon)
        assertEquals(original.primary, copy.primary)
        assertEquals(original.ongoing, copy.ongoing)
        assertEquals(original.isLight, copy.isLight)
    }

    @Test
    fun mutatingASchemeFieldDoesNotAffectAFreshlyBuiltScheme() {
        // Fields are individually mutable (backed by mutableStateOf) to support
        // recomposition; verify that mutation is instance-local.
        val mutated = darkScheme()
        mutated.primary = CloudStreamPalette.Ongoing

        val fresh = darkScheme()
        assertEquals(CloudStreamPalette.Primary, fresh.primary)
        assertEquals(CloudStreamPalette.Ongoing, mutated.primary)
    }
}
