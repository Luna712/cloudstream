package com.lagradost.cloudstream4.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource

@Composable
actual fun resolveDynamicPrimaryColor(): Color {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        colorResource(android.R.color.system_accent1_200)
    else CloudStreamPrimaryColor.NORMAL.color
}

@Composable
actual fun resolveDynamicSecondaryColor(): Color {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        colorResource(android.R.color.system_accent2_200)
    else CloudStreamPrimaryColor.NORMAL.color
}
