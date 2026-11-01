package com.degenk.boorusama

import com.degenk.boorusama.model.BooruConfig
import com.degenk.boorusama.model.BooruConfigRatingFilter
import com.degenk.boorusama.model.BooruType
import com.degenk.boorusama.model.Post
import com.degenk.boorusama.model.Rating
import com.degenk.boorusama.util.BooruRegistry
import com.degenk.boorusama.util.DTextParser
import com.degenk.boorusama.util.FilenameGenerator
import com.degenk.boorusama.util.TagExpressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoorusamaCoreTest {

    private val samplePost = Post(
        id = 123456,
        booruUrl = "https://safebooru.donmai.us",
        thumbnailImageUrl = "https://safebooru.donmai.us/preview/123.jpg",
        sampleImageUrl = "https://safebooru.donmai.us/sample/123.jpg",
        originalImageUrl = "https://safebooru.donmai.us/original/123.png",
        tags = listOf("1girl", "solo", "hatsune_miku", "vocaloid", "blue_hair"),
        artistTags = listOf("miku_artist"),
        characterTags = listOf("hatsune_miku"),
        copyrightTags = listOf("vocaloid"),
        generalTags = listOf("1girl", "solo", "blue_hair"),
        rating = Rating.GENERAL,
        score = 85,
        upvotes = 90,
        downvotes = 5,
        favCount = 120,
        width = 1920,
        height = 1080,
        format = "png",
        md5 = "abcdef1234567890abcdef1234567890"
    )

    @Test
    fun testFilenameGeneratorTokensAndModifiers() {
        val result = FilenameGenerator.generate(
            format = "{id}_{artist}_{character}_{md5:maxlength=8}.{ext}",
            post = samplePost
        )
        assertEquals("123456_miku_artist_hatsune_miku_abcdef12.png", result)
    }

    @Test
    fun testTagExpressionEngineBlacklistMatching() {
        assertTrue(TagExpressionEngine.matchesBlacklistRule(samplePost, "1girl blue_hair"))
        assertFalse(TagExpressionEngine.matchesBlacklistRule(samplePost, "1girl -blue_hair"))
        assertTrue(TagExpressionEngine.matchesBlacklistRule(samplePost, "score:>50 rating:general"))
        assertFalse(TagExpressionEngine.matchesBlacklistRule(samplePost, "score:<20"))
        assertTrue(TagExpressionEngine.matchesBlacklistRule(samplePost, "hatsune*"))

        val config = BooruConfig(ratingFilter = BooruConfigRatingFilter.HIDE_EXPLICIT)
        val filtered = TagExpressionEngine.filterPosts(
            posts = listOf(samplePost),
            config = config,
            globalBlacklist = listOf("1girl solo")
        )
        assertTrue(filtered.visiblePosts.isEmpty())
        assertEquals(1, filtered.blacklistedBreakdown["1girl solo"])
    }

    @Test
    fun testBooruRegistryUrlDetection() {
        assertEquals(BooruType.DANBOORU, BooruRegistry.detectBooruTypeFromUrl("https://danbooru.donmai.us/"))
        assertEquals(BooruType.GELBOORU, BooruRegistry.detectBooruTypeFromUrl("https://gelbooru.com/"))
        assertEquals(BooruType.MOEBOORU, BooruRegistry.detectBooruTypeFromUrl("https://yande.re/"))
        assertEquals(BooruType.E621, BooruRegistry.detectBooruTypeFromUrl("https://e926.net/"))
        assertEquals(BooruType.PHILOMENA, BooruRegistry.detectBooruTypeFromUrl("https://derpibooru.org/"))
    }

    @Test
    fun testDTextParserStrippingAndLinks() {
        val annotated = DTextParser.parseToAnnotatedString("Hello [[hatsune_miku|Miku]] and [b]bold[/b] on post #42")
        assertTrue(annotated.text.contains("Miku"))
        assertTrue(annotated.text.contains("bold"))
        assertTrue(annotated.text.contains("post #42"))
    }
}
