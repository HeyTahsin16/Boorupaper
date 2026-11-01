package com.degenk.boorusama.model

import kotlinx.serialization.Serializable

@Serializable
enum class BooruType(
    val id: Int,
    val displayName: String,
    val defaultUrl: String,
    val canFavorite: Boolean = false,
    val canComment: Boolean = true,
    val hasNotes: Boolean = false,
    val hasPools: Boolean = false,
    val hasExplore: Boolean = false,
    val hasForum: Boolean = false,
    val hasWiki: Boolean = false,
    val hasArtists: Boolean = false,
    val hasFavoriteGroups: Boolean = false,
    val hasSavedSearches: Boolean = false,
    val loginFields: List<String> = listOf("login", "apiKey")
) {
    DANBOORU(
        id = 20,
        displayName = "Danbooru",
        defaultUrl = "https://safebooru.donmai.us/",
        canFavorite = true,
        canComment = true,
        hasNotes = true,
        hasPools = true,
        hasExplore = true,
        hasForum = true,
        hasWiki = true,
        hasArtists = true,
        hasFavoriteGroups = true,
        hasSavedSearches = true,
        loginFields = listOf("login", "apiKey")
    ),
    GELBOORU(
        id = 21,
        displayName = "Gelbooru 0.2.5",
        defaultUrl = "https://gelbooru.com/",
        canFavorite = true,
        canComment = true,
        hasNotes = true,
        hasPools = false,
        hasExplore = false
    ),
    GELBOORU_V1(
        id = 22,
        displayName = "Gelbooru 0.1.x",
        defaultUrl = "https://illusioncards.booru.org/",
        canComment = false
    ),
    GELBOORU_V2(
        id = 23,
        displayName = "Gelbooru 0.2",
        defaultUrl = "https://safebooru.org/",
        canComment = true,
        hasNotes = true
    ),
    MOEBOORU(
        id = 24,
        displayName = "Moebooru",
        defaultUrl = "https://konachan.net/",
        canFavorite = true,
        canComment = true,
        hasNotes = true,
        hasPools = true,
        hasExplore = true,
        loginFields = listOf("login", "passwordHash")
    ),
    E621(
        id = 25,
        displayName = "e621 / e926",
        defaultUrl = "https://e926.net/",
        canFavorite = true,
        canComment = true,
        hasNotes = true,
        hasPools = true,
        hasExplore = true,
        hasArtists = true,
        hasWiki = true
    ),
    ZEROCHAN(
        id = 26,
        displayName = "Zerochan",
        defaultUrl = "https://www.zerochan.net/",
        canComment = false
    ),
    SANKAKU(
        id = 27,
        displayName = "Sankaku",
        defaultUrl = "https://chan.sankakucomplex.com/",
        canFavorite = true,
        canComment = true,
        hasNotes = true,
        hasExplore = true
    ),
    PHILOMENA(
        id = 28,
        displayName = "Philomena",
        defaultUrl = "https://derpibooru.org/",
        canFavorite = true,
        canComment = true,
        loginFields = listOf("apiKey")
    ),
    SHIMMIE2(
        id = 29,
        displayName = "Shimmie2",
        defaultUrl = "https://rule34.paheal.net/",
        canComment = false
    ),
    SZURUBOORU(
        id = 30,
        displayName = "Szurubooru",
        defaultUrl = "https://demo.szurubooru.org/",
        canFavorite = true,
        canComment = true,
        hasPools = true
    ),
    HYDRUS(
        id = 31,
        displayName = "Hydrus Network",
        defaultUrl = "http://127.0.0.1:45869/",
        canFavorite = true,
        canComment = false,
        loginFields = listOf("apiKey")
    ),
    ANIME_PICTURES(
        id = 32,
        displayName = "Anime Pictures",
        defaultUrl = "https://anime-pictures.net/",
        canFavorite = true,
        hasExplore = true,
        loginFields = listOf("cookie")
    ),
    HYBOORU(
        id = 33,
        displayName = "Hybooru",
        defaultUrl = "https://booru.funmaker.moe/",
        canComment = false
    ),
    ESHUUSHUU(
        id = 34,
        displayName = "E-Shuushuu",
        defaultUrl = "https://e-shuushuu.net/",
        canComment = false
    ),
    NOZOMI(
        id = 35,
        displayName = "Nozomi.la",
        defaultUrl = "https://nozomi.la/",
        canComment = false
    );

    companion object {
        fun fromId(id: Int): BooruType = entries.find { it.id == id } ?: DANBOORU
    }
}

