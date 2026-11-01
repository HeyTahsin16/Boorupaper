package com.degenk.boorusama.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import com.degenk.boorusama.NativeAndroidBridge
import com.degenk.boorusama.model.BooruConfigBannedPostVisibility
import com.degenk.boorusama.model.BooruConfigDeletedItemBehavior
import com.degenk.boorusama.model.BooruConfigRatingFilter
import com.degenk.boorusama.model.BooruType
import com.degenk.boorusama.model.ImageGridType
import com.degenk.boorusama.model.ImageQuality
import com.degenk.boorusama.model.PageMode
import com.degenk.boorusama.model.Post
import com.degenk.boorusama.model.Rating
import com.degenk.boorusama.model.ThemePreset
import com.degenk.boorusama.ui.AppScreen
import com.degenk.boorusama.ui.BoorusamaViewModel
import com.degenk.boorusama.util.BooruRegistry
import com.degenk.boorusama.util.FilenameGenerator
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooruConfigManageScreen(viewModel: BoorusamaViewModel) {
    val configs by viewModel.configs.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booru Profiles (${configs.size})") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.startCreateBooruConfig() },
                modifier = Modifier.testTag("create_booru_profile_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Booru Profile")
            }
        }
    ) { innerPadding ->
        if (configs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "No Booru Profiles Added",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tap below to add your first Booru profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { viewModel.startCreateBooruConfig() },
                        modifier = Modifier.testTag("manage_empty_add_profile_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Booru Profile")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(configs, key = { it.id }) { cfg ->
                    val isCurrent = cfg.id == activeConfig.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectBooruConfig(cfg) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cfg.name, style = MaterialTheme.typography.titleMedium)
                                    if (isCurrent) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Badge { Text("ACTIVE") }
                                    }
                                }
                                Text(
                                    "${cfg.booruType.displayName} • ${cfg.url}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Rating Filter: ${cfg.ratingFilter.label}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { viewModel.startEditBooruConfig(cfg) },
                                    modifier = Modifier.testTag("edit_config_${cfg.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                                }
                                IconButton(
                                    onClick = { viewModel.deleteBooruConfig(cfg.id) },
                                    modifier = Modifier.testTag("delete_config_${cfg.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete profile")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BooruConfigEditorScreen(viewModel: BoorusamaViewModel) {
    val initial by viewModel.editingConfig.collectAsState()
    var draft by remember(initial) {
        mutableStateOf(initial ?: com.degenk.boorusama.model.BooruConfig())
    }
    var showWebViewCookieModal by remember { mutableStateOf(false) }

    val samplePost = remember {
        Post(
            id = 8421054,
            booruUrl = draft.url,
            thumbnailImageUrl = "",
            sampleImageUrl = "",
            originalImageUrl = "",
            tags = listOf("1girl", "solo", "hatsune_miku", "vocaloid"),
            artistTags = listOf("ke-ta"),
            characterTags = listOf("hatsune_miku"),
            copyrightTags = listOf("vocaloid"),
            rating = Rating.GENERAL,
            score = 240,
            width = 1920,
            height = 1080,
            format = "png",
            md5 = "a1b2c3d4e5f60718293a4b5c6d7e8f90"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (draft.id == 0) "Create Booru Profile" else "Edit ${draft.name}") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveEditingConfig(draft) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_booru_config_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = { Text("Profile Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("config_name_input")
            )

            OutlinedTextField(
                value = draft.url,
                onValueChange = { url ->
                    val detected = BooruRegistry.detectBooruTypeFromUrl(url)
                    draft = draft.copy(url = url, booruId = detected.id, booruIdHint = detected.id)
                },
                label = { Text("Site URL (https://...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("config_url_input")
            )

            // Engine Selector
            Text("Booru Engine Type", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BooruType.entries.forEach { engine ->
                    FilterChip(
                        selected = draft.booruIdHint == engine.id,
                        onClick = { draft = draft.copy(booruId = engine.id, booruIdHint = engine.id) },
                        label = { Text(engine.displayName) }
                    )
                }
            }

            HorizontalDivider()

            // Authentication
            Text("Authentication (Optional)", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = draft.login,
                onValueChange = { draft = draft.copy(login = it) },
                label = { Text("Username / User ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("config_login_input")
            )
            OutlinedTextField(
                value = draft.apiKey,
                onValueChange = { draft = draft.copy(apiKey = it) },
                label = { Text("API Key / Password Hash") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("config_apikey_input")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft.cookie,
                    onValueChange = { draft = draft.copy(cookie = it) },
                    label = { Text("Session Cookie (cf_clearance / pass_hash)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { showWebViewCookieModal = true },
                    modifier = Modifier.testTag("grab_webview_cookie_button")
                ) {
                    Icon(Icons.Default.Cookie, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WebView")
                }
            }

            HorizontalDivider()

            // Rating & Content Visibility Filters
            Text("Content & Rating Filter", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BooruConfigRatingFilter.entries.forEach { rf ->
                    FilterChip(
                        selected = draft.ratingFilter == rf,
                        onClick = { draft = draft.copy(ratingFilter = rf) },
                        label = { Text(rf.label) }
                    )
                }
            }
            if (draft.ratingFilter == BooruConfigRatingFilter.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Rating.entries.forEach { r ->
                        val isHidden = r in draft.granularRatingFilters
                        FilterChip(
                            selected = isHidden,
                            onClick = {
                                val next = if (isHidden) draft.granularRatingFilters - r else draft.granularRatingFilters + r
                                draft = draft.copy(granularRatingFilters = next)
                            },
                            label = { Text("Hide ${r.label}") }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BooruConfigDeletedItemBehavior.entries.forEach { beh ->
                    FilterChip(
                        selected = draft.deletedItemBehavior == beh,
                        onClick = { draft = draft.copy(deletedItemBehavior = beh) },
                        label = { Text(beh.label) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BooruConfigBannedPostVisibility.entries.forEach { vis ->
                    FilterChip(
                        selected = draft.bannedPostVisibility == vis,
                        onClick = { draft = draft.copy(bannedPostVisibility = vis) },
                        label = { Text(vis.label) }
                    )
                }
            }

            OutlinedTextField(
                value = draft.alwaysIncludeTags,
                onValueChange = { draft = draft.copy(alwaysIncludeTags = it) },
                label = { Text("Always Include Tags (appended to every search)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // Custom Filename Generator Format
            Text("Custom Download Filename Format", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = draft.customDownloadFileNameFormat,
                onValueChange = { draft = draft.copy(customDownloadFileNameFormat = it) },
                label = { Text("Single Download Format") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("config_filename_format_input")
            )
            val previewName = remember(draft.customDownloadFileNameFormat) {
                FilenameGenerator.generate(draft.customDownloadFileNameFormat, samplePost, "hatsune_miku")
            }
            Text(
                text = "Preview: $previewName",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("{id}", "{md5:maxlength=8}", "{artist}", "{character}", "{copyright}", "{rating}", "{width}x{height}", "{ext}").forEach { token ->
                    AssistChip(
                        onClick = {
                            draft = draft.copy(
                                customDownloadFileNameFormat = draft.customDownloadFileNameFormat + token
                            )
                        },
                        label = { Text(token) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showWebViewCookieModal) {
        AlertDialog(
            onDismissRequest = { showWebViewCookieModal = false },
            title = { Text("Login / Cloudflare Cookie Grabber") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        val cookie = NativeAndroidBridge.getCookiesForUrl(url ?: draft.url)
                                        if (!cookie.isNullOrBlank()) {
                                            draft = draft.copy(cookie = cookie)
                                        }
                                    }
                                }
                                loadUrl(draft.url)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cookie = NativeAndroidBridge.getCookiesForUrl(draft.url)
                        if (!cookie.isNullOrBlank()) {
                            draft = draft.copy(cookie = cookie)
                        }
                        showWebViewCookieModal = false
                    }
                ) {
                    Text("Use Captured Cookies")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebViewCookieModal = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: BoorusamaViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var cacheBytes by remember { mutableStateOf(viewModel.repository.getCacheSizeBytes()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("settings_back_button")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Appearance & Kurumi Themes
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Appearance & Kurumi Theme", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = settings.themePreset == preset,
                                onClick = {
                                    viewModel.repository.updateSettings { it.copy(themePreset = preset) }
                                },
                                label = { Text(preset.displayName) },
                                modifier = Modifier.testTag("theme_preset_${preset.id}")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Android 12+ Material You Dynamic Colors")
                        Switch(
                            checked = settings.dynamicColorEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.repository.updateSettings { it.copy(dynamicColorEnabled = enabled) }
                            }
                        )
                    }
                }
            }

            // 2. Grid & Listing Layout
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Image Grid & Pagination", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageGridType.entries.forEach { gt ->
                            FilterChip(
                                selected = settings.gridType == gt,
                                onClick = { viewModel.repository.updateSettings { it.copy(gridType = gt) } },
                                label = { Text(gt.label) }
                            )
                        }
                    }
                    Text("Columns: ${settings.gridColumns}")
                    Slider(
                        value = settings.gridColumns.toFloat(),
                        onValueChange = { v ->
                            viewModel.repository.updateSettings { it.copy(gridColumns = v.toInt().coerceIn(1, 5)) }
                        },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageMode.entries.forEach { pm ->
                            FilterChip(
                                selected = settings.pageMode == pm,
                                onClick = { viewModel.repository.updateSettings { it.copy(pageMode = pm) } },
                                label = { Text(pm.label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Scores & Favorites on Thumbnails")
                        Switch(
                            checked = settings.showScoresOnGrid,
                            onCheckedChange = { v ->
                                viewModel.repository.updateSettings { it.copy(showScoresOnGrid = v) }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Blur Questionable / Explicit Thumbnails")
                        Switch(
                            checked = settings.blurExplicitMedia,
                            onCheckedChange = { v ->
                                viewModel.repository.updateSettings { it.copy(blurExplicitMedia = v) }
                            }
                        )
                    }
                }
            }

            // 3. Image Viewer & Download Quality
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Viewer & Downloads", style = MaterialTheme.typography.titleMedium)
                    Text("Download Quality", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageQuality.entries.forEach { q ->
                            FilterChip(
                                selected = settings.downloadQuality == q,
                                onClick = { viewModel.repository.updateSettings { it.copy(downloadQuality = q) } },
                                label = { Text(q.label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Organize Downloads in Site Subfolders")
                        Switch(
                            checked = settings.downloadSubfolderBySite,
                            onCheckedChange = { v ->
                                viewModel.repository.updateSettings { it.copy(downloadSubfolderBySite = v) }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Translation Notes Overlay by Default")
                        Switch(
                            checked = settings.showNotesOverlayByDefault,
                            onCheckedChange = { v ->
                                viewModel.repository.updateSettings { it.copy(showNotesOverlayByDefault = v) }
                            }
                        )
                    }
                }
            }

            // 4. Privacy & App Lock
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Privacy & Security", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Incognito Search (Disable Search History)")
                        Switch(
                            checked = settings.enableIncognitoSearchHistory,
                            onCheckedChange = { v ->
                                viewModel.repository.updateSettings { it.copy(enableIncognitoSearchHistory = v) }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hide App Preview in Recent Apps Switcher")
                        Switch(
                            checked = settings.privacyScreenInRecents,
                            onCheckedChange = { v ->
                                viewModel.repository.updateSettings { it.copy(privacyScreenInRecents = v) }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable PIN / Biometric App Lock")
                        Switch(
                            checked = settings.appLockEnabled,
                            onCheckedChange = { v ->
                                viewModel.repository.updateSettings { it.copy(appLockEnabled = v) }
                            }
                        )
                    }
                    if (settings.appLockEnabled) {
                        OutlinedTextField(
                            value = settings.appLockPin,
                            onValueChange = { pin ->
                                viewModel.repository.updateSettings { it.copy(appLockPin = pin.take(8)) }
                            },
                            label = { Text("4-8 Digit PIN Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 5. Data, Cache, Backup & Restore, Diagnostics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Data, Storage & Backup", style = MaterialTheme.typography.titleMedium)
                    val mb = String.format(Locale.US, "%.2f MB", cacheBytes / (1024.0 * 1024.0))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Temporary Cache Size: $mb")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { NativeAndroidBridge.openCacheDocumentsTree(context) }
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Browse")
                            }
                            Button(
                                onClick = {
                                    viewModel.viewModelScope.launch {
                                        viewModel.repository.clearImageCache()
                                        cacheBytes = viewModel.repository.getCacheSizeBytes()
                                        viewModel.showMessage("Cleared image cache")
                                    }
                                }
                            ) {
                                Text("Clear")
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    backupJsonText = viewModel.repository.exportBackupJson()
                                    showBackupDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("backup_restore_button")
                        ) {
                            Text("Backup / Restore JSON")
                        }
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(AppScreen.DEBUG_LOGS) },
                            modifier = Modifier.weight(1f).testTag("open_debug_logs_button")
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Debug Logs")
                        }
                    }

                    OutlinedButton(
                        onClick = { showChangelogDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Boorupaper v4.6.1+188 Release Notes & About")
                    }
                }
            }
        }
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup & Restore Data (JSON)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Copy this JSON to back up your Booru profiles, bookmarks, blacklisted tags, favorite tags, and settings, or paste a backup JSON to restore.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = backupJsonText,
                        onValueChange = { backupJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(backupJsonText))
                            viewModel.showMessage("Copied backup JSON to clipboard")
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = {
                            viewModel.viewModelScope.launch {
                                val ok = viewModel.repository.importBackupJson(backupJsonText)
                                viewModel.showMessage(if (ok) "Restored backup successfully" else "Invalid backup JSON format")
                                if (ok) showBackupDialog = false
                            }
                        }
                    ) {
                        Text("Restore")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Close") }
            }
        )
    }

    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = { Text("Boorupaper 4.6.1+188") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("A feature-rich multi-engine booru client and wallpaper studio built with Kotlin & Jetpack Compose.")
                    Text("• Interactive Wallpaper Studio: resize, crop, zoom, pan, rotate, dim, and set any image directly as your Home or Lock Screen wallpaper.")
                    Text("• Full support for 14 booru engines (Danbooru, Gelbooru, Moebooru, e621/e926, Philomena, Zerochan, Sankaku, Szurubooru, Hydrus, Anime Pictures, Shimmie2, Hybooru, E-Shuushuu, Nozomi).")
                    Text("• Kurumi color themes, categorized tag syntax highlighting, DText markup parser, interactive translation notes, and custom filename token generator.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelogDialog = false }) { Text("Close") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogsScreen(viewModel: BoorusamaViewModel) {
    val logs by viewModel.debugLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network & Debug Logs (${logs.size})") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.repository.apiClient.clearLogs() }) {
                        Text("Clear")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(logs.reversed()) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "[${entry.level}] ${entry.tag}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (entry.level == "ERROR") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = entry.message,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(viewModel: BoorusamaViewModel) {
    var enteredPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = "Locked",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Boorupaper Locked", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Enter your PIN code to unlock",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = enteredPin,
            onValueChange = { enteredPin = it },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.width(220.dp).testTag("applock_pin_input")
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.unlockWithPin(enteredPin) },
            modifier = Modifier.width(220.dp).testTag("applock_unlock_button")
        ) {
            Text("Unlock")
        }
    }
}
