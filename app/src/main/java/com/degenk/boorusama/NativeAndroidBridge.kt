package com.degenk.boorusama

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.CookieManager
import java.io.File
import java.io.FileNotFoundException

data class WebCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String = "/"
)

object NativeAndroidBridge {
    private const val TAG = "NativeAndroidBridge"

    fun openCacheDocuments(
        context: Context,
        kind: String,
        imagesLabel: String = "Image cache",
        videosLabel: String = "Video cache"
    ): Result<Unit> = runCatching {
        require(kind == "images" || kind == "videos") { "Unknown cache kind: $kind" }
        context.getSharedPreferences("cache_documents", 0)
            .edit()
            .putString("images", imagesLabel)
            .putString("videos", videosLabel)
            .commit()
        context.contentResolver.notifyChange(
            DocumentsContract.buildRootsUri("${context.packageName}.cache.documents"),
            null
        )
        val uri = DocumentsContract.buildRootUri("${context.packageName}.cache.documents", kind)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, DocumentsContract.Root.MIME_TYPE_ITEM)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    @Suppress("DEPRECATION")
    fun refreshMediaGallery(context: Context, path: String): String {
        return try {
            val file = File(path)
            if (!file.exists()) {
                throw FileNotFoundException("File does not exist: $path")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                context.sendBroadcast(
                    Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file))
                )
            } else {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.toString()),
                    arrayOf(file.name)
                ) { scannedPath, uri ->
                    Log.d(TAG, "Scan completed: $scannedPath, URI: $uri")
                }
            }
            "Success show image $path in Gallery"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan media: ${e.message}")
            "Error: ${e.message}"
        }
    }

    fun getCookiesForUrl(url: String): String? {
        return runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
    }

    fun openCacheDocumentsTree(context: Context) {
        openCacheDocuments(context, "images")
    }

    fun getWebViewCookies(url: String): List<WebCookie> {
        val host = runCatching { Uri.parse(url).host }.getOrNull() ?: return emptyList()
        val header = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull() ?: return emptyList()
        return header.split(";").mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            WebCookie(
                name = part.substring(0, separator).trim(),
                value = part.substring(separator + 1).trim(),
                domain = host,
                path = "/"
            )
        }
    }
}
