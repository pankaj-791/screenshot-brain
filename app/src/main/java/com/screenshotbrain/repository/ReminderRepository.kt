package com.screenshotbrain.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.screenshotbrain.database.ReminderDao
import com.screenshotbrain.database.ReminderEntity
import com.screenshotbrain.database.ReminderWithInfo
import com.screenshotbrain.workers.ReminderWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ReminderRepository(
    private val context: Context,
    private val dao: ReminderDao
) {

    fun observeAll(): Flow<List<ReminderWithInfo>> = dao.observeAllWithInfo()

    /** Returns the new reminder's id, or null if the title is blank. */
    suspend fun create(
        title: String,
        note: String?,
        screenshotId: Long?,
        remindAt: Long
    ): Long? = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim().take(100).trim()
        if (cleanTitle.isEmpty()) return@withContext null
        val id = dao.insert(
            ReminderEntity(
                title = cleanTitle,
                note = note?.trim()?.ifEmpty { null },
                screenshotId = screenshotId,
                remindAt = remindAt
            )
        )
        schedule(id, remindAt, replace = true)
        id
    }

    /** Returns false if the reminder no longer exists or the title is blank. */
    suspend fun update(
        id: Long,
        title: String,
        note: String?,
        screenshotId: Long?,
        remindAt: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = dao.getById(id) ?: return@withContext false
        val cleanTitle = title.trim().take(100).trim()
        if (cleanTitle.isEmpty()) return@withContext false
        dao.update(
            existing.copy(
                title = cleanTitle,
                note = note?.trim()?.ifEmpty { null },
                screenshotId = screenshotId,
                remindAt = remindAt,
                isDone = false
            )
        )
        schedule(id, remindAt, replace = true)
        true
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
        dao.deleteById(id)
    }

    suspend fun markDone(id: Long) = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
        dao.setDone(id, true)
    }

    /** Makes sure every upcoming reminder has a scheduled job. Safe to call often. */
    suspend fun rescheduleAll() = withContext(Dispatchers.IO) {
        for (reminder in dao.getNotDone()) {
            schedule(reminder.id, reminder.remindAt, replace = false)
        }
    }

    private fun schedule(id: Long, remindAt: Long, replace: Boolean) {
        val delay = (remindAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_ID to id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(id),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun workName(id: Long) = "reminder_$id"
}
