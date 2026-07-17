package com.lagradost.cloudstream4.ui.theme

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.lagradost.cloudstream4.generated.resources.Res
import com.lagradost.cloudstream4.generated.resources.productsans_black
import com.lagradost.cloudstream4.generated.resources.productsans_blackitalic
import com.lagradost.cloudstream4.generated.resources.productsans_bold
import com.lagradost.cloudstream4.generated.resources.productsans_bolditalic
import com.lagradost.cloudstream4.generated.resources.productsans_italic
import com.lagradost.cloudstream4.generated.resources.productsans_light
import com.lagradost.cloudstream4.generated.resources.productsans_lightitalic
import com.lagradost.cloudstream4.generated.resources.productsans_medium
import com.lagradost.cloudstream4.generated.resources.productsans_mediumitalic
import com.lagradost.cloudstream4.generated.resources.productsans_regular
import com.lagradost.cloudstream4.generated.resources.productsans_thin
import com.lagradost.cloudstream4.generated.resources.productsans_thinitalic

object CloudStreamDefaultFonts {
    val GoogleSans: CloudStreamFontSpec.Bundled = CloudStreamFontSpec.Bundled(
        listOf(
            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_thin, FontWeight.Thin, FontStyle.Normal),
            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_thinitalic, FontWeight.Thin, FontStyle.Italic),

            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_light, FontWeight.Light, FontStyle.Normal),
            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_lightitalic, FontWeight.Light, FontStyle.Italic),

            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_regular, FontWeight.Normal, FontStyle.Normal),
            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_italic, FontWeight.Normal, FontStyle.Italic),

            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_medium, FontWeight.Medium, FontStyle.Normal),
            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_mediumitalic, FontWeight.Medium, FontStyle.Italic),

            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_bold, FontWeight.Bold, FontStyle.Normal),
            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_bolditalic, FontWeight.Bold, FontStyle.Italic),

            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_black, FontWeight.Black, FontStyle.Normal),
            CloudStreamFontSpec.Bundled.Entry(Res.font.productsans_blackitalic, FontWeight.Black, FontStyle.Italic),
        )
    )
}
