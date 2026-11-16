package com.screenshotbrain.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class CollectionWithInfo(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val isFavourite: Boolean,
    val coverScreenshotId: Long?,
    val itemCount: Int,
    val coverUri: String?
)

@Dao
interface CollectionDao {

    @Query(
        "SELECT c.id AS id, c.name AS name, c.createdAt AS createdAt, " +
            "c.isFavourite AS isFavourite, c.coverScreenshotId AS coverScreenshotId, " +
            "(SELECT COUNT(*) FROM collection_screenshots cs WHERE cs.collectionId = c.id) AS itemCount, " +
            "COALESCE(" +
            "(SELECT s.uri FROM screenshots s INNER JOIN collection_screenshots x " +
            "ON x.screenshotId = s.id WHERE x.collectionId = c.id AND s.id = c.coverScreenshotId), " +
            "(SELECT s2.uri FROM collection_screenshots cs2 INNER JOIN screenshots s2 " +
            "ON s2.id = cs2.screenshotId WHERE cs2.collectionId = c.id " +
            "ORDER BY cs2.addedAt DESC LIMIT 1)" +
            ") AS coverUri " +
            "FROM collections c ORDER BY c.isFavourite DESC, c.createdAt DESC"
    )
    fun observeAllWithInfo(): Flow<List<CollectionWithInfo>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun observeById(id: Long): Flow<CollectionEntity?>

    @Insert
    suspend fun insert(collection: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE collections SET isFavourite = :favourite WHERE id = :id")
    suspend fun setFavourite(id: Long, favourite: Boolean)

    @Query("UPDATE collections SET coverScreenshotId = :screenshotId WHERE id = :id")
    suspend fun setCover(id: Long, screenshotId: Long?)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "SELECT s.* FROM screenshots s INNER JOIN collection_screenshots cs " +
            "ON cs.screenshotId = s.id WHERE cs.collectionId = :collectionId " +
            "ORDER BY cs.addedAt DESC"
    )
    fun observeScreenshots(collectionId: Long): Flow<List<ScreenshotEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLinks(links: List<CollectionScreenshotEntity>)

    @Query(
        "DELETE FROM collection_screenshots " +
            "WHERE collectionId = :collectionId AND screenshotId = :screenshotId"
    )
    suspend fun removeLink(collectionId: Long, screenshotId: Long)

    @Query(
        "UPDATE collections SET coverScreenshotId = NULL " +
            "WHERE id = :collectionId AND coverScreenshotId = :screenshotId"
    )
    suspend fun clearCoverIf(collectionId: Long, screenshotId: Long)

    @Query("SELECT collectionId FROM collection_screenshots WHERE screenshotId = :screenshotId")
    fun observeCollectionIdsFor(screenshotId: Long): Flow<List<Long>>
}
