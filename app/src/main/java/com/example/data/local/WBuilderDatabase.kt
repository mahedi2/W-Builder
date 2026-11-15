package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.converter.WBuilderTypeConverters
import com.example.data.local.dao.CustomComponentDao
import com.example.data.local.dao.WidgetLayerDao
import com.example.data.local.dao.WidgetProjectDao
import com.example.data.local.entity.AppWidgetBindingEntity
import com.example.data.local.entity.CustomComponentEntity
import com.example.data.local.entity.WidgetLayerEntity
import com.example.data.local.entity.WidgetProjectEntity

@Database(
    entities = [
        WidgetProjectEntity::class,
        WidgetLayerEntity::class,
        CustomComponentEntity::class,
        AppWidgetBindingEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(WBuilderTypeConverters::class)
abstract class WBuilderDatabase : RoomDatabase() {

    abstract fun widgetProjectDao(): WidgetProjectDao
    abstract fun widgetLayerDao(): WidgetLayerDao
    abstract fun customComponentDao(): CustomComponentDao

    companion object {
        private const val DATABASE_NAME = "w_builder.db"

        @Volatile
        private var INSTANCE: WBuilderDatabase? = null

        fun getInstance(context: Context): WBuilderDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WBuilderDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
