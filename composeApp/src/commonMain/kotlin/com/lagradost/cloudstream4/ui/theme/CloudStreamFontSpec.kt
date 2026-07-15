package com.lagradost.cloudstream4.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.Font as ResourceFont

sealed interface CloudStreamFontSpec {

    data object SystemDefault : CloudStreamFontSpec

    /** A font bundled via composeResources */
    data class Bundled(val entries: List<Entry>) : CloudStreamFontSpec {
        data class Entry(
            val resource: FontResource,
            val weight: FontWeight,
            val style: FontStyle = FontStyle.Normal,
        )
    }

    /** A user-uploaded font, resolved later from a file path/URI. */
    data class Custom(val path: String) : CloudStreamFontSpec
}

// Not yet implemented
/** Platform-specific loading of a user-uploaded font from a file path/URI. */
// expect fun loadCustomFontFamily(path: String): FontFamily?

@Composable
fun CloudStreamFontSpec.resolve(): FontFamily = when (this) {
    is CloudStreamFontSpec.SystemDefault -> FontFamily.Default

    is CloudStreamFontSpec.Bundled -> {
            val fonts: List<Font> = entries.map { entry ->
                ResourceFont(entry.resource, weight = entry.weight, style = entry.style)
            }
            val family: FontFamily = remember(fonts) { FontFamily(fonts) }
            family
        }

    // Not yet implemented
    is CloudStreamFontSpec.Custom -> Unit /* remember(path) {
        loadCustomFontFamily(path) ?: FontFamily.Default
    } */
}
