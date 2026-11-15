package com.screenshotbrain.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "screenshots",
    indices = [Index(value = ["uri"], unique = true)]
)
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val fileName: String,
    val importedAt: Long,
    val takenAt: Long?
)
