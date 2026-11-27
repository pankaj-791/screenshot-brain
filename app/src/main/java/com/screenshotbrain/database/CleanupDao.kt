package com.screenshotbrain.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class CleanupCandidate(
    val id: Long,
    val uri: String,
    val title: String?,
    val fileName: String,
    val importedAt: Long,
    val takenAt: Long?,
    val isFavourite: Boolean,
    val note: String?,
    val ocrText: String?,
    val contentHash: String?,
    val visualHash: Long?,
    val sharpness: Double?,
    val cleanupStatus: Int,
    val collectionCount: Int,
    val reminderCount: Int
)

@Dao
interface CleanupDao {

    @Query(
        "SELECT s.id AS id, s.uri AS uri, s.title AS title, s.fileName AS fileName, " +
            "s.importedAt AS importedAt, s.takenAt AS takenAt, s.isFavourite AS isFavourite, " +
            "s.note AS note, SUBSTR(s.ocrText, 1, 1500) AS ocrText, " +
            "s.contentHash AS contentHash, s.visualHash AS visualHash, " +
            "s.sharpness AS sharpness, s.cleanupStatus AS cleanupStatus, " +
            "(SELECT COUNT(*) FROM collection_screenshots cs WHERE cs.screenshotId = s.id) " +
            "AS collectionCount, " +
            "(SELECT COUNT(*) FROM reminders r WHERE r.screenshotId = s.id) AS reminderCount " +
            "FROM screenshots s ORDER BY s.importedAt DESC"
    )
    fun observeCandidates(): Flow<List<CleanupCandidate>>

    @Query("SELECT COUNT(*) FROM screenshots WHERE cleanupStatus = 0")
    fun observeUnanalyzedCount(): Flow<Int>

    @Query("SELECT id FROM screenshots WHERE cleanupStatus = 0")
    suspend fun getUnanalyzedIds(): List<Long>

    @Query(
        "UPDATE screenshots SET contentHash = :contentHash, visualHash = :visualHash, " +
            "sharpness = :sharpness, cleanupStatus = :status WHERE id = :id"
    )
    suspend fun saveAnalysis(
        id: Long,
        contentHash: String?,
        visualHash: Long?,
        sharpness: Double?,
        status: Int
    )

    @Query("UPDATE screenshots SET cleanupStatus = :status WHERE id = :id")
    suspend fun setCleanupStatus(id: Long, status: Int)
}
