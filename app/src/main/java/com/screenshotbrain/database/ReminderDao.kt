package com.screenshotbrain.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ReminderWithInfo(
    val id: Long,
    val title: String,
    val note: String?,
    val screenshotId: Long?,
    val remindAt: Long,
    val isDone: Boolean,
    val screenshotUri: String?,
    val screenshotTitle: String?
)

@Dao
interface ReminderDao {

    @Query(
        "SELECT r.id AS id, r.title AS title, r.note AS note, " +
            "r.screenshotId AS screenshotId, r.remindAt AS remindAt, r.isDone AS isDone, " +
            "s.uri AS screenshotUri, s.title AS screenshotTitle " +
            "FROM reminders r LEFT JOIN screenshots s ON s.id = r.screenshotId " +
            "ORDER BY r.remindAt ASC"
    )
    fun observeAllWithInfo(): Flow<List<ReminderWithInfo>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isDone = 0")
    suspend fun getNotDone(): List<ReminderEntity>

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
