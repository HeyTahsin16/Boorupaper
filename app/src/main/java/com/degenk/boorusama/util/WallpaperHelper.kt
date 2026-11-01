package com.degenk.boorusama.util

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import androidx.core.content.FileProvider
import com.degenk.boorusama.data.remote.BooruApiClient
import com.degenk.boorusama.model.BooruConfig
import com.degenk.boorusama.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

enum class WallpaperTarget(val label: String) {
    HOME_SCREEN("Home Screen"),
    LOCK_SCREEN("Lock Screen"),
    BOTH("Home & Lock Screens")
}

enum class WallpaperFitMode(val label: String) {
    CROP_FILL("Crop to Fill"),
    FIT_CENTER("Fit Center"),
    STRETCH("Stretch to Screen"),
    CENTER_ORIGINAL("Center 1:1")
}

data class WallpaperTransformConfig(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDegrees: Float = 0f,
    val fitMode: WallpaperFitMode = WallpaperFitMode.CROP_FILL,
    val dimAmount: Float = 0f,
    val useOriginalQuality: Boolean = true
)

object WallpaperHelper {

    suspend fun fetchPostBitmap(
        apiClient: BooruApiClient,
        config: BooruConfig,
        post: Post,
        useOriginalQuality: Boolean = true
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = if (useOriginalQuality) {
                post.originalImageUrl.ifBlank { post.sampleImageUrl.ifBlank { post.thumbnailImageUrl } }
            } else {
                post.sampleImageUrl.ifBlank { post.originalImageUrl.ifBlank { post.thumbnailImageUrl } }
            }
            require(targetUrl.isNotBlank()) { "Post has no valid image URL" }

            val req = apiClient.buildDownloadRequest(config, targetUrl)
            apiClient.getOkHttpClient(config).newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}: Failed to download image for wallpaper")
                }
                val bytes = response.body?.bytes() ?: throw IllegalStateException("Empty image body")
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw IllegalStateException("Unable to decode image bitmap")
            }
        }
    }

    fun getTargetWallpaperDimensions(context: Context): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        val screenW = metrics.widthPixels.coerceAtLeast(720)
        val screenH = metrics.heightPixels.coerceAtLeast(1280)
        return screenW to screenH
    }

    fun renderTransformedWallpaperBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        transform: WallpaperTransformConfig
    ): Bitmap {
        val outWidth = targetWidth.coerceIn(360, 4096)
        val outHeight = targetHeight.coerceIn(360, 4096)
        val result = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Fill background with deep black for letterboxed areas
        canvas.drawColor(Color.BLACK)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        val srcW = source.width.toFloat().coerceAtLeast(1f)
        val srcH = source.height.toFloat().coerceAtLeast(1f)

        val isRotated90or270 = (transform.rotationDegrees.toInt() % 180) != 0
        val effectiveSrcW = if (isRotated90or270) srcH else srcW
        val effectiveSrcH = if (isRotated90or270) srcW else srcH

        val matrix = Matrix()
        // Move source center to origin
        matrix.postTranslate(-srcW / 2f, -srcH / 2f)

        // Apply rotation
        if (transform.rotationDegrees != 0f) {
            matrix.postRotate(transform.rotationDegrees)
        }

        // Compute base scale from fitMode
        when (transform.fitMode) {
            WallpaperFitMode.CROP_FILL -> {
                val baseScale = max(outWidth / effectiveSrcW, outHeight / effectiveSrcH)
                val finalScale = baseScale * transform.scale.coerceIn(0.25f, 5f)
                matrix.postScale(finalScale, finalScale)
            }
            WallpaperFitMode.FIT_CENTER -> {
                val baseScale = min(outWidth / effectiveSrcW, outHeight / effectiveSrcH)
                val finalScale = baseScale * transform.scale.coerceIn(0.25f, 5f)
                matrix.postScale(finalScale, finalScale)
            }
            WallpaperFitMode.STRETCH -> {
                val scaleX = (outWidth / effectiveSrcW) * transform.scale.coerceIn(0.25f, 5f)
                val scaleY = (outHeight / effectiveSrcH) * transform.scale.coerceIn(0.25f, 5f)
                matrix.postScale(scaleX, scaleY)
            }
            WallpaperFitMode.CENTER_ORIGINAL -> {
                val finalScale = transform.scale.coerceIn(0.25f, 5f)
                matrix.postScale(finalScale, finalScale)
            }
        }

        // Translate to output center + user pan offset
        val panPixelsX = transform.offsetX * (outWidth * 0.75f)
        val panPixelsY = transform.offsetY * (outHeight * 0.75f)
        matrix.postTranslate((outWidth / 2f) + panPixelsX, (outHeight / 2f) + panPixelsY)

        canvas.drawBitmap(source, matrix, paint)

        // Apply optional dim overlay for home screen icon readability
        val dim = transform.dimAmount.coerceIn(0f, 0.85f)
        if (dim > 0.01f) {
            val alpha = (dim * 255).toInt().coerceIn(0, 220)
            canvas.drawColor(Color.argb(alpha, 0, 0, 0))
        }

        return result
    }

    suspend fun applyWallpaper(
        context: Context,
        sourceBitmap: Bitmap,
        transform: WallpaperTransformConfig,
        target: WallpaperTarget
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val (targetW, targetH) = getTargetWallpaperDimensions(context)
            val rendered = renderTransformedWallpaperBitmap(
                source = sourceBitmap,
                targetWidth = targetW,
                targetHeight = targetH,
                transform = transform
            )
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val flags = when (target) {
                    WallpaperTarget.HOME_SCREEN -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK_SCREEN -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wallpaperManager.setBitmap(rendered, null, true, flags)
            } else {
                wallpaperManager.setBitmap(rendered)
            }
            "Wallpaper applied to ${target.label}"
        }
    }

    suspend fun launchSystemWallpaperCropper(
        context: Context,
        sourceBitmap: Bitmap,
        postId: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cacheDir = File(context.cacheDir, "wallpapers").apply { mkdirs() }
            val tempFile = File(cacheDir, "boorupaper_${postId}.png")
            FileOutputStream(tempFile).use { out ->
                sourceBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            )
            val wallpaperManager = WallpaperManager.getInstance(context)
            val intent = try {
                wallpaperManager.getCropAndSetWallpaperIntent(uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (_: Exception) {
                Intent(Intent.ACTION_ATTACH_DATA).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    setDataAndType(uri, "image/*")
                    putExtra("mimeType", "image/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(intent, "Set as Wallpaper").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }
}
