package com.example.data.repository

import com.example.data.local.entity.CustomComponentEntity
import com.example.data.local.entity.WidgetLayerEntity
import com.example.data.local.entity.WidgetProjectEntity
import com.example.data.local.entity.WidgetProjectWithLayers
import com.example.domain.model.CustomComponent
import com.example.domain.model.WidgetLayer
import com.example.domain.model.WidgetProject

fun WidgetProjectWithLayers.toDomain(): WidgetProject = WidgetProject(
    id = project.id,
    name = project.name,
    description = project.description,
    dimension = project.dimension,
    customWidthDp = project.customWidthDp,
    customHeightDp = project.customHeightDp,
    gridSnapDp = project.gridSnapDp,
    backgroundColorHex = project.backgroundColorHex,
    backgroundGradient = project.backgroundGradient,
    backgroundCornerRadiusDp = project.backgroundCornerRadiusDp,
    customXmlOverlay = project.customXmlOverlay,
    previewAssetPath = project.previewAssetPath,
    createdAt = project.createdAt,
    updatedAt = project.updatedAt,
    layers = layers
        .sortedBy { it.sortOrder }
        .map { it.toDomain() }
)

fun WidgetLayerEntity.toDomain(): WidgetLayer = WidgetLayer(
    id = id,
    projectId = projectId,
    name = name,
    type = type,
    style = style,
    payload = payload,
    tapAction = tapAction,
    isVisible = isVisible,
    isLocked = isLocked
)

fun WidgetProject.toEntity(): WidgetProjectEntity = WidgetProjectEntity(
    id = id,
    name = name,
    description = description,
    dimension = dimension,
    customWidthDp = customWidthDp,
    customHeightDp = customHeightDp,
    gridSnapDp = gridSnapDp,
    backgroundColorHex = backgroundColorHex,
    backgroundGradient = backgroundGradient,
    backgroundCornerRadiusDp = backgroundCornerRadiusDp,
    customXmlOverlay = customXmlOverlay,
    previewAssetPath = previewAssetPath,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WidgetLayer.toEntity(sortOrder: Int): WidgetLayerEntity = WidgetLayerEntity(
    id = id,
    projectId = projectId,
    name = name,
    type = type,
    style = style,
    payload = payload,
    tapAction = tapAction,
    isVisible = isVisible,
    isLocked = isLocked,
    sortOrder = sortOrder
)

fun CustomComponentEntity.toDomain(): CustomComponent = CustomComponent(
    id = id,
    name = name,
    category = category,
    description = description,
    widthDp = widthDp,
    heightDp = heightDp,
    layers = layers,
    rawXmlSnippet = rawXmlSnippet,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CustomComponent.toEntity(): CustomComponentEntity = CustomComponentEntity(
    id = id,
    name = name,
    category = category,
    description = description,
    widthDp = widthDp,
    heightDp = heightDp,
    layers = layers,
    rawXmlSnippet = rawXmlSnippet,
    createdAt = createdAt,
    updatedAt = updatedAt
)
