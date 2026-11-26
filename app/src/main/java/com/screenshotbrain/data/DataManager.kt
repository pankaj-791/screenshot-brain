package com.screenshotbrain.data

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.screenshotbrain.database.AppDatabase
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class StorageStats(
    val screenshots: Int,
    val collections: Int,
    val reminders: Int,
    val databaseBytes: Long
)

class DataManager(context: Context) {

    private val appContext = context.applicationContext
    private val database = AppDatabase.get(appContext)

    suspend fun stats(): StorageStats = withContext(Dispatchers.IO) {
        StorageStats(
            screenshots = database.screenshotDao().count(),
            collections = database.collectionDao().observeAllWithInfo().first().size,
            reminders = database.reminderDao().observeAllWithInfo().first().size,
            databaseBytes = databaseSize()
        )
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val screenshots = database.screenshotDao().observeAll().first()
        val collections = database.collectionDao().observeAllWithInfo().first()
        val reminders = database.reminderDao().observeAllWithInfo().first()

        val shotsJson = JSONArray()
        for (s in screenshots) {
            shotsJson.put(
                JSONObject()
                    .put("id", s.id)
                    .put("fileName", s.fileName)
                    .putOrNull("title", s.title)
                    .putOrNull("category", s.category)
                    .putOrNull("price", s.price)
                    .putOrNull("keywords", s.keywords)
                    .putOrNull("note", s.note)
                    .putOrNull("extractedText", s.ocrText)
                    .put("favourite", s.isFavourite)
                    .putOrNull("takenAt", s.takenAt)
                    .put("importedAt", s.importedAt)
                    .put("imageReference", s.uri)
            )
        }

        val collectionsJson = JSONArray()
        for (c in collections) {
            val ids = JSONArray()
            database.collectionDao().observeScreenshots(c.id).first().forEach { ids.put(it.id) }
            collectionsJson.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("favourite", c.isFavourite)
                    .put("createdAt", c.createdAt)
                    .put("screenshotIds", ids)
            )
        }

        val remindersJson = JSONArray()
        for (r in reminders) {
            remindersJson.put(
                JSONObject()
                    .put("id", r.id)
                    .put("title", r.title)
                    .putOrNull("note", r.note)
                    .putOrNull("screenshotId", r.screenshotId)
                    .put("remindAt", r.remindAt)
                    .put("done", r.isDone)
            )
        }

        JSONObject()
            .put("app", "Screenshot Brain")
            .put("exportedAt", System.currentTimeMillis())
            .put("note", "Image files are not included, only references and extracted information.")
            .put("screenshots", shotsJson)
            .put("collections", collectionsJson)
            .put("reminders", remindersJson)
            .toString(2)
    }

    /** Removes everything the app stores. Original photos in the gallery are never touched. */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        WorkManager.getInstance(appContext).cancelAllWork()
        database.clearAllTables()
        SettingsStore(appContext).reset()
        NotificationManagerCompat.from(appContext).cancelAll()
        for (permission in appContext.contentResolver.persistedUriPermissions.toList()) {
            try {
                appContext.contentResolver.releasePersistableUriPermission(
                    permission.uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Already released. Fine.
            }
        }
    }

    fun versionName(): String =
        try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
                ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }

    private fun databaseSize(): Long {
        val base = appContext.getDatabasePath(DATABASE_NAME).path
        return listOf(base, "$base-wal", "$base-shm").sumOf { path ->
            File(path).let { if (it.exists()) it.length() else 0L }
        }
    }

    private fun JSONObject.putOrNull(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private companion object {
        const val DATABASE_NAME = "screenshot_brain.db"
    }
}
