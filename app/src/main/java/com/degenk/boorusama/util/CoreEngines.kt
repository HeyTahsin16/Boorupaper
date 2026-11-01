package com.degenk.boorusama.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.degenk.boorusama.model.BooruConfig
import com.degenk.boorusama.model.BooruConfigBannedPostVisibility
import com.degenk.boorusama.model.BooruConfigDeletedItemBehavior
import com.degenk.boorusama.model.BooruConfigRatingFilter
import com.degenk.boorusama.model.BooruType
import com.degenk.boorusama.model.Post
import com.degenk.boorusama.model.Rating
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class KnownBooruSite(
    val url: String,
    val booruType: BooruType,
    val description: String = ""
)

data class TagDefinitions(
    val metatags: List<String> = emptyList(),
    val freeMetatags: List<String> = emptyList(),
    val defaultBlacklistedTags: List<String> = emptyList(),
    val r18Tags: List<String> = emptyList()
)

object BooruRegistry {
    val knownSites: List<KnownBooruSite> = listOf(
        KnownBooruSite("https://safebooru.donmai.us/", BooruType.DANBOORU, "Danbooru Safe Mode (Default)"),
        KnownBooruSite("https://danbooru.donmai.us/", BooruType.DANBOORU, "Danbooru Main"),
        KnownBooruSite("https://aibooru.online/", BooruType.DANBOORU, "AIBooru"),
        KnownBooruSite("https://testbooru.donmai.us/", BooruType.DANBOORU, "Danbooru Testbooru"),
        KnownBooruSite("https://gelbooru.com/", BooruType.GELBOORU, "Gelbooru 0.2.5"),
        KnownBooruSite("https://safebooru.org/", BooruType.GELBOORU_V2, "Safebooru.org (Gelbooru 0.2)"),
        KnownBooruSite("https://tbib.org/", BooruType.GELBOORU_V2, "The Big ImageBoard"),
        KnownBooruSite("https://rule34.xxx/", BooruType.GELBOORU_V2, "Rule34.xxx"),
        KnownBooruSite("https://realbooru.com/", BooruType.GELBOORU_V2, "Realbooru"),
        KnownBooruSite("https://konachan.net/", BooruType.MOEBOORU, "Konachan Safe"),
        KnownBooruSite("https://konachan.com/", BooruType.MOEBOORU, "Konachan Main"),
        KnownBooruSite("https://yande.re/", BooruType.MOEBOORU, "Yande.re"),
        KnownBooruSite("https://sakugabooru.com/", BooruType.MOEBOORU, "Sakugabooru"),
        KnownBooruSite("https://e926.net/", BooruType.E621, "e926 Safe"),
        KnownBooruSite("https://e621.net/", BooruType.E621, "e621 Main"),
        KnownBooruSite("https://www.zerochan.net/", BooruType.ZEROCHAN, "Zerochan"),
        KnownBooruSite("https://kpop.asiachan.com/", BooruType.ZEROCHAN, "Asiachan"),
        KnownBooruSite("https://chan.sankakucomplex.com/", BooruType.SANKAKU, "Sankaku Channel"),
        KnownBooruSite("https://derpibooru.org/", BooruType.PHILOMENA, "Derpibooru"),
        KnownBooruSite("https://ponybooru.org/", BooruType.PHILOMENA, "Ponybooru"),
        KnownBooruSite("https://rule34.paheal.net/", BooruType.SHIMMIE2, "Paheal Shimmie2"),
        KnownBooruSite("https://anime-pictures.net/", BooruType.ANIME_PICTURES, "Anime Pictures"),
        KnownBooruSite("https://booru.funmaker.moe/", BooruType.HYBOORU, "Hybooru"),
        KnownBooruSite("https://e-shuushuu.net/", BooruType.ESHUUSHUU, "E-Shuushuu"),
        KnownBooruSite("https://nozomi.la/", BooruType.NOZOMI, "Nozomi.la"),
        KnownBooruSite("https://illusioncards.booru.org/", BooruType.GELBOORU_V1, "IllusionCards")
    )