@Serializable
enum class Rating(val shortCode: String, val label: String) {
    GENERAL("g", "General"),
    SENSITIVE("s", "Sensitive"),
    QUESTIONABLE("q", "Questionable"),
    EXPLICIT("e", "Explicit");

    companion object {
        fun fromCode(code: String?): Rating = when (code?.lowercase()?.trim()) {
            "g", "general", "safe" -> GENERAL
            "s", "sensitive" -> SENSITIVE
            "q", "questionable" -> QUESTIONABLE
            "e", "explicit", "unsafe" -> EXPLICIT
            else -> GENERAL
        }
    }
}

@Serializable
enum class BooruConfigRatingFilter(val label: String) {
    NONE("None (Follow site)"),
    HIDE_EXPLICIT("Safe / Moderate (Hide Explicit)"),
    HIDE_NSFW("Safe Only (General)"),
    CUSTOM("Custom Granular Ratings")
}

@Serializable
enum class BooruConfigDeletedItemBehavior(val label: String) {
    SHOW("Show deleted posts"),
    HIDE("Hide deleted posts")
}

@Serializable
enum class BooruConfigBannedPostVisibility(val label: String) {
    SHOW("Show banned posts"),
    HIDE("Hide banned posts")
}

@Serializable
data class ProxySettings(
    val enable: Boolean = false,
    val type: String = "HTTP",
    val host: String = "",
    val port: Int = 8080,
    val username: String = "",
    val password: String = ""
)

@Serializable
data class BooruConfig(
    val id: Int = 1,
    val booruId: Int = BooruType.DANBOORU.id,
    val booruIdHint: Int = BooruType.DANBOORU.id,
    val url: String = "https://safebooru.donmai.us/",
    val name: String = "Default profile",
    val login: String = "",
    val apiKey: String = "",
    val passHash: String = "",
    val cookie: String = "",
    val ratingFilter: BooruConfigRatingFilter = BooruConfigRatingFilter.NONE,
    val granularRatingFilters: Set<Rating> = emptySet(),
    val deletedItemBehavior: BooruConfigDeletedItemBehavior = BooruConfigDeletedItemBehavior.SHOW,
    val bannedPostVisibility: BooruConfigBannedPostVisibility = BooruConfigBannedPostVisibility.SHOW,
    val customDownloadFileNameFormat: String = "{id}_{md5:maxlength=8}.{ext}",
    val customBulkDownloadFileNameFormat: String = "{search}/{id}_{md5:maxlength=8}.{ext}",
    val imageDetaisQuality: String = "sample",
    val blacklistedTags: List<String> = emptyList(),
    val alwaysIncludeTags: String = "",
    val proxySettings: ProxySettings = ProxySettings(),
    val customHeaders: Map<String, String> = emptyMap(),
    val postsPerPageOverride: Int? = null
) {
    val booruType: BooruType
        get() = BooruType.fromId(booruIdHint)

    val isAuthenticated: Boolean
        get() = (login.isNotBlank() && apiKey.isNotBlank()) || apiKey.isNotBlank() || cookie.isNotBlank()
}

@Serializable
enum class TagCategory(val id: Int, val label: String) {
    GENERAL(0, "General"),
    ARTIST(1, "Artist"),
    COPYRIGHT(3, "Copyright"),
    CHARACTER(4, "Character"),
    META(5, "Meta");

    companion object {
        fun fromId(id: Int): TagCategory = when (id) {
            1 -> ARTIST
            3 -> COPYRIGHT
            4 -> CHARACTER
            5 -> META
            else -> GENERAL
        }
    }
}

@Serializable
data class TagItem(
    val name: String,
    val category: TagCategory = TagCategory.GENERAL,
    val postCount: Int = 0,
    val antecedent: String? = null
) {
    val displayName: String
        get() = name.replace('_', ' ')
}

@Serializable
data class PostNote(
    val id: Int,
    val postId: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val body: String
)

@Serializable
data class PostComment(
    val id: Int,
    val postId: Int,
    val creatorName: String,
    val creatorId: Int = 0,
    val body: String,
    val score: Int = 0,
    val createdAt: String = "",
    val isEdited: Boolean = false
)

