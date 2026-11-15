package com.example.data.local.converter

import androidx.room.TypeConverter
import com.example.domain.model.GradientConfig
import com.example.domain.model.LayerPayload
import com.example.domain.model.LayerStyle
import com.example.domain.model.LayerType
import com.example.domain.model.TapAction
import com.example.domain.model.WidgetDimension
import com.example.domain.model.WidgetLayer
import kotlinx.serialization.json.Json

object WBuilderJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        classDiscriminator = "schemaType"
    }
}

class WBuilderTypeConverters {
    private val json = WBuilderJson.instance

    @TypeConverter
    fun fromWidgetDimension(value: WidgetDimension): String = value.name

    @TypeConverter
    fun toWidgetDimension(value: String): WidgetDimension =
        runCatching { WidgetDimension.valueOf(value) }.getOrDefault(WidgetDimension.SIZE_4X2)

    @TypeConverter
    fun fromLayerType(value: LayerType): String = value.name

    @TypeConverter
    fun toLayerType(value: String): LayerType =
        runCatching { LayerType.valueOf(value) }.getOrDefault(LayerType.CUSTOM_TEXT)

    @TypeConverter
    fun fromGradientConfig(value: GradientConfig): String =
        json.encodeToString(GradientConfig.serializer(), value)

    @TypeConverter
    fun toGradientConfig(value: String): GradientConfig =
        runCatching { json.decodeFromString(GradientConfig.serializer(), value) }
            .getOrDefault(GradientConfig())

    @TypeConverter
    fun fromLayerStyle(value: LayerStyle): String =
        json.encodeToString(LayerStyle.serializer(), value)

    @TypeConverter
    fun toLayerStyle(value: String): LayerStyle =
        runCatching { json.decodeFromString(LayerStyle.serializer(), value) }
            .getOrDefault(LayerStyle())

    @TypeConverter
    fun fromLayerPayload(value: LayerPayload): String =
        json.encodeToString(LayerPayload.serializer(), value)

    @TypeConverter
    fun toLayerPayload(value: String): LayerPayload =
        runCatching { json.decodeFromString(LayerPayload.serializer(), value) }
            .getOrDefault(LayerPayload.CustomText())

    @TypeConverter
    fun fromTapAction(value: TapAction): String =
        json.encodeToString(TapAction.serializer(), value)

    @TypeConverter
    fun toTapAction(value: String): TapAction =
        runCatching { json.decodeFromString(TapAction.serializer(), value) }
            .getOrDefault(TapAction.None)

    @TypeConverter
    fun fromWidgetLayerList(value: List<WidgetLayer>): String =
        json.encodeToString(kotlinx.serialization.builtins.ListSerializer(WidgetLayer.serializer()), value)

    @TypeConverter
    fun toWidgetLayerList(value: String): List<WidgetLayer> =
        runCatching {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(WidgetLayer.serializer()),
                value
            )
        }.getOrDefault(emptyList())
}
