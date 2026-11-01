package com.degenk.boorusama.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.degenk.boorusama.model.ExploreCategory
import com.degenk.boorusama.model.Post
import com.degenk.boorusama.model.TimeScale
import com.degenk.boorusama.ui.AppScreen
import com.degenk.boorusama.ui.BoorusamaViewModel
import com.degenk.boorusama.util.DTextParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(viewModel: BoorusamaViewModel) {
    val exploreState by viewModel.exploreState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore") },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("explore_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = exploreState.category.ordinal) {
                ExploreCategory.entries.forEach { category ->
                    Tab(
                        selected = exploreState.category == category,
                        onClick = { viewModel.loadExplore(category = category) },
                        text = { Text(category.label) }
                    )
                }
            }

            if (exploreState.category == ExploreCategory.POPULAR) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scale:", style = MaterialTheme.typography.labelLarge)
                    TimeScale.entries.forEach { scale ->
                        FilterChip(
                            selected = exploreState.timeScale == scale,
                            onClick = { viewModel.loadExplore(timeScale = scale) },
                            label = { Text(scale.label) }
                        )
                    }
                }
            }

            when {
                exploreState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                exploreState.category == ExploreCategory.TRENDING_TAGS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(exploreState.trendingTags, key = { it.name }) { tagItem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.searchByTagFromDetails(tagItem.name)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tagItem.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Badge { Text("${tagItem.postCount} searches") }
                                }
                            }
                        }
                    }
                }
                exploreState.posts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(exploreState.error ?: "No popular posts found.")
                    }
                }
                else -> {
                    PostGridView(
                        posts = exploreState.posts,
                        settings = settings,
                        onPostClick = { index -> viewModel.openPostDetails(exploreState.posts, index) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolsScreen(viewModel: BoorusamaViewModel) {
    val pools by viewModel.pools.collectAsState()
    val isLoading by viewModel.isLoadingPools.collectAsState()
    var searchName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pools") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchName,
                    onValueChange = { searchName = it },
                    placeholder = { Text("Search pool title...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.loadPools(searchName, selectedCategory) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search pools")
                        }
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("" to "All", "series" to "Series", "collection" to "Collection").forEach { (cat, label) ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            viewModel.loadPools(searchName, cat)
                        },
                        label = { Text(label) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pools, key = { it.id }) { pool ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openPoolDetail(pool) }
                                .testTag("pool_item_${pool.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pool.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Badge { Text("${pool.postCount} posts") }
                                }
                                if (pool.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = DTextParser.stripNestedDText(pool.description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolDetailScreen(viewModel: BoorusamaViewModel) {
    val pool by viewModel.selectedPool.collectAsState()
    val posts by viewModel.poolPosts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(pool?.displayName ?: "Pool", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${posts.size} posts loaded", style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                PostGridView(
                    posts = posts,
                    settings = settings,
                    onPostClick = { idx -> viewModel.openPostDetails(posts, idx) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumsScreen(viewModel: BoorusamaViewModel) {
    val topics by viewModel.forumTopics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forums") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(topics, key = { it.id }) { topic ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openForumTopic(topic) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (topic.isSticky) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            if (topic.isLocked) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = topic.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${topic.responseCount} replies",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = topic.updatedAt.take(10),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumPostsScreen(viewModel: BoorusamaViewModel) {
    val topic by viewModel.selectedForumTopic.collectAsState()
    val posts by viewModel.forumPosts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topic?.title ?: "Topic", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(posts, key = { it.id }) { forumPost ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = forumPost.creatorName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = forumPost.createdAt.take(10),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = DTextParser.parseToAnnotatedString(forumPost.body),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(viewModel: BoorusamaViewModel) {
    val artists by viewModel.artists.collectAsState()
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artists") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search artist name or alias...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { viewModel.loadArtists(query) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search artists")
                    }
                }
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(artists, key = { it.id }) { artist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openArtistDetail(artist) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = artist.name.replace('_', ' '),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (artist.otherNames.isNotEmpty()) {
                                Text(
                                    text = "Other names: ${artist.otherNames.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (artist.urls.isNotEmpty()) {
                                Text(
                                    text = "${artist.urls.size} external links",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(viewModel: BoorusamaViewModel) {
    val artist by viewModel.selectedArtist.collectAsState()
    val posts by viewModel.artistPosts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artist?.name?.replace('_', ' ') ?: "Artist") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            artist?.let { art ->
                if (art.otherNames.isNotEmpty() || art.urls.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (art.otherNames.isNotEmpty()) {
                                Text("Aliases: ${art.otherNames.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (art.urls.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    art.urls.take(6).forEach { link ->
                                        AssistChip(onClick = {}, label = { Text(link.removePrefix("https://").take(28)) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                PostGridView(
                    posts = posts,
                    settings = settings,
                    onPostClick = { idx -> viewModel.openPostDetails(posts, idx) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteGroupsScreen(viewModel: BoorusamaViewModel) {
    val groups by viewModel.favoriteGroups.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorite Groups") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groups, key = { it.id }) { group ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.searchByTagFromDetails("favgroup:${group.id}")
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(group.name, style = MaterialTheme.typography.titleMedium)
                            Text("Updated ${group.updatedAt.take(10)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Badge { Text("${group.postIds.size} posts") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSearchesScreen(viewModel: BoorusamaViewModel) {
    val savedSearches by viewModel.savedSearches.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Searches") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!activeConfig.isAuthenticated) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Add your Danbooru login and API key in Booru Profiles to sync Saved Searches.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedSearches, key = { it.id }) { s ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.searchByTagFromDetails(s.query) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(s.query, style = MaterialTheme.typography.titleMedium)
                            if (s.labels.isNotEmpty()) {
                                Text("Labels: ${s.labels.joinToString(", ")}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(viewModel: BoorusamaViewModel) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    var filterTag by remember { mutableStateOf("") }

    val filtered = remember(bookmarks, filterTag) {
        if (filterTag.isBlank()) bookmarks
        else bookmarks.filter { bm -> bm.tags.any { it.contains(filterTag.trim(), ignoreCase = true) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks (${bookmarks.size})") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = filterTag,
                onValueChange = { filterTag = it },
                placeholder = { Text("Filter bookmarks by tag...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("bookmarks_filter_input"),
                trailingIcon = {
                    if (filterTag.isNotEmpty()) {
                        IconButton(onClick = { filterTag = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No saved bookmarks match.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { bm ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.8f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable {
                                    val asPosts = filtered.map { item ->
                                        Post(
                                            id = item.postId,
                                            booruUrl = item.sourceUrl,
                                            thumbnailImageUrl = item.thumbnailUrl,
                                            sampleImageUrl = item.sampleUrl,
                                            originalImageUrl = item.originalUrl,
                                            tags = item.tags,
                                            generalTags = item.tags,
                                            width = item.width,
                                            height = item.height,
                                            md5 = item.md5,
                                            rating = item.rating,
                                            format = item.format
                                        )
                                    }
                                    viewModel.openPostDetails(asPosts, filtered.indexOf(bm))
                                }
                        ) {
                            AsyncImage(
                                model = bm.sampleUrl.ifBlank { bm.thumbnailUrl },
                                contentDescription = "Bookmark #${bm.postId}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = {
                                    viewModel.viewModelScope.launch {
                                        viewModel.repository.removeBookmark(bm.id)
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove bookmark",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(viewModel: BoorusamaViewModel) {
    val blacklisted by viewModel.blacklistedTags.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newRuleText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blacklisted Tags (${blacklisted.size})") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_blacklist_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add blacklist rule")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Supports multi-tag expressions, negation (-tag), rating:explicit, score:<-5, uploader:name, and wildcards (*).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    viewModel.tagDefinitions.defaultBlacklistedTags.forEach {
                                        viewModel.repository.addBlacklistedTag(it)
                                    }
                                    viewModel.showMessage("Added default safe preset tags")
                                }
                            },
                            label = { Text("Import Safe Defaults") }
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(blacklisted, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = item.isActive,
                                onCheckedChange = {
                                    viewModel.viewModelScope.launch {
                                        viewModel.repository.toggleBlacklistedTag(item)
                                    }
                                }
                            )
                            IconButton(
                                onClick = {
                                    viewModel.viewModelScope.launch {
                                        viewModel.repository.deleteBlacklistedTag(item.id)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete rule")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Blacklist Rule") },
            text = {
                OutlinedTextField(
                    value = newRuleText,
                    onValueChange = { newRuleText = it },
                    placeholder = { Text("e.g. rating:explicit or monochrome -comic") },
                    modifier = Modifier.fillMaxWidth().testTag("blacklist_rule_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            newRuleText.lines().filter { it.isNotBlank() }.forEach {
                                viewModel.repository.addBlacklistedTag(it)
                            }
                            newRuleText = ""
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_blacklist_button")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteTagsScreen(viewModel: BoorusamaViewModel) {
    val favoriteTags by viewModel.favoriteTags.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var labelInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorite Tags (${favoriteTags.size})") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_favorite_tag_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add favorite tag")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(favoriteTags, key = { it.name }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.searchByTagFromDetails(item.name) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            if (item.labels.isNotEmpty()) {
                                Text("Labels: ${item.labels.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    viewModel.repository.deleteFavoriteTag(item.name)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Favorite Tag") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Tag or Query") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("favorite_tag_name_input")
                    )
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Optional Label / Group") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            val labels = labelInput.split(',').map { it.trim() }.filter { it.isNotBlank() }
                            viewModel.repository.saveFavoriteTag(tagInput, labels)
                            tagInput = ""
                            labelInput = ""
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_favorite_tag_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkDownloadScreen(viewModel: BoorusamaViewModel) {
    val tasks by viewModel.bulkTasks.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()

    var tagsInput by remember { mutableStateOf("") }
    var pagesCount by remember { mutableIntStateOf(1) }
    var excludeBlacklisted by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Downloads") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("New Bulk Download Task (${activeConfig.name})", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text("Tags to download (e.g. scenery order:score)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bulk_download_tags_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pages to fetch (~25 posts/page): $pagesCount")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 2, 3, 5).forEach { p ->
                                FilterChip(
                                    selected = pagesCount == p,
                                    onClick = { pagesCount = p },
                                    label = { Text("${p}p") }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Exclude Blacklisted Tags")
                        Switch(checked = excludeBlacklisted, onCheckedChange = { excludeBlacklisted = it })
                    }
                    Button(
                        onClick = {
                            viewModel.createBulkDownload(tagsInput, pagesCount, excludeBlacklisted)
                            tagsInput = ""
                        },
                        modifier = Modifier.fillMaxWidth().testTag("start_bulk_download_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Bulk Download")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Task Queue (${tasks.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks, key = { it.id }) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(task.tags, style = MaterialTheme.typography.titleMedium)
                                Badge { Text(task.status.name) }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "${task.downloadedItems} / ${task.totalItems} downloaded (${task.failedItems} failed)",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (task.totalItems > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        (task.downloadedItems.toFloat() / task.totalItems.toFloat()).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerScreen(viewModel: BoorusamaViewModel) {
    val records by viewModel.downloadRecords.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads (${records.size})") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (records.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    viewModel.repository.clearDownloadRecords()
                                }
                            }
                        ) {
                            Text("Clear History")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No downloads yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = item.fileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = item.savedPath.ifBlank { item.errorMessage ?: item.status.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Badge { Text(item.status.name) }
                        }
                    }
                }
            }
        }
    }
}