@Serializable
data class Post(
    val id: Int,
    val booruUrl: String,
    val booruType: BooruType = BooruType.DANBOORU,
    val thumbnailImageUrl: String,
    val sampleImageUrl: String,
    val originalImageUrl: String,
    val tags: List<String>,
    val artistTags: List<String> = emptyList(),
    val characterTags: List<String> = emptyList(),
    val copyrightTags: List<String> = emptyList(),
    val generalTags: List<String> = emptyList(),
    val metaTags: List<String> = emptyList(),
    val rating: Rating = Rating.GENERAL,
    val score: Int = 0,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val favCount: Int = 0,
    val width: Int = 1000,
    val height: Int = 1000,
    val format: String = "jpg",
    val md5: String = "",
    val fileSize: Long = 0L,
    val source: String = "",
    val createdAt: String = "",
    val uploaderId: Int = 0,
    val uploaderName: String = "",
    val hasComment: Boolean = false,
    val hasNote: Boolean = false,
    val hasParentOrChildren: Boolean = false,
    val parentId: Int? = null,
    val duration: Double = 0.0,
    val isFavorited: Boolean = false,
    val isBanned: Boolean = false,
    val isDeleted: Boolean = false,
    val isFlagged: Boolean = false,
    val isPending: Boolean = false,
    val pixivId: Int? = null,
    val approverId: Int? = null
) {
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) (width.toFloat() / height.toFloat()).coerceIn(0.3f, 3.0f) else 1f

    val isVideo: Boolean
        get() = format.lowercase() in setOf("mp4", "webm", "zip")

    val isAnimated: Boolean
        get() = isVideo || format.lowercase() == "gif" || "animated" in tags

    val postLink: String
        get() = "${booruUrl.trimEnd('/')}/posts/$id"
}

@Serializable
data class PoolItem(
    val id: Int,
    val name: String,
    val description: String = "",
    val postIds: List<Int> = emptyList(),
    val postCount: Int = 0,
    val category: String = "series",
    val createdAt: String = "",
    val updatedAt: String = "",
    val coverUrl: String? = null
) {
    val displayName: String
        get() = name.replace('_', ' ')
}

@Serializable
data class ForumTopicItem(
    val id: Int,
    val title: String,
    val creatorName: String = "",
    val responseCount: Int = 0,
    val isSticky: Boolean = false,
    val isLocked: Boolean = false,
    val updatedAt: String = "",
    val categoryId: Int = 0
)

@Serializable
data class ForumPostItem(
    val id: Int,
    val topicId: Int,
    val creatorName: String,
    val body: String,
    val createdAt: String,
    val score: Int = 0
)

@Serializable
data class ArtistItem(
    val id: Int,
    val name: String,
    val otherNames: List<String> = emptyList(),
    val groupName: String = "",
    val urls: List<String> = emptyList(),
    val isBanned: Boolean = false,
    val postCount: Int = 0
)

@Serializable
data class WikiPage(
    val id: Int,
    val title: String,
    val body: String,
    val otherNames: List<String> = emptyList(),
    val updatedAt: String = ""
)

@Serializable
data class FavoriteGroup(
    val id: Int,
    val name: String,
    val creatorName: String = "",
    val postIds: List<Int> = emptyList(),
    val isPublic: Boolean = true,
    val updatedAt: String = ""
)

@Serializable
data class SavedSearch(
    val id: Int,
    val query: String,
    val labels: List<String> = emptyList(),
    val createdAt: String = ""
)

