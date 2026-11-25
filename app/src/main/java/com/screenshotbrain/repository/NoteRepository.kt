package com.screenshotbrain.repository

import com.screenshotbrain.database.ScreenshotDao
import com.screenshotbrain.database.ScreenshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NoteRepository(private val dao: ScreenshotDao) {

    fun observe(id: Long): Flow<ScreenshotEntity?> = dao.observeById(id)

    /** An empty note is stored as "no note". */
    suspend fun saveNote(id: Long, note: String?) = withContext(Dispatchers.IO) {
        dao.updateNote(id, note?.trim()?.take(MAX_LENGTH)?.ifEmpty { null })
    }

    companion object {
        const val MAX_LENGTH = 20_000
    }
}
