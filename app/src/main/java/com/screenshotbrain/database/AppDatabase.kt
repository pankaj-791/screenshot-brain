package com.screenshotbrain.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScreenshotEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun screenshotDao(): ScreenshotDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE screenshots ADD COLUMN ocrText TEXT")
                db.execSQL("ALTER TABLE screenshots ADD COLUMN title TEXT")
                db.execSQL("ALTER TABLE screenshots ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE screenshots ADD COLUMN keywords TEXT")
                db.execSQL("ALTER TABLE screenshots ADD COLUMN price TEXT")
                db.execSQL(
                    "ALTER TABLE screenshots ADD COLUMN ocrStatus INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screenshot_brain.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
