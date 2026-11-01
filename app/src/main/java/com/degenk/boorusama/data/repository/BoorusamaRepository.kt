package com.degenk.boorusama.data.repository

import android.content.Context
import android.os.Environment
import com.degenk.boorusama.NativeAndroidBridge
import com.degenk.boorusama.data.local.AppDatabase
import com.degenk.boorusama.data.local.BlacklistedTagEntity
import com.degenk.boorusama.data.local.BookmarkEntity
import com.degenk.boorusama.data.local.BooruConfigEntity
import com.degenk.boorusama.data.local.BulkDownloadTaskEntity
import com.degenk.boorusama.data.local.DownloadRecordEntity
import com.degenk.boorusama.data.local.FavoriteTagEntity
import com.degenk.boorusama.data.local.SearchHistoryEntity
import com.degenk.boorusama.data.remote.BooruApiClient
import com.degenk.boorusama.model.AppSettings
import com.degenk.boorusama.model.BackupPayload
import com.degenk.boorusama.model.BlacklistedTagItem
import com.degenk.boorusama.model.BookmarkItem
import com.degenk.boorusama.model.BooruConfig
import com.degenk.boorusama.model.BooruConfigBannedPostVisibility
import com.degenk.boorusama.model.BooruConfigDeletedItemBehavior
import com.degenk.boorusama.model.BooruConfigRatingFilter
import com.degenk.boorusama.model.BooruType
import com.degenk.boorusama.model.BulkDownloadStatus
import com.degenk.boorusama.model.BulkDownloadTask
import com.degenk.boorusama.model.DownloadStatus
import com.degenk.boorusama.model.DownloadTaskItem
import com.degenk.boorusama.model.FavoriteTagItem
import com.degenk.boorusama.model.ImageQuality
import com.degenk.boorusama.model.Post
import com.degenk.boorusama.model.ProxySettings
import com.degenk.boorusama.model.Rating
import com.degenk.boorusama.model.SearchHistoryItem
import com.degenk.boorusama.util.FilenameGenerator
import com.degenk.boorusama.util.TagExpressionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class BoorusamaRepository(
    private val context: Context,
    private val database: AppDatabase,
    val apiClient: BooruApiClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val prefs = context.getSharedPreferences("boorusama_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _activeConfigId = MutableStateFlow(prefs.getInt("active_config_id", 1))
    val activeConfigId: StateFlow<Int> = _activeConfigId.asStateFlow()

    val configsFlow: Flow<List<BooruConfig>> = database.booruConfigDao()
        .observeAll()
        .map { list -> list.map { it.toDomain() } }

    val bookmarksFlow: Flow<List<BookmarkItem>> = database.bookmarkDao()
        .observeAll()
        .map { list -> list.map { it.toDomain() } }

    val blacklistedTagsFlow: Flow<List<BlacklistedTagItem>> = database.blacklistedTagDao()
        .observeAll()
        .map { list -> list.map { it.toDomain() } }

    val favoriteTagsFlow: Flow<List<FavoriteTagItem>> = database.favoriteTagDao()
        .observeAll()
        .map { list -> list.map { it.toDomain() } }

    val searchHistoryFlow: Flow<List<SearchHistoryItem>> = database.searchHistoryDao()
        .observeAll()
        .map { list -> list.map { it.toDomain() } }

    val bulkTasksFlow: Flow<List<BulkDownloadTask>> = database.bulkDownloadDao()
        .observeTasks()
        .map { list -> list.map { it.toDomain() } }

    val downloadRecordsFlow: Flow<List<DownloadTaskItem>> = database.bulkDownloadDao()
        .observeRecords()
        .map { list -> list.map { it.toDomain() } }

    suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        // No preset/default profiles are seeded; the user adds their own profiles from the start screen.
    }

    private fun loadSettings(): AppSettings {
        val raw = prefs.getString("app_settings_json", null) ?: return AppSettings()
        return try {
            json.decodeFromString<AppSettings>(raw)
        } catch (_: Exception) {
            AppSettings()
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        prefs.edit().putString("app_settings_json", json.encodeToString(updated)).apply()
    }

    fun setActiveConfigId(id: Int) {
        _activeConfigId.value = id
        prefs.edit().putInt("active_config_id", id).apply()
    }

    suspend fun saveBooruConfig(config: BooruConfig): Int = withContext(Dispatchers.IO) {
        val entity = config.toEntity()
        if (config.id == 0) {
            database.booruConfigDao().insert(entity).toInt()
        } else {
            database.booruConfigDao().insert(entity)
            config.id
        }
    }

    suspend fun deleteBooruConfig(id: Int) = withContext(Dispatchers.IO) {
        database.booruConfigDao().deleteById(id)
        val remaining = database.booruConfigDao().getAll()
        if (remaining.isNotEmpty() && _activeConfigId.value == id) {
            setActiveConfigId(remaining.first().id)
        }
    }

    suspend fun toggleBookmark(post: Post, config: BooruConfig): Boolean = withContext(Dispatchers.IO) {
        val all = database.bookmarkDao().getAll()
        val existing = all.firstOrNull { it.postId == post.id && it.sourceUrl == post.booruUrl }
        if (existing != null) {
            database.bookmarkDao().deleteById(existing.id)
            false
        } else {
            database.bookmarkDao().insert(
                BookmarkEntity(
                    booruId = config.booruIdHint,
                    postId = post.id,
                    sourceUrl = post.booruUrl,
                    thumbnailUrl = post.thumbnailImageUrl,
                    sampleUrl = post.sampleImageUrl,
                    originalUrl = post.originalImageUrl,
                    width = post.width,
                    height = post.height,
                    md5 = post.md5,
                    tagsJson = json.encodeToString(post.tags),
                    rating = post.rating.name,
                    format = post.format
                )
            )
            true
        }
    }

    suspend fun removeBookmark(id: Int) = withContext(Dispatchers.IO) {
        database.bookmarkDao().deleteById(id)
    }

    suspend fun addBlacklistedTag(name: String, booruConfigId: Int? = null) = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@withContext
        database.blacklistedTagDao().insert(
            BlacklistedTagEntity(name = trimmed, isActive = true, booruConfigId = booruConfigId)
        )
    }

    suspend fun toggleBlacklistedTag(item: BlacklistedTagItem) = withContext(Dispatchers.IO) {
        database.blacklistedTagDao().update(
            item.copy(isActive = !item.isActive).toEntity()
        )
    }

    suspend fun deleteBlacklistedTag(id: Int) = withContext(Dispatchers.IO) {
        database.blacklistedTagDao().deleteById(id)
    }

    suspend fun saveFavoriteTag(name: String, labels: List<String> = emptyList()) = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@withContext
        database.favoriteTagDao().insert(
            FavoriteTagEntity(
                name = trimmed,
                labelsJson = json.encodeToString(labels),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteFavoriteTag(name: String) = withContext(Dispatchers.IO) {
        database.favoriteTagDao().deleteByName(name)
    }

    suspend fun recordSearchHistory(query: String, booruType: BooruType) = withContext(Dispatchers.IO) {
        if (_settings.value.enableIncognitoSearchHistory) return@withContext
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext
        val existing = database.searchHistoryDao().findByQuery(trimmed)
        database.searchHistoryDao().insert(
            SearchHistoryEntity(
                query = trimmed,
                createdAt = System.currentTimeMillis(),
                searchCount = (existing?.searchCount ?: 0) + 1,
                booruTypeName = booruType.name
            )
        )
    }

    suspend fun deleteSearchHistory(query: String) = withContext(Dispatchers.IO) {
        database.searchHistoryDao().deleteByQuery(query)
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        database.searchHistoryDao().clearAll()
    }

    suspend fun downloadPost(
        post: Post,
        config: BooruConfig,
        searchQuery: String = ""
    ): DownloadTaskItem = withContext(Dispatchers.IO) {
        val taskId = "${post.id}_${System.currentTimeMillis()}"
        val fileName = FilenameGenerator.generate(
            format = config.customDownloadFileNameFormat,
            post = post,
            searchQuery = searchQuery
        )
        val downloadUrl = when (_settings.value.downloadQuality) {
            ImageQuality.LOW -> post.thumbnailImageUrl
            ImageQuality.MEDIUM -> post.sampleImageUrl
            ImageQuality.ORIGINAL -> post.originalImageUrl
        }.ifBlank { post.sampleImageUrl.ifBlank { post.thumbnailImageUrl } }

        val initialRecord = DownloadTaskItem(
            id = taskId,
            postId = post.id,
            fileName = fileName,
            url = downloadUrl,
            thumbnailUrl = post.thumbnailImageUrl,
            siteUrl = config.url,
            status = DownloadStatus.DOWNLOADING,
            progress = 0.1f
        )
        database.bulkDownloadDao().upsertRecord(initialRecord.toEntity())

        try {
            val baseDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.getExternalFilesDir(null), "Downloads")
            val siteSubfolder = if (_settings.value.downloadSubfolderBySite) {
                config.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            } else ""
            val targetDir = if (siteSubfolder.isNotBlank()) {
                File(baseDownloadsDir, "Boorupaper/$siteSubfolder")
            } else {
                File(baseDownloadsDir, "Boorupaper")
            }
            val destFile = File(targetDir, fileName)
            destFile.parentFile?.mkdirs()

            if (_settings.value.skipExistingDownloads && destFile.exists() && destFile.length() > 0) {
                val skipped = initialRecord.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 1f,
                    fileSize = destFile.length(),
                    savedPath = destFile.absolutePath
                )
                database.bulkDownloadDao().upsertRecord(skipped.toEntity())
                return@withContext skipped
            }

            val req = apiClient.buildDownloadRequest(config, downloadUrl)
            apiClient.getOkHttpClient(config).newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalBytes = body.contentLength()
                var bytesCopied = 0L
                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesCopied += read
                        }
                        output.flush()
                    }
                }
                NativeAndroidBridge.refreshMediaGallery(context, destFile.absolutePath)
                val completed = initialRecord.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 1f,
                    fileSize = if (totalBytes > 0) totalBytes else bytesCopied,
                    savedPath = destFile.absolutePath
                )
                database.bulkDownloadDao().upsertRecord(completed.toEntity())
                completed
            }
        } catch (e: Exception) {
            val failed = initialRecord.copy(
                status = DownloadStatus.FAILED,
                progress = 0f,
                errorMessage = e.message ?: "Download failed"
            )
            database.bulkDownloadDao().upsertRecord(failed.toEntity())
            failed
        }
    }

    suspend fun runBulkDownloadTask(
        tags: String,
        config: BooruConfig,
        maxPages: Int = 2,
        excludeBlacklisted: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val taskId = UUID.randomUUID().toString()
        val initialTask = BulkDownloadTask(
            id = taskId,
            tags = tags,
            siteUrl = config.url,
            status = BulkDownloadStatus.RUNNING
        )
        database.bulkDownloadDao().upsertTask(initialTask.toEntity())

        try {
            val blacklist = database.blacklistedTagDao().getAll().filter { it.isActive }.map { it.name }
            val allPosts = mutableListOf<Post>()
            for (page in 1..maxPages) {
                val fetched = apiClient.fetchPosts(config, tags, page = page, limit = 25)
                if (fetched.isEmpty()) break
                val visible = if (excludeBlacklisted) {
                    TagExpressionEngine.filterPosts(fetched, config, blacklist).visiblePosts
                } else {
                    fetched
                }
                allPosts.addAll(visible)
            }

            var downloaded = 0
            var failed = 0
            database.bulkDownloadDao().upsertTask(
                initialTask.copy(totalItems = allPosts.size).toEntity()
            )

            for (post in allPosts) {
                val res = downloadPost(post, config, searchQuery = tags)
                if (res.status == DownloadStatus.COMPLETED) downloaded++ else failed++
                database.bulkDownloadDao().upsertTask(
                    initialTask.copy(
                        totalItems = allPosts.size,
                        downloadedItems = downloaded,
                        failedItems = failed,
                        status = BulkDownloadStatus.RUNNING
                    ).toEntity()
                )
            }

            database.bulkDownloadDao().upsertTask(
                initialTask.copy(
                    totalItems = allPosts.size,
                    downloadedItems = downloaded,
                    failedItems = failed,
                    status = BulkDownloadStatus.COMPLETED
                ).toEntity()
            )
        } catch (_: Exception) {
            database.bulkDownloadDao().upsertTask(
                initialTask.copy(status = BulkDownloadStatus.FAILED).toEntity()
            )
        }
    }

    suspend fun deleteBulkTask(id: String) = withContext(Dispatchers.IO) {
        database.bulkDownloadDao().deleteTask(id)
    }

    suspend fun clearDownloadRecords() = withContext(Dispatchers.IO) {
        database.bulkDownloadDao().clearRecords()
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val payload = BackupPayload(
            configs = database.booruConfigDao().getAll().map { it.toDomain() },
            bookmarks = database.bookmarkDao().getAll().map { it.toDomain() },
            blacklistedTags = database.blacklistedTagDao().getAll().map { it.toDomain() },
            favoriteTags = database.favoriteTagDao().getAll().map { it.toDomain() },
            searchHistory = database.searchHistoryDao().getAll().map { it.toDomain() },
            settings = _settings.value
        )
        json.encodeToString(payload)
    }

    suspend fun importBackupJson(rawJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = json.decodeFromString<BackupPayload>(rawJson)
            if (payload.configs.isNotEmpty()) {
                database.booruConfigDao().deleteAll()
                payload.configs.forEach { database.booruConfigDao().insert(it.toEntity()) }
            }
            payload.bookmarks.forEach { database.bookmarkDao().insert(it.toEntity()) }
            payload.blacklistedTags.forEach { database.blacklistedTagDao().insert(it.toEntity()) }
            payload.favoriteTags.forEach { database.favoriteTagDao().insert(it.toEntity()) }
            payload.searchHistory.forEach { database.searchHistoryDao().insert(it.toEntity()) }
            updateSettings { payload.settings }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getCacheSizeBytes(): Long {
        return try {
            context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun clearImageCache() = withContext(Dispatchers.IO) {
        try {
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        } catch (_: Exception) {
        }
    }

    // Entity <-> Domain Mappers
    private fun BooruConfigEntity.toDomain(): BooruConfig {
        val granularRatings = try {
            json.decodeFromString<Set<Rating>>(granularRatingFiltersJson)
        } catch (_: Exception) {
            emptySet()
        }
        val blacklisted = try {
            json.decodeFromString<List<String>>(blacklistedTagsJson)
        } catch (_: Exception) {
            emptyList()
        }
        val proxy = try {
            json.decodeFromString<ProxySettings>(proxySettingsJson)
        } catch (_: Exception) {
            ProxySettings()
        }
        val headers = try {
            json.decodeFromString<Map<String, String>>(customHeadersJson)
        } catch (_: Exception) {
            emptyMap()
        }
        return BooruConfig(
            id = id,
            booruId = booruId,
            booruIdHint = booruIdHint,
            url = url,
            name = name,
            login = login,
            apiKey = apiKey,
            passHash = passHash,
            cookie = cookie,
            ratingFilter = BooruConfigRatingFilter.entries.find { it.name == ratingFilter }
                ?: BooruConfigRatingFilter.NONE,
            granularRatingFilters = granularRatings,
            deletedItemBehavior = BooruConfigDeletedItemBehavior.entries.find { it.name == deletedItemBehavior }
                ?: BooruConfigDeletedItemBehavior.SHOW,
            bannedPostVisibility = BooruConfigBannedPostVisibility.entries.find { it.name == bannedPostVisibility }
                ?: BooruConfigBannedPostVisibility.SHOW,
            customDownloadFileNameFormat = customDownloadFileNameFormat,
            customBulkDownloadFileNameFormat = customBulkDownloadFileNameFormat,
            imageDetaisQuality = imageDetailsQuality,
            blacklistedTags = blacklisted,
            alwaysIncludeTags = alwaysIncludeTags,
            proxySettings = proxy,
            customHeaders = headers,
            postsPerPageOverride = postsPerPageOverride
        )
    }

    private fun BooruConfig.toEntity(): BooruConfigEntity = BooruConfigEntity(
        id = id,
        booruId = booruId,
        booruIdHint = booruIdHint,
        url = url,
        name = name,
        login = login,
        apiKey = apiKey,
        passHash = passHash,
        cookie = cookie,
        ratingFilter = ratingFilter.name,
        granularRatingFiltersJson = json.encodeToString(granularRatingFilters),
        deletedItemBehavior = deletedItemBehavior.name,
        bannedPostVisibility = bannedPostVisibility.name,
        customDownloadFileNameFormat = customDownloadFileNameFormat,
        customBulkDownloadFileNameFormat = customBulkDownloadFileNameFormat,
        imageDetailsQuality = imageDetaisQuality,
        blacklistedTagsJson = json.encodeToString(blacklistedTags),
        alwaysIncludeTags = alwaysIncludeTags,
        proxySettingsJson = json.encodeToString(proxySettings),
        customHeadersJson = json.encodeToString(customHeaders),
        postsPerPageOverride = postsPerPageOverride
    )

    private fun BookmarkEntity.toDomain(): BookmarkItem = BookmarkItem(
        id = id,
        booruId = booruId,
        postId = postId,
        sourceUrl = sourceUrl,
        thumbnailUrl = thumbnailUrl,
        sampleUrl = sampleUrl,
        originalUrl = originalUrl,
        width = width,
        height = height,
        md5 = md5,
        tags = try {
            json.decodeFromString<List<String>>(tagsJson)
        } catch (_: Exception) {
            emptyList()
        },
        rating = Rating.entries.find { it.name == rating } ?: Rating.GENERAL,
        format = format,
        createdAt = createdAt
    )

    private fun BookmarkItem.toEntity(): BookmarkEntity = BookmarkEntity(
        id = id,
        booruId = booruId,
        postId = postId,
        sourceUrl = sourceUrl,
        thumbnailUrl = thumbnailUrl,
        sampleUrl = sampleUrl,
        originalUrl = originalUrl,
        width = width,
        height = height,
        md5 = md5,
        tagsJson = json.encodeToString(tags),
        rating = rating.name,
        format = format,
        createdAt = createdAt
    )

    private fun BlacklistedTagEntity.toDomain(): BlacklistedTagItem = BlacklistedTagItem(
        id = id,
        name = name,
        isActive = isActive,
        booruConfigId = booruConfigId,
        createdAt = createdAt
    )

    private fun BlacklistedTagItem.toEntity(): BlacklistedTagEntity = BlacklistedTagEntity(
        id = id,
        name = name,
        isActive = isActive,
        booruConfigId = booruConfigId,
        createdAt = createdAt
    )

    private fun FavoriteTagEntity.toDomain(): FavoriteTagItem = FavoriteTagItem(
        name = name,
        labels = try {
            json.decodeFromString<List<String>>(labelsJson)
        } catch (_: Exception) {
            emptyList()
        },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun FavoriteTagItem.toEntity(): FavoriteTagEntity = FavoriteTagEntity(
        name = name,
        labelsJson = json.encodeToString(labels),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun SearchHistoryEntity.toDomain(): SearchHistoryItem = SearchHistoryItem(
        query = query,
        createdAt = createdAt,
        searchCount = searchCount,
        booruTypeName = booruTypeName
    )

    private fun SearchHistoryItem.toEntity(): SearchHistoryEntity = SearchHistoryEntity(
        query = query,
        createdAt = createdAt,
        searchCount = searchCount,
        booruTypeName = booruTypeName
    )

    private fun BulkDownloadTaskEntity.toDomain(): BulkDownloadTask = BulkDownloadTask(
        id = id,
        tags = tags,
        siteUrl = siteUrl,
        status = BulkDownloadStatus.entries.find { it.name == status } ?: BulkDownloadStatus.CREATED,
        totalItems = totalItems,
        downloadedItems = downloadedItems,
        failedItems = failedItems,
        quality = quality,
        skipDuplicates = skipDuplicates,
        excludeBlacklisted = excludeBlacklisted,
        createdAt = createdAt
    )

    private fun BulkDownloadTask.toEntity(): BulkDownloadTaskEntity = BulkDownloadTaskEntity(
        id = id,
        tags = tags,
        siteUrl = siteUrl,
        status = status.name,
        totalItems = totalItems,
        downloadedItems = downloadedItems,
        failedItems = failedItems,
        quality = quality,
        skipDuplicates = skipDuplicates,
        excludeBlacklisted = excludeBlacklisted,
        createdAt = createdAt
    )

    private fun DownloadRecordEntity.toDomain(): DownloadTaskItem = DownloadTaskItem(
        id = id,
        postId = postId,
        fileName = fileName,
        url = url,
        thumbnailUrl = thumbnailUrl,
        siteUrl = siteUrl,
        status = DownloadStatus.entries.find { it.name == status } ?: DownloadStatus.PENDING,
        progress = progress,
        fileSize = fileSize,
        savedPath = savedPath,
        errorMessage = errorMessage,
        createdAt = createdAt
    )

    private fun DownloadTaskItem.toEntity(): DownloadRecordEntity = DownloadRecordEntity(
        id = id,
        postId = postId,
        fileName = fileName,
        url = url,
        thumbnailUrl = thumbnailUrl,
        siteUrl = siteUrl,
        status = status.name,
        progress = progress,
        fileSize = fileSize,
        savedPath = savedPath,
        errorMessage = errorMessage,
        createdAt = createdAt
    )
}
