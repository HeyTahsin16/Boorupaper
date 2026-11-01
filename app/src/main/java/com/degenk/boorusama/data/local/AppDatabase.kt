package com.degenk.boorusama.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "booru_configs")
data class BooruConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val booruId: Int,
    val booruIdHint: Int,
    val url: String,
    val name: String,
    val login: String = "",
    val apiKey: String = "",
    val passHash: String = "",
    val cookie: String = "",
    val ratingFilter: String = "NONE",
    val granularRatingFiltersJson: String = "[]",
    val deletedItemBehavior: String = "SHOW",
    val bannedPostVisibility: String = "SHOW",
    val customDownloadFileNameFormat: String = "{id}_{md5:maxlength=8}.{ext}",
    val customBulkDownloadFileNameFormat: String = "{search}/{id}_{md5:maxlength=8}.{ext}",
    val imageDetailsQuality: String = "sample",
    val blacklistedTagsJson: String = "[]",
    val alwaysIncludeTags: String = "",
    val proxySettingsJson: String = "{}",
    val customHeadersJson: String = "{}",
    val postsPerPageOverride: Int? = null
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val booruId: Int,
    val postId: Int,
    val sourceUrl: String,
    val thumbnailUrl: String,
    val sampleUrl: String,
    val originalUrl: String,
    val width: Int,
    val height: Int,
    val md5: String,
    val tagsJson: String,
    val rating: String = "GENERAL",
    val format: String = "jpg",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blacklisted_tags")
data class BlacklistedTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val isActive: Boolean = true,
    val booruConfigId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_tags")
data class FavoriteTagEntity(
    @PrimaryKey
    val name: String,
    val labelsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey
    val query: String,
    val createdAt: Long = System.currentTimeMillis(),
    val searchCount: Int = 1,
    val booruTypeName: String = "DANBOORU"
)

@Entity(tableName = "bulk_download_tasks")
data class BulkDownloadTaskEntity(
    @PrimaryKey
    val id: String,
    val tags: String,
    val siteUrl: String,
    val status: String,
    val totalItems: Int,
    val downloadedItems: Int,
    val failedItems: Int,
    val quality: String,
    val skipDuplicates: Boolean,
    val excludeBlacklisted: Boolean,
    val createdAt: Long
)

@Entity(tableName = "download_records")
data class DownloadRecordEntity(
    @PrimaryKey
    val id: String,
    val postId: Int,
    val fileName: String,
    val url: String,
    val thumbnailUrl: String,
    val siteUrl: String,
    val status: String,
    val progress: Float,
    val fileSize: Long,
    val savedPath: String,
    val errorMessage: String?,
    val createdAt: Long
)

@Dao
interface BooruConfigDao {
    @Query("SELECT * FROM booru_configs ORDER BY id ASC")
    fun observeAll(): Flow<List<BooruConfigEntity>>

    @Query("SELECT * FROM booru_configs ORDER BY id ASC")
    suspend fun getAll(): List<BooruConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BooruConfigEntity): Long

    @Update
    suspend fun update(entity: BooruConfigEntity)

    @Query("DELETE FROM booru_configs WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM booru_configs")
    suspend fun deleteAll()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAll(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM bookmarks WHERE postId = :postId AND sourceUrl = :sourceUrl")
    suspend fun deleteByPost(postId: Int, sourceUrl: String)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}

@Dao
interface BlacklistedTagDao {
    @Query("SELECT * FROM blacklisted_tags ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BlacklistedTagEntity>>

    @Query("SELECT * FROM blacklisted_tags ORDER BY createdAt DESC")
    suspend fun getAll(): List<BlacklistedTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlacklistedTagEntity): Long

    @Update
    suspend fun update(entity: BlacklistedTagEntity)

    @Query("DELETE FROM blacklisted_tags WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM blacklisted_tags")
    suspend fun deleteAll()
}

@Dao
interface FavoriteTagDao {
    @Query("SELECT * FROM favorite_tags ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<FavoriteTagEntity>>

    @Query("SELECT * FROM favorite_tags ORDER BY updatedAt DESC")
    suspend fun getAll(): List<FavoriteTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteTagEntity)

    @Query("DELETE FROM favorite_tags WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("DELETE FROM favorite_tags")
    suspend fun deleteAll()
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY createdAt DESC LIMIT 100")
    fun observeAll(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history ORDER BY createdAt DESC")
    suspend fun getAll(): List<SearchHistoryEntity>

    @Query("SELECT * FROM search_history WHERE query = :query LIMIT 1")
    suspend fun findByQuery(query: String): SearchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteByQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}

@Dao
interface BulkDownloadDao {
    @Query("SELECT * FROM bulk_download_tasks ORDER BY createdAt DESC")
    fun observeTasks(): Flow<List<BulkDownloadTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(entity: BulkDownloadTaskEntity)

    @Query("DELETE FROM bulk_download_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("SELECT * FROM download_records ORDER BY createdAt DESC")
    fun observeRecords(): Flow<List<DownloadRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(entity: DownloadRecordEntity)

    @Query("DELETE FROM download_records WHERE id = :id")
    suspend fun deleteRecord(id: String)

    @Query("DELETE FROM download_records")
    suspend fun clearRecords()
}

@Database(
    entities = [
        BooruConfigEntity::class,
        BookmarkEntity::class,
        BlacklistedTagEntity::class,
        FavoriteTagEntity::class,
        SearchHistoryEntity::class,
        BulkDownloadTaskEntity::class,
        DownloadRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun booruConfigDao(): BooruConfigDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun blacklistedTagDao(): BlacklistedTagDao
    abstract fun favoriteTagDao(): FavoriteTagDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun bulkDownloadDao(): BulkDownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "boorusama_database.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}
