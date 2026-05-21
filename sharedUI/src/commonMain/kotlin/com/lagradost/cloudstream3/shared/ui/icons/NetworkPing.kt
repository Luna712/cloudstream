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
public val network_ping: ImageVector
  get() {
    if (_network_ping != null) {
      return _network_ping!!
    }
    _network_ping =
      ImageVector.Builder(
          name = "network_ping",
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
            moveTo(4f, 18f)
            verticalLineTo(16f)
            horizontalLineToRelative(6.5f)
            lineTo(2f, 7.5f)
            lineTo(3.4f, 6.1f)
            lineTo(12f, 14.68f)
            lineToRelative(5.2f, -5.2f)
            quadTo(17.1f, 9.25f, 17.05f, 9.01f)
            reflectiveQuadTo(17f, 8.5f)
            quadTo(17f, 7.45f, 17.73f, 6.72f)
            reflectiveQuadTo(19.5f, 6f)
            reflectiveQuadToRelative(1.78f, 0.72f)
            reflectiveQuadTo(22f, 8.5f)
            reflectiveQuadToRelative(-0.72f, 1.77f)
            reflectiveQuadTo(19.5f, 11f)
            quadToRelative(-0.22f, 0f, -0.44f, -0.04f)
            quadTo(18.85f, 10.93f, 18.65f, 10.85f)
            lineTo(13.5f, 16f)
            horizontalLineTo(20f)
            verticalLineToRelative(2f)
            horizontalLineTo(4f)
            close()
          }
        }
        .build()
    return _network_ping!!
  }

private var _network_ping: ImageVector? = null
