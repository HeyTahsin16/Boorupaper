package com.degenk.boorusama.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SavedSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.degenk.boorusama.R
import com.degenk.boorusama.model.AppSettings
import com.degenk.boorusama.model.BooruConfig
import com.degenk.boorusama.model.ImageGridType
import com.degenk.boorusama.model.ImageQuality
import com.degenk.boorusama.model.PageMode
import com.degenk.boorusama.model.Post
import com.degenk.boorusama.model.Rating
import com.degenk.boorusama.ui.AppScreen
import com.degenk.boorusama.ui.BoorusamaViewModel
import com.degenk.boorusama.ui.PostsFeedUiState
import com.degenk.boorusama.ui.theme.ratingBadgeColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: BoorusamaViewModel) {
    val configs by viewModel.configs.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()
    val feedState by viewModel.feedState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val favoriteTags by viewModel.favoriteTags.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showBlacklistBreakdownSheet by remember { mutableStateOf(false) }
    var showProfileSwitcherSheet by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideBarDrawerContent(
                configs = configs,
                activeConfig = activeConfig,
                onSelectConfig = { cfg ->
                    viewModel.selectBooruConfig(cfg)
                    scope.launch { drawerState.close() }
                },
                onNavigate = { screen ->
                    scope.launch { drawerState.close() }
                    viewModel.navigateTo(screen)
                },
                onAddConfig = {
                    scope.launch { drawerState.close() }
                    viewModel.startCreateBooruConfig()
                }
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpanded = maxWidth >= 720.dp
            Row(modifier = Modifier.fillMaxSize()) {
                if (isExpanded) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        NavigationRailItem(
                            selected = true,
                            onClick = { viewModel.loadHomePosts(query = "", page = 1) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") }
                        )
                        if (activeConfig.booruType.hasExplore) {
                            NavigationRailItem(
                                selected = false,
                                onClick = { viewModel.navigateTo(AppScreen.EXPLORE) },
                                icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                                label = { Text("Explore") }
                            )
                        }
                        if (activeConfig.booruType.hasPools) {
                            NavigationRailItem(
                                selected = false,
                                onClick = { viewModel.navigateTo(AppScreen.POOLS) },
                                icon = { Icon(Icons.Default.Collections, contentDescription = "Pools") },
                                label = { Text("Pools") }
                            )
                        }
                        NavigationRailItem(
                            selected = false,
                            onClick = { viewModel.navigateTo(AppScreen.BOOKMARKS) },
                            icon = { Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks") },
                            label = { Text("Bookmarks") }
                        )
                        NavigationRailItem(
                            selected = false,
                            onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }

                Scaffold(
                    topBar = {
                        if (feedState.isMultiSelectMode) {
                            TopAppBar(
                                title = { Text("${feedState.selectedPostIds.size} selected") },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { viewModel.clearMultiSelect() },
                                        modifier = Modifier.testTag("clear_multiselect_button")
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Cancel selection")
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = { viewModel.selectAllVisiblePosts() },
                                        modifier = Modifier.testTag("select_all_posts_button")
                                    ) {
                                        Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                                    }
                                    IconButton(
                                        onClick = { viewModel.bookmarkSelectedPosts() },
                                        modifier = Modifier.testTag("bookmark_selected_button")
                                    ) {
                                        Icon(Icons.Default.Bookmark, contentDescription = "Bookmark selected")
                                    }
                                    IconButton(
                                        onClick = { viewModel.downloadSelectedPosts() },
                                        modifier = Modifier.testTag("download_selected_button")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Download selected")
                                    }
                                }
                            )
                        } else {
                            TopAppBar(
                                navigationIcon = {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("open_drawer_button")
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                                    }
                                },
                                title = {
                                    if (configs.isEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Image(
                                                painter = painterResource(id = R.drawable.logo_icon),
                                                contentDescription = "Boorupaper Logo",
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = stringResource(R.string.app_name),
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { showProfileSwitcherSheet = true }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                .testTag("active_booru_selector")
                                        ) {
                                            Column {
                                                Text(
                                                    text = activeConfig.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${activeConfig.booruType.displayName} • ${activeConfig.url.removePrefix("https://").trimEnd('/')}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Default.SwapHoriz,
                                                contentDescription = "Switch Booru Profile",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    if (configs.isEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.startCreateBooruConfig() },
                                            modifier = Modifier.testTag("top_add_profile_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_booru))
                                        }
                                    } else {
                                        IconButton(
                                            onClick = { viewModel.navigateTo(AppScreen.SEARCH) },
                                            modifier = Modifier.testTag("open_search_button")
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_hint))
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.loadHomePosts(
                                                    query = feedState.currentQuery,
                                                    page = feedState.currentPage,
                                                    append = false
                                                )
                                            },
                                            modifier = Modifier.testTag("refresh_feed_button")
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Refresh posts")
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    if (configs.isEmpty()) {
                        // Start screen when no Booru profiles are added yet
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.logo_icon),
                                        contentDescription = "Boorupaper Logo",
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.empty_no_profiles_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.empty_no_profiles_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { viewModel.startCreateBooruConfig() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("add_first_booru_profile_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.action_add_booru))
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Persistent Search Bar & Quick Tag Chips
                            if (settings.persistentSearchBar) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable { viewModel.navigateTo(AppScreen.SEARCH) }
                                        .testTag("home_search_bar_pill"),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = feedState.currentQuery.ifBlank { stringResource(R.string.search_hint) },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (feedState.currentQuery.isBlank()) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (feedState.currentQuery.isNotBlank()) {
                                            IconButton(
                                                onClick = { viewModel.loadHomePosts(query = "", page = 1) },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .testTag("clear_active_query_button")
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Clear search",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Quick Filter Row: Favorite tags, Explore, Pools, Blacklist breakdown counter
                            val totalHidden = feedState.blacklistedBreakdown.values.sum()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (totalHidden > 0) {
                                    AssistChip(
                                        onClick = { showBlacklistBreakdownSheet = true },
                                        label = { Text("$totalHidden hidden") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.testTag("blacklisted_counter_chip")
                                    )
                                }
                                FilterChip(
                                    selected = feedState.currentQuery == "order:rank",
                                    onClick = {
                                        val next = if (feedState.currentQuery == "order:rank") "" else "order:rank"
                                        viewModel.loadHomePosts(query = next, page = 1)
                                    },
                                    label = { Text("Hot") },
                                    modifier = Modifier.testTag("quick_filter_hot")
                                )
                                FilterChip(
                                    selected = feedState.currentQuery == "order:score",
                                    onClick = {
                                        val next = if (feedState.currentQuery == "order:score") "" else "order:score"
                                        viewModel.loadHomePosts(query = next, page = 1)
                                    },
                                    label = { Text("Top Score") },
                                    modifier = Modifier.testTag("quick_filter_score")
                                )
                                favoriteTags.forEach { fav ->
                                    FilterChip(
                                        selected = feedState.currentQuery == fav.name,
                                        onClick = {
                                            val next = if (feedState.currentQuery == fav.name) "" else fav.name
                                            viewModel.loadHomePosts(query = next, page = 1)
                                        },
                                        label = { Text(fav.name.replace('_', ' ')) }
                                    )
                                }
                            }

                            // Main Post Feed
                            Box(modifier = Modifier.weight(1f)) {
                                when {
                                    feedState.isLoading -> {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.testTag("feed_loading_indicator"))
                                        }
                                    }
                                    feedState.error != null && feedState.visiblePosts.isEmpty() -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = feedState.error ?: stringResource(R.string.error_network),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.loadHomePosts(
                                                        query = feedState.currentQuery,
                                                        page = feedState.currentPage
                                                    )
                                                },
                                                modifier = Modifier.testTag("retry_feed_button")
                                            ) {
                                                Text(stringResource(R.string.action_retry))
                                            }
                                        }
                                    }
                                    feedState.visiblePosts.isEmpty() -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.empty_posts),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            if (totalHidden > 0) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedButton(onClick = { showBlacklistBreakdownSheet = true }) {
                                                    Text("Inspect $totalHidden Blacklisted Posts")
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        PostGridView(
                                            posts = feedState.visiblePosts,
                                            settings = settings,
                                            feedState = feedState,
                                            onPostClick = { index ->
                                                if (feedState.isMultiSelectMode) {
                                                    viewModel.togglePostSelection(feedState.visiblePosts[index].id)
                                                } else {
                                                    viewModel.openPostDetails(feedState.visiblePosts, index)
                                                }
                                            },
                                            onPostLongClick = { post ->
                                                viewModel.togglePostSelection(post.id)
                                            },
                                            onLoadMore = { viewModel.loadNextPageOrAppend() },
                                            onPageChange = { page -> viewModel.jumpToPage(page) }
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

    if (showBlacklistBreakdownSheet) {
        ModalBottomSheet(onDismissRequest = { showBlacklistBreakdownSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Blacklisted Posts Breakdown",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Tap a rule to temporarily reveal matching posts in this feed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                feedState.blacklistedBreakdown.forEach { (rule, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleTemporaryRuleWhitelist(rule) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = rule, style = MaterialTheme.typography.bodyLarge)
                        Badge { Text("$count") }
                    }
                    HorizontalDivider()
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showProfileSwitcherSheet) {
        ModalBottomSheet(onDismissRequest = { showProfileSwitcherSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Switch Booru Profile", style = MaterialTheme.typography.titleLarge)
                    Row {
                        TextButton(
                            onClick = {
                                showProfileSwitcherSheet = false
                                viewModel.startCreateBooruConfig()
                            }
                        ) {
                            Text("Add New")
                        }
                        TextButton(
                            onClick = {
                                showProfileSwitcherSheet = false
                                viewModel.navigateTo(AppScreen.BOORU_CONFIGS)
                            }
                        ) {
                            Text("Manage All")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                configs.forEach { cfg ->
                    val isSelected = cfg.id == activeConfig.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                showProfileSwitcherSheet = false
                                viewModel.selectBooruConfig(cfg)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cfg.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${cfg.booruType.displayName} • ${cfg.url}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SideBarDrawerContent(
    configs: List<BooruConfig>,
    activeConfig: BooruConfig,
    onSelectConfig: (BooruConfig) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onAddConfig: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            // Active Booru Header & Quick Switcher
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_icon),
                        contentDescription = "Boorupaper Logo",
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Boorupaper",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (configs.isEmpty()) {
                        "No Booru profile configured"
                    } else {
                        "Active: ${activeConfig.name} (${activeConfig.booruType.displayName})"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    configs.forEach { cfg ->
                        FilterChip(
                            selected = cfg.id == activeConfig.id,
                            onClick = { onSelectConfig(cfg) },
                            label = { Text(cfg.name) }
                        )
                    }
                    AssistChip(
                        onClick = onAddConfig,
                        label = { Text("Add") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("drawer_add_booru_button")
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Site Discovery Section
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_home)) },
                selected = true,
                onClick = { onNavigate(AppScreen.HOME) },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_home")
            )
            if (activeConfig.booruType.hasExplore) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_explore)) },
                    selected = false,
                    onClick = { onNavigate(AppScreen.EXPLORE) },
                    icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_explore")
                )
            }
            if (activeConfig.booruType.hasPools) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_pools)) },
                    selected = false,
                    onClick = { onNavigate(AppScreen.POOLS) },
                    icon = { Icon(Icons.Default.Collections, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_pools")
                )
            }
            if (activeConfig.booruType.hasArtists) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_artists)) },
                    selected = false,
                    onClick = { onNavigate(AppScreen.ARTISTS) },
                    icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_artists")
                )
            }
            if (activeConfig.booruType.hasForum) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nav_forums)) },
                    selected = false,
                    onClick = { onNavigate(AppScreen.FORUMS) },
                    icon = { Icon(Icons.Default.Forum, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_forums")
                )
            }
            if (activeConfig.booruType.hasFavoriteGroups) {
                NavigationDrawerItem(
                    label = { Text("Favorite Groups") },
                    selected = false,
                    onClick = { onNavigate(AppScreen.FAVORITE_GROUPS) },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            if (activeConfig.booruType.hasSavedSearches) {
                NavigationDrawerItem(
                    label = { Text("Saved Searches") },
                    selected = false,
                    onClick = { onNavigate(AppScreen.SAVED_SEARCHES) },
                    icon = { Icon(Icons.Default.SavedSearch, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Personal Collection & Tools Section
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_bookmarks)) },
                selected = false,
                onClick = { onNavigate(AppScreen.BOOKMARKS) },
                icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_bookmarks")
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_favorite_tags)) },
                selected = false,
                onClick = { onNavigate(AppScreen.FAVORITE_TAGS) },
                icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_favorite_tags")
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_blacklisted_tags)) },
                selected = false,
                onClick = { onNavigate(AppScreen.BLACKLIST) },
                icon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_blacklist")
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_bulk_downloads)) },
                selected = false,
                onClick = { onNavigate(AppScreen.BULK_DOWNLOADS) },
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_bulk_downloads")
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_download_manager)) },
                selected = false,
                onClick = { onNavigate(AppScreen.DOWNLOAD_MANAGER) },
                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_downloads")
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_booru_configs)) },
                selected = false,
                onClick = { onNavigate(AppScreen.BOORU_CONFIGS) },
                icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_booru_configs")
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_settings)) },
                selected = false,
                onClick = { onNavigate(AppScreen.SETTINGS) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_nav_settings")
            )
        }
    }
}

