package com.example.domain.xml

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Declarative AST node schema for W builder's "Code / Custom XML & JSON" mode.
 * Supports safe rendering in both the Jetpack Compose canvas preview and
 * Jetpack Glance home screen RemoteViews without arbitrary code execution.
 */
@Serializable
sealed interface CustomXmlNode {
    val id: String?
    val widthDp: Float?
    val heightDp: Float?
    val fillMaxWidth: Boolean
    val fillMaxHeight: Boolean
    val paddingDp: Float
    val backgroundColorHex: String?
    val cornerRadiusDp: Float
    val alpha: Float

    @Serializable
    @SerialName("container")
    data class Container(
        override val id: String? = null,
        val tagName: String = "LinearLayout",
        val orientation: XmlOrientation = XmlOrientation.VERTICAL,
        val gravity: XmlGravity = XmlGravity.START,
        override val widthDp: Float? = null,
        override val heightDp: Float? = null,
        override val fillMaxWidth: Boolean = true,
        override val fillMaxHeight: Boolean = false,
        override val paddingDp: Float = 12f,
        override val backgroundColorHex: String? = null,
        override val cornerRadiusDp: Float = 16f,
        override val alpha: Float = 1f,
        val borderColorHex: String? = null,
        val borderWidthDp: Float = 0f,
        val gradientStartHex: String? = null,
        val gradientEndHex: String? = null,
        val gradientAngleDegrees: Float = 45f,
        val spacingDp: Float = 6f,
        val children: List<CustomXmlNode> = emptyList()
    ) : CustomXmlNode

    @Serializable
    @SerialName("text")
    data class Text(
        override val id: String? = null,
        val text: String = "",
        val textSizeSp: Float = 14f,
        val textColorHex: String = "#FFFFFFFF",
        val bold: Boolean = false,
        val fontFamily: String = "Default",
        val align: String = "start",
        val maxLines: Int = 3,
        override val widthDp: Float? = null,
        override val heightDp: Float? = null,
        override val fillMaxWidth: Boolean = false,
        override val fillMaxHeight: Boolean = false,
        override val paddingDp: Float = 0f,
        override val backgroundColorHex: String? = null,
        override val cornerRadiusDp: Float = 0f,
        override val alpha: Float = 1f
    ) : CustomXmlNode

    @Serializable
    @SerialName("progress")
    data class Progress(
        override val id: String? = null,
        val progressExpression: String = "{{battery}}",
        val max: Float = 100f,
        val trackColorHex: String = "#33FFFFFF",
        val indicatorColorHex: String = "#FF38BDF8",
        override val widthDp: Float? = null,
        override val heightDp: Float? = 8f,
        override val fillMaxWidth: Boolean = true,
        override val fillMaxHeight: Boolean = false,
        override val paddingDp: Float = 0f,
        override val backgroundColorHex: String? = null,
        override val cornerRadiusDp: Float = 4f,
        override val alpha: Float = 1f
    ) : CustomXmlNode

    @Serializable
    @SerialName("image")
    data class Image(
        override val id: String? = null,
        val source: String = "",
        val tintColorHex: String? = null,
        override val widthDp: Float? = 28f,
        override val heightDp: Float? = 28f,
        override val fillMaxWidth: Boolean = false,
        override val fillMaxHeight: Boolean = false,
        override val paddingDp: Float = 0f,
        override val backgroundColorHex: String? = null,
        override val cornerRadiusDp: Float = 8f,
        override val alpha: Float = 1f
    ) : CustomXmlNode

    @Serializable
    @SerialName("spacer")
    data class Spacer(
        override val id: String? = null,
        override val widthDp: Float? = 8f,
        override val heightDp: Float? = 8f,
        override val fillMaxWidth: Boolean = false,
        override val fillMaxHeight: Boolean = false,
        override val paddingDp: Float = 0f,
        override val backgroundColorHex: String? = null,
        override val cornerRadiusDp: Float = 0f,
        override val alpha: Float = 1f
    ) : CustomXmlNode

    @Serializable
    @SerialName("icon")
    data class Icon(
        override val id: String? = null,
        val symbolKey: String = "bolt",
        val tintColorHex: String = "#FF5CE1E6",
        override val widthDp: Float? = 20f,
        override val heightDp: Float? = 20f,
        override val fillMaxWidth: Boolean = false,
        override val fillMaxHeight: Boolean = false,
        override val paddingDp: Float = 0f,
        override val backgroundColorHex: String? = null,
        override val cornerRadiusDp: Float = 0f,
        override val alpha: Float = 1f
    ) : CustomXmlNode

    @Serializable
    @SerialName("divider")
    data class Divider(
        override val id: String? = null,
        val colorHex: String = "#33FFFFFF",
        val thicknessDp: Float = 1f,
        override val widthDp: Float? = null,
        override val heightDp: Float? = 1f,
        override val fillMaxWidth: Boolean = true,
        override val fillMaxHeight: Boolean = false,
        override val paddingDp: Float = 0f,
        override val backgroundColorHex: String? = null,
        override val cornerRadiusDp: Float = 0f,
        override val alpha: Float = 1f
    ) : CustomXmlNode
}

@Serializable
enum class XmlOrientation {
    VERTICAL,
    HORIZONTAL,
    OVERLAY
}

@Serializable
enum class XmlGravity {
    START,
    CENTER,
    END
}

object CustomXmlSchema {
    val supportedTags = setOf(
        "LinearLayout",
        "FrameLayout",
        "Box",
        "Column",
        "Row",
        "Text",
        "TextView",
        "DigitalClock",
        "ProgressBar",
        "Image",
        "ImageView",
        "Icon",
        "Divider",
        "Spacer"
    )

    val supportedTokens = listOf(
        "{{time}}",
        "{{time_24}}",
        "{{date}}",
        "{{day}}",
        "{{greeting}}",
        "{{battery}}",
        "{{battery_status}}",
        "{{ram_used}}",
        "{{storage_used}}",
        "{{volume}}",
        "{{network}}",
        "{{alarm}}",
        "{{weather_temp}}",
        "{{weather_city}}",
        "{{weather_cond}}",
        "{{weather_feels}}",
        "{{humidity}}",
        "{{wind}}"
    )
}
