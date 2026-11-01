package com.degenk.boorusama.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.degenk.boorusama.model.Post
import com.degenk.boorusama.model.PostNote
import com.degenk.boorusama.model.TagCategory
import com.degenk.boorusama.ui.AppScreen
import com.degenk.boorusama.ui.BoorusamaViewModel
import com.degenk.boorusama.ui.theme.LocalTagColors
import com.degenk.boorusama.util.DTextParser
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostDetailsScreen(viewModel: BoorusamaViewModel) {
    val detailsState by viewModel.detailsState.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val posts = detailsState.posts
    if (posts.isEmpty()) {
        LaunchedEffect(Unit) { viewModel.navigateBack() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = detailsState.currentIndex.coerceIn(0, posts.lastIndex),
        pageCount = { posts.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPostDetailsPageChanged(pagerState.currentPage)
    }

    val currentPost = posts.getOrElse(pagerState.currentPage) { posts.first() }
    val isBookmarked = bookmarks.any { it.postId == currentPost.id && it.sourceUrl == currentPost.booruUrl }
    val postNotes = detailsState.notesByPostId[currentPost.id].orEmpty()
    val postComments = detailsState.commentsByPostId[currentPost.id].orEmpty()

    var showInfoSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<PostNote?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Post #${currentPost.id}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${pagerState.currentPage + 1} / ${posts.size} • ${currentPost.width}x${currentPost.height} (${currentPost.format.uppercase()})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("details_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentPost.hasNote || postNotes.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.toggleNotesOverlay() },
                            modifier = Modifier.testTag("toggle_notes_button")
                        ) {
                            Icon(
                                Icons.Default.SpeakerNotes,
                                contentDescription = "Toggle translation notes",
                                tint = if (detailsState.showNotesOverlay) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.openWallpaperEditor(currentPost) },
                        modifier = Modifier.testTag("top_set_wallpaper_button")
                    ) {
                        Icon(
                            Icons.Default.Wallpaper,
                            contentDescription = "Set as Wallpaper",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, currentPost.postLink)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Post #${currentPost.id}"))
                        },
                        modifier = Modifier.testTag("share_post_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share post")
                    }
                    IconButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentPost.postLink)))
                            }
                        },
                        modifier = Modifier.testTag("open_browser_button")
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(
                        onClick = { viewModel.favoritePostOnServer(currentPost) },
                        modifier = Modifier.testTag("favorite_post_button")
                    ) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite")
                    }
                    IconButton(
                        onClick = { viewModel.toggleBookmark(currentPost) },
                        modifier = Modifier.testTag("bookmark_post_button")
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.downloadSinglePost(currentPost) },
                        modifier = Modifier.testTag("download_post_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                    IconButton(
                        onClick = { viewModel.openWallpaperEditor(currentPost) },
                        modifier = Modifier.testTag("set_wallpaper_button")
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = "Set as Wallpaper")
                    }
                    IconButton(
                        onClick = {
                            viewModel.forceLoadComments(currentPost.id)
                            showCommentsSheet = true
                        },
                        modifier = Modifier.testTag("open_comments_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comments")
                    }
                    IconButton(
                        onClick = { showInfoSheet = true },
                        modifier = Modifier.testTag("open_tags_info_button")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Tags & Details")
                    }
                },
                floatingActionButton = {
                    Button(
                        onClick = { viewModel.openWallpaperEditor(currentPost) },
                        modifier = Modifier.testTag("set_wallpaper_fab")
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wallpaper")
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
            // Horizontal Pager for Image / Video with Zoom & Translation Notes
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("post_details_pager")
            ) { page ->
                val post = posts[page]
                val notes = detailsState.notesByPostId[post.id].orEmpty()
                ZoomableMediaContainer(
                    post = post,
                    notes = if (detailsState.showNotesOverlay) notes else emptyList(),
                    autoPlayVideo = settings.autoPlayVideo,
                    onNoteSelected = { selectedNote = it }
                )
            }

            // Quick horizontal tag bar at bottom of image
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tagColors = LocalTagColors.current
                    currentPost.artistTags.forEach { tag ->
                        ColoredTagPill(
                            tag = tag,
                            color = tagColors.artist,
                            onClick = { viewModel.searchByTagFromDetails(tag) },
                            onWikiClick = { viewModel.loadWikiForTag(tag) },
                            onFavoriteTag = { viewModel.viewModelScope.launch { viewModel.repository.saveFavoriteTag(tag) } },
                            onBlacklistTag = { viewModel.viewModelScope.launch { viewModel.repository.addBlacklistedTag(tag) } }
                        )
                    }
                    currentPost.copyrightTags.forEach { tag ->
                        ColoredTagPill(
                            tag = tag,
                            color = tagColors.copyright,
                            onClick = { viewModel.searchByTagFromDetails(tag) },
                            onWikiClick = { viewModel.loadWikiForTag(tag) },
                            onFavoriteTag = { viewModel.viewModelScope.launch { viewModel.repository.saveFavoriteTag(tag) } },
                            onBlacklistTag = { viewModel.viewModelScope.launch { viewModel.repository.addBlacklistedTag(tag) } }
                        )
                    }
                    currentPost.characterTags.forEach { tag ->
                        ColoredTagPill(
                            tag = tag,
                            color = tagColors.character,
                            onClick = { viewModel.searchByTagFromDetails(tag) },
                            onWikiClick = { viewModel.loadWikiForTag(tag) },
                            onFavoriteTag = { viewModel.viewModelScope.launch { viewModel.repository.saveFavoriteTag(tag) } },
                            onBlacklistTag = { viewModel.viewModelScope.launch { viewModel.repository.addBlacklistedTag(tag) } }
                        )
                    }
                    currentPost.generalTags.take(25).forEach { tag ->
                        ColoredTagPill(
                            tag = tag,
                            color = tagColors.general,
                            onClick = { viewModel.searchByTagFromDetails(tag) },
                            onWikiClick = { viewModel.loadWikiForTag(tag) },
                            onFavoriteTag = { viewModel.viewModelScope.launch { viewModel.repository.saveFavoriteTag(tag) } },
                            onBlacklistTag = { viewModel.viewModelScope.launch { viewModel.repository.addBlacklistedTag(tag) } }
                        )
                    }
                }
            }
        }
    }

    // Selected Note Dialog
    selectedNote?.let { note ->
        AlertDialog(
            onDismissRequest = { selectedNote = null },
            title = { Text("Translation Note") },
            text = {
                Text(DTextParser.parseToAnnotatedString(note.body))
            },
            confirmButton = {
                TextButton(onClick = { selectedNote = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Wiki Dialog
    if (detailsState.activeWikiPage != null || detailsState.isLoadingWiki) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissWikiDialog() },
            title = {
                Text(
                    detailsState.activeWikiPage?.title?.replace('_', ' ') ?: "Loading Wiki..."
                )
            },
            text = {
                if (detailsState.isLoadingWiki) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val wiki = detailsState.activeWikiPage
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        if (!wiki?.otherNames.isNullOrEmpty()) {
                            Text(
                                text = "Aliases: ${wiki?.otherNames?.joinToString(", ")}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = DTextParser.parseToAnnotatedString(wiki?.body.orEmpty()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                detailsState.activeWikiPage?.let { wiki ->
                    TextButton(
                        onClick = {
                            viewModel.dismissWikiDialog()
                            viewModel.searchByTagFromDetails(wiki.title)
                        }
                    ) {
                        Text("Search Tag")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissWikiDialog() }) {
                    Text("Close")
                }
            }
        )
    }

    // Full Tags & File Details Sheet
    if (showInfoSheet) {
        ModalBottomSheet(onDismissRequest = { showInfoSheet = false }) {
            val tagColors = LocalTagColors.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Post #${currentPost.id} Metadata", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Rating: ${currentPost.rating.label}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Score: ${currentPost.score} (↑${currentPost.upvotes} / ↓${currentPost.downvotes}) • Favorites: ${currentPost.favCount}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Dimensions: ${currentPost.width} × ${currentPost.height} (${currentPost.format.uppercase()})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (currentPost.fileSize > 0) {
                            val kb = String.format(Locale.US, "%.1f KB", currentPost.fileSize / 1024.0)
                            Text("Size: $kb", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (currentPost.md5.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "MD5: ${currentPost.md5}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(currentPost.md5))
                                        viewModel.showMessage("Copied MD5")
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy MD5", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (currentPost.source.isNotBlank()) {
                            Text(
                                "Source: ${currentPost.source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (currentPost.createdAt.isNotBlank()) {
                            Text("Created: ${currentPost.createdAt}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val sections = listOf(
                    Triple("Artist", currentPost.artistTags, tagColors.artist),
                    Triple("Copyright", currentPost.copyrightTags, tagColors.copyright),
                    Triple("Character", currentPost.characterTags, tagColors.character),
                    Triple("General", currentPost.generalTags, tagColors.general),
                    Triple("Meta", currentPost.metaTags, tagColors.meta)
                )

                sections.forEach { (label, list, color) ->
                    if (list.isNotEmpty()) {
                        Text(
                            text = "$label (${list.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = color,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            list.forEach { tag ->
                                ColoredTagPill(
                                    tag = tag,
                                    color = color,
                                    onClick = {
                                        showInfoSheet = false
                                        viewModel.searchByTagFromDetails(tag)
                                    },
                                    onWikiClick = { viewModel.loadWikiForTag(tag) },
                                    onFavoriteTag = {
                                        viewModel.viewModelScope.launch {
                                            viewModel.repository.saveFavoriteTag(tag)
                                            viewModel.showMessage("Added '$tag' to Favorite Tags")
                                        }
                                    },
                                    onBlacklistTag = {
                                        viewModel.viewModelScope.launch {
                                            viewModel.repository.addBlacklistedTag(tag)
                                            viewModel.showMessage("Blacklisted '$tag'")
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Comments Sheet
    if (showCommentsSheet) {
        ModalBottomSheet(onDismissRequest = { showCommentsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Comments (${postComments.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    detailsState.isLoadingComments -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    postComments.isEmpty() -> {
                        Text(
                            "No comments on Post #${currentPost.id}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.height(360.dp)
                        ) {
                            items(postComments, key = { it.id }) { comment ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = comment.creatorName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Score: ${comment.score}",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = DTextParser.parseToAnnotatedString(comment.body),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
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
private fun ZoomableMediaContainer(
    post: Post,
    notes: List<PostNote>,
    autoPlayVideo: Boolean,
    onNoteSelected: (PostNote) -> Unit
) {
    val context = LocalContext.current
    var scale by remember(post.id) { mutableFloatStateOf(1f) }
    var offset by remember(post.id) { mutableStateOf(Offset.Zero) }

    if (post.isVideo && post.originalImageUrl.endsWith(".mp4", ignoreCase = true)) {
        val exoPlayer = remember(post.id) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(post.originalImageUrl.ifBlank { post.sampleImageUrl }))
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                playWhenReady = autoPlayVideo
                prepare()
            }
        }
        DisposableEffect(post.id) {
            onDispose { exoPlayer.release() }
        }
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(post.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.05f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(post.id) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = newScale
                        if (newScale > 1f) {
                            offset += pan
                        } else {
                            offset = Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val containerWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val containerHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = post.sampleImageUrl.ifBlank { post.originalImageUrl.ifBlank { post.thumbnailImageUrl } },
                    contentDescription = "Post #${post.id}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay Translation Notes proportionally when image dimensions are known
                if (notes.isNotEmpty() && post.width > 0 && post.height > 0) {
                    val imageRatio = post.width.toFloat() / post.height.toFloat()
                    val containerRatio = if (containerHeightPx > 0) containerWidthPx / containerHeightPx else 1f
                    val renderedWidth = if (imageRatio > containerRatio) containerWidthPx else containerHeightPx * imageRatio
                    val renderedHeight = if (imageRatio > containerRatio) containerWidthPx / imageRatio else containerHeightPx
                    val leftPadding = (containerWidthPx - renderedWidth) / 2f
                    val topPadding = (containerHeightPx - renderedHeight) / 2f
                    val density = LocalDensity.current

                    notes.forEach { note ->
                        val noteLeftPx = leftPadding + (note.x.toFloat() / post.width.toFloat()) * renderedWidth
                        val noteTopPx = topPadding + (note.y.toFloat() / post.height.toFloat()) * renderedHeight
                        val noteWidthDp = with(density) {
                            ((note.width.toFloat() / post.width.toFloat()) * renderedWidth).coerceAtLeast(24f).toDp()
                        }
                        val noteHeightDp = with(density) {
                            ((note.height.toFloat() / post.height.toFloat()) * renderedHeight).coerceAtLeast(24f).toDp()
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset { IntOffset(noteLeftPx.roundToInt(), noteTopPx.roundToInt()) }
                                .size(width = noteWidthDp, height = noteHeightDp)
                                .background(Color(0x55FFF59D))
                                .border(1.dp, Color(0xFFFFEE58))
                                .clickable { onNoteSelected(note) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColoredTagPill(
    tag: String,
    color: Color,
    onClick: () -> Unit,
    onWikiClick: () -> Unit = {},
    onFavoriteTag: () -> Unit = {},
    onBlacklistTag: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Surface(
            color = color.copy(alpha = 0.16f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
        ) {
            Text(
                text = tag.replace('_', ' '),
                color = color,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Search '$tag'") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onClick()
                }
            )
            DropdownMenuItem(
                text = { Text("View Wiki Definition") },
                leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onWikiClick()
                }
            )
            DropdownMenuItem(
                text = { Text("Add to Favorite Tags") },
                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onFavoriteTag()
                }
            )
            DropdownMenuItem(
                text = { Text("Add to Blacklist") },
                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onBlacklistTag()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(viewModel: BoorusamaViewModel) {
    val feedState by viewModel.feedState.collectAsState()
    val suggestions by viewModel.autocompleteSuggestions.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val favoriteTags by viewModel.favoriteTags.collectAsState()
    val tagColors = LocalTagColors.current

    var queryText by remember { mutableStateOf(feedState.currentQuery) }

    val currentLastToken = remember(queryText) {
        queryText.substringAfterLast(' ').trim()
    }

    LaunchedEffect(currentLastToken) {
        viewModel.queryAutocomplete(currentLastToken)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("search_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        placeholder = { Text("Search tags (e.g. 1girl -animated order:score)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_query_input"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.navigateTo(AppScreen.HOME)
                                viewModel.loadHomePosts(query = queryText.trim(), page = 1)
                            }
                        ),
                        trailingIcon = {
                            if (queryText.isNotEmpty()) {
                                IconButton(onClick = { queryText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.navigateTo(AppScreen.HOME)
                            viewModel.loadHomePosts(query = queryText.trim(), page = 1)
                        },
                        modifier = Modifier.testTag("execute_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
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
            // Operator & Metatag Quick Bar
            val operators = listOf(
                "-" to "Negate (-)",
                "~" to "OR (~)",
                "*" to "Wildcard (*)",
                "order:score " to "order:score",
                "order:rank " to "order:rank",
                "rating:general " to "rating:general",
                "rating:sensitive " to "rating:sensitive",
                "filetype:mp4 " to "filetype:mp4"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                operators.forEach { (insertText, label) ->
                    AssistChip(
                        onClick = {
                            queryText = if (insertText.length == 1 && !queryText.endsWith(" ") && queryText.isNotEmpty()) {
                                "$queryText $insertText"
                            } else {
                                "${queryText.trimEnd()} $insertText".trimStart()
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Live Autocomplete Suggestions
                if (suggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Autocomplete Suggestions",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(suggestions, key = { it.name }) { item ->
                        val catColor = tagColors.colorFor(item.category)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val prefix = if (currentLastToken.startsWith("-")) "-" else if (currentLastToken.startsWith("~")) "~" else ""
                                    val base = if (' ' in queryText) queryText.substringBeforeLast(' ') + " " else ""
                                    queryText = "$base$prefix${item.name} "
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                                .testTag("suggestion_${item.name}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.displayName,
                                    color = catColor,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                if (!item.antecedent.isNullOrBlank()) {
                                    Text(
                                        text = "alias of ${item.antecedent}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (item.postCount > 0) {
                                Text(
                                    text = "${item.postCount}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }

                // Favorite Tags Section
                if (favoriteTags.isNotEmpty()) {
                    item {
                        Text(
                            text = "Favorite Tags",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            favoriteTags.forEach { fav ->
                                FilterChip(
                                    selected = fav.name in queryText.split(" "),
                                    onClick = {
                                        queryText = "${queryText.trim()} ${fav.name} ".trimStart()
                                    },
                                    label = { Text(fav.name) },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }

                // Search History Section
                if (searchHistory.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Searches", style = MaterialTheme.typography.titleSmall)
                            TextButton(
                                onClick = {
                                    viewModel.viewModelScope.launch { viewModel.repository.clearSearchHistory() }
                                }
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear All")
                            }
                        }
                    }
                    items(searchHistory, key = { it.query }) { historyItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.navigateTo(AppScreen.HOME)
                                    viewModel.loadHomePosts(query = historyItem.query, page = 1)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(historyItem.query, style = MaterialTheme.typography.bodyLarge)
                            }
                            IconButton(
                                onClick = {
                                    viewModel.viewModelScope.launch {
                                        viewModel.repository.deleteSearchHistory(historyItem.query)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