@Composable
fun PostGridView(
    posts: List<Post>,
    settings: AppSettings,
    feedState: PostsFeedUiState? = null,
    onPostClick: (Int) -> Unit,
    onPostLongClick: (Post) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onPageChange: (Int) -> Unit = {}
) {
    val spacing = settings.gridSpacingDp.dp
    val columns = settings.gridColumns.coerceIn(1, 6)

    if (settings.gridType == ImageGridType.MASONRY) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .testTag("post_masonry_grid"),
            contentPadding = PaddingValues(spacing),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalItemSpacing = spacing
        ) {
            itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
                if (settings.pageMode == PageMode.INFINITE && index >= posts.size - 6) {
                    LaunchedEffect(posts.size) { onLoadMore() }
                }
                val isSelected = feedState?.selectedPostIds?.contains(post.id) == true
                PostGridThumbnailCard(
                    post = post,
                    settings = settings,
                    isSelected = isSelected,
                    useNaturalAspectRatio = true,
                    onClick = { onPostClick(index) },
                    onLongClick = { onPostLongClick(post) }
                )
            }

            if (feedState != null) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    PaginationFooter(
                        feedState = feedState,
                        pageMode = settings.pageMode,
                        onLoadMore = onLoadMore,
                        onPageChange = onPageChange
                    )
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .testTag("post_standard_grid"),
            contentPadding = PaddingValues(spacing),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
                if (settings.pageMode == PageMode.INFINITE && index >= posts.size - 6) {
                    LaunchedEffect(posts.size) { onLoadMore() }
                }
                val isSelected = feedState?.selectedPostIds?.contains(post.id) == true
                PostGridThumbnailCard(
                    post = post,
                    settings = settings,
                    isSelected = isSelected,
                    useNaturalAspectRatio = false,
                    onClick = { onPostClick(index) },
                    onLongClick = { onPostLongClick(post) }
                )
            }

            if (feedState != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PaginationFooter(
                        feedState = feedState,
                        pageMode = settings.pageMode,
                        onLoadMore = onLoadMore,
                        onPageChange = onPageChange
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PostGridThumbnailCard(
    post: Post,
    settings: AppSettings,
    isSelected: Boolean,
    useNaturalAspectRatio: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cornerShape = RoundedCornerShape(settings.imageBorderRadiusDp.dp)
    val imageUrl = when (settings.thumbnailQuality) {
        ImageQuality.LOW -> post.thumbnailImageUrl
        ImageQuality.MEDIUM -> post.sampleImageUrl.ifBlank { post.thumbnailImageUrl }
        ImageQuality.ORIGINAL -> post.originalImageUrl.ifBlank { post.sampleImageUrl }
    }
    val shouldBlur = settings.blurExplicitMedia && (post.rating == Rating.EXPLICIT || post.rating == Rating.QUESTIONABLE)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (useNaturalAspectRatio) post.aspectRatio else 0.78f)
            .clip(cornerShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, cornerShape)
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("post_card_${post.id}")
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Post #${post.id}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (shouldBlur) Modifier.blur(18.dp) else Modifier)
        )

        // Top badges: Video / GIF indicator + Rating indicator
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (post.isAnimated) {
                Surface(
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayCircleFilled,
                            contentDescription = "Animated",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = post.format.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
            Surface(
                color = ratingBadgeColor(post.rating).copy(alpha = 0.85f),
                shape = CircleShape
            ) {
                Text(
                    text = post.rating.shortCode.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Bottom bar: Score & comment/note indicators
        if (settings.showScoresOnGrid && (post.score != 0 || post.favCount > 0)) {
            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(topEnd = 8.dp),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${post.score}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (post.favCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF8A8B),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${post.favCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            )
        }
    }
}

@Composable
private fun PaginationFooter(
    feedState: PostsFeedUiState,
    pageMode: PageMode,
    onLoadMore: () -> Unit,
    onPageChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            feedState.isAppending -> {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
            pageMode == PageMode.PAGINATED -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onPageChange((feedState.currentPage - 1).coerceAtLeast(1)) },
                        enabled = feedState.currentPage > 1,
                        modifier = Modifier.testTag("prev_page_button")
                    ) {
                        Text("Previous")
                    }
                    Text(
                        text = "Page ${feedState.currentPage}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Button(
                        onClick = { onPageChange(feedState.currentPage + 1) },
                        modifier = Modifier.testTag("next_page_button")
                    ) {
                        Text("Next")
                    }
                }
            }
            else -> {
                OutlinedButton(
                    onClick = onLoadMore,
                    modifier = Modifier.testTag("load_more_posts_button")
                ) {
                    Text("Load More (Page ${feedState.currentPage + 1})")
                }
            }
        }
    }
}
