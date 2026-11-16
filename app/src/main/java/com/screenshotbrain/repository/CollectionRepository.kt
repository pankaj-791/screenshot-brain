package com.screenshotbrain.repository

import com.screenshotbrain.database.CollectionDao
import com.screenshotbrain.database.CollectionEntity
import com.screenshotbrain.database.CollectionScreenshotEntity
import com.screenshotbrain.database.CollectionWithInfo
import com.screenshotbrain.database.ScreenshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CollectionRepository(private val dao: CollectionDao) {

    fun observeAll(): Flow<List<CollectionWithInfo>> = dao.observeAllWithInfo()

    fun observeById(id: Long): Flow<CollectionEntity?> = dao.observeById(id)

    fun observeScreenshots(collectionId: Long): Flow<List<ScreenshotEntity>> =
        dao.observeScreenshots(collectionId)

    fun observeCollectionIdsFor(screenshotId: Long): Flow<List<Long>> =
        dao.observeCollectionIdsFor(screenshotId)

    /** Returns the new collection's id, or null if the name is blank. */
    suspend fun create(name: String): Long? = withContext(Dispatchers.IO) {
        val clean = cleanName(name) ?: return@withContext null
        dao.insert(CollectionEntity(name = clean, createdAt = System.currentTimeMillis()))
    }

    /** Returns false if the new name is blank. */
    suspend fun rename(id: Long, name: String): Boolean = withContext(Dispatchers.IO) {
        val clean = cleanName(name) ?: return@withContext false
        dao.rename(id, clean)
        true
    }

    suspend fun setFavourite(id: Long, favourite: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavourite(id, favourite)
    }

    /** Deletes only the collection. The screenshots inside it are not touched. */
    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun addScreenshots(collectionId: Long, screenshotIds: Collection<Long>) =
        withContext(Dispatchers.IO) {
            if (screenshotIds.isNotEmpty()) {
                val now = System.currentTimeMillis()
                dao.addLinks(
                    screenshotIds.map { CollectionScreenshotEntity(collectionId, it, now) }
                )
            }
        }

    suspend fun removeScreenshot(collectionId: Long, screenshotId: Long) =
        withContext(Dispatchers.IO) {
            dao.removeLink(collectionId, screenshotId)
            dao.clearCoverIf(collectionId, screenshotId)
        }

    suspend fun setCover(collectionId: Long, screenshotId: Long?) =
        withContext(Dispatchers.IO) {
            dao.setCover(collectionId, screenshotId)
        }

    private fun cleanName(name: String): String? =
        name.trim().take(60).trim().ifEmpty { null }
}
