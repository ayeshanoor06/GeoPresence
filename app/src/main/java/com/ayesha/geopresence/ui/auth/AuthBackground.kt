package com.ayesha.geopresence.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Soft blue "map" background used by Splash, Login and Register.
 * Everything is drawn from M3 color roles, so it adapts to light/dark.
 */
@Composable
fun AuthBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val tint = colors.primaryContainer
    val paper = colors.surfaceContainerLowest
    val accent = colors.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to lerp(tint, paper, 0.45f),
                    0.5f to lerp(tint, paper, 0.80f),
                    1f to lerp(tint, paper, 0.50f)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTopWaves(accent)
            drawPin(
                center = Offset(size.width * 0.93f, size.height * 0.088f),
                r = size.width * 0.10f,
                color = accent.copy(alpha = 0.10f),
                hole = paper.copy(alpha = 0.55f)
            )
            drawMap(accent)
            drawBottomWaves(accent, paper)
            drawMapPinAndRoute(accent, paper)
        }
        content()
    }
}

private fun DrawScope.drawPin(center: Offset, r: Float, color: Color, hole: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + 2.1f * r)
        cubicTo(
            center.x - 0.3f * r, center.y + 1.3f * r,
            center.x - r, center.y + 0.9f * r,
            center.x - r, center.y
        )
        arcTo(
            Rect(center.x - r, center.y - r, center.x + r, center.y + r),
            180f, 180f, false
        )
        cubicTo(
            center.x + r, center.y + 0.9f * r,
            center.x + 0.3f * r, center.y + 1.3f * r,
            center.x, center.y + 2.1f * r
        )
        close()
    }
    drawPath(path, color)
    drawCircle(hole, radius = r * 0.42f, center = center)
}

private fun DrawScope.drawTopWaves(accent: Color) {
    val w = size.width
    val h = size.height

    val back = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.62f, 0f)
        cubicTo(w * 0.45f, h * 0.03f, w * 0.18f, h * 0.09f, 0f, h * 0.20f)
        close()
    }
    drawPath(back, accent.copy(alpha = 0.12f))

    val front = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.40f, 0f)
        cubicTo(w * 0.28f, h * 0.035f, w * 0.10f, h * 0.07f, 0f, h * 0.13f)
        close()
    }
    drawPath(front, accent.copy(alpha = 0.28f))

    val line = Path().apply {
        moveTo(0f, h * 0.095f)
        cubicTo(w * 0.20f, h * 0.06f, w * 0.30f, h * 0.03f, w * 0.38f, 0f)
    }
    drawPath(line, accent.copy(alpha = 0.40f), style = Stroke(width = 1.5.dp.toPx()))
}

private fun DrawScope.drawMap(accent: Color) {
    val w = size.width
    val h = size.height
    val blockW = w * 0.20f
    val blockH = w * 0.11f
    val gap = w * 0.035f
    val brush = Brush.horizontalGradient(
        colors = listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.04f)),
        startX = 0f,
        endX = w
    )
    rotate(degrees = 14f, pivot = Offset(0f, h * 0.76f)) {
        for (row in 0..6) {
            for (col in -1..6) {
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(col * (blockW + gap), h * 0.76f + row * (blockH + gap)),
                    size = Size(blockW, blockH),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
            }
        }
    }
}

private fun DrawScope.drawBottomWaves(accent: Color, paper: Color) {
    val w = size.width
    val h = size.height

    val back = Path().apply {
        moveTo(w * 0.25f, h)
        cubicTo(w * 0.50f, h * 0.945f, w * 0.78f, h * 0.93f, w, h * 0.88f)
        lineTo(w, h)
        close()
    }
    drawPath(back, accent.copy(alpha = 0.14f))

    val front = Path().apply {
        moveTo(w * 0.45f, h)
        cubicTo(w * 0.62f, h * 0.95f, w * 0.82f, h * 0.915f, w, h * 0.845f)
        lineTo(w, h)
        close()
    }
    drawPath(front, accent.copy(alpha = 0.32f))

    val line = Path().apply {
        moveTo(w * 0.50f, h)
        cubicTo(w * 0.70f, h * 0.965f, w * 0.88f, h * 0.95f, w, h * 0.915f)
    }
    drawPath(line, paper.copy(alpha = 0.7f), style = Stroke(width = 1.5.dp.toPx()))
}

private fun DrawScope.drawMapPinAndRoute(accent: Color, paper: Color) {
    val w = size.width
    val h = size.height

    drawPin(
        center = Offset(w * 0.21f, h * 0.775f),
        r = w * 0.052f,
        color = accent.copy(alpha = 0.38f),
        hole = paper.copy(alpha = 0.85f)
    )

    val route = Path().apply {
        moveTo(w * 0.225f, h * 0.842f)
        cubicTo(w * 0.32f, h * 0.855f, w * 0.41f, h * 0.862f, w * 0.395f, h * 0.900f)
        cubicTo(w * 0.385f, h * 0.935f, w * 0.310f, h * 0.935f, w * 0.315f, h * 0.990f)
    }
    drawPath(
        route,
        accent.copy(alpha = 0.40f),
        style = Stroke(
            width = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 9.dp.toPx()))
        )
    )
}