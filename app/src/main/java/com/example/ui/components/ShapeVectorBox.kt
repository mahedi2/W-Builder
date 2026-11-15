package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.domain.model.ShapeKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ShapeVectorBox(
    shapeKind: ShapeKind,
    fgColor: Color,
    accentColor: Color,
    cornerRadiusPx: Float,
    lineThicknessPx: Float,
    startAngleDeg: Float = -90f,
    sweepAngleDeg: Float = 120f,
    customPathData: String = "",
    filled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        val cx = w / 2f
        val cy = h / 2f
        val outlineStroke = Stroke(width = lineThicknessPx.coerceAtLeast(3f))

        when (shapeKind) {
            ShapeKind.LINE -> {
                drawLine(
                    color = fgColor,
                    start = Offset(0f, cy),
                    end = Offset(w, cy),
                    strokeWidth = lineThicknessPx.coerceAtLeast(2f),
                    cap = StrokeCap.Round
                )
            }
            ShapeKind.SQUARE -> {
                val inset = if (filled) 0f else outlineStroke.width / 2f
                val side = (minDim - inset * 2f).coerceAtLeast(4f)
                val sqLeft = cx - side / 2f
                val sqTop = cy - side / 2f
                if (filled) {
                    drawRoundRect(
                        color = fgColor.copy(alpha = 0.28f),
                        topLeft = Offset(sqLeft, sqTop),
                        size = Size(side, side),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                } else {
                    drawRoundRect(
                        color = fgColor.copy(alpha = 0.9f),
                        topLeft = Offset(sqLeft, sqTop),
                        size = Size(side, side),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = outlineStroke
                    )
                }
            }
            ShapeKind.CIRCLE -> {
                if (filled) {
                    drawCircle(
                        color = fgColor.copy(alpha = 0.28f),
                        radius = minDim / 2f
                    )
                } else {
                    drawCircle(
                        color = fgColor.copy(alpha = 0.9f),
                        radius = ((minDim - outlineStroke.width) / 2f).coerceAtLeast(2f),
                        style = outlineStroke
                    )
                }
            }
            ShapeKind.OVAL -> {
                if (filled) {
                    drawOval(color = fgColor.copy(alpha = 0.28f))
                } else {
                    val inset = outlineStroke.width / 2f
                    drawOval(
                        color = fgColor.copy(alpha = 0.9f),
                        topLeft = Offset(inset, inset),
                        size = Size((w - inset * 2f).coerceAtLeast(4f), (h - inset * 2f).coerceAtLeast(4f)),
                        style = outlineStroke
                    )
                }
            }
            ShapeKind.RING -> {
                val stroke = (minDim * 0.14f).coerceAtLeast(4f)
                drawCircle(
                    color = fgColor.copy(alpha = 0.85f),
                    radius = (minDim - stroke) / 2f,
                    style = Stroke(width = stroke)
                )
            }
            ShapeKind.CIRCLE_SLICE -> {
                val inset = if (filled) 0f else outlineStroke.width / 2f
                val side = (minDim - inset * 2f).coerceAtLeast(4f)
                val left = cx - side / 2f
                val top = cy - side / 2f
                if (filled) {
                    drawArc(
                        color = fgColor.copy(alpha = 0.28f),
                        startAngle = startAngleDeg,
                        sweepAngle = sweepAngleDeg,
                        useCenter = true,
                        topLeft = Offset(left, top),
                        size = Size(side, side)
                    )
                }
                drawArc(
                    color = if (filled) accentColor.copy(alpha = 0.72f) else fgColor.copy(alpha = 0.9f),
                    startAngle = startAngleDeg,
                    sweepAngle = sweepAngleDeg,
                    useCenter = true,
                    topLeft = Offset(left, top),
                    size = Size(side, side),
                    style = if (filled) Stroke(width = 1.6f) else outlineStroke
                )
            }
            ShapeKind.ARC -> {
                val stroke = lineThicknessPx.coerceAtLeast(4f)
                val arcDim = (minDim - stroke).coerceAtLeast(4f)
                val left = cx - arcDim / 2f
                val top = cy - arcDim / 2f
                drawArc(
                    color = fgColor.copy(alpha = 0.85f),
                    startAngle = startAngleDeg,
                    sweepAngle = sweepAngleDeg,
                    useCenter = false,
                    topLeft = Offset(left, top),
                    size = Size(arcDim, arcDim),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            ShapeKind.PILL -> {
                val r = h / 2f
                if (filled) {
                    drawRoundRect(
                        color = fgColor.copy(alpha = 0.28f),
                        cornerRadius = CornerRadius(r, r)
                    )
                } else {
                    val inset = outlineStroke.width / 2f
                    drawRoundRect(
                        color = fgColor.copy(alpha = 0.9f),
                        topLeft = Offset(inset, inset),
                        size = Size((w - inset * 2f).coerceAtLeast(4f), (h - inset * 2f).coerceAtLeast(4f)),
                        cornerRadius = CornerRadius(r, r),
                        style = outlineStroke
                    )
                }
            }
            ShapeKind.RECTANGLE -> {
                if (filled) {
                    drawRoundRect(
                        color = fgColor.copy(alpha = 0.24f),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                    )
                } else {
                    val inset = outlineStroke.width / 2f
                    drawRoundRect(
                        color = fgColor.copy(alpha = 0.9f),
                        topLeft = Offset(inset, inset),
                        size = Size((w - inset * 2f).coerceAtLeast(4f), (h - inset * 2f).coerceAtLeast(4f)),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = outlineStroke
                    )
                }
            }
            ShapeKind.GLASS_CARD -> {
                val r = cornerRadiusPx.coerceAtLeast(12f)
                if (filled) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),
                            accentColor.copy(alpha = 0.35f)
                        )
                    ),
                    cornerRadius = CornerRadius(r, r),
                    style = if (filled) Stroke(width = 1.8f) else outlineStroke
                )
            }
            ShapeKind.SQUIRCLE -> {
                val r = minDim * 0.34f
                if (filled) {
                    drawRoundRect(
                        color = fgColor.copy(alpha = 0.26f),
                        cornerRadius = CornerRadius(r, r)
                    )
                } else {
                    val inset = outlineStroke.width / 2f
                    drawRoundRect(
                        color = fgColor.copy(alpha = 0.9f),
                        topLeft = Offset(inset, inset),
                        size = Size((w - inset * 2f).coerceAtLeast(4f), (h - inset * 2f).coerceAtLeast(4f)),
                        cornerRadius = CornerRadius(r, r),
                        style = outlineStroke
                    )
                }
            }
            ShapeKind.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(cx, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                if (filled) {
                    drawPath(path, color = fgColor.copy(alpha = 0.28f))
                } else {
                    drawPath(path, color = fgColor.copy(alpha = 0.9f), style = outlineStroke)
                }
            }
            ShapeKind.RIGHT_TRIANGLE -> {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                if (filled) {
                    drawPath(path, color = fgColor.copy(alpha = 0.28f))
                } else {
                    drawPath(path, color = fgColor.copy(alpha = 0.9f), style = outlineStroke)
                }
            }
            ShapeKind.DIAMOND -> {
                val path = Path().apply {
                    moveTo(cx, 0f)
                    lineTo(w, cy)
                    lineTo(cx, h)
                    lineTo(0f, cy)
                    close()
                }
                if (filled) {
                    drawPath(path, color = fgColor.copy(alpha = 0.28f))
                } else {
                    drawPath(path, color = fgColor.copy(alpha = 0.9f), style = outlineStroke)
                }
            }
            ShapeKind.HEXAGON -> {
                val path = polygonPath(cx, cy, minDim * 0.48f, sides = 6, startAngleDeg = -90f)
                if (filled) {
                    drawPath(path, color = fgColor.copy(alpha = 0.28f))
                } else {
                    drawPath(path, color = fgColor.copy(alpha = 0.9f), style = outlineStroke)
                }
            }
            ShapeKind.STAR -> {
                val path = starPath(cx, cy, outerR = minDim * 0.48f, innerR = minDim * 0.21f, points = 5)
                if (filled) {
                    drawPath(path, color = accentColor.copy(alpha = 0.35f))
                }
                drawPath(path, color = fgColor.copy(alpha = 0.85f), style = if (filled) Stroke(width = 1.6f) else outlineStroke)
            }
            ShapeKind.ARROW_BADGE -> {
                val tipX = w * 0.72f
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(tipX, 0f)
                    lineTo(w, cy)
                    lineTo(tipX, h)
                    lineTo(0f, h)
                    lineTo(w * 0.16f, cy)
                    close()
                }
                if (filled) {
                    drawPath(path, color = accentColor.copy(alpha = 0.28f))
                } else {
                    drawPath(path, color = fgColor.copy(alpha = 0.9f), style = outlineStroke)
                }
            }
            ShapeKind.PATH -> {
                val path = CustomShapePathParser.toComposePath(customPathData, 0f, 0f, w, h)
                if (filled) {
                    drawPath(path, color = fgColor.copy(alpha = 0.28f))
                }
                drawPath(
                    path,
                    color = if (filled) accentColor.copy(alpha = 0.75f) else fgColor.copy(alpha = 0.9f),
                    style = if (filled) Stroke(width = 1.6f) else outlineStroke
                )
            }
        }
    }
}

private fun polygonPath(cx: Float, cy: Float, radius: Float, sides: Int, startAngleDeg: Float): Path {
    val path = Path()
    for (i in 0 until sides) {
        val rad = ((360f / sides) * i + startAngleDeg) * (PI / 180.0)
        val x = cx + (radius * cos(rad)).toFloat()
        val y = cy + (radius * sin(rad)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun starPath(cx: Float, cy: Float, outerR: Float, innerR: Float, points: Int): Path {
    val path = Path()
    val step = 180f / points
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        val rad = (-90f + i * step) * (PI / 180.0)
        val x = cx + (r * cos(rad)).toFloat()
        val y = cy + (r * sin(rad)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