    private var cachedTagDefs: TagDefinitions? = null

    fun detectBooruTypeFromUrl(url: String): BooruType {
        val normalized = url.lowercase().trim()
        knownSites.firstOrNull { normalized.contains(it.url.removePrefix("https://").removePrefix("http://").trimEnd('/')) }
            ?.let { return it.booruType }
        return when {
            "donmai.us" in normalized || "aibooru" in normalized -> BooruType.DANBOORU
            "gelbooru.com" in normalized -> BooruType.GELBOORU
            "safebooru.org" in normalized || "tbib.org" in normalized || "rule34.xxx" in normalized -> BooruType.GELBOORU_V2
            "konachan" in normalized || "yande.re" in normalized || "sakugabooru" in normalized -> BooruType.MOEBOORU
            "e621.net" in normalized || "e926.net" in normalized -> BooruType.E621
            "zerochan" in normalized || "asiachan" in normalized -> BooruType.ZEROCHAN
            "sankakucomplex" in normalized -> BooruType.SANKAKU
            "derpibooru" in normalized || "ponybooru" in normalized -> BooruType.PHILOMENA
            "paheal" in normalized -> BooruType.SHIMMIE2
            "anime-pictures" in normalized -> BooruType.ANIME_PICTURES
            "funmaker.moe" in normalized -> BooruType.HYBOORU
            "e-shuushuu" in normalized -> BooruType.ESHUUSHUU
            "nozomi.la" in normalized -> BooruType.NOZOMI
            "127.0.0.1:45869" in normalized || "hydrus" in normalized -> BooruType.HYDRUS
            else -> BooruType.DANBOORU
        }
    }

