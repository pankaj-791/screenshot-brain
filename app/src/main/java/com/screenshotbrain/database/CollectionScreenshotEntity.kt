package com.screenshotbrain.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "collection_screenshots",
    primaryKeys = ["collectionId", "screenshotId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["screenshotId"])]
)
data class CollectionScreenshotEntity(
    val collectionId: Long,
    val screenshotId: Long,
    val addedAt: Long
)
