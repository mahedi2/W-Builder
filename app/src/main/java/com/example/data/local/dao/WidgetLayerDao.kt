package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.WidgetLayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetLayerDao {

    @Query("SELECT * FROM widget_layers WHERE projectId = :projectId ORDER BY sortOrder ASC")
    fun observeLayersForProject(projectId: String): Flow<List<WidgetLayerEntity>>

    @Query("SELECT * FROM widget_layers WHERE projectId = :projectId ORDER BY sortOrder ASC")
    suspend fun getLayersForProject(projectId: String): List<WidgetLayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLayer(layer: WidgetLayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLayers(layers: List<WidgetLayerEntity>)

    @Query("DELETE FROM widget_layers WHERE id = :layerId")
    suspend fun deleteLayerById(layerId: String)

    @Query("DELETE FROM widget_layers WHERE projectId = :projectId")
    suspend fun deleteLayersForProject(projectId: String)
}
