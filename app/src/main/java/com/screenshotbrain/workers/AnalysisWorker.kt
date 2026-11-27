package com.screenshotbrain.workers

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.utils.ImageAnalyzer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnalysisWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1L)
        if (id < 0) return Result.failure()

        val database = AppDatabase.get(applicationContext)
        val screenshot = database.screenshotDao().getById(id) ?: return Result.success()

        // Already analyzed: never do the same work twice.
        if (screenshot.cleanupStatus != STATUS_PENDING) return Result.success()

        val dao = database.cleanupDao()
        return try {
            val analysis = withContext(Dispatchers.IO) {
                ImageAnalyzer.analyze(applicationContext, Uri.parse(screenshot.uri))
            }
            if (analysis == null) {
                dao.setCleanupStatus(id, STATUS_UNAVAILABLE)
            } else {
                dao.saveAnalysis(
                    id = id,
                    contentHash = analysis.contentHash,
                    visualHash = analysis.visualHash,
                    sharpness = analysis.sharpness,
                    status = STATUS_DONE
                )
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            dao.setCleanupStatus(id, STATUS_UNAVAILABLE)
            Result.success()
        } catch (e: OutOfMemoryError) {
            dao.setCleanupStatus(id, STATUS_UNAVAILABLE)
            Result.success()
        }
    }

    companion object {
        const val KEY_ID = "analysis_screenshot_id"
        const val STATUS_PENDING = 0
        const val STATUS_DONE = 1
        const val STATUS_UNAVAILABLE = 2
    }
}
