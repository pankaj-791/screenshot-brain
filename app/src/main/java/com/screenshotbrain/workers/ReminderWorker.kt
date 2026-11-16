package com.screenshotbrain.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.notifications.NotificationHelper

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1L)
        if (id < 0) return Result.failure()

        val dao = AppDatabase.get(applicationContext).reminderDao()
        val reminder = dao.getById(id) ?: return Result.success()

        // Already fired: never notify twice.
        if (reminder.isDone) return Result.success()

        NotificationHelper.showReminder(
            context = applicationContext,
            reminderId = id,
            title = reminder.title,
            text = reminder.note ?: "Tap to open Screenshot Brain"
        )
        dao.setDone(id, true)
        return Result.success()
    }

    companion object {
        const val KEY_ID = "reminder_id"
    }
}
