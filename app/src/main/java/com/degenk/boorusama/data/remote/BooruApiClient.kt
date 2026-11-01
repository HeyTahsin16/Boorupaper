package com.degenk.boorusama.data.remote

import com.degenk.boorusama.model.ArtistItem
import com.degenk.boorusama.model.BooruConfig
import com.degenk.boorusama.model.BooruType
import com.degenk.boorusama.model.ExploreCategory
import com.degenk.boorusama.model.FavoriteGroup
import com.degenk.boorusama.model.ForumPostItem
import com.degenk.boorusama.model.ForumTopicItem
import com.degenk.boorusama.model.PoolItem
import com.degenk.boorusama.model.Post
import com.degenk.boorusama.model.PostComment
import com.degenk.boorusama.model.PostNote
import com.degenk.boorusama.model.Rating
import com.degenk.boorusama.model.SavedSearch
import com.degenk.boorusama.model.TagCategory
import com.degenk.boorusama.model.TagItem
import com.degenk.boorusama.model.TimeScale
import com.degenk.boorusama.model.WikiPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class DebugLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO",
    val tag: String = "HTTP",
    val message: String
)

class BooruApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val defaultClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _debugLogs = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val debugLogs: StateFlow<List<DebugLogEntry>> = _debugLogs.asStateFlow()

    fun log(level: String, tag: String, message: String) {
        val entry = DebugLogEntry(level = level, tag = tag, message = message)
        _debugLogs.value = (_debugLogs.value + entry).takeLast(250)
    }

    fun clearLogs() {
        _debugLogs.value = emptyList()
    }

    private fun clientForConfig(config: BooruConfig): OkHttpClient {
        val proxy = config.proxySettings
        if (!proxy.enable || proxy.host.isBlank()) return defaultClient
        val proxyType = if (proxy.type.equals("SOCKS", ignoreCase = true)) Proxy.Type.SOCKS else Proxy.Type.HTTP
        return defaultClient.newBuilder()
            .proxy(Proxy(proxyType, InetSocketAddress(proxy.host, proxy.port)))
            .build()
    }

    private fun buildRequest(config: BooruConfig, url: String): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "Boorusama/4.6.1 (Android; Kotlin; Jetpack Compose)")
            .header("Accept", "application/json, text/javascript, */*; q=0.01")

        if (config.cookie.isNotBlank()) {
            builder.header("Cookie", config.cookie)
        }
        for ((k, v) in config.customHeaders) {
            if (k.isNotBlank()) builder.header(k, v)
        }
        when (config.booruType) {
            BooruType.DANBOORU, BooruType.E621 -> {
                if (config.login.isNotBlank() && config.apiKey.isNotBlank()) {
                    builder.header("Authorization", Credentials.basic(config.login, config.apiKey))
                }
            }
            BooruType.HYDRUS -> {
                if (config.apiKey.isNotBlank()) {
                    builder.header("Hydrus-Client-API-Access-Key", config.apiKey)
                }
            }
            BooruType.SZURUBOORU -> {
                if (config.login.isNotBlank() && config.apiKey.isNotBlank()) {
                    builder.header("Authorization", Credentials.basic(config.login, config.apiKey))
                }
            }
            else -> Unit
        }
        return builder
    }

    private suspend fun executeGet(config: BooruConfig, url: String): String = withContext(Dispatchers.IO) {
        log("INFO", "GET", url)
        val req = buildRequest(config, url).get().build()
        clientForConfig(config).newCall(req).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                log("ERROR", "HTTP ${response.code}", "$url -> ${response.message}")
                throw IllegalStateException("HTTP ${response.code}: ${response.message}")
            }
            log("INFO", "200 OK", "$url (${body.length} bytes)")
            body
        }
    }

    fun getOkHttpClient(config: BooruConfig): OkHttpClient = clientForConfig(config)

    fun buildDownloadRequest(config: BooruConfig, url: String): Request {
        return buildRequest(config, url)
            .header("Referer", config.url)
            .get()
            .build()
    }

    suspend fun fetchPosts(
        config: BooruConfig,
        tags: String,
        page: Int = 1,
        limit: Int = 40
    ): List<Post> = withContext(Dispatchers.IO) {
        val baseUrl = config.url.trimEnd('/')
        val combinedTags = listOf(tags.trim(), config.alwaysIncludeTags.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val encodedTags = URLEncoder.encode(combinedTags, "UTF-8")

        when (config.booruType) {
            BooruType.DANBOORU -> {
                val authParams = if (config.login.isNotBlank() && config.apiKey.isNotBlank()) {
                    "&login=${URLEncoder.encode(config.login, "UTF-8")}&api_key=${URLEncoder.encode(config.apiKey, "UTF-8")}"
                } else ""
                val url = "$baseUrl/posts.json?page=$page&limit=$limit&tags=$encodedTags$authParams"
                val raw = executeGet(config, url)
                parseDanbooruPosts(raw, baseUrl, config.booruType)
            }
            BooruType.GELBOORU, BooruType.GELBOORU_V2, BooruType.GELBOORU_V1 -> {
                val pid = (page - 1).coerceAtLeast(0)
                val authParams = if (config.login.isNotBlank() && config.apiKey.isNotBlank()) {
                    "&user_id=${URLEncoder.encode(config.login, "UTF-8")}&api_key=${URLEncoder.encode(config.apiKey, "UTF-8")}"
                } else ""
                val url = "$baseUrl/index.php?page=dapi&s=post&q=index&json=1&pid=$pid&limit=$limit&tags=$encodedTags$authParams"
                val raw = executeGet(config, url)
                parseGelbooruPosts(raw, baseUrl, config.booruType)
            }
            BooruType.MOEBOORU -> {
                val url = "$baseUrl/post.json?page=$page&limit=$limit&tags=$encodedTags"
                val raw = executeGet(config, url)
                parseMoebooruPosts(raw, baseUrl)
            }
            BooruType.E621 -> {
                val url = "$baseUrl/posts.json?page=$page&limit=$limit&tags=$encodedTags"
                val raw = executeGet(config, url)
                parseE621Posts(raw, baseUrl)
            }
            BooruType.PHILOMENA -> {
                val q = if (combinedTags.isBlank()) "*" else combinedTags
                val keyParam = if (config.apiKey.isNotBlank()) "&key=${URLEncoder.encode(config.apiKey, "UTF-8")}" else ""
                val url = "$baseUrl/api/v1/json/search/images?q=${URLEncoder.encode(q, "UTF-8")}&page=$page&per_page=$limit$keyParam"
                val raw = executeGet(config, url)
                parsePhilomenaPosts(raw, baseUrl)
            }
            BooruType.ZEROCHAN -> {
                val path = if (combinedTags.isBlank()) "" else "/${URLEncoder.encode(combinedTags.replace(' ', '+'), "UTF-8")}"
                val url = "$baseUrl$path?json&p=$page&l=$limit"
                val raw = executeGet(config, url)
                parseZerochanPosts(raw, baseUrl)
            }
            BooruType.ANIME_PICTURES -> {
                val tagParam = if (combinedTags.isNotBlank()) "&search_tag=${URLEncoder.encode(combinedTags, "UTF-8")}" else ""
                val url = "$baseUrl/api/v3/posts?page=${(page - 1).coerceAtLeast(0)}&posts_per_page=$limit$tagParam"
                val raw = executeGet(config, url)
                parseAnimePicturesPosts(raw, baseUrl)
            }
            BooruType.SZURUBOORU -> {
                val offset = ((page - 1).coerceAtLeast(0)) * limit
                val url = "$baseUrl/api/posts/?offset=$offset&limit=$limit&query=$encodedTags"
                val raw = executeGet(config, url)
                parseSzurubooruPosts(raw, baseUrl)
            }
            BooruType.SANKAKU -> {
                val apiBase = if ("chan.sankakucomplex" in baseUrl) "https://capi-v2.sankakucomplex.com" else baseUrl
                val url = "$apiBase/posts?lang=en&page=$page&limit=$limit&tags=$encodedTags"
                val raw = executeGet(config, url)
                parseSankakuPosts(raw, baseUrl)
            }
            else -> {
                // Fallback to Danbooru-compatible JSON endpoint or Gelbooru DAPI endpoint
                val url = "$baseUrl/posts.json?page=$page&limit=$limit&tags=$encodedTags"
                val raw = executeGet(config, url)
                parseDanbooruPosts(raw, baseUrl, config.booruType)
            }
        }
    }

    suspend fun fetchPopularPosts(
        config: BooruConfig,
        category: ExploreCategory,
        date: String? = null,
        timeScale: TimeScale = TimeScale.DAY,
        page: Int = 1,
        limit: Int = 40
    ): List<Post> = withContext(Dispatchers.IO) {
        val baseUrl = config.url.trimEnd('/')
        when (config.booruType) {
            BooruType.DANBOORU -> {
                when (category) {
                    ExploreCategory.POPULAR -> {
                        val dateQuery = if (!date.isNullOrBlank()) "&date=$date" else ""
                        val url = "$baseUrl/explore/posts/popular.json?scale=${timeScale.queryParam}&page=$page&limit=$limit$dateQuery"
                        parseDanbooruPosts(executeGet(config, url), baseUrl, config.booruType)
                    }
                    ExploreCategory.MOST_VIEWED -> {
                        val dateQuery = if (!date.isNullOrBlank()) "?date=$date" else ""
                        val url = "$baseUrl/explore/posts/viewed.json$dateQuery"
                        parseDanbooruPosts(executeGet(config, url), baseUrl, config.booruType)
                    }
                    ExploreCategory.HOT, ExploreCategory.TRENDING_TAGS -> {
                        fetchPosts(config, "order:rank", page, limit)
                    }
                }
            }
            BooruType.MOEBOORU -> {
                val endpoint = when (timeScale) {
                    TimeScale.DAY -> "popular_by_day.json"
                    TimeScale.WEEK -> "popular_by_week.json"
                    TimeScale.MONTH -> "popular_by_month.json"
                }
                val url = "$baseUrl/post/$endpoint"
                parseMoebooruPosts(executeGet(config, url), baseUrl)
            }
            BooruType.E621 -> {
                val url = "$baseUrl/popular.json?scale=${timeScale.queryParam}"
                parseE621Posts(executeGet(config, url), baseUrl)
            }
            else -> fetchPosts(config, "sort:score", page, limit)
        }
    }

    suspend fun fetchTrendingTags(
        config: BooruConfig,
        date: String? = null
    ): List<TagItem> = withContext(Dispatchers.IO) {
        val baseUrl = config.url.trimEnd('/')
        try {
            if (config.booruType == BooruType.DANBOORU) {
                val dateParam = if (!date.isNullOrBlank()) "?date=$date" else ""
                val url = "$baseUrl/explore/posts/searches.json$dateParam"
                val raw = executeGet(config, url)
                val arr = json.parseToJsonElement(raw).jsonArray
                arr.mapNotNull { elem ->
                    when (elem) {
                        is JsonArray -> {
                            val tag = elem.getOrNull(0)?.jsonPrimitive?.content ?: return@mapNotNull null
                            val count = elem.getOrNull(1)?.jsonPrimitive?.doubleOrNull?.toInt() ?: 0
                            TagItem(name = tag, category = TagCategory.GENERAL, postCount = count)
                        }
                        is JsonObject -> {
                            val tag = elem["tag"]?.jsonPrimitive?.content ?: return@mapNotNull null
                            val count = elem["count"]?.jsonPrimitive?.intOrNull ?: 0
                            TagItem(name = tag, category = TagCategory.GENERAL, postCount = count)
                        }
                        else -> null
                    }
                }
            } else {
                fetchTagAutocomplete(config, "")
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchTagAutocomplete(
        config: BooruConfig,
        query: String
    ): List<TagItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim().trimStart('-', '~')
        if (trimmed.isBlank()) return@withContext emptyList()
        val baseUrl = config.url.trimEnd('/')
        val encoded = URLEncoder.encode(trimmed, "UTF-8")

        try {
            when (config.booruType) {
                BooruType.DANBOORU -> {
                    val url = "$baseUrl/autocomplete.json?search[query]=$encoded&search[type]=tag_query&limit=20"
                    val raw = executeGet(config, url)
                    val arr = json.parseToJsonElement(raw).jsonArray
                    arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        val value = obj["value"]?.jsonPrimitive?.content ?: obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val categoryId = obj["category"]?.jsonPrimitive?.intOrNull ?: 0
                        val postCount = obj["post_count"]?.jsonPrimitive?.intOrNull ?: 0
                        val antecedent = obj["antecedent"]?.jsonPrimitive?.content
                        TagItem(
                            name = value,
                            category = TagCategory.fromId(categoryId),
                            postCount = postCount,
                            antecedent = antecedent
                        )
                    }
                }
                BooruType.GELBOORU -> {
                    val url = "$baseUrl/index.php?page=autocomplete2&term=$encoded&type=tag_query&limit=20"
                    val raw = executeGet(config, url)
                    val arr = json.parseToJsonElement(raw).jsonArray
                    arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        val value = obj["value"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val postCount = obj["post_count"]?.jsonPrimitive?.intOrNull ?: 0
                        val catStr = obj["category"]?.jsonPrimitive?.content.orEmpty()
                        val category = when (catStr.lowercase()) {
                            "artist" -> TagCategory.ARTIST
                            "character" -> TagCategory.CHARACTER
                            "copyright" -> TagCategory.COPYRIGHT
                            "metadata", "meta" -> TagCategory.META
                            else -> TagCategory.GENERAL
                        }
                        TagItem(name = value, category = category, postCount = postCount)
                    }
                }
                BooruType.GELBOORU_V2 -> {
                    val url = "$baseUrl/autocomplete.php?q=$encoded"
                    val raw = executeGet(config, url)
                    val arr = json.parseToJsonElement(raw).jsonArray
                    arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        val value = obj["value"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        TagItem(name = value)
                    }
                }
                BooruType.MOEBOORU -> {
                    val url = "$baseUrl/tag.json?name=$encoded*&limit=20&order=count"
                    val raw = executeGet(config, url)
                    val arr = json.parseToJsonElement(raw).jsonArray
                    arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val count = obj["count"]?.jsonPrimitive?.intOrNull ?: 0
                        val typeId = obj["type"]?.jsonPrimitive?.intOrNull ?: 0
                        TagItem(name = name, category = TagCategory.fromId(typeId), postCount = count)
                    }
                }
                BooruType.E621 -> {
                    val url = "$baseUrl/tags/autocomplete.json?search[name_matches]=$encoded"
                    val raw = executeGet(config, url)
                    val arr = json.parseToJsonElement(raw).jsonArray
                    arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val count = obj["post_count"]?.jsonPrimitive?.intOrNull ?: 0
                        val cat = obj["category"]?.jsonPrimitive?.intOrNull ?: 0
                        val antecedent = obj["antecedent_name"]?.jsonPrimitive?.content
                        TagItem(
                            name = name,
                            category = TagCategory.fromId(cat),
                            postCount = count,
                            antecedent = antecedent
                        )
                    }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchComments(config: BooruConfig, postId: Int): List<PostComment> = withContext(Dispatchers.IO) {
        val baseUrl = config.url.trimEnd('/')
        try {
            when (config.booruType) {
                BooruType.DANBOORU, BooruType.E621 -> {
                    val url = "$baseUrl/comments.json?group_by=comment&search[post_id]=$postId"
                    val raw = executeGet(config, url)
                    val arr = json.parseToJsonElement(raw).jsonArray
                    arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        val isDeleted = obj["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false
                        if (isDeleted) return@mapNotNull null
                        PostComment(
                            id = obj["id"]?.jsonPrimitive?.intOrNull ?: 0,
                            postId = postId,
                            creatorName = obj["creator_name"]?.jsonPrimitive?.content
                                ?: "User #${obj["creator_id"]?.jsonPrimitive?.intOrNull ?: 0}",
                            creatorId = obj["creator_id"]?.jsonPrimitive?.intOrNull ?: 0,
                            body = obj["body"]?.jsonPrimitive?.content.orEmpty(),
                            score = obj["score"]?.jsonPrimitive?.intOrNull ?: 0,
                            createdAt = obj["created_at"]?.jsonPrimitive?.content.orEmpty()
                        )
                    }
                }
                BooruType.MOEBOORU -> {
                    val url = "$baseUrl/comment.json?post_id=$postId"
                    val raw = executeGet(config, url)
                    val arr = json.parseToJsonElement(raw).jsonArray
                    arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        PostComment(
                            id = obj["id"]?.jsonPrimitive?.intOrNull ?: 0,
                            postId = postId,
                            creatorName = obj["creator"]?.jsonPrimitive?.content.orEmpty(),
                            creatorId = obj["creator_id"]?.jsonPrimitive?.intOrNull ?: 0,
                            body = obj["body"]?.jsonPrimitive?.content.orEmpty(),
                            score = obj["score"]?.jsonPrimitive?.intOrNull ?: 0,
                            createdAt = obj["created_at"]?.jsonPrimitive?.content.orEmpty()
                        )
                    }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchNotes(config: BooruConfig, postId: Int): List<PostNote> = withContext(Dispatchers.IO) {
        val baseUrl = config.url.trimEnd('/')
        try {
            when (config.booruType) {
                BooruType.DANBOORU, BooruType.E621 -> {
                    val url = "$baseUrl/notes.json?search[post_id]=$postId&limit=200"
                    val raw = executeGet(config, url)
                    val arr = json.parseToJsonElement(raw).jsonArray
                    arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        val isActive = obj["is_active"]?.jsonPrimitive?.booleanOrNull ?: true
                        if (!isActive) return@mapNotNull null
                        PostNote(
                            id = obj["id"]?.jsonPrimitive?.intOrNull ?: 0,
                            postId = postId,
                            x = obj["x"]?.jsonPrimitive?.intOrNull ?: 0,
                            y = obj["y"]?.jsonPrimitive?.intOrNull ?: 0,
                            width = obj["width"]?.jsonPrimitive?.intOrNull ?: 0,
                            height = obj["height"]?.jsonPrimitive?.intOrNull ?: 0,
                            body = obj["body"]?.jsonPrimitive?.content.orEmpty()
                        )
                    }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchPools(
        config: BooruConfig,
        query: String = "",
        category: String = "",
        page: Int = 1
    ): List<PoolItem> = withContext(Dispatchers.IO) {
        val baseUrl = config.url.trimEnd('/')
        try {
            when (config.booruType) {
                BooruType.DANBOORU, BooruType.E621 -> {
                    val nameParam = if (query.isNotBlank()) "&search[name_matches]=${URLEncoder.encode(query, "UTF-8")}" else ""
                    val catParam = if (category.isNotBlank()) "&search[category]=$category" else ""
                    val url = "$baseUrl/pools.json?page=$page&limit=20$nameParam$catParam"
                    val raw = executeGet(config, url)
                    val arr = json.parseToJsonElement(raw).jsonArray
                    arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        val postIds = obj["post_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: emptyList()
                        PoolItem(
                            id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null,
                            name = obj["name"]?.jsonPrimitive?.content.orEmpty(),
                            description = obj["description"]?.jsonPrimitive?.content.orEmpty(),
                            postIds = postIds,
                            postCount = obj["post_count"]?.jsonPrimitive?.intOrNull ?: postIds.size,
                            category = obj["category"]?.jsonPrimitive?.content ?: "series",
                            createdAt = obj["created_at"]?.jsonPrimitive?.content.orEmpty(),
                            updatedAt = obj["updated_at"]?.jsonPrimitive?.content.orEmpty()
                        )
                    }
                }
                BooruType.MOEBOORU -> {
                    val queryParam = if (query.isNotBlank()) "&query=${URLEncoder.encode(query, "UTF-8")}" else ""
                    val url = "$baseUrl/pool.json?page=$page$queryParam"
                    val raw = executeGet(config, url)
                    val arr = json.parseToJsonElement(raw).jsonArray
                    arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        PoolItem(
                            id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null,
                            name = obj["name"]?.jsonPrimitive?.content.orEmpty(),
                            description = obj["description"]?.jsonPrimitive?.content.orEmpty(),
                            postCount = obj["post_count"]?.jsonPrimitive?.intOrNull ?: 0,
                            createdAt = obj["created_at"]?.jsonPrimitive?.content.orEmpty(),
                            updatedAt = obj["updated_at"]?.jsonPrimitive?.content.orEmpty()
                        )
                    }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchForumTopics(config: BooruConfig, page: Int = 1): List<ForumTopicItem> = withContext(Dispatchers.IO) {
        val baseUrl = config.url.trimEnd('/')
        try {
            val url = "$baseUrl/forum_topics.json?page=$page&limit=30&search[order]=sticky"
            val raw = executeGet(config, url)
            val arr = json.parseToJsonElement(raw).jsonArray
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                ForumTopicItem(
                    id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null,
                    title = obj["title"]?.jsonPrimitive?.content.orEmpty(),
                    creatorName = "User #${obj["creator_id"]?.jsonPrimitive?.intOrNull ?: 0}",
                    responseCount = obj["response_count"]?.jsonPrimitive?.intOrNull ?: 0,
                    isSticky = obj["is_sticky"]?.jsonPrimitive?.booleanOrNull ?: false,
                    isLocked = obj["is_locked"]?.jsonPrimitive?.booleanOrNull ?: false,
                    updatedAt = obj["updated_at"]?.jsonPrimitive?.content.orEmpty(),
                    categoryId = obj["category_id"]?.jsonPrimitive?.intOrNull ?: 0
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchForumPosts(config: BooruConfig, topicId: Int): List<ForumPostItem> = withContext(Dispatchers.IO) {
        val baseUrl = config.url.trimEnd('/')
        try {
            val url = "$baseUrl/forum_posts.json?search[topic_id]=$topicId&limit=50"
            val raw = executeGet(config, url)
            val arr = json.parseToJsonElement(raw).jsonArray
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                ForumPostItem(
                    id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null,
                    topicId = topicId,
                    creatorName = "User #${obj["creator_id"]?.jsonPrimitive?.intOrNull ?: 0}",
                    body = obj["body"]?.jsonPrimitive?.content.orEmpty(),
                    createdAt = obj["created_at"]?.jsonPrimitive?.content.orEmpty()
                )
            }.reversed()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchArtists(config: BooruConfig, query: String, page: Int = 1): List<ArtistItem> = withContext(Dispatchers.IO) {
        val baseUrl = config.url.trimEnd('/')
        try {
            val nameParam = if (query.isNotBlank()) "&search[any_name_matches]=${URLEncoder.encode(query, "UTF-8")}" else ""
            val url = "$baseUrl/artists.json?page=$page&limit=25&search[order]=post_count$nameParam"
            val raw = executeGet(config, url)
            val arr = json.parseToJsonElement(raw).jsonArray
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                val otherNames = obj["other_names"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                val urls = obj["urls"]?.jsonArray?.mapNotNull {
                    it.jsonObject["url"]?.jsonPrimitive?.content
                } ?: emptyList()
                ArtistItem(
                    id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null,
                    name = obj["name"]?.jsonPrimitive?.content.orEmpty(),
                    otherNames = otherNames,
                    groupName = obj["group_name"]?.jsonPrimitive?.content.orEmpty(),
                    urls = urls,
                    isBanned = obj["is_banned"]?.jsonPrimitive?.booleanOrNull ?: false
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchWikiPage(config: BooruConfig, tag: String): WikiPage? = withContext(Dispatchers.IO) {
        val baseUrl = config.url.trimEnd('/')
        try {
            val encoded = URLEncoder.encode(tag.trim(), "UTF-8")
            val url = "$baseUrl/wiki_pages.json?search[title]=$encoded&limit=1"
            val raw = executeGet(config, url)
            val arr = json.parseToJsonElement(raw).jsonArray
            val obj = arr.firstOrNull()?.jsonObject ?: return@withContext null
            WikiPage(
                id = obj["id"]?.jsonPrimitive?.intOrNull ?: 0,
                title = obj["title"]?.jsonPrimitive?.content ?: tag,
                body = obj["body"]?.jsonPrimitive?.content.orEmpty(),
                otherNames = obj["other_names"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList(),
                updatedAt = obj["updated_at"]?.jsonPrimitive?.content.orEmpty()
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchFavoriteGroups(config: BooruConfig, query: String = "", page: Int = 1): List<FavoriteGroup> = withContext(Dispatchers.IO) {
        val baseUrl = config.url.trimEnd('/')
        try {
            val nameParam = if (query.isNotBlank()) "&search[name_contains]=${URLEncoder.encode(query, "UTF-8")}" else ""
            val url = "$baseUrl/favorite_groups.json?page=$page&limit=25$nameParam"
            val raw = executeGet(config, url)
            val arr = json.parseToJsonElement(raw).jsonArray
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                val postIds = obj["post_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: emptyList()
                FavoriteGroup(
                    id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null,
                    name = obj["name"]?.jsonPrimitive?.content.orEmpty().replace('_', ' '),
                    postIds = postIds,
                    isPublic = obj["is_public"]?.jsonPrimitive?.booleanOrNull ?: true,
                    updatedAt = obj["updated_at"]?.jsonPrimitive?.content.orEmpty()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchSavedSearches(config: BooruConfig): List<SavedSearch> = withContext(Dispatchers.IO) {
        if (!config.isAuthenticated) return@withContext emptyList()
        val baseUrl = config.url.trimEnd('/')
        try {
            val url = "$baseUrl/saved_searches.json"
            val raw = executeGet(config, url)
            val arr = json.parseToJsonElement(raw).jsonArray
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                val labels = obj["labels"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
                SavedSearch(
                    id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null,
                    query = obj["query"]?.jsonPrimitive?.content.orEmpty(),
                    labels = labels,
                    createdAt = obj["created_at"]?.jsonPrimitive?.content.orEmpty()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun favoritePost(config: BooruConfig, postId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!config.isAuthenticated) return@withContext false
        val baseUrl = config.url.trimEnd('/')
        try {
            val url = "$baseUrl/favorites.json"
            val body = FormBody.Builder().add("post_id", postId.toString()).build()
            val req = buildRequest(config, url).post(body).build()
            clientForConfig(config).newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    private fun parseDanbooruPosts(raw: String, baseUrl: String, booruType: BooruType): List<Post> {
        val element = json.parseToJsonElement(raw)
        val array = when (element) {
            is JsonArray -> element
            is JsonObject -> element["posts"]?.jsonArray ?: return emptyList()
            else -> return emptyList()
        }

        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null

            val previewUrl = resolveUrl(baseUrl, obj["preview_file_url"]?.jsonPrimitive?.content)
            val largeUrl = resolveUrl(baseUrl, obj["large_file_url"]?.jsonPrimitive?.content)
            val fileUrl = resolveUrl(baseUrl, obj["file_url"]?.jsonPrimitive?.content)

            // Fallback to media_asset variants when direct URLs are omitted
            val variants = obj["media_asset"]?.jsonObject?.get("variants")?.jsonArray
            val variantPreview = variants?.firstOrNull {
                it.jsonObject["type"]?.jsonPrimitive?.content in setOf("360x360", "720x720", "180x180")
            }?.jsonObject?.get("url")?.jsonPrimitive?.content
            val variantSample = variants?.firstOrNull {
                it.jsonObject["type"]?.jsonPrimitive?.content in setOf("sample", "720x720")
            }?.jsonObject?.get("url")?.jsonPrimitive?.content
            val variantOriginal = variants?.firstOrNull {
                it.jsonObject["type"]?.jsonPrimitive?.content == "original"
            }?.jsonObject?.get("url")?.jsonPrimitive?.content

            val thumb = previewUrl.ifBlank { variantPreview ?: largeUrl.ifBlank { fileUrl } }
            val sample = largeUrl.ifBlank { variantSample ?: fileUrl.ifBlank { thumb } }
            val original = fileUrl.ifBlank { variantOriginal ?: sample.ifBlank { thumb } }
            if (thumb.isBlank()) return@mapNotNull null

            val generalTags = splitTags(obj["tag_string_general"]?.jsonPrimitive?.content)
            val artistTags = splitTags(obj["tag_string_artist"]?.jsonPrimitive?.content)
            val characterTags = splitTags(obj["tag_string_character"]?.jsonPrimitive?.content)
            val copyrightTags = splitTags(obj["tag_string_copyright"]?.jsonPrimitive?.content)
            val metaTags = splitTags(obj["tag_string_meta"]?.jsonPrimitive?.content)
            val allTags = splitTags(obj["tag_string"]?.jsonPrimitive?.content)
                .ifEmpty { artistTags + copyrightTags + characterTags + generalTags + metaTags }

            Post(
                id = id,
                booruUrl = baseUrl,
                booruType = booruType,
                thumbnailImageUrl = thumb,
                sampleImageUrl = sample,
                originalImageUrl = original,
                tags = allTags,
                artistTags = artistTags,
                characterTags = characterTags,
                copyrightTags = copyrightTags,
                generalTags = generalTags,
                metaTags = metaTags,
                rating = Rating.fromCode(obj["rating"]?.jsonPrimitive?.content),
                score = obj["score"]?.jsonPrimitive?.intOrNull ?: 0,
                upvotes = obj["up_score"]?.jsonPrimitive?.intOrNull ?: 0,
                downvotes = kotlin.math.abs(obj["down_score"]?.jsonPrimitive?.intOrNull ?: 0),
                favCount = obj["fav_count"]?.jsonPrimitive?.intOrNull ?: 0,
                width = obj["image_width"]?.jsonPrimitive?.intOrNull ?: 1000,
                height = obj["image_height"]?.jsonPrimitive?.intOrNull ?: 1000,
                format = obj["file_ext"]?.jsonPrimitive?.content ?: "jpg",
                md5 = obj["md5"]?.jsonPrimitive?.content.orEmpty(),
                fileSize = obj["file_size"]?.jsonPrimitive?.longOrNull ?: 0L,
                source = obj["source"]?.jsonPrimitive?.content.orEmpty(),
                createdAt = obj["created_at"]?.jsonPrimitive?.content.orEmpty(),
                uploaderId = obj["uploader_id"]?.jsonPrimitive?.intOrNull ?: 0,
                hasComment = (obj["last_comment_bumped_at"]?.jsonPrimitive?.content != null) ||
                    (obj["last_commented_at"]?.jsonPrimitive?.content != null),
                hasNote = obj["last_noted_at"]?.jsonPrimitive?.content != null,
                hasParentOrChildren = (obj["parent_id"]?.jsonPrimitive?.intOrNull != null) ||
                    (obj["has_children"]?.jsonPrimitive?.booleanOrNull == true) ||
                    (obj["has_active_children"]?.jsonPrimitive?.booleanOrNull == true),
                parentId = obj["parent_id"]?.jsonPrimitive?.intOrNull,
                duration = obj["media_asset"]?.jsonObject?.get("duration")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                isBanned = obj["is_banned"]?.jsonPrimitive?.booleanOrNull ?: false,
                isDeleted = obj["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false,
                isFlagged = obj["is_flagged"]?.jsonPrimitive?.booleanOrNull ?: false,
                isPending = obj["is_pending"]?.jsonPrimitive?.booleanOrNull ?: false,
                pixivId = obj["pixiv_id"]?.jsonPrimitive?.intOrNull,
                approverId = obj["approver_id"]?.jsonPrimitive?.intOrNull
            )
        }
    }

    private fun parseGelbooruPosts(raw: String, baseUrl: String, booruType: BooruType): List<Post> {
        if (raw.isBlank()) return emptyList()
        val element = json.parseToJsonElement(raw)
        val array = when (element) {
            is JsonArray -> element
            is JsonObject -> element["post"]?.let { if (it is JsonArray) it else null } ?: return emptyList()
            else -> return emptyList()
        }

        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val fileUrl = resolveUrl(baseUrl, obj["file_url"]?.jsonPrimitive?.content)
            val sampleUrl = resolveUrl(baseUrl, obj["sample_url"]?.jsonPrimitive?.content).ifBlank { fileUrl }
            val previewUrl = resolveUrl(baseUrl, obj["preview_url"]?.jsonPrimitive?.content).ifBlank { sampleUrl }
            if (previewUrl.isBlank()) return@mapNotNull null

            val tags = splitTags(obj["tags"]?.jsonPrimitive?.content)
            val ext = fileUrl.substringAfterLast('.', "jpg").substringBefore('?')

            Post(
                id = id,
                booruUrl = baseUrl,
                booruType = booruType,
                thumbnailImageUrl = previewUrl,
                sampleImageUrl = sampleUrl,
                originalImageUrl = fileUrl.ifBlank { sampleUrl },
                tags = tags,
                generalTags = tags,
                rating = Rating.fromCode(obj["rating"]?.jsonPrimitive?.content),
                score = obj["score"]?.jsonPrimitive?.intOrNull ?: 0,
                width = obj["width"]?.jsonPrimitive?.intOrNull ?: 1000,
                height = obj["height"]?.jsonPrimitive?.intOrNull ?: 1000,
                format = ext,
                md5 = obj["md5"]?.jsonPrimitive?.content ?: obj["hash"]?.jsonPrimitive?.content.orEmpty(),
                source = obj["source"]?.jsonPrimitive?.content.orEmpty(),
                createdAt = obj["created_at"]?.jsonPrimitive?.content.orEmpty(),
                uploaderName = obj["owner"]?.jsonPrimitive?.content.orEmpty(),
                parentId = obj["parent_id"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 },
                hasNote = (obj["has_notes"]?.jsonPrimitive?.content == "true"),
                hasComment = (obj["comment_count"]?.jsonPrimitive?.intOrNull ?: 0) > 0 ||
                    (obj["has_comments"]?.jsonPrimitive?.content == "true")
            )
        }
    }

    private fun parseMoebooruPosts(raw: String, baseUrl: String): List<Post> {
        val arr = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val previewUrl = resolveUrl(baseUrl, obj["preview_url"]?.jsonPrimitive?.content)
            val sampleUrl = resolveUrl(baseUrl, obj["sample_url"]?.jsonPrimitive?.content).ifBlank { previewUrl }
            val fileUrl = resolveUrl(baseUrl, obj["file_url"]?.jsonPrimitive?.content).ifBlank { sampleUrl }
            if (previewUrl.isBlank()) return@mapNotNull null

            val tags = splitTags(obj["tags"]?.jsonPrimitive?.content)
            Post(
                id = id,
                booruUrl = baseUrl,
                booruType = BooruType.MOEBOORU,
                thumbnailImageUrl = previewUrl,
                sampleImageUrl = sampleUrl,
                originalImageUrl = fileUrl,
                tags = tags,
                generalTags = tags,
                rating = Rating.fromCode(obj["rating"]?.jsonPrimitive?.content),
                score = obj["score"]?.jsonPrimitive?.intOrNull ?: 0,
                width = obj["width"]?.jsonPrimitive?.intOrNull ?: 1000,
                height = obj["height"]?.jsonPrimitive?.intOrNull ?: 1000,
                format = obj["file_ext"]?.jsonPrimitive?.content ?: fileUrl.substringAfterLast('.', "jpg"),
                md5 = obj["md5"]?.jsonPrimitive?.content.orEmpty(),
                fileSize = obj["file_size"]?.jsonPrimitive?.longOrNull ?: 0L,
                source = obj["source"]?.jsonPrimitive?.content.orEmpty(),
                uploaderName = obj["author"]?.jsonPrimitive?.content.orEmpty(),
                hasParentOrChildren = (obj["has_children"]?.jsonPrimitive?.booleanOrNull == true) ||
                    (obj["parent_id"]?.jsonPrimitive?.intOrNull != null)
            )
        }
    }

    private fun parseE621Posts(raw: String, baseUrl: String): List<Post> {
        val root = json.parseToJsonElement(raw).jsonObject
        val arr = root["posts"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val fileObj = obj["file"]?.jsonObject
            val previewObj = obj["preview"]?.jsonObject
            val sampleObj = obj["sample"]?.jsonObject

            val fileUrl = fileObj?.get("url")?.jsonPrimitive?.content.orEmpty()
            val sampleUrl = sampleObj?.get("url")?.jsonPrimitive?.content.orEmpty().ifBlank { fileUrl }
            val previewUrl = previewObj?.get("url")?.jsonPrimitive?.content.orEmpty().ifBlank { sampleUrl }
            if (previewUrl.isBlank()) return@mapNotNull null

            val tagsObj = obj["tags"]?.jsonObject
            val artistTags = tagsObj?.get("artist")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val characterTags = tagsObj?.get("character")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val copyrightTags = tagsObj?.get("copyright")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val generalTags = (tagsObj?.get("general")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()) +
                (tagsObj?.get("species")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList())
            val metaTags = tagsObj?.get("meta")?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()

            val scoreObj = obj["score"]?.jsonObject
            val sources = obj["sources"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()

            Post(
                id = id,
                booruUrl = baseUrl,
                booruType = BooruType.E621,
                thumbnailImageUrl = previewUrl,
                sampleImageUrl = sampleUrl,
                originalImageUrl = fileUrl.ifBlank { sampleUrl },
                tags = artistTags + copyrightTags + characterTags + generalTags + metaTags,
                artistTags = artistTags,
                characterTags = characterTags,
                copyrightTags = copyrightTags,
                generalTags = generalTags,
                metaTags = metaTags,
                rating = Rating.fromCode(obj["rating"]?.jsonPrimitive?.content),
                score = scoreObj?.get("total")?.jsonPrimitive?.intOrNull ?: 0,
                upvotes = scoreObj?.get("up")?.jsonPrimitive?.intOrNull ?: 0,
                downvotes = kotlin.math.abs(scoreObj?.get("down")?.jsonPrimitive?.intOrNull ?: 0),
                favCount = obj["fav_count"]?.jsonPrimitive?.intOrNull ?: 0,
                width = fileObj?.get("width")?.jsonPrimitive?.intOrNull ?: 1000,
                height = fileObj?.get("height")?.jsonPrimitive?.intOrNull ?: 1000,
                format = fileObj?.get("ext")?.jsonPrimitive?.content ?: "jpg",
                md5 = fileObj?.get("md5")?.jsonPrimitive?.content.orEmpty(),
                fileSize = fileObj?.get("size")?.jsonPrimitive?.longOrNull ?: 0L,
                source = sources.firstOrNull().orEmpty(),
                createdAt = obj["created_at"]?.jsonPrimitive?.content.orEmpty(),
                hasComment = (obj["comment_count"]?.jsonPrimitive?.intOrNull ?: 0) > 0
            )
        }
    }

    private fun parsePhilomenaPosts(raw: String, baseUrl: String): List<Post> {
        val root = json.parseToJsonElement(raw).jsonObject
        val arr = root["images"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val reps = obj["representations"]?.jsonObject
            val thumb = reps?.get("thumb")?.jsonPrimitive?.content.orEmpty()
            val medium = reps?.get("large")?.jsonPrimitive?.content.orEmpty().ifBlank { thumb }
            val full = reps?.get("full")?.jsonPrimitive?.content.orEmpty().ifBlank { medium }
            if (thumb.isBlank()) return@mapNotNull null

            val tags = obj["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content?.replace(' ', '_') } ?: emptyList()
            Post(
                id = id,
                booruUrl = baseUrl,
                booruType = BooruType.PHILOMENA,
                thumbnailImageUrl = thumb,
                sampleImageUrl = medium,
                originalImageUrl = full,
                tags = tags,
                generalTags = tags,
                score = obj["score"]?.jsonPrimitive?.intOrNull ?: 0,
                upvotes = obj["upvotes"]?.jsonPrimitive?.intOrNull ?: 0,
                downvotes = obj["downvotes"]?.jsonPrimitive?.intOrNull ?: 0,
                favCount = obj["faves"]?.jsonPrimitive?.intOrNull ?: 0,
                width = obj["width"]?.jsonPrimitive?.intOrNull ?: 1000,
                height = obj["height"]?.jsonPrimitive?.intOrNull ?: 1000,
                format = obj["format"]?.jsonPrimitive?.content ?: "jpg",
                source = obj["source_url"]?.jsonPrimitive?.content.orEmpty(),
                createdAt = obj["created_at"]?.jsonPrimitive?.content.orEmpty(),
                uploaderName = obj["uploader"]?.jsonPrimitive?.content.orEmpty()
            )
        }
    }

    private fun parseZerochanPosts(raw: String, baseUrl: String): List<Post> {
        val root = json.parseToJsonElement(raw).jsonObject
        val arr = root["items"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val thumb = obj["thumbnail"]?.jsonPrimitive?.content.orEmpty()
            val full = obj["full"]?.jsonPrimitive?.content.orEmpty().ifBlank { thumb }
            if (thumb.isBlank()) return@mapNotNull null
            val tags = obj["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content?.replace(' ', '_') } ?: emptyList()
            Post(
                id = id,
                booruUrl = baseUrl,
                booruType = BooruType.ZEROCHAN,
                thumbnailImageUrl = thumb,
                sampleImageUrl = full,
                originalImageUrl = full,
                tags = tags,
                generalTags = tags,
                width = obj["width"]?.jsonPrimitive?.intOrNull ?: 1000,
                height = obj["height"]?.jsonPrimitive?.intOrNull ?: 1000,
                md5 = obj["md5"]?.jsonPrimitive?.content.orEmpty(),
                source = obj["source"]?.jsonPrimitive?.content.orEmpty()
            )
        }
    }

    private fun parseAnimePicturesPosts(raw: String, baseUrl: String): List<Post> {
        val root = json.parseToJsonElement(raw).jsonObject
        val arr = root["posts"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val md5 = obj["md5"]?.jsonPrimitive?.content.orEmpty()
            val thumb = "https://opreviews.anime-pictures.net/${md5.take(3)}/${md5}_cp.jpg"
            Post(
                id = id,
                booruUrl = baseUrl,
                booruType = BooruType.ANIME_PICTURES,
                thumbnailImageUrl = thumb,
                sampleImageUrl = thumb,
                originalImageUrl = thumb,
                tags = emptyList(),
                score = obj["score_number"]?.jsonPrimitive?.intOrNull ?: 0,
                width = obj["width"]?.jsonPrimitive?.intOrNull ?: 1000,
                height = obj["height"]?.jsonPrimitive?.intOrNull ?: 1000,
                md5 = md5
            )
        }
    }

    private fun parseSzurubooruPosts(raw: String, baseUrl: String): List<Post> {
        val root = json.parseToJsonElement(raw).jsonObject
        val arr = root["results"]?.jsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val thumb = resolveUrl(baseUrl, obj["thumbnailUrl"]?.jsonPrimitive?.content)
            val contentUrl = resolveUrl(baseUrl, obj["contentUrl"]?.jsonPrimitive?.content).ifBlank { thumb }
            if (thumb.isBlank()) return@mapNotNull null
            val tags = obj["tags"]?.jsonArray?.mapNotNull {
                it.jsonObject["names"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
            } ?: emptyList()
            Post(
                id = id,
                booruUrl = baseUrl,
                booruType = BooruType.SZURUBOORU,
                thumbnailImageUrl = thumb,
                sampleImageUrl = contentUrl,
                originalImageUrl = contentUrl,
                tags = tags,
                generalTags = tags,
                score = obj["score"]?.jsonPrimitive?.intOrNull ?: 0,
                favCount = obj["favoriteCount"]?.jsonPrimitive?.intOrNull ?: 0,
                width = obj["canvasWidth"]?.jsonPrimitive?.intOrNull ?: 1000,
                height = obj["canvasHeight"]?.jsonPrimitive?.intOrNull ?: 1000
            )
        }
    }

    private fun parseSankakuPosts(raw: String, baseUrl: String): List<Post> {
        val arr = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val previewUrl = resolveUrl(baseUrl, obj["preview_url"]?.jsonPrimitive?.content)
            val sampleUrl = resolveUrl(baseUrl, obj["sample_url"]?.jsonPrimitive?.content).ifBlank { previewUrl }
            val fileUrl = resolveUrl(baseUrl, obj["file_url"]?.jsonPrimitive?.content).ifBlank { sampleUrl }
            if (previewUrl.isBlank()) return@mapNotNull null
            val tags = obj["tags"]?.jsonArray?.mapNotNull {
                it.jsonObject["name"]?.jsonPrimitive?.content
            } ?: emptyList()
            Post(
                id = id,
                booruUrl = baseUrl,
                booruType = BooruType.SANKAKU,
                thumbnailImageUrl = previewUrl,
                sampleImageUrl = sampleUrl,
                originalImageUrl = fileUrl,
                tags = tags,
                generalTags = tags,
                rating = Rating.fromCode(obj["rating"]?.jsonPrimitive?.content),
                score = obj["total_score"]?.jsonPrimitive?.intOrNull ?: 0,
                favCount = obj["fav_count"]?.jsonPrimitive?.intOrNull ?: 0,
                width = obj["width"]?.jsonPrimitive?.intOrNull ?: 1000,
                height = obj["height"]?.jsonPrimitive?.intOrNull ?: 1000
            )
        }
    }

    private fun resolveUrl(baseUrl: String, url: String?): String {
        if (url.isNullOrBlank()) return ""
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "${baseUrl.trimEnd('/')}$url"
            else -> "${baseUrl.trimEnd('/')}/$url"
        }
    }

    private fun splitTags(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }
}
