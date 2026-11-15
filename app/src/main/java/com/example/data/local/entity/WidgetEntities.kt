package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.domain.model.GradientConfig
import com.example.domain.model.LayerPayload
import com.example.domain.model.LayerStyle
import com.example.domain.model.LayerType
import com.example.domain.model.TapAction
import com.example.domain.model.WidgetDimension
import com.example.domain.model.WidgetLayer

@Entity(tableName = "widget_projects")
data class WidgetProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val dimension: WidgetDimension,
    val customWidthDp: Float,
    val customHeightDp: Float,
    val gridSnapDp: Float,
    val backgroundColorHex: String,
    val backgroundGradient: GradientConfig,
    val backgroundCornerRadiusDp: Float,
    val customXmlOverlay: String?,
    val previewAssetPath: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "widget_layers",
    foreignKeys = [
        ForeignKey(
            entity = WidgetProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class WidgetLayerEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val name: String,
    val type: LayerType,
    val style: LayerStyle,
    val payload: LayerPayload,
    val tapAction: TapAction,
    val isVisible: Boolean,
    val isLocked: Boolean,
    val sortOrder: Int
)

@Entity(tableName = "custom_components")
data class CustomComponentEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val widthDp: Float,
    val heightDp: Float,
    val layers: List<WidgetLayer>,
    val rawXmlSnippet: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "app_widget_bindings",
    foreignKeys = [
        ForeignKey(
            entity = WidgetProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class AppWidgetBindingEntity(
    @PrimaryKey
    val appWidgetId: Int,
    val projectId: String,
    val lastUpdatedAt: Long
)

data class WidgetProjectWithLayers(
    @Embedded
    val project: WidgetProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val layers: List<WidgetLayerEntity>
)
