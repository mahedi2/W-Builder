package com.example.ui.components

import android.graphics.Path as AndroidPath
import androidx.compose.ui.graphics.Path as ComposePath

/**
 * Parses compact SVG path commands (`M`, `L`, `H`, `V`, `Q`, `C`, `Z`) defined on a normalized
 * `0..100` coordinate grid and scales them cleanly to any bounding rectangle.
 */
object CustomShapePathParser {

    val PRESETS: List<Pair<String, String>> = listOf(
        "Pentagon" to "M 50 4 L 96 38 L 78 94 L 22 94 L 4 38 Z",
        "Shield" to "M 50 4 L 90 18 L 86 58 Q 50 96 50 96 Q 14 58 10 18 Z",
        "Lightning" to "M 58 4 L 18 54 L 46 54 L 38 96 L 82 44 L 54 44 Z",
        "Heart" to "M 50 90 C 18 68 6 44 12 24 C 18 10 38 10 50 26 C 62 10 82 10 88 24 C 94 44 82 68 50 90 Z",
        "Wave" to "M 0 65 Q 25 25 50 55 Q 75 85 100 35 L 100 100 L 0 100 Z"
    )

    fun toComposePath(raw: String, left: Float, top: Float, width: Float, height: Float): ComposePath {
        val path = ComposePath()
        parseCommands(
            raw = raw,
            left = left,
            top = top,
            width = width,
            height = height,
            moveTo = { x, y -> path.moveTo(x, y) },
            lineTo = { x, y -> path.lineTo(x, y) },
            quadTo = { cx, cy, x, y -> path.quadraticBezierTo(cx, cy, x, y) },
            cubicTo = { c1x, c1y, c2x, c2y, x, y -> path.cubicTo(c1x, c1y, c2x, c2y, x, y) },
            close = { path.close() }
        )
        return path
    }

    fun toAndroidPath(raw: String, left: Float, top: Float, width: Float, height: Float): AndroidPath {
        val path = AndroidPath()
        parseCommands(
            raw = raw,
            left = left,
            top = top,
            width = width,
            height = height,
            moveTo = { x, y -> path.moveTo(x, y) },
            lineTo = { x, y -> path.lineTo(x, y) },
            quadTo = { cx, cy, x, y -> path.quadTo(cx, cy, x, y) },
            cubicTo = { c1x, c1y, c2x, c2y, x, y -> path.cubicTo(c1x, c1y, c2x, c2y, x, y) },
            close = { path.close() }
        )
        return path
    }

    private fun parseCommands(
        raw: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        moveTo: (Float, Float) -> Unit,
        lineTo: (Float, Float) -> Unit,
        quadTo: (Float, Float, Float, Float) -> Unit,
        cubicTo: (Float, Float, Float, Float, Float, Float) -> Unit,
        close: () -> Unit
    ) {
        val tokens = Regex("[MLHVQCZmlhvqcz]|-?\\d+(?:\\.\\d+)?")
            .findAll(raw.ifBlank { PRESETS.first().second })
            .map { it.value }
            .toList()

        fun sx(v: Float) = left + (v / 100f) * width
        fun sy(v: Float) = top + (v / 100f) * height

        var idx = 0
        var cmd = "M"
        var curX = 0f
        var curY = 0f

        while (idx < tokens.size) {
            val tok = tokens[idx]
            if (tok.length == 1 && tok[0].isLetter()) {
                cmd = tok
                idx++
                if (cmd.equals("Z", ignoreCase = true)) {
                    close()
                }
                continue
            }
            when (cmd) {
                "M", "m" -> {
                    val nx = tokens.getOrNull(idx)?.toFloatOrNull() ?: break
                    val ny = tokens.getOrNull(idx + 1)?.toFloatOrNull() ?: break
                    idx += 2
                    if (cmd == "m") {
                        curX += nx; curY += ny
                    } else {
                        curX = nx; curY = ny
                    }
                    moveTo(sx(curX), sy(curY))
                    cmd = if (cmd == "m") "l" else "L"
                }
                "L", "l" -> {
                    val nx = tokens.getOrNull(idx)?.toFloatOrNull() ?: break
                    val ny = tokens.getOrNull(idx + 1)?.toFloatOrNull() ?: break
                    idx += 2
                    if (cmd == "l") {
                        curX += nx; curY += ny
                    } else {
                        curX = nx; curY = ny
                    }
                    lineTo(sx(curX), sy(curY))
                }
                "H", "h" -> {
                    val nx = tokens.getOrNull(idx)?.toFloatOrNull() ?: break
                    idx += 1
                    curX = if (cmd == "h") curX + nx else nx
                    lineTo(sx(curX), sy(curY))
                }
                "V", "v" -> {
                    val ny = tokens.getOrNull(idx)?.toFloatOrNull() ?: break
                    idx += 1
                    curY = if (cmd == "v") curY + ny else ny
                    lineTo(sx(curX), sy(curY))
                }
                "Q", "q" -> {
                    val cx = tokens.getOrNull(idx)?.toFloatOrNull() ?: break
                    val cy = tokens.getOrNull(idx + 1)?.toFloatOrNull() ?: break
                    val nx = tokens.getOrNull(idx + 2)?.toFloatOrNull() ?: break
                    val ny = tokens.getOrNull(idx + 3)?.toFloatOrNull() ?: break
                    idx += 4
                    val absCx = if (cmd == "q") curX + cx else cx
                    val absCy = if (cmd == "q") curY + cy else cy
                    curX = if (cmd == "q") curX + nx else nx
                    curY = if (cmd == "q") curY + ny else ny
                    quadTo(sx(absCx), sy(absCy), sx(curX), sy(curY))
                }
                "C", "c" -> {
                    val c1x = tokens.getOrNull(idx)?.toFloatOrNull() ?: break
                    val c1y = tokens.getOrNull(idx + 1)?.toFloatOrNull() ?: break
                    val c2x = tokens.getOrNull(idx + 2)?.toFloatOrNull() ?: break
                    val c2y = tokens.getOrNull(idx + 3)?.toFloatOrNull() ?: break
                    val nx = tokens.getOrNull(idx + 4)?.toFloatOrNull() ?: break
                    val ny = tokens.getOrNull(idx + 5)?.toFloatOrNull() ?: break
                    idx += 6
                    val a1x = if (cmd == "c") curX + c1x else c1x
                    val a1y = if (cmd == "c") curY + c1y else c1y
                    val a2x = if (cmd == "c") curX + c2x else c2x
                    val a2y = if (cmd == "c") curY + c2y else c2y
                    curX = if (cmd == "c") curX + nx else nx
                    curY = if (cmd == "c") curY + ny else ny
                    cubicTo(sx(a1x), sy(a1y), sx(a2x), sy(a2y), sx(curX), sy(curY))
                }
                else -> idx++
            }
        }
    }
}
