package com.screenshotbrain.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(screenshot: ScreenshotEntity): Long

    @Query("SELECT * FROM screenshots ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<ScreenshotEntity>>

    @Query("SELECT COUNT(*) FROM screenshots")
    suspend fun count(): Int

    @Query("SELECT * FROM screenshots WHERE id = :id")
    suspend fun getById(id: Long): ScreenshotEntity?

    @Query("SELECT id FROM screenshots WHERE ocrStatus = 0")
    suspend fun getPendingIds(): List<Long>

    @Query(
        "UPDATE screenshots SET ocrText = :text, title = :title, category = :category, " +
            "keywords = :keywords, price = :price, ocrStatus = :status WHERE id = :id"
    )
    suspend fun saveOcrResult(
        id: Long,
        text: String?,
        title: String?,
        category: String?,
        keywords: String?,
        price: String?,
        status: Int
    )

    @Query("UPDATE screenshots SET ocrStatus = :status WHERE id = :id")
    suspend fun setOcrStatus(id: Long, status: Int)
}
