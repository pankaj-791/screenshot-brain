package com.screenshotbrain.workers

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.ocr.OcrEngine
import com.screenshotbrain.ocr.ScreenshotAnalyzer
import kotlinx.coroutines.CancellationException

class OcrWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1L)
        if (id < 0) return Result.failure()

        val dao = AppDatabase.get(applicationContext).screenshotDao()
        val screenshot = dao.getById(id) ?: return Result.success()

        // Already processed: never do the same work twice.
        if (screenshot.ocrStatus != STATUS_PENDING) return Result.success()

        return try {
            val text = OcrEngine(applicationContext).recognize(Uri.parse(screenshot.uri))
            val analysis = ScreenshotAnalyzer.analyze(text)
            dao.saveOcrResult(
                id = id,
                text = text.ifEmpty { null },
                title = analysis.title,
                category = analysis.category,
                keywords = analysis.keywords,
                price = analysis.price,
                status = if (text.isEmpty()) STATUS_FAILED else STATUS_DONE
            )
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            dao.setOcrStatus(id, STATUS_FAILED)
            Result.success()
        }
    }

    companion object {
        const val KEY_ID = "screenshot_id"
        const val STATUS_PENDING = 0
        const val STATUS_DONE = 1
        const val STATUS_FAILED = 2
    }
}
