package com.lagradost.cloudstream3.shared.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val arrow_forward_ios: ImageVector
  get() {
    if (_arrow_forward_ios != null) {
      return _arrow_forward_ios!!
    }
    _arrow_forward_ios =
      ImageVector.Builder(
          name = "arrow_forward_ios",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.03f, 22f)
            lineTo(6.25f, 20.23f)
            lineTo(14.48f, 12f)
            lineTo(6.25f, 3.77f)
            lineTo(8.03f, 2f)
            lineToRelative(10f, 10f)
            lineToRelative(-10f, 10f)
            close()
          }
        }
        .build()
    return _arrow_forward_ios!!
  }

private var _arrow_forward_ios: ImageVector? = null
