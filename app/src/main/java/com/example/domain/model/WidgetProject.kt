package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WidgetLayer(
    val id: String,
    val projectId: String,
    val name: String,
    val type: LayerType,
    val style: LayerStyle = LayerStyle(),
    val payload: LayerPayload,
    val tapAction: TapAction = TapAction.None,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false
)

@Serializable
data class WidgetProject(
    val id: String,
    val name: String,
    val description: String = "",
    val dimension: WidgetDimension = WidgetDimension.SIZE_4X2,
    val customWidthDp: Float = 320f,
    val customHeightDp: Float = 160f,
    val gridSnapDp: Float = 8f,
    val backgroundColorHex: String = "#FF181C2E",
    val backgroundGradient: GradientConfig = GradientConfig(),
    val backgroundCornerRadiusDp: Float = 24f,
    val customXmlOverlay: String? = null,
    val previewAssetPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val layers: List<WidgetLayer> = emptyList()
) {
    val resolvedWidthDp: Float
        get() = if (dimension == WidgetDimension.CUSTOM) customWidthDp else dimension.defaultWidthDp

    val resolvedHeightDp: Float
        get() = if (dimension == WidgetDimension.CUSTOM) customHeightDp else dimension.defaultHeightDp
}

@Serializable
data class CustomComponent(
    val id: String,
    val name: String,
    val category: String = "General",
    val description: String = "",
    val widthDp: Float = 160f,
    val heightDp: Float = 80f,
    val layers: List<WidgetLayer> = emptyList(),
    val rawXmlSnippet: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class WBuilderArchive(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val projects: List<WidgetProject> = emptyList(),
    val customComponents: List<CustomComponent> = emptyList(),
    val embeddedAssetsBase64: Map<String, String> = emptyMap()
)
