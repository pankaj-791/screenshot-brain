package com.screenshotbrain.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.screenshotbrain.database.AppDatabase
import com.screenshotbrain.domain.CleanupFinder
import com.screenshotbrain.domain.CleanupSuggestions
import com.screenshotbrain.workers.AnalysisWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CleanupRepository(
    private val context: Context,
    database: AppDatabase
) {
    private val dao = database.cleanupDao()
    private val screenshots = ScreenshotRepository(context, database.screenshotDao())

    @OptIn(FlowPreview::class)
    fun observeSuggestions(): Flow<CleanupSuggestions> =
        dao.observeCandidates()
            .debounce(400)
            .map { CleanupFinder.find(it) }
            .flowOn(Dispatchers.Default)

    fun observeUnanalyzedCount(): Flow<Int> = dao.observeUnanalyzedCount()

    /** Queues a background analysis for every screenshot that has not been analyzed yet. */
    suspend fun startAnalysis() = withContext(Dispatchers.IO) {
        for (id in dao.getUnanalyzedIds()) enqueue(id)
    }

    /** Tries again on screenshots whose image could not be opened. */
    suspend fun retry(ids: Collection<Long>) = withContext(Dispatchers.IO) {
        for (id in ids) {
            dao.setCleanupStatus(id, 0)
            enqueue(id)
        }
    }

    /** Removes the screenshots from the app. Original photos in the gallery are not touched. */
    suspend fun removeFromApp(ids: Collection<Long>) = withContext(Dispatchers.IO) {
        for (id in ids) {
            WorkManager.getInstance(context).cancelUniqueWork("analysis_$id")
            screenshots.delete(id)
        }
    }

    private fun enqueue(id: Long) {
        val request = OneTimeWorkRequestBuilder<AnalysisWorker>()
            .setInputData(workDataOf(AnalysisWorker.KEY_ID to id))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("analysis_$id", ExistingWorkPolicy.KEEP, request)
    }
}
