package com.screenshotbrain.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScreenshotEntity::class,
        CollectionEntity::class,
        CollectionScreenshotEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun screenshotDao(): ScreenshotDao

    abstract fun collectionDao(): CollectionDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE screenshots ADD COLUMN isFavourite INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("ALTER TABLE screenshots ADD COLUMN note TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `collections` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`isFavourite` INTEGER NOT NULL DEFAULT 0, " +
                        "`coverScreenshotId` INTEGER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `collection_screenshots` (" +
                        "`collectionId` INTEGER NOT NULL, " +
                        "`screenshotId` INTEGER NOT NULL, " +
                        "`addedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`collectionId`, `screenshotId`), " +
                        "FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`screenshotId`) REFERENCES `screenshots`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_collection_screenshots_screenshotId` " +
                        "ON `collection_screenshots` (`screenshotId`)"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
