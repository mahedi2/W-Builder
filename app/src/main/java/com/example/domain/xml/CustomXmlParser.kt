package com.example.domain.xml

import android.util.Xml
import com.example.data.local.converter.WBuilderJson
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object CustomXmlParser {

    fun parse(rawInput: String, forceJson: Boolean = false): Result<CustomXmlNode> = runCatching {
        val trimmed = rawInput.trim()
        require(trimmed.isNotEmpty()) { "Markup cannot be empty." }

        if (forceJson || trimmed.startsWith("{")) {
            WBuilderJson.instance.decodeFromString(CustomXmlNode.serializer(), trimmed)
        } else {
            parseXml(trimmed)
        }
    }

    private fun parseXml(xmlText: String): CustomXmlNode {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xmlText))

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                return readElement(parser)
            }
            event = parser.next()
        }
        error("No root XML element found.")
    }

    private fun readElement(parser: XmlPullParser): CustomXmlNode {
        val tagName = parser.name ?: error("Invalid XML tag.")
        val attrs = mutableMapOf<String, String>()
        for (i in 0 until parser.attributeCount) {
            attrs[parser.getAttributeName(i)] = parser.getAttributeValue(i)
        }

        val id = attrs["id"]
        val widthRaw = attrs["width"] ?: attrs["android:layout_width"]
        val heightRaw = attrs["height"] ?: attrs["android:layout_height"]
        val fillMaxWidth = widthRaw.equals("match_parent", ignoreCase = true) ||
            widthRaw.equals("fill", ignoreCase = true)
        val fillMaxHeight = heightRaw.equals("match_parent", ignoreCase = true) ||
            heightRaw.equals("fill", ignoreCase = true)
        val widthDp = parseDp(widthRaw)
        val heightDp = parseDp(heightRaw)
        val paddingDp = parseDp(attrs["padding"] ?: attrs["android:padding"]) ?: 0f
        val bgHex = attrs["background"] ?: attrs["android:background"]
        val cornerRadiusDp = parseDp(attrs["cornerRadius"]) ?: 0f
        val alpha = attrs["alpha"]?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f

        val children = mutableListOf<CustomXmlNode>()
        val innerText = StringBuilder()

        var event = parser.next()
        while (event != XmlPullParser.END_TAG) {
            when (event) {
                XmlPullParser.START_TAG -> children.add(readElement(parser))
                XmlPullParser.TEXT -> innerText.append(parser.text.orEmpty())
                XmlPullParser.END_DOCUMENT -> error("Unexpected end of XML inside <$tagName>.")
            }
            event = parser.next()
        }

        return when (tagName.lowercase()) {
            "linearlayout", "column", "row", "framelayout", "box" -> {
                val orientation = when {
                    tagName.equals("Row", ignoreCase = true) -> XmlOrientation.HORIZONTAL
                    tagName.equals("Column", ignoreCase = true) -> XmlOrientation.VERTICAL
                    tagName.equals("FrameLayout", ignoreCase = true) ||
                        tagName.equals("Box", ignoreCase = true) -> XmlOrientation.OVERLAY
                    attrs["orientation"].equals("horizontal", ignoreCase = true) ||
                        attrs["android:orientation"].equals("horizontal", ignoreCase = true) -> XmlOrientation.HORIZONTAL
                    else -> XmlOrientation.VERTICAL
                }
                val gravity = when (attrs["gravity"]?.lowercase()) {
                    "center" -> XmlGravity.CENTER
                    "end", "right" -> XmlGravity.END
                    else -> XmlGravity.START
                }
                CustomXmlNode.Container(
                    id = id,
                    tagName = tagName,
                    orientation = orientation,
                    gravity = gravity,
                    widthDp = widthDp,
                    heightDp = heightDp,
                    fillMaxWidth = fillMaxWidth || widthDp == null,
                    fillMaxHeight = fillMaxHeight,
                    paddingDp = if (attrs.containsKey("padding") || attrs.containsKey("android:padding")) paddingDp else 12f,
                    backgroundColorHex = bgHex,
                    cornerRadiusDp = if (attrs.containsKey("cornerRadius")) cornerRadiusDp else 16f,
                    alpha = alpha,
                    borderColorHex = attrs["borderColor"],
                    borderWidthDp = parseDp(attrs["borderWidth"]) ?: 0f,
                    gradientStartHex = attrs["gradientStart"],
                    gradientEndHex = attrs["gradientEnd"],
                    gradientAngleDegrees = attrs["gradientAngle"]?.toFloatOrNull() ?: 45f,
                    spacingDp = parseDp(attrs["spacing"]) ?: 6f,
                    children = children
                )
            }
            "text", "textview", "digitalclock" -> {
                val textAttr = attrs["text"] ?: attrs["android:text"]
                val content = textAttr ?: innerText.toString().trim().ifEmpty {
                    if (tagName.equals("DigitalClock", ignoreCase = true)) "{{time}}" else ""
                }
                CustomXmlNode.Text(
                    id = id,
                    text = content,
                    textSizeSp = parseDp(attrs["textSize"] ?: attrs["android:textSize"]) ?: 14f,
                    textColorHex = attrs["textColor"] ?: attrs["android:textColor"] ?: "#FFFFFFFF",
                    bold = attrs["bold"].equals("true", ignoreCase = true) ||
                        attrs["textStyle"].equals("bold", ignoreCase = true),
                    fontFamily = attrs["fontFamily"] ?: "Default",
                    align = attrs["align"] ?: "start",
                    maxLines = attrs["maxLines"]?.toIntOrNull()?.coerceAtLeast(1) ?: 3,
                    widthDp = widthDp,
                    heightDp = heightDp,
                    fillMaxWidth = fillMaxWidth,
                    fillMaxHeight = fillMaxHeight,
                    paddingDp = paddingDp,
                    backgroundColorHex = bgHex,
                    cornerRadiusDp = cornerRadiusDp,
                    alpha = alpha
                )
            }
            "progressbar" -> {
                CustomXmlNode.Progress(
                    id = id,
                    progressExpression = attrs["progress"] ?: "{{battery}}",
                    max = attrs["max"]?.toFloatOrNull() ?: 100f,
                    trackColorHex = attrs["trackColor"] ?: "#33FFFFFF",
                    indicatorColorHex = attrs["indicatorColor"] ?: "#FF38BDF8",
                    widthDp = widthDp,
                    heightDp = heightDp ?: 8f,
                    fillMaxWidth = fillMaxWidth || widthDp == null,
                    fillMaxHeight = fillMaxHeight,
                    paddingDp = paddingDp,
                    backgroundColorHex = bgHex,
                    cornerRadiusDp = cornerRadiusDp,
                    alpha = alpha
                )
            }
            "image", "imageview" -> {
                CustomXmlNode.Image(
                    id = id,
                    source = attrs["src"] ?: attrs["android:src"] ?: "",
                    tintColorHex = attrs["tint"],
                    widthDp = widthDp ?: 28f,
                    heightDp = heightDp ?: 28f,
                    fillMaxWidth = fillMaxWidth,
                    fillMaxHeight = fillMaxHeight,
                    paddingDp = paddingDp,
                    backgroundColorHex = bgHex,
                    cornerRadiusDp = cornerRadiusDp,
                    alpha = alpha
                )
            }
            "icon" -> {
                val sizeDp = parseDp(attrs["size"])
                CustomXmlNode.Icon(
                    id = id,
                    symbolKey = attrs["name"] ?: attrs["symbol"] ?: "bolt",
                    tintColorHex = attrs["color"] ?: attrs["tint"] ?: "#FF5CE1E6",
                    widthDp = widthDp ?: sizeDp ?: 20f,
                    heightDp = heightDp ?: sizeDp ?: 20f,
                    fillMaxWidth = fillMaxWidth,
                    fillMaxHeight = fillMaxHeight,
                    paddingDp = paddingDp,
                    backgroundColorHex = bgHex,
                    cornerRadiusDp = cornerRadiusDp,
                    alpha = alpha
                )
            }
            "divider" -> {
                val thickness = parseDp(attrs["thickness"]) ?: 1f
                CustomXmlNode.Divider(
                    id = id,
                    colorHex = attrs["color"] ?: "#33FFFFFF",
                    thicknessDp = thickness,
                    widthDp = widthDp,
                    heightDp = heightDp ?: thickness,
                    fillMaxWidth = fillMaxWidth || widthDp == null,
                    fillMaxHeight = fillMaxHeight,
                    paddingDp = paddingDp,
                    backgroundColorHex = bgHex,
                    cornerRadiusDp = cornerRadiusDp,
                    alpha = alpha
                )
            }
            "spacer" -> {
                CustomXmlNode.Spacer(
                    id = id,
                    widthDp = widthDp ?: 8f,
                    heightDp = heightDp ?: 8f,
                    fillMaxWidth = fillMaxWidth,
                    fillMaxHeight = fillMaxHeight,
                    paddingDp = paddingDp,
                    backgroundColorHex = bgHex,
                    cornerRadiusDp = cornerRadiusDp,
                    alpha = alpha
                )
            }
            else -> error("Unsupported tag <$tagName>. Supported tags: ${CustomXmlSchema.supportedTags.joinToString()}.")
        }
    }

    private fun parseDp(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim()
            .removeSuffix("dp")
            .removeSuffix("sp")
            .removeSuffix("px")
            .trim()
        return cleaned.toFloatOrNull()
    }
}