    fun loadTagDefinitions(context: Context): TagDefinitions {
        cachedTagDefs?.let { return it }
        return try {
            val raw = context.assets.open("tagdef.json").bufferedReader().use { it.readText() }
            val root = Json.parseToJsonElement(raw).jsonObject
            val defs = TagDefinitions(
                metatags = root["metatags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                freeMetatags = root["free_metatags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                defaultBlacklistedTags = root["default_blacklisted_tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                r18Tags = root["r18_tags"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            )
            cachedTagDefs = defs
            defs
        } catch (_: Exception) {
            TagDefinitions(
                metatags = listOf("age", "approver", "arttags", "chartags", "commentary", "copytags", "date", "downvotes", "fav", "favcount", "filetype", "gentags", "has", "height", "id", "is", "md5", "metatags", "mpixels", "note", "order", "parent", "pixiv", "pool", "ank", "rating", "ratio", "score", "source", "status", "tagcount", "upvotes", "user", "width"),
                freeMetatags = listOf("rating", "order", "status", "is")
            )
        }
    }
}

/**
 * Port of lib/core/posts/filter/src/filter.dart & tag_expression.dart
 * Evaluates blacklist entries, config rating filters, and search expressions against a Post.
 */
object TagExpressionEngine {

    fun matchesBlacklistRule(post: Post, ruleLine: String): Boolean {
        val tokens = ruleLine.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false

        return tokens.all { token ->
            val isNegated = token.startsWith("-") && token.length > 1
            val rawExpr = if (isNegated) token.substring(1) else token
            val matched = evaluateSingleToken(post, rawExpr.lowercase())
            if (isNegated) !matched else matched
        }
    }

    private fun evaluateSingleToken(post: Post, expr: String): Boolean {
        return when {
            expr.startsWith("rating:") -> {
                val target = expr.substringAfter("rating:")
                post.rating.shortCode.equals(target.take(1), ignoreCase = true) ||
                    post.rating.label.equals(target, ignoreCase = true)
            }
            expr.startsWith("score:") -> {
                val cond = expr.substringAfter("score:")
                evaluateNumericComparison(post.score, cond)
            }
            expr.startsWith("downvotes:") -> {
                val cond = expr.substringAfter("downvotes:")
                evaluateNumericComparison(post.downvotes, cond)
            }
            expr.startsWith("upvotes:") -> {
                val cond = expr.substringAfter("upvotes:")
                evaluateNumericComparison(post.upvotes, cond)
            }
            expr.startsWith("favcount:") -> {
                val cond = expr.substringAfter("favcount:")
                evaluateNumericComparison(post.favCount, cond)
            }
            expr.startsWith("id:") -> {
                val cond = expr.substringAfter("id:")
                evaluateNumericComparison(post.id, cond)
            }
            expr.startsWith("width:") -> {
                val cond = expr.substringAfter("width:")
                evaluateNumericComparison(post.width, cond)
            }
            expr.startsWith("height:") -> {
                val cond = expr.substringAfter("height:")
                evaluateNumericComparison(post.height, cond)
            }
            expr.startsWith("uploader:") || expr.startsWith("user:") -> {
                val target = expr.substringAfter(":")
                post.uploaderName.equals(target, ignoreCase = true)
            }
            expr.startsWith("uploaderid:") -> {
                val target = expr.substringAfter("uploaderid:").toIntOrNull()
                target != null && post.uploaderId == target
            }
            expr.startsWith("status:") -> {
                when (expr.substringAfter("status:")) {
                    "deleted" -> post.isDeleted
                    "banned" -> post.isBanned
                    "flagged" -> post.isFlagged
                    "pending" -> post.isPending
                    "active" -> !post.isDeleted && !post.isBanned && !post.isPending
                    else -> false
                }
            }
            expr.startsWith("source:") -> {
                val target = expr.substringAfter("source:")
                post.source.lowercase().contains(target)
            }
            expr.startsWith("filetype:") || expr.startsWith("ext:") -> {
                val target = expr.substringAfter(":")
                post.format.equals(target, ignoreCase = true)
            }
            else -> {
                if ('*' in expr) {
                    val regexPattern = "^" + expr.split('*').joinToString(".*") { Regex.escape(it) } + "$"
                    val regex = Regex(regexPattern, RegexOption.IGNORE_CASE)
                    post.tags.any { regex.matches(it) }
                } else {
                    post.tags.any { it.equals(expr, ignoreCase = true) }
                }
            }
        }
    }

    private fun evaluateNumericComparison(actual: Int, condition: String): Boolean {
        return when {
            condition.startsWith(">=") -> condition.drop(2).toIntOrNull()?.let { actual >= it } ?: false
            condition.startsWith("<=") -> condition.drop(2).toIntOrNull()?.let { actual <= it } ?: false
            condition.startsWith(">") -> condition.drop(1).toIntOrNull()?.let { actual > it } ?: false
            condition.startsWith("<") -> condition.drop(1).toIntOrNull()?.let { actual < it } ?: false
            condition.contains("..") -> {
                val parts = condition.split("..")
                val min = parts.getOrNull(0)?.toIntOrNull() ?: Int.MIN_VALUE
                val max = parts.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
                actual in min..max
            }
            else -> condition.toIntOrNull()?.let { actual == it } ?: false
        }
    }

    data class FilterResult(
        val visiblePosts: List<Post>,
        val blacklistedBreakdown: Map<String, Int>
    )

    fun filterPosts(
        posts: List<Post>,
        config: BooruConfig,
        globalBlacklist: List<String>
    ): FilterResult {
        val activeRules = (globalBlacklist + config.blacklistedTags)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val breakdown = mutableMapOf<String, Int>()
        val visible = mutableListOf<Post>()

        for (post in posts) {
            if (config.deletedItemBehavior == BooruConfigDeletedItemBehavior.HIDE && post.isDeleted) {
                continue
            }
            if (config.bannedPostVisibility == BooruConfigBannedPostVisibility.HIDE && post.isBanned) {
                continue
            }
            val ratingBlocked = when (config.ratingFilter) {
                BooruConfigRatingFilter.NONE -> false
                BooruConfigRatingFilter.HIDE_EXPLICIT -> post.rating == Rating.EXPLICIT
                BooruConfigRatingFilter.HIDE_NSFW -> post.rating != Rating.GENERAL
                BooruConfigRatingFilter.CUSTOM -> post.rating in config.granularRatingFilters
            }
            if (ratingBlocked) continue

            val matchedRules = activeRules.filter { matchesBlacklistRule(post, it) }
            if (matchedRules.isNotEmpty()) {
                for (rule in matchedRules) {
                    breakdown[rule] = (breakdown[rule] ?: 0) + 1
                }
            } else {
                visible.add(post)
            }
        }

        return FilterResult(visiblePosts = visible, blacklistedBreakdown = breakdown)
    }
}

/**
 * Port of packages/filename_generator/lib/src/generator.dart
 * Supports tokens like {id}, {md5:maxlength=8}, {artist}, {character}, {copyright}, {tags}, {rating}, {ext}, {width}, {height}, {score}, {date}, {search}, {uuid}
 */
object FilenameGenerator {
    private val tokenRegex = Regex("\\{([a-zA-Z0-9_]+)(?::([^}]+))?\\}")
    private val invalidCharsRegex = Regex("[\\\\/:*?\"<>|]")

    fun generate(
        format: String,
        post: Post,
        searchQuery: String = "",
        index: Int? = null
    ): String {
        val effectiveFormat = format.ifBlank { "{id}_{md5:maxlength=8}.{ext}" }
        val rawResult = tokenRegex.replace(effectiveFormat) { matchResult ->
            val tokenName = matchResult.groupValues[1].lowercase()
            val optionsRaw = matchResult.groupValues[2]
            val options = parseOptions(optionsRaw)

            val rawValue = when (tokenName) {
                "id" -> post.id.toString()
                "md5" -> post.md5.ifBlank { post.id.toString() }
                "ext", "extension" -> post.format.lowercase().ifBlank { "jpg" }
                "rating" -> post.rating.label.lowercase()
                "score" -> post.score.toString()
                "width" -> post.width.toString()
                "height" -> post.height.toString()
                "mpixels" -> String.format(Locale.US, "%.2f", (post.width.toDouble() * post.height.toDouble()) / 1_000_000.0)
                "source" -> sanitizeSegment(post.source)
                "artist" -> formatTagList(post.artistTags.ifEmpty { listOf("unknown_artist") }, options)
                "character", "characters" -> formatTagList(post.characterTags, options)
                "copyright", "series" -> formatTagList(post.copyrightTags, options)
                "general" -> formatTagList(post.generalTags, options)
                "meta" -> formatTagList(post.metaTags, options)
                "tags" -> formatTagList(post.tags, options)
                "search" -> sanitizeSegment(searchQuery.ifBlank { "browse" })
                "index" -> (index ?: 1).toString()
                "uuid" -> UUID.randomUUID().toString()
                "date" -> {
                    val pattern = options["format"] ?: "yyyy-MM-dd"
                    try {
                        SimpleDateFormat(pattern, Locale.US).format(Date())
                    } catch (_: Exception) {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    }
                }
                else -> ""
            }

            applyModifiers(rawValue, options)
        }

        // Preserve directory separator `/` if format specifies subfolders, but sanitize each path component
        val segments = rawResult.split('/')
            .map { seg -> seg.replace(invalidCharsRegex, "_").trim().trim('.') }
            .filter { it.isNotBlank() }

        return if (segments.isEmpty()) {
            "${post.id}.${post.format.ifBlank { "jpg" }}"
        } else {
            segments.joinToString("/")
        }
    }

    private fun parseOptions(optionsRaw: String): Map<String, String> {
        if (optionsRaw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        optionsRaw.split(',').forEach { part ->
            val trimmed = part.trim()
            if ('=' in trimmed) {
                val (k, v) = trimmed.split('=', limit = 2)
                map[k.trim().lowercase()] = v.trim()
            } else if (trimmed.isNotBlank()) {
                map[trimmed.lowercase()] = "true"
            }
        }
        return map
    }

    private fun formatTagList(tags: List<String>, options: Map<String, String>): String {
        if (tags.isEmpty()) return ""
        val delimiter = options["delimiter"] ?: " "
        val limit = options["limit"]?.toIntOrNull() ?: 10
        val cleanMod = options.containsKey("nomod")
        val sorted = when (options["sort"]) {
            "asc", "name" -> tags.sorted()
            "desc" -> tags.sortedDescending()
            "length" -> tags.sortedBy { it.length }
            else -> tags
        }
        val processed = sorted.take(limit).map { tag ->
            val base = if (cleanMod) tag.substringBefore('(').trim('_') else tag
            sanitizeSegment(base)
        }
        return processed.joinToString(delimiter)
    }

    private fun applyModifiers(value: String, options: Map<String, String>): String {
        var result = value
        when (options["case"]) {
            "upper" -> result = result.uppercase()
            "lower" -> result = result.lowercase()
        }
        options["maxlength"]?.toIntOrNull()?.let { maxLen ->
            if (maxLen > 0 && result.length > maxLen) {
                result = result.take(maxLen)
            }
        }
        return result
    }

    private fun sanitizeSegment(input: String): String {
        return input.replace(invalidCharsRegex, "_").trim()
    }
}

/**
 * Port of packages/dtext markup parser used across Danbooru/e621 comments, wikis, notes, and forum posts.
 */
object DTextParser {
    private val wikiLinkRegex = Regex("\\[\\[([^|\\]]+)(?:\\|([^\\]]+))?\\]\\]")
    private val postIdRegex = Regex("\\bpost #(\\d+)\\b", RegexOption.IGNORE_CASE)
    private val quoteBlockRegex = Regex("\\[quote\\]([\\s\\S]*?)\\[/quote\\]", RegexOption.IGNORE_CASE)
    private val codeBlockRegex = Regex("\\[code\\]([\\s\\S]*?)\\[/code\\]", RegexOption.IGNORE_CASE)
    private val spoilerRegex = Regex("\\[spoiler\\]([\\s\\S]*?)\\[/spoiler\\]", RegexOption.IGNORE_CASE)
    private val boldRegex = Regex("\\[b\\]([\\s\\S]*?)\\[/b\\]", RegexOption.IGNORE_CASE)
    private val italicRegex = Regex("\\[i\\]([\\s\\S]*?)\\[/i\\]", RegexOption.IGNORE_CASE)
    private val underlineRegex = Regex("\\[u\\]([\\s\\S]*?)\\[/u\\]", RegexOption.IGNORE_CASE)
    private val strikeRegex = Regex("\\[s\\]([\\s\\S]*?)\\[/s\\]", RegexOption.IGNORE_CASE)
    private val headerRegex = Regex("(?m)^h([1-6])\\.\\s*(.+)$")
    private val htmlTagRegex = Regex("<[^>]*>")

    fun parseToAnnotatedString(
        rawText: String,
        linkColor: Color = Color(0xFF019AE6),
        quoteColor: Color = Color(0xFF9E9E9E),
        spoilerRevealed: Boolean = false
    ): AnnotatedString {
        if (rawText.isBlank()) return AnnotatedString("")

        var cleaned = rawText
            .replace("\r\n", "\n")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(htmlTagRegex, "")

        // Transform headers
        cleaned = headerRegex.replace(cleaned) { match ->
            "[b]${match.groupValues[2]}[/b]"
        }

        return buildAnnotatedString {
            var cursor = 0
            // Parse line by line / block by block while highlighting wiki links, post refs, quotes, spoilers, bold/italics
            val combinedRegex = Regex(
                "(\\[quote\\][\\s\\S]*?\\[/quote\\])|" +
                    "(\\[code\\][\\s\\S]*?\\[/code\\])|" +
                    "(\\[spoiler\\][\\s\\S]*?\\[/spoiler\\])|" +
                    "(\\[b\\][\\s\\S]*?\\[/b\\])|" +
                    "(\\[i\\][\\s\\S]*?\\[/i\\])|" +
                    "(\\[u\\][\\s\\S]*?\\[/u\\])|" +
                    "(\\[s\\][\\s\\S]*?\\[/s\\])|" +
                    "(\\[\\[[^\\]]+\\]\\])|" +
                    "(\\bpost #\\d+\\b)",
                RegexOption.IGNORE_CASE
            )

            for (match in combinedRegex.findAll(cleaned)) {
                if (match.range.first > cursor) {
                    append(cleaned.substring(cursor, match.range.first))
                }
                val token = match.value
                when {
                    token.startsWith("[quote]", ignoreCase = true) -> {
                        val content = token.substring(7, token.length - 8).trim()
                        withStyle(SpanStyle(color = quoteColor, fontStyle = FontStyle.Italic)) {
                            append("▎ ")
                            append(stripNestedDText(content))
                        }
                    }
                    token.startsWith("[code]", ignoreCase = true) -> {
                        val content = token.substring(6, token.length - 7).trim()
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = quoteColor.copy(alpha = 0.16f))) {
                            append(content)
                        }
                    }
                    token.startsWith("[spoiler]", ignoreCase = true) -> {
                        val content = token.substring(9, token.length - 10)
                        if (spoilerRevealed) {
                            withStyle(SpanStyle(background = quoteColor.copy(alpha = 0.25f))) {
                                append(stripNestedDText(content))
                            }
                        } else {
                            withStyle(SpanStyle(background = quoteColor.copy(alpha = 0.35f), color = quoteColor)) {
                                append(" [Spoiler: ${stripNestedDText(content)}] ")
                            }
                        }
                    }
                    token.startsWith("[b]", ignoreCase = true) -> {
                        val content = token.substring(3, token.length - 4)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stripNestedDText(content))
                        }
                    }
                    token.startsWith("[i]", ignoreCase = true) -> {
                        val content = token.substring(3, token.length - 4)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(stripNestedDText(content))
                        }
                    }
                    token.startsWith("[u]", ignoreCase = true) -> {
                        val content = token.substring(3, token.length - 4)
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append(stripNestedDText(content))
                        }
                    }
                    token.startsWith("[s]", ignoreCase = true) -> {
                        val content = token.substring(3, token.length - 4)
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(stripNestedDText(content))
                        }
                    }
                    token.startsWith("[[") -> {
                        val wikiMatch = wikiLinkRegex.matchEntire(token)
                        val target = wikiMatch?.groupValues?.getOrNull(1)?.trim() ?: token.trim('[', ']')
                        val label = wikiMatch?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() } ?: target
                        pushStringAnnotation(tag = "WIKI_TAG", annotation = target.replace(' ', '_').lowercase())
                        withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Medium)) {
                            append(label.replace('_', ' '))
                        }
                        pop()
                    }
                    token.lowercase().startsWith("post #") -> {
                        val id = token.substringAfter("#").trim()
                        pushStringAnnotation(tag = "POST_ID", annotation = id)
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                            append(token)
                        }
                        pop()
                    }
                    else -> append(token)
                }
                cursor = match.range.last + 1
            }

            if (cursor < cleaned.length) {
                append(cleaned.substring(cursor))
            }
        }
    }

    fun stripNestedDText(input: String): String {
        return input
            .replace(wikiLinkRegex) { m -> m.groupValues[2].ifBlank { m.groupValues[1] } }
            .replace(Regex("\\[/?(?:b|i|u|s|quote|code|spoiler|expand|tn)\\]", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
