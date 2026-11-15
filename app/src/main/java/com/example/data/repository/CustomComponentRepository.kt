package com.example.data.repository

import com.example.data.local.WBuilderDatabase
import com.example.domain.model.CustomComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomComponentRepository(database: WBuilderDatabase) {

    private val componentDao = database.customComponentDao()

    fun observeAllComponents(): Flow<List<CustomComponent>> =
        componentDao.observeAllComponents().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getAllComponentsOnce(): List<CustomComponent> =
        componentDao.getAllComponentsOnce().map { it.toDomain() }

    suspend fun getComponentById(componentId: String): CustomComponent? =
        componentDao.getComponentById(componentId)?.toDomain()

    suspend fun saveComponent(component: CustomComponent) {
        val updated = component.copy(updatedAt = System.currentTimeMillis())
        componentDao.upsertComponent(updated.toEntity())
    }

    suspend fun saveComponents(components: List<CustomComponent>) {
        if (components.isEmpty()) return
        val now = System.currentTimeMillis()
        componentDao.upsertComponents(
            components.map { it.copy(updatedAt = now).toEntity() }
        )
    }

    suspend fun deleteComponent(componentId: String) {
        componentDao.deleteComponentById(componentId)
    }
}
