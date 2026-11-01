package com.degenk.boorusama.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.degenk.boorusama.data.local.AppDatabase
import com.degenk.boorusama.data.remote.BooruApiClient
import com.degenk.boorusama.data.remote.DebugLogEntry
import com.degenk.boorusama.data.repository.BoorusamaRepository
import com.degenk.boorusama.model.AppSettings
import com.degenk.boorusama.model.ArtistItem
import com.degenk.boorusama.model.BlacklistedTagItem
import com.degenk.boorusama.model.BookmarkItem
import com.degenk.boorusama.model.BooruConfig
import com.degenk.boorusama.model.BulkDownloadTask
import com.degenk.boorusama.model.DownloadTaskItem
import com.degenk.boorusama.model.ExploreCategory
import com.degenk.boorusama.model.FavoriteGroup
import com.degenk.boorusama.model.FavoriteTagItem
import com.degenk.boorusama.model.ForumPostItem
import com.degenk.boorusama.model.ForumTopicItem
import com.degenk.boorusama.model.PageMode
import com.degenk.boorusama.model.PoolItem
import com.degenk.boorusama.model.Post
import com.degenk.boorusama.model.PostComment
import com.degenk.boorusama.model.PostNote
import com.degenk.boorusama.model.SavedSearch
import com.degenk.boorusama.model.SearchHistoryItem
import com.degenk.boorusama.model.TagItem
import com.degenk.boorusama.model.TimeScale
import com.degenk.boorusama.model.WikiPage
import com.degenk.boorusama.util.BooruRegistry
import com.degenk.boorusama.util.TagDefinitions
import com.degenk.boorusama.util.TagExpressionEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    SEARCH,
    POST_DETAILS,
    WALLPAPER_EDITOR,
    EXPLORE,
    POOLS,
    POOL_DETAIL,
    FORUMS,
    FORUM_POSTS,
    ARTISTS,
    ARTIST_DETAIL,
    FAVORITE_GROUPS,
    SAVED_SEARCHES,
    BOOKMARKS,
    BLACKLIST,
    FAVORITE_TAGS,
    BULK_DOWNLOADS,
    DOWNLOAD_MANAGER,
    BOORU_CONFIGS,
    BOORU_CONFIG_EDIT,
    SETTINGS,
    DEBUG_LOGS
}

data class PostsFeedUiState(
    val isLoading: Boolean = false,
    val isAppending: Boolean = false,
    val error: String? = null,
    val rawPosts: List<Post> = emptyList(),
    val visiblePosts: List<Post> = emptyList(),
    val blacklistedBreakdown: Map<String, Int> = emptyMap(),
    val temporarilyWhitelistedRules: Set<String> = emptySet(),
    val currentQuery: String = "",
    val currentPage: Int = 1,
    val selectedPostIds: Set<Int> = emptySet(),
    val isMultiSelectMode: Boolean = false
)

data class ExploreUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val category: ExploreCategory = ExploreCategory.POPULAR,
    val timeScale: TimeScale = TimeScale.DAY,
    val selectedDate: String = "",
    val posts: List<Post> = emptyList(),
    val trendingTags: List<TagItem> = emptyList()
)

data class PostDetailsUiState(
    val posts: List<Post> = emptyList(),
    val currentIndex: Int = 0,
    val commentsByPostId: Map<Int, List<PostComment>> = emptyMap(),
    val notesByPostId: Map<Int, List<PostNote>> = emptyMap(),
    val isLoadingComments: Boolean = false,
    val isLoadingNotes: Boolean = false,
    val showNotesOverlay: Boolean = true,
    val activeWikiPage: WikiPage? = null,
    val isLoadingWiki: Boolean = false,
    val isSlideshowRunning: Boolean = false
)

class BoorusamaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val apiClient = BooruApiClient()
    val repository = BoorusamaRepository(application, database, apiClient)

    val tagDefinitions: TagDefinitions = BooruRegistry.loadTagDefinitions(application)

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val screenBackStack = ArrayDeque<AppScreen>()

    val settings: StateFlow<AppSettings> = repository.settings
    val debugLogs: StateFlow<List<DebugLogEntry>> = apiClient.debugLogs

    val configs: StateFlow<List<BooruConfig>> = repository.configsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeConfigId: StateFlow<Int> = repository.activeConfigId

    private val emptyPlaceholderConfig = BooruConfig(id = -1, name = "", url = "")

    val activeConfig: StateFlow<BooruConfig> = combine(configs, activeConfigId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull() ?: emptyPlaceholderConfig
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyPlaceholderConfig)

    val bookmarks: StateFlow<List<BookmarkItem>> = repository.bookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blacklistedTags: StateFlow<List<BlacklistedTagItem>> = repository.blacklistedTagsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTags: StateFlow<List<FavoriteTagItem>> = repository.favoriteTagsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryItem>> = repository.searchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bulkTasks: StateFlow<List<BulkDownloadTask>> = repository.bulkTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadRecords: StateFlow<List<DownloadTaskItem>> = repository.downloadRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _feedState = MutableStateFlow(PostsFeedUiState())
    val feedState: StateFlow<PostsFeedUiState> = _feedState.asStateFlow()

    private val _exploreState = MutableStateFlow(ExploreUiState())
    val exploreState: StateFlow<ExploreUiState> = _exploreState.asStateFlow()

    private val _detailsState = MutableStateFlow(PostDetailsUiState())
    val detailsState: StateFlow<PostDetailsUiState> = _detailsState.asStateFlow()

    private val _wallpaperPost = MutableStateFlow<Post?>(null)
    val wallpaperPost: StateFlow<Post?> = _wallpaperPost.asStateFlow()

    private val _autocompleteSuggestions = MutableStateFlow<List<TagItem>>(emptyList())
    val autocompleteSuggestions: StateFlow<List<TagItem>> = _autocompleteSuggestions.asStateFlow()

    private val _pools = MutableStateFlow<List<PoolItem>>(emptyList())
    val pools: StateFlow<List<PoolItem>> = _pools.asStateFlow()
    private val _isLoadingPools = MutableStateFlow(false)
    val isLoadingPools: StateFlow<Boolean> = _isLoadingPools.asStateFlow()
    private val _selectedPool = MutableStateFlow<PoolItem?>(null)
    val selectedPool: StateFlow<PoolItem?> = _selectedPool.asStateFlow()
    private val _poolPosts = MutableStateFlow<List<Post>>(emptyList())
    val poolPosts: StateFlow<List<Post>> = _poolPosts.asStateFlow()

    private val _forumTopics = MutableStateFlow<List<ForumTopicItem>>(emptyList())
    val forumTopics: StateFlow<List<ForumTopicItem>> = _forumTopics.asStateFlow()
    private val _selectedForumTopic = MutableStateFlow<ForumTopicItem?>(null)
    val selectedForumTopic: StateFlow<ForumTopicItem?> = _selectedForumTopic.asStateFlow()
    private val _forumPosts = MutableStateFlow<List<ForumPostItem>>(emptyList())
    val forumPosts: StateFlow<List<ForumPostItem>> = _forumPosts.asStateFlow()

    private val _artists = MutableStateFlow<List<ArtistItem>>(emptyList())
    val artists: StateFlow<List<ArtistItem>> = _artists.asStateFlow()
    private val _selectedArtist = MutableStateFlow<ArtistItem?>(null)
    val selectedArtist: StateFlow<ArtistItem?> = _selectedArtist.asStateFlow()
    private val _artistPosts = MutableStateFlow<List<Post>>(emptyList())
    val artistPosts: StateFlow<List<Post>> = _artistPosts.asStateFlow()

    private val _favoriteGroups = MutableStateFlow<List<FavoriteGroup>>(emptyList())
    val favoriteGroups: StateFlow<List<FavoriteGroup>> = _favoriteGroups.asStateFlow()

    private val _savedSearches = MutableStateFlow<List<SavedSearch>>(emptyList())
    val savedSearches: StateFlow<List<SavedSearch>> = _savedSearches.asStateFlow()

    private val _editingConfig = MutableStateFlow<BooruConfig?>(null)
    val editingConfig: StateFlow<BooruConfig?> = _editingConfig.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var autocompleteJob: Job? = null
    private var feedLoadJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            if (!settings.value.appLockEnabled) {
                _isUnlocked.value = true
            }
            // Observe activeConfig changes so that when a profile is added or selected, we load posts
            activeConfig
                .distinctUntilChanged { old, new -> old.id == new.id && old.url == new.url }
                .collect { cfg ->
                    if (cfg.id > 0 && cfg.url.isNotBlank() && _feedState.value.rawPosts.isEmpty() && !_feedState.value.isLoading) {
                        loadHomePosts(query = "", page = 1, append = false, overrideConfig = cfg)
                    }
                }
        }
        viewModelScope.launch {
            combine(
                _feedState,
                blacklistedTags,
                activeConfig
            ) { feed, blacklist, config ->
                Triple(feed.rawPosts, blacklist, config)
            }.collect { (rawPosts, blacklist, config) ->
                if (rawPosts.isNotEmpty() && config.id > 0) {
                    recalculateFilteredFeed(rawPosts, blacklist, config)
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenBackStack.addLast(_currentScreen.value)
            _currentScreen.value = screen
        }
        when (screen) {
            AppScreen.EXPLORE -> if (_exploreState.value.posts.isEmpty()) loadExplore()
            AppScreen.POOLS -> if (_pools.value.isEmpty()) loadPools()
            AppScreen.FORUMS -> if (_forumTopics.value.isEmpty()) loadForumTopics()
            AppScreen.ARTISTS -> if (_artists.value.isEmpty()) loadArtists("")
            AppScreen.FAVORITE_GROUPS -> if (_favoriteGroups.value.isEmpty()) loadFavoriteGroups()
            AppScreen.SAVED_SEARCHES -> loadSavedSearches()
            else -> Unit
        }
    }

    fun navigateBack(): Boolean {
        if (_feedState.value.isMultiSelectMode) {
            clearMultiSelect()
            return true
        }
        return if (screenBackStack.isNotEmpty()) {
            _currentScreen.value = screenBackStack.removeLast()
            true
        } else if (_currentScreen.value != AppScreen.HOME) {
            _currentScreen.value = AppScreen.HOME
            true
        } else {
            false
        }
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun consumeMessage() {
        _snackbarMessage.value = null
    }

    fun unlockWithPin(pin: String): Boolean {
        val expected = settings.value.appLockPin
        return if (expected.isBlank() || pin == expected) {
            _isUnlocked.value = true
            true
        } else {
            showMessage("Incorrect PIN code")
            false
        }
    }

    fun unlockWithBiometricSuccess() {
        _isUnlocked.value = true
    }

    fun selectBooruConfig(config: BooruConfig) {
        repository.setActiveConfigId(config.id)
        _feedState.value = PostsFeedUiState()
        _exploreState.value = ExploreUiState()
        _pools.value = emptyList()
        _forumTopics.value = emptyList()
        _artists.value = emptyList()
        loadHomePosts(query = "", page = 1, append = false, overrideConfig = config)
        showMessage("Switched to ${config.name}")
    }

    fun loadHomePosts(
        query: String = _feedState.value.currentQuery,
        page: Int = 1,
        append: Boolean = false,
        overrideConfig: BooruConfig? = null
    ) {
        val config = overrideConfig ?: activeConfig.value
        if (config.id <= 0 || config.url.isBlank()) {
            _feedState.value = PostsFeedUiState()
            return
        }

        feedLoadJob?.cancel()
        feedLoadJob = viewModelScope.launch {
            val limit = config.postsPerPageOverride ?: settings.value.postsPerPage
            _feedState.value = _feedState.value.copy(
                isLoading = !append,
                isAppending = append,
                error = null,
                currentQuery = query,
                currentPage = page
            )

            if (query.isNotBlank() && page == 1 && !append) {
                repository.recordSearchHistory(query, config.booruType)
            }

            try {
                val fetched = apiClient.fetchPosts(
                    config = config,
                    tags = query,
                    page = page,
                    limit = limit
                )
                val combinedRaw = if (append) {
                    (_feedState.value.rawPosts + fetched).distinctBy { it.id }
                } else {
                    fetched
                }
                recalculateFilteredFeed(combinedRaw, blacklistedTags.value, config)
                _feedState.value = _feedState.value.copy(
                    isLoading = false,
                    isAppending = false,
                    error = null
                )
            } catch (e: Exception) {
                _feedState.value = _feedState.value.copy(
                    isLoading = false,
                    isAppending = false,
                    error = e.message ?: "Failed to load posts from ${config.name}"
                )
            }
        }
    }

    fun loadNextPageOrAppend() {
        val state = _feedState.value
        if (state.isLoading || state.isAppending) return
        val nextPage = state.currentPage + 1
        val append = settings.value.pageMode == PageMode.INFINITE
        loadHomePosts(query = state.currentQuery, page = nextPage, append = append)
    }

    fun jumpToPage(page: Int) {
        val target = page.coerceAtLeast(1)
        loadHomePosts(query = _feedState.value.currentQuery, page = target, append = false)
    }

    private fun recalculateFilteredFeed(
        rawPosts: List<Post>,
        blacklistItems: List<BlacklistedTagItem>,
        config: BooruConfig
    ) {
        val whitelisted = _feedState.value.temporarilyWhitelistedRules
        val activeRules = blacklistItems
            .filter { it.isActive && (it.booruConfigId == null || it.booruConfigId == config.id) }
            .map { it.name }
            .filter { it !in whitelisted }

        val filterResult = TagExpressionEngine.filterPosts(rawPosts, config, activeRules)
        _feedState.value = _feedState.value.copy(
            rawPosts = rawPosts,
            visiblePosts = filterResult.visiblePosts,
            blacklistedBreakdown = filterResult.blacklistedBreakdown
        )
    }

    fun toggleTemporaryRuleWhitelist(rule: String) {
        val current = _feedState.value.temporarilyWhitelistedRules
        val updated = if (rule in current) current - rule else current + rule
        _feedState.value = _feedState.value.copy(temporarilyWhitelistedRules = updated)
        recalculateFilteredFeed(_feedState.value.rawPosts, blacklistedTags.value, activeConfig.value)
    }

    fun togglePostSelection(postId: Int) {
        val current = _feedState.value.selectedPostIds
        val updated = if (postId in current) current - postId else current + postId
        _feedState.value = _feedState.value.copy(
            selectedPostIds = updated,
            isMultiSelectMode = updated.isNotEmpty()
        )
    }

    fun selectAllVisiblePosts() {
        val allIds = _feedState.value.visiblePosts.map { it.id }.toSet()
        _feedState.value = _feedState.value.copy(
            selectedPostIds = allIds,
            isMultiSelectMode = allIds.isNotEmpty()
        )
    }

    fun clearMultiSelect() {
        _feedState.value = _feedState.value.copy(
            selectedPostIds = emptySet(),
            isMultiSelectMode = false
        )
    }

    fun bookmarkSelectedPosts() {
        val selected = _feedState.value.visiblePosts.filter { it.id in _feedState.value.selectedPostIds }
        val config = activeConfig.value
        viewModelScope.launch {
            selected.forEach { repository.toggleBookmark(it, config) }
            clearMultiSelect()
            showMessage("Bookmarked ${selected.size} posts")
        }
    }

    fun downloadSelectedPosts() {
        val selected = _feedState.value.visiblePosts.filter { it.id in _feedState.value.selectedPostIds }
        val config = activeConfig.value
        val query = _feedState.value.currentQuery
        clearMultiSelect()
        showMessage("Queued ${selected.size} downloads")
        viewModelScope.launch {
            selected.forEach { post ->
                repository.downloadPost(post, config, searchQuery = query)
            }
            showMessage("Finished downloading ${selected.size} posts")
        }
    }

    fun queryAutocomplete(partialToken: String) {
        autocompleteJob?.cancel()
        val config = activeConfig.value
        if (partialToken.isBlank() || config.id <= 0 || config.url.isBlank()) {
            _autocompleteSuggestions.value = emptyList()
            return
        }
        autocompleteJob = viewModelScope.launch {
            delay(180)
            val results = apiClient.fetchTagAutocomplete(config, partialToken)
            _autocompleteSuggestions.value = results
        }
    }

    fun openPostDetails(posts: List<Post>, startIndex: Int) {
        val safeIndex = startIndex.coerceIn(0, (posts.size - 1).coerceAtLeast(0))
        _detailsState.value = PostDetailsUiState(
            posts = posts,
            currentIndex = safeIndex,
            showNotesOverlay = settings.value.showNotesOverlayByDefault
        )
        navigateTo(AppScreen.POST_DETAILS)
        posts.getOrNull(safeIndex)?.let { loadPostExtras(it) }
    }

    fun openWallpaperEditor(post: Post) {
        _wallpaperPost.value = post
        navigateTo(AppScreen.WALLPAPER_EDITOR)
    }

    fun onPostDetailsPageChanged(newIndex: Int) {
        val posts = _detailsState.value.posts
        if (newIndex !in posts.indices) return
        _detailsState.value = _detailsState.value.copy(currentIndex = newIndex)
        loadPostExtras(posts[newIndex])
    }

    fun loadPostExtras(post: Post) {
        val config = activeConfig.value
        if (config.id <= 0) return
        if (post.hasNote && !_detailsState.value.notesByPostId.containsKey(post.id)) {
            viewModelScope.launch {
                _detailsState.value = _detailsState.value.copy(isLoadingNotes = true)
                val notes = apiClient.fetchNotes(config, post.id)
                _detailsState.value = _detailsState.value.copy(
                    isLoadingNotes = false,
                    notesByPostId = _detailsState.value.notesByPostId + (post.id to notes)
                )
            }
        }
        if (post.hasComment && !_detailsState.value.commentsByPostId.containsKey(post.id)) {
            viewModelScope.launch {
                _detailsState.value = _detailsState.value.copy(isLoadingComments = true)
                val comments = apiClient.fetchComments(config, post.id)
                _detailsState.value = _detailsState.value.copy(
                    isLoadingComments = false,
                    commentsByPostId = _detailsState.value.commentsByPostId + (post.id to comments)
                )
            }
        }
    }

    fun forceLoadComments(postId: Int) {
        val config = activeConfig.value
        if (config.id <= 0) return
        viewModelScope.launch {
            _detailsState.value = _detailsState.value.copy(isLoadingComments = true)
            val comments = apiClient.fetchComments(config, postId)
            _detailsState.value = _detailsState.value.copy(
                isLoadingComments = false,
                commentsByPostId = _detailsState.value.commentsByPostId + (postId to comments)
            )
        }
    }

    fun toggleNotesOverlay() {
        _detailsState.value = _detailsState.value.copy(
            showNotesOverlay = !_detailsState.value.showNotesOverlay
        )
    }

    fun loadWikiForTag(tag: String) {
        val config = activeConfig.value
        if (config.id <= 0) return
        viewModelScope.launch {
            _detailsState.value = _detailsState.value.copy(isLoadingWiki = true, activeWikiPage = null)
            val wiki = apiClient.fetchWikiPage(config, tag)
                ?: WikiPage(
                    id = 0,
                    title = tag,
                    body = "No wiki entry found on ${config.name} for [[${tag}]]."
                )
            _detailsState.value = _detailsState.value.copy(
                isLoadingWiki = false,
                activeWikiPage = wiki
            )
        }
    }

    fun dismissWikiDialog() {
        _detailsState.value = _detailsState.value.copy(activeWikiPage = null, isLoadingWiki = false)
    }

    fun toggleBookmark(post: Post) {
        viewModelScope.launch {
            val added = repository.toggleBookmark(post, activeConfig.value)
            showMessage(if (added) "Saved Post #${post.id} to Bookmarks" else "Removed Post #${post.id} from Bookmarks")
        }
    }

    fun downloadSinglePost(post: Post) {
        viewModelScope.launch {
            showMessage("Downloading Post #${post.id}...")
            val record = repository.downloadPost(post, activeConfig.value, _feedState.value.currentQuery)
            if (record.status.name == "COMPLETED") {
                showMessage("Saved ${record.fileName}")
            } else {
                showMessage("Download failed: ${record.errorMessage ?: "Unknown error"}")
            }
        }
    }

    fun favoritePostOnServer(post: Post) {
        val config = activeConfig.value
        if (!config.isAuthenticated) {
            showMessage("Add login credentials in Booru Profile to favorite on server; bookmarked locally instead.")
            toggleBookmark(post)
            return
        }
        viewModelScope.launch {
            val ok = apiClient.favoritePost(config, post.id)
            showMessage(if (ok) "Favorited Post #${post.id} on ${config.name}" else "Could not favorite on server")
        }
    }

    fun searchByTagFromDetails(tag: String) {
        screenBackStack.clear()
        _currentScreen.value = AppScreen.HOME
        loadHomePosts(query = tag, page = 1, append = false)
    }

    // Explore
    fun loadExplore(
        category: ExploreCategory = _exploreState.value.category,
        timeScale: TimeScale = _exploreState.value.timeScale,
        date: String = _exploreState.value.selectedDate
    ) {
        val config = activeConfig.value
        if (config.id <= 0 || config.url.isBlank()) return
        viewModelScope.launch {
            _exploreState.value = _exploreState.value.copy(
                isLoading = true,
                error = null,
                category = category,
                timeScale = timeScale,
                selectedDate = date
            )
            try {
                val trending = apiClient.fetchTrendingTags(config, date.ifBlank { null })
                val posts = apiClient.fetchPopularPosts(
                    config = config,
                    category = category,
                    date = date.ifBlank { null },
                    timeScale = timeScale,
                    page = 1,
                    limit = 40
                )
                val blacklist = blacklistedTags.value.filter { it.isActive }.map { it.name }
                val filtered = TagExpressionEngine.filterPosts(posts, config, blacklist).visiblePosts
                _exploreState.value = _exploreState.value.copy(
                    isLoading = false,
                    posts = filtered,
                    trendingTags = trending
                )
            } catch (e: Exception) {
                _exploreState.value = _exploreState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load Explore feed"
                )
            }
        }
    }

    // Pools
    fun loadPools(query: String = "", category: String = "") {
        val config = activeConfig.value
        if (config.id <= 0 || config.url.isBlank()) return
        viewModelScope.launch {
            _isLoadingPools.value = true
            _pools.value = apiClient.fetchPools(config, query = query, category = category)
            _isLoadingPools.value = false
        }
    }

    fun openPoolDetail(pool: PoolItem) {
        _selectedPool.value = pool
        _poolPosts.value = emptyList()
        navigateTo(AppScreen.POOL_DETAIL)
        viewModelScope.launch {
            val posts = apiClient.fetchPosts(activeConfig.value, tags = "pool:${pool.id}", page = 1, limit = 50)
            _poolPosts.value = posts
        }
    }

    // Forums
    fun loadForumTopics() {
        val config = activeConfig.value
        if (config.id <= 0 || config.url.isBlank()) return
        viewModelScope.launch {
            _forumTopics.value = apiClient.fetchForumTopics(config)
        }
    }

    fun openForumTopic(topic: ForumTopicItem) {
        _selectedForumTopic.value = topic
        _forumPosts.value = emptyList()
        navigateTo(AppScreen.FORUM_POSTS)
        viewModelScope.launch {
            _forumPosts.value = apiClient.fetchForumPosts(activeConfig.value, topic.id)
        }
    }

    // Artists
    fun loadArtists(query: String) {
        val config = activeConfig.value
        if (config.id <= 0 || config.url.isBlank()) return
        viewModelScope.launch {
            _artists.value = apiClient.fetchArtists(config, query)
        }
    }

    fun openArtistDetail(artist: ArtistItem) {
        _selectedArtist.value = artist
        _artistPosts.value = emptyList()
        navigateTo(AppScreen.ARTIST_DETAIL)
        viewModelScope.launch {
            _artistPosts.value = apiClient.fetchPosts(activeConfig.value, tags = artist.name, page = 1, limit = 40)
        }
    }

    // Favorite Groups & Saved Searches
    fun loadFavoriteGroups(query: String = "") {
        val config = activeConfig.value
        if (config.id <= 0 || config.url.isBlank()) return
        viewModelScope.launch {
            _favoriteGroups.value = apiClient.fetchFavoriteGroups(config, query)
        }
    }

    fun loadSavedSearches() {
        val config = activeConfig.value
        if (config.id <= 0 || config.url.isBlank()) return
        viewModelScope.launch {
            _savedSearches.value = apiClient.fetchSavedSearches(config)
        }
    }

    // Booru Config Editor (starts with empty fields, no preset default profiles)
    fun startCreateBooruConfig() {
        _editingConfig.value = BooruConfig(
            id = 0,
            name = "",
            url = ""
        )
        navigateTo(AppScreen.BOORU_CONFIG_EDIT)
    }

    fun startEditBooruConfig(config: BooruConfig) {
        _editingConfig.value = config
        navigateTo(AppScreen.BOORU_CONFIG_EDIT)
    }

    fun saveEditingConfig(config: BooruConfig) {
        val rawUrl = config.url.trim()
        if (rawUrl.isBlank()) {
            showMessage("Please enter a Booru URL (e.g. https://safebooru.donmai.us/)")
            return
        }
        val normalizedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        val effectiveName = config.name.trim().ifBlank {
            runCatching { Uri.parse(normalizedUrl).host }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: config.booruType.displayName
        }
        val normalizedConfig = config.copy(url = normalizedUrl, name = effectiveName)
        viewModelScope.launch {
            val id = repository.saveBooruConfig(normalizedConfig)
            repository.setActiveConfigId(id)
            showMessage("Saved profile '$effectiveName'")
            navigateBack()
            loadHomePosts(query = "", page = 1, append = false, overrideConfig = normalizedConfig.copy(id = id))
        }
    }

    fun deleteBooruConfig(id: Int) {
        viewModelScope.launch {
            repository.deleteBooruConfig(id)
            if (configs.value.size <= 1) {
                _feedState.value = PostsFeedUiState()
            }
            showMessage("Deleted Booru profile")
        }
    }

    // Bulk Downloads
    fun createBulkDownload(tags: String, maxPages: Int, excludeBlacklisted: Boolean) {
        val config = activeConfig.value
        if (config.id <= 0 || config.url.isBlank()) {
            showMessage("Add a Booru profile first")
            return
        }
        if (tags.isBlank()) {
            showMessage("Enter at least one tag to bulk download")
            return
        }
        showMessage("Started bulk download for '$tags'")
        viewModelScope.launch {
            repository.runBulkDownloadTask(
                tags = tags.trim(),
                config = config,
                maxPages = maxPages.coerceIn(1, 10),
                excludeBlacklisted = excludeBlacklisted
            )
            showMessage("Completed bulk download for '$tags'")
        }
    }
}
