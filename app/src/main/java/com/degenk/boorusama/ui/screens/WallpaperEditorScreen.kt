package com.degenk.boorusama.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.degenk.boorusama.ui.BoorusamaViewModel
import com.degenk.boorusama.util.WallpaperFitMode
import com.degenk.boorusama.util.WallpaperHelper
import com.degenk.boorusama.util.WallpaperTarget
import com.degenk.boorusama.util.WallpaperTransformConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperEditorScreen(viewModel: BoorusamaViewModel) {
    val post by viewModel.wallpaperPost.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentPost = post
    if (currentPost == null) {
        LaunchedEffect(Unit) { viewModel.navigateBack() }
        return
    }

    var sourceBitmap by remember(currentPost.id) { mutableStateOf<Bitmap?>(null) }
    var isLoadingBitmap by remember(currentPost.id) { mutableStateOf(true) }
    var loadError by remember(currentPost.id) { mutableStateOf<String?>(null) }
    var isApplyingWallpaper by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var fitMode by remember { mutableStateOf(WallpaperFitMode.CROP_FILL) }
    var dimAmount by remember { mutableFloatStateOf(0f) }
    var targetScreen by remember { mutableStateOf(WallpaperTarget.BOTH) }
    var showMockupOverlay by remember { mutableStateOf(true) }

    val (deviceW, deviceH) = remember { WallpaperHelper.getTargetWallpaperDimensions(context) }
    val previewAspectRatio = remember(deviceW, deviceH) {
        (deviceW.toFloat() / deviceH.toFloat()).coerceIn(0.42f, 0.75f)
    }

    val transformConfig = remember(scale, offsetX, offsetY, rotationDegrees, fitMode, dimAmount) {
        WallpaperTransformConfig(
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotationDegrees = rotationDegrees,
            fitMode = fitMode,
            dimAmount = dimAmount
        )
    }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentPost.id) {
        isLoadingBitmap = true
        loadError = null
        val result = WallpaperHelper.fetchPostBitmap(
            apiClient = viewModel.apiClient,
            config = activeConfig,
            post = currentPost,
            useOriginalQuality = true
        )
        result.onSuccess { bmp ->
            sourceBitmap = bmp
            isLoadingBitmap = false
        }.onFailure { err ->
            loadError = err.message ?: "Failed to load image for wallpaper"
            isLoadingBitmap = false
        }
    }

    LaunchedEffect(sourceBitmap, transformConfig) {
        val src = sourceBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val previewW = 540
            val previewH = (previewW / previewAspectRatio).toInt().coerceAtLeast(720)
            previewBitmap = WallpaperHelper.renderTransformedWallpaperBitmap(
                source = src,
                targetWidth = previewW,
                targetHeight = previewH,
                transform = transformConfig
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Wallpaper Studio", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Post #${currentPost.id} • ${currentPost.width}×${currentPost.height}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("wallpaper_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            rotationDegrees = 0f
                            fitMode = WallpaperFitMode.CROP_FILL
                            dimAmount = 0f
                        },
                        modifier = Modifier.testTag("wallpaper_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoadingBitmap -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Preparing high-resolution image for wallpaper...")
                    }
                }
            }
            loadError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = loadError ?: "Error loading image",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.navigateBack() }) {
                            Text("Go Back")
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Interactive Phone Screen Wallpaper Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(previewAspectRatio)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Black)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.4f, 4.0f)
                                        offsetX = (offsetX + pan.x / 350f).coerceIn(-1.2f, 1.2f)
                                        offsetY = (offsetY + pan.y / 500f).coerceIn(-1.2f, 1.2f)
                                    }
                                }
                                .testTag("wallpaper_interactive_preview")
                        ) {
                            previewBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Wallpaper Preview",
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Optional Lock/Home Screen Clock & App Icons Mockup Overlay
                            if (showMockupOverlay) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "09:41",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Drag or pinch to resize & position",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        repeat(4) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.3f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Controls Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Target Screen
                            Text("Apply Wallpaper To", style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WallpaperTarget.entries.forEach { target ->
                                    FilterChip(
                                        selected = targetScreen == target,
                                        onClick = { targetScreen = target },
                                        label = { Text(target.label) },
                                        modifier = Modifier.testTag("wallpaper_target_${target.name}")
                                    )
                                }
                            }

                            HorizontalDivider()

                            // Fit / Resize Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Resize & Fit Mode", style = MaterialTheme.typography.titleSmall)
                                OutlinedButton(
                                    onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                                    modifier = Modifier.testTag("wallpaper_rotate_button")
                                ) {
                                    Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${rotationDegrees.toInt()}°")
                                }
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WallpaperFitMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = fitMode == mode,
                                        onClick = { fitMode = mode },
                                        label = { Text(mode.label) },
                                        modifier = Modifier.testTag("wallpaper_fit_${mode.name}")
                                    )
                                }
                            }

                            // Zoom / Scale Slider
                            Text(
                                text = String.format(Locale.US, "Zoom / Scale: %.2fx", scale),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = scale,
                                onValueChange = { scale = it },
                                valueRange = 0.4f..4.0f,
                                modifier = Modifier.testTag("wallpaper_scale_slider")
                            )

                            // Horizontal & Vertical Pan Sliders
                            Text(
                                text = String.format(Locale.US, "Horizontal Position (X): %+d%%", (offsetX * 100).toInt()),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = offsetX,
                                onValueChange = { offsetX = it },
                                valueRange = -1.0f..1.0f
                            )

                            Text(
                                text = String.format(Locale.US, "Vertical Position (Y): %+d%%", (offsetY * 100).toInt()),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = offsetY,
                                onValueChange = { offsetY = it },
                                valueRange = -1.0f..1.0f
                            )

                            // Dim Overlay Slider
                            Text(
                                text = "Darken / Dim Overlay: ${(dimAmount * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = dimAmount,
                                onValueChange = { dimAmount = it },
                                valueRange = 0f..0.75f
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Show Clock & Home Icons Guide")
                                Switch(
                                    checked = showMockupOverlay,
                                    onCheckedChange = { showMockupOverlay = it }
                                )
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val src = sourceBitmap ?: return@OutlinedButton
                                scope.launch {
                                    WallpaperHelper.launchSystemWallpaperCropper(context, src, currentPost.id)
                                        .onFailure { e ->
                                            viewModel.showMessage("System cropper unavailable: ${e.message}")
                                        }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("system_wallpaper_cropper_button")
                        ) {
                            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("System Cropper")
                        }

                        Button(
                            onClick = {
                                val src = sourceBitmap ?: return@Button
                                isApplyingWallpaper = true
                                scope.launch {
                                    val res = WallpaperHelper.applyWallpaper(
                                        context = context,
                                        sourceBitmap = src,
                                        transform = transformConfig,
                                        target = targetScreen
                                    )
                                    isApplyingWallpaper = false
                                    res.onSuccess { msg ->
                                        viewModel.showMessage(msg)
                                    }.onFailure { err ->
                                        viewModel.showMessage("Failed to set wallpaper: ${err.message}")
                                    }
                                }
                            },
                            enabled = !isApplyingWallpaper,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("apply_wallpaper_button")
                        ) {
                            if (isApplyingWallpaper) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Wallpaper")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
