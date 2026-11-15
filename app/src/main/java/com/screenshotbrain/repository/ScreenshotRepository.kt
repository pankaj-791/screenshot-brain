package com.screenshotbrain.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.screenshotbrain.database.ScreenshotDao
import com.screenshotbrain.database.ScreenshotEntity
import com.screenshotbrain.workers.OcrWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ScreenshotRepository(
    private val context: Context,
    private val dao: ScreenshotDao
) {

    fun observeAll(): Flow<List<ScreenshotEntity>> = dao.observeAll()

    /** Imports the picked images. Returns how many were new (duplicates are skipped). */
    suspend fun importUris(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var added = 0
        for (uri in uris) {
            tryKeepAccess(uri)
            val entity = ScreenshotEntity(
                uri = uri.toString(),
                fileName = readString(uri, OpenableColumns.DISPLAY_NAME) ?: "Screenshot",
                importedAt = System.currentTimeMillis(),
                takenAt = readString(uri, MediaStore.Images.ImageColumns.DATE_TAKEN)?.toLongOrNull()
            )
            val rowId = dao.insert(entity)
            if (rowId != -1L) {
                added++
                enqueueOcr(rowId)
            }
        }
        added
    }

    /** Queues OCR for every screenshot that is still waiting (e.g. imported before OCR existed). */
    suspend fun processPending() = withContext(Dispatchers.IO) {
        for (id in dao.getPendingIds()) {
            enqueueOcr(id)
        }
    }

    private fun enqueueOcr(id: Long) {
        val request = OneTimeWorkRequestBuilder<OcrWorker>()
            .setInputData(workDataOf(OcrWorker.KEY_ID to id))
            .build()
        // KEEP: if a job for this screenshot already exists, do not queue another.
        WorkManager.getInstance(context)
            .enqueueUniqueWork("ocr_$id", ExistingWorkPolicy.KEEP, request)
    }

    private fun tryKeepAccess(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Android may refuse for picker images. Import still works for now.
        }
    }

    private fun readString(uri: Uri, column: String): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(column)
                if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                    cursor.getString(index)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
