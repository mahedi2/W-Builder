package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CustomComponentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomComponentDao {

    @Query("SELECT * FROM custom_components ORDER BY updatedAt DESC")
    fun observeAllComponents(): Flow<List<CustomComponentEntity>>

    @Query("SELECT * FROM custom_components ORDER BY updatedAt DESC")
    suspend fun getAllComponentsOnce(): List<CustomComponentEntity>

    @Query("SELECT * FROM custom_components WHERE id = :componentId LIMIT 1")
    suspend fun getComponentById(componentId: String): CustomComponentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComponent(component: CustomComponentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComponents(components: List<CustomComponentEntity>)

    @Query("DELETE FROM custom_components WHERE id = :componentId")
    suspend fun deleteComponentById(componentId: String)
}
