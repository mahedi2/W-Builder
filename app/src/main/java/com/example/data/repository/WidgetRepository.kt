package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.WBuilderDatabase
import com.example.data.local.entity.AppWidgetBindingEntity
import com.example.domain.model.WidgetLayer
import com.example.domain.model.WidgetProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WidgetRepository(private val database: WBuilderDatabase) {

    private val projectDao = database.widgetProjectDao()
    private val layerDao = database.widgetLayerDao()

    fun observeAllProjects(): Flow<List<WidgetProject>> =
        projectDao.observeAllProjectsWithLayers().map { list ->
            list.map { it.toDomain() }
        }

    fun observeProject(projectId: String): Flow<WidgetProject?> =
        projectDao.observeProjectWithLayers(projectId).map { it?.toDomain() }

    suspend fun getProjectOnce(projectId: String): WidgetProject? =
        projectDao.getProjectWithLayers(projectId)?.toDomain()

    suspend fun getAllProjectsOnce(): List<WidgetProject> =
        projectDao.getAllProjectsWithLayersOnce().map { it.toDomain() }

    suspend fun saveProject(project: WidgetProject) {
        val now = System.currentTimeMillis()
        val updatedProject = project.copy(updatedAt = now)
        val normalizedLayers = updatedProject.layers.mapIndexed { index, layer ->
            layer.copy(projectId = updatedProject.id).toEntity(sortOrder = index)
        }
        database.withTransaction {
            projectDao.upsertProject(updatedProject.toEntity())
            layerDao.deleteLayersForProject(updatedProject.id)
            if (normalizedLayers.isNotEmpty()) {
                layerDao.upsertLayers(normalizedLayers)
            }
        }
    }

    suspend fun upsertLayer(layer: WidgetLayer, sortOrder: Int) {
        layerDao.upsertLayer(layer.toEntity(sortOrder))
    }

    suspend fun deleteLayer(layerId: String) {
        layerDao.deleteLayerById(layerId)
    }

    suspend fun deleteProject(projectId: String) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun bindAppWidget(appWidgetId: Int, projectId: String) {
        projectDao.upsertWidgetBinding(
            AppWidgetBindingEntity(
                appWidgetId = appWidgetId,
                projectId = projectId,
                lastUpdatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getBoundProjectId(appWidgetId: Int): String? =
        projectDao.getWidgetBinding(appWidgetId)?.projectId

    suspend fun getBindingsForProject(projectId: String): List<Int> =
        projectDao.getBindingsForProject(projectId).map { it.appWidgetId }

    suspend fun getAllBoundAppWidgetIds(): List<Int> =
        projectDao.getAllBindings().map { it.appWidgetId }

    suspend fun unbindAppWidget(appWidgetId: Int) {
        projectDao.deleteWidgetBinding(appWidgetId)
    }
}
