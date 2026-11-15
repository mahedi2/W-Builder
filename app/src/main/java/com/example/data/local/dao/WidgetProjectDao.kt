package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.AppWidgetBindingEntity
import com.example.data.local.entity.WidgetProjectEntity
import com.example.data.local.entity.WidgetProjectWithLayers
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetProjectDao {

    @Transaction
    @Query("SELECT * FROM widget_projects ORDER BY updatedAt DESC")
    fun observeAllProjectsWithLayers(): Flow<List<WidgetProjectWithLayers>>

    @Transaction
    @Query("SELECT * FROM widget_projects WHERE id = :projectId LIMIT 1")
    fun observeProjectWithLayers(projectId: String): Flow<WidgetProjectWithLayers?>

    @Transaction
    @Query("SELECT * FROM widget_projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectWithLayers(projectId: String): WidgetProjectWithLayers?

    @Transaction
    @Query("SELECT * FROM widget_projects ORDER BY updatedAt DESC")
    suspend fun getAllProjectsWithLayersOnce(): List<WidgetProjectWithLayers>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: WidgetProjectEntity)

    @Query("DELETE FROM widget_projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    @Query("SELECT * FROM app_widget_bindings WHERE appWidgetId = :appWidgetId LIMIT 1")
    fun observeWidgetBinding(appWidgetId: Int): Flow<AppWidgetBindingEntity?>

    @Query("SELECT * FROM app_widget_bindings WHERE appWidgetId = :appWidgetId LIMIT 1")
    suspend fun getWidgetBinding(appWidgetId: Int): AppWidgetBindingEntity?

    @Query("SELECT * FROM app_widget_bindings WHERE projectId = :projectId")
    suspend fun getBindingsForProject(projectId: String): List<AppWidgetBindingEntity>

    @Query("SELECT * FROM app_widget_bindings")
    suspend fun getAllBindings(): List<AppWidgetBindingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWidgetBinding(binding: AppWidgetBindingEntity)

    @Query("DELETE FROM app_widget_bindings WHERE appWidgetId = :appWidgetId")
    suspend fun deleteWidgetBinding(appWidgetId: Int)
}
