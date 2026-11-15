package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.DeviceTelemetrySnapshot
import com.example.data.repository.WeatherSnapshot
import com.example.domain.model.GradientConfig
import com.example.domain.resolver.DynamicDataResolver
import com.example.domain.xml.CustomXmlNode
import com.example.domain.xml.XmlGravity
import com.example.domain.xml.XmlOrientation
import com.example.ui.editor.resolveVectorIcon
import java.io.File

@Composable
fun CustomXmlComposeRenderer(
    node: CustomXmlNode,
    scale: Float,
    telemetry: DeviceTelemetrySnapshot,
    weather: WeatherSnapshot?,
    useCelsius: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape((node.cornerRadiusDp * scale).coerceAtLeast(0f).dp)
    val bgBrush = if (node is CustomXmlNode.Container && !node.gradientStartHex.isNullOrBlank() && !node.gradientEndHex.isNullOrBlank()) {
        buildGradientOrSolidBrush(
            solidHex = node.backgroundColorHex ?: "#00000000",
            gradient = GradientConfig(
                enabled = true,
                startColorHex = node.gradientStartHex,
                endColorHex = node.gradientEndHex,
                angleDegrees = node.gradientAngleDegrees
            ),
            fallbackColor = Color.Transparent
        )
    } else {
        buildGradientOrSolidBrush(
            solidHex = node.backgroundColorHex ?: "#00000000",
            gradient = GradientConfig(),
            fallbackColor = Color.Transparent
        )
    }
    val nodeModifier = modifier
        .alpha(node.alpha.coerceIn(0f, 1f))
        .then(if (node.fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
        .then(if (node.fillMaxHeight) Modifier.fillMaxHeight() else Modifier)
        .then(if (node.widthDp != null && !node.fillMaxWidth) Modifier.width((node.widthDp!! * scale).dp) else Modifier)
        .then(if (node.heightDp != null && !node.fillMaxHeight) Modifier.height((node.heightDp!! * scale).dp) else Modifier)
        .clip(shape)
        .background(bgBrush)
        .padding((node.paddingDp * scale).dp)

    when (node) {
        is CustomXmlNode.Container -> {
            val containerModifier = nodeModifier.then(
                if (node.borderWidthDp > 0f && !node.borderColorHex.isNullOrBlank()) {
                    Modifier.border(
                        width = (node.borderWidthDp * scale).coerceAtLeast(0.5f).dp,
                        color = parseHexColorSafe(node.borderColorHex, Color.White.copy(alpha = 0.2f)),
                        shape = shape
                    )
                } else Modifier
            )
            when (node.orientation) {
                XmlOrientation.VERTICAL -> {
                    val align = when (node.gravity) {
                        XmlGravity.CENTER -> Alignment.CenterHorizontally
                        XmlGravity.END -> Alignment.End
                        XmlGravity.START -> Alignment.Start
                    }
                    Column(
                        modifier = containerModifier,
                        verticalArrangement = Arrangement.spacedBy((node.spacingDp * scale).dp),
                        horizontalAlignment = align
                    ) {
                        node.children.forEach { child ->
                            CustomXmlComposeRenderer(
                                node = child,
                                scale = scale,
                                telemetry = telemetry,
                                weather = weather,
                                useCelsius = useCelsius
                            )
                        }
                    }
                }
                XmlOrientation.HORIZONTAL -> {
                    val align = when (node.gravity) {
                        XmlGravity.CENTER -> Alignment.CenterVertically
                        XmlGravity.END -> Alignment.Bottom
                        XmlGravity.START -> Alignment.Top
                    }
                    Row(
                        modifier = containerModifier,
                        horizontalArrangement = Arrangement.spacedBy((node.spacingDp * scale).dp),
                        verticalAlignment = align
                    ) {
                        node.children.forEach { child ->
                            CustomXmlComposeRenderer(
                                node = child,
                                scale = scale,
                                telemetry = telemetry,
                                weather = weather,
                                useCelsius = useCelsius
                            )
                        }
                    }
                }
                XmlOrientation.OVERLAY -> {
                    val align = when (node.gravity) {
                        XmlGravity.CENTER -> Alignment.Center
                        XmlGravity.END -> Alignment.BottomEnd
                        XmlGravity.START -> Alignment.TopStart
                    }
                    Box(modifier = containerModifier, contentAlignment = align) {
                        node.children.forEach { child ->
                            CustomXmlComposeRenderer(
                                node = child,
                                scale = scale,
                                telemetry = telemetry,
                                weather = weather,
                                useCelsius = useCelsius
                            )
                        }
                    }
                }
            }
        }
        is CustomXmlNode.Text -> {
            val resolved = DynamicDataResolver.resolveTokens(
                template = node.text,
                telemetry = telemetry,
                weather = weather,
                useCelsius = useCelsius
            )
            val textAlign = when (node.align.lowercase()) {
                "center" -> TextAlign.Center
                "end", "right" -> TextAlign.End
                else -> TextAlign.Start
            }
            Text(
                text = resolved,
                color = parseHexColorSafe(node.textColorHex, Color.White),
                fontFamily = resolveComposeFontFamily(node.fontFamily, null),
                fontSize = (node.textSizeSp * scale).coerceIn(7f, 48f).sp,
                fontWeight = if (node.bold) FontWeight.Bold else FontWeight.Medium,
                textAlign = textAlign,
                maxLines = node.maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = nodeModifier
            )
        }
        is CustomXmlNode.Progress -> {
            val resolvedValue = DynamicDataResolver.resolveTokens(
                template = node.progressExpression,
                telemetry = telemetry,
                weather = weather,
                useCelsius = useCelsius
            ).replace("%", "").trim().toFloatOrNull() ?: 50f
            val fraction = (resolvedValue / node.max.coerceAtLeast(1f)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { fraction },
                color = parseHexColorSafe(node.indicatorColorHex, Color(0xFF38BDF8)),
                trackColor = parseHexColorSafe(node.trackColorHex, Color.White.copy(alpha = 0.2f)),
                modifier = nodeModifier.height(((node.heightDp ?: 8f) * scale).coerceAtLeast(3f).dp)
            )
        }
        is CustomXmlNode.Image -> {
            val file = node.source.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
            if (file != null) {
                AsyncImage(
                    model = file,
                    contentDescription = "XML image",
                    contentScale = ContentScale.Crop,
                    modifier = nodeModifier
                )
            } else {
                Box(modifier = nodeModifier, contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = "XML image placeholder",
                        tint = node.tintColorHex?.let { parseHexColorSafe(it, Color.White) } ?: Color.White,
                        modifier = Modifier.size(((node.widthDp ?: 24f) * scale).coerceAtLeast(12f).dp)
                    )
                }
            }
        }
        is CustomXmlNode.Icon -> {
            Icon(
                imageVector = resolveVectorIcon(node.symbolKey),
                contentDescription = node.symbolKey,
                tint = parseHexColorSafe(node.tintColorHex, Color(0xFF5CE1E6)),
                modifier = nodeModifier.size(((node.widthDp ?: 20f) * scale).coerceAtLeast(10f).dp)
            )
        }
        is CustomXmlNode.Divider -> {
            HorizontalDivider(
                thickness = (node.thicknessDp * scale).coerceAtLeast(1f).dp,
                color = parseHexColorSafe(node.colorHex, Color.White.copy(alpha = 0.25f)),
                modifier = nodeModifier
            )
        }
        is CustomXmlNode.Spacer -> {
            Spacer(
                modifier = Modifier.size(
                    width = ((node.widthDp ?: 8f) * scale).dp,
                    height = ((node.heightDp ?: 8f) * scale).dp
                )
            )
        }
    }
}
