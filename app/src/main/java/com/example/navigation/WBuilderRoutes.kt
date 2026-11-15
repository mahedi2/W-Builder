package com.example.navigation

import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

@Serializable
data class EditorRoute(
    val projectId: String
)

@Serializable
data class ComponentBuilderRoute(
    val componentId: String
)
