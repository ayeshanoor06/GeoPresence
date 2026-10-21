package com.ayesha.geopresence.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AttendanceRing(
    percent: Int,
    trackColor: Color,
    progressColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 104.dp,
    strokeWidth: Dp = 11.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge
) {
    val target = percent.coerceIn(0, 100) / 100f
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(target) { sweep.animateTo(target, tween(durationMillis = 900)) }

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(size.width - strokePx, size.height - strokePx)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * sweep.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$percent%",
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}