@Serializable
data class BookmarkItem(
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
    val tags: List<String>,
    val rating: Rating = Rating.GENERAL,
    val format: String = "jpg",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class BlacklistedTagItem(
    val id: Int = 0,
    val name: String,
    val isActive: Boolean = true,
    val booruConfigId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class FavoriteTagItem(
    val name: String,
    val labels: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SearchHistoryItem(
    val query: String,
    val createdAt: Long = System.currentTimeMillis(),
    val searchCount: Int = 1,
    val booruTypeName: String = "DANBOORU"
)

@Serializable
enum class BulkDownloadStatus {
    CREATED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
}

@Serializable
data class BulkDownloadTask(
    val id: String,
    val tags: String,
    val siteUrl: String,
    val status: BulkDownloadStatus = BulkDownloadStatus.CREATED,
    val totalItems: Int = 0,
    val downloadedItems: Int = 0,
    val failedItems: Int = 0,
    val quality: String = "original",
    val skipDuplicates: Boolean = true,
    val excludeBlacklisted: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class DownloadStatus {
    PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
}

@Serializable
data class DownloadTaskItem(
    val id: String,
    val postId: Int,
    val fileName: String,
    val url: String,
    val thumbnailUrl: String,
    val siteUrl: String,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f,
    val fileSize: Long = 0L,
    val savedPath: String = "",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class ThemePreset(val id: String, val displayName: String, val isDark: Boolean) {
    DANBOORU_DARK("danbooruDark", "Danbooru Dark (Kurumi)", true),
    DANBOORU_LIGHT("danbooruLight", "Danbooru Light (Kurumi)", false),
    OLED_BLACK("oledBlack", "AMOLED Midnight", true),
    GREEN("green", "Forest Green", false),
    DARK_GREEN("darkGreen", "Dark Emerald", true),
    CORAL_PINK("coralPink", "Coral Blossom", false),
    HACKER("hacker", "Terminal Matrix", true),
    CYBERPUNK("cyberpunk", "Neon Cyberpunk", true),
    DYNAMIC_DARK("dynamicDark", "Dynamic Material You (Dark)", true),
    DYNAMIC_LIGHT("dynamicLight", "Dynamic Material You (Light)", false)
}

@Serializable
enum class ImageGridType(val label: String) {
    MASONRY("Masonry"),
    STANDARD("Standard Grid"),
    CLASSIC("Classic Aspect")
}

@Serializable
enum class PageMode(val label: String) {
    INFINITE("Infinite Scroll"),
    PAGINATED("Paged Navigation")
}

@Serializable
enum class ImageQuality(val id: String, val label: String) {
    LOW("preview", "Preview (Fast)"),
    MEDIUM("sample", "Sample (Recommended)"),
    ORIGINAL("original", "Original (Full Size)")
}

@Serializable
enum class ExploreCategory(val label: String) {
    POPULAR("Popular"),
    MOST_VIEWED("Most Viewed"),
    HOT("Hot / Recent"),
    TRENDING_TAGS("Trending Tags")
}

@Serializable
enum class TimeScale(val queryParam: String, val label: String) {
    DAY("day", "Day"),
    WEEK("week", "Week"),
    MONTH("month", "Month")
}

@Serializable
data class AppSettings(
    val themePreset: ThemePreset = ThemePreset.DANBOORU_DARK,
    val dynamicColorEnabled: Boolean = false,
    val gridType: ImageGridType = ImageGridType.MASONRY,
    val gridColumns: Int = 3,
    val gridSpacingDp: Int = 4,
    val imageBorderRadiusDp: Int = 8,
    val pageMode: PageMode = PageMode.INFINITE,
    val postsPerPage: Int = 40,
    val showScoresOnGrid: Boolean = true,
    val showPostListConfigHeader: Boolean = true,
    val blurExplicitMedia: Boolean = false,
    val thumbnailQuality: ImageQuality = ImageQuality.MEDIUM,
    val viewerQuality: ImageQuality = ImageQuality.MEDIUM,
    val downloadQuality: ImageQuality = ImageQuality.ORIGINAL,
    val slideshowIntervalSeconds: Int = 5,
    val doubleTapToZoom: Boolean = true,
    val showNotesOverlayByDefault: Boolean = true,
    val autoPlayVideo: Boolean = true,
    val muteVideoByDefault: Boolean = false,
    val hidePostDetailsAppBarOnTap: Boolean = true,
    val enableIncognitoSearchHistory: Boolean = false,
    val autoFocusSearchBar: Boolean = true,
    val persistentSearchBar: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val biometricLockEnabled: Boolean = false,
    val privacyScreenInRecents: Boolean = false,
    val downloadSubfolderBySite: Boolean = true,
    val skipExistingDownloads: Boolean = true,
    val languageCode: String = "en-US",
    val booruConfigIdOrders: List<Int> = emptyList()
)

@Serializable
data class BackupPayload(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "4.6.1+188",
    val configs: List<BooruConfig> = emptyList(),
    val bookmarks: List<BookmarkItem> = emptyList(),
    val blacklistedTags: List<BlacklistedTagItem> = emptyList(),
    val favoriteTags: List<FavoriteTagItem> = emptyList(),
    val searchHistory: List<SearchHistoryItem> = emptyList(),
    val settings: AppSettings = AppSettings()
)
