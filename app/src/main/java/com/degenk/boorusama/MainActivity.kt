package com.degenk.boorusama

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.degenk.boorusama.ui.AppScreen
import com.degenk.boorusama.ui.BoorusamaViewModel
import com.degenk.boorusama.ui.screens.AppLockScreen
import com.degenk.boorusama.ui.screens.ArtistDetailScreen
import com.degenk.boorusama.ui.screens.ArtistsScreen
import com.degenk.boorusama.ui.screens.BlacklistScreen
import com.degenk.boorusama.ui.screens.BookmarksScreen
import com.degenk.boorusama.ui.screens.BooruConfigEditorScreen
import com.degenk.boorusama.ui.screens.BooruConfigManageScreen
import com.degenk.boorusama.ui.screens.BulkDownloadScreen
import com.degenk.boorusama.ui.screens.DebugLogsScreen
import com.degenk.boorusama.ui.screens.DownloadManagerScreen
import com.degenk.boorusama.ui.screens.ExploreScreen
import com.degenk.boorusama.ui.screens.FavoriteGroupsScreen
import com.degenk.boorusama.ui.screens.FavoriteTagsScreen
import com.degenk.boorusama.ui.screens.ForumPostsScreen
import com.degenk.boorusama.ui.screens.ForumsScreen
import com.degenk.boorusama.ui.screens.HomeScreen
import com.degenk.boorusama.ui.screens.PoolDetailScreen
import com.degenk.boorusama.ui.screens.PoolsScreen
import com.degenk.boorusama.ui.screens.PostDetailsScreen
import com.degenk.boorusama.ui.screens.SavedSearchesScreen
import com.degenk.boorusama.ui.screens.SearchScreen
import com.degenk.boorusama.ui.screens.SettingsScreen
import com.degenk.boorusama.ui.screens.WallpaperEditorScreen
import com.degenk.boorusama.ui.theme.BoorusamaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BoorusamaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()

            LaunchedEffect(settings.privacyScreenInRecents) {
                if (settings.privacyScreenInRecents) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            BoorusamaTheme(
                themePreset = settings.themePreset,
                dynamicColorEnabled = settings.dynamicColorEnabled
            ) {
                BoorupaperAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BoorupaperAppRoot(viewModel: BoorusamaViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val feedState by viewModel.feedState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeMessage()
        }
    }

    BackHandler(enabled = currentScreen != AppScreen.HOME || feedState.isMultiSelectMode) {
        viewModel.navigateBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { rootPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(rootPadding)
        ) {
            if (settings.appLockEnabled && !isUnlocked) {
                AppLockScreen(viewModel = viewModel)
            } else {
                when (currentScreen) {
                    AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                    AppScreen.SEARCH -> SearchScreen(viewModel = viewModel)
                    AppScreen.POST_DETAILS -> PostDetailsScreen(viewModel = viewModel)
                    AppScreen.WALLPAPER_EDITOR -> WallpaperEditorScreen(viewModel = viewModel)
                    AppScreen.EXPLORE -> ExploreScreen(viewModel = viewModel)
                    AppScreen.POOLS -> PoolsScreen(viewModel = viewModel)
                    AppScreen.POOL_DETAIL -> PoolDetailScreen(viewModel = viewModel)
                    AppScreen.FORUMS -> ForumsScreen(viewModel = viewModel)
                    AppScreen.FORUM_POSTS -> ForumPostsScreen(viewModel = viewModel)
                    AppScreen.ARTISTS -> ArtistsScreen(viewModel = viewModel)
                    AppScreen.ARTIST_DETAIL -> ArtistDetailScreen(viewModel = viewModel)
                    AppScreen.FAVORITE_GROUPS -> FavoriteGroupsScreen(viewModel = viewModel)
                    AppScreen.SAVED_SEARCHES -> SavedSearchesScreen(viewModel = viewModel)
                    AppScreen.BOOKMARKS -> BookmarksScreen(viewModel = viewModel)
                    AppScreen.BLACKLIST -> BlacklistScreen(viewModel = viewModel)
                    AppScreen.FAVORITE_TAGS -> FavoriteTagsScreen(viewModel = viewModel)
                    AppScreen.BULK_DOWNLOADS -> BulkDownloadScreen(viewModel = viewModel)
                    AppScreen.DOWNLOAD_MANAGER -> DownloadManagerScreen(viewModel = viewModel)
                    AppScreen.BOORU_CONFIGS -> BooruConfigManageScreen(viewModel = viewModel)
                    AppScreen.BOORU_CONFIG_EDIT -> BooruConfigEditorScreen(viewModel = viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    AppScreen.DEBUG_LOGS -> DebugLogsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
