package com.example.wellnessguide.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SymptomEntity::class], version = 1)
abstract class WellnessDatabase : RoomDatabase() {
    abstract fun symptomDao(): SymptomDao

    companion object {
        @Volatile private var INSTANCE: WellnessDatabase? = null

        fun getDatabase(context: Context): WellnessDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    WellnessDatabase::class.java,
                    "wellness_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}