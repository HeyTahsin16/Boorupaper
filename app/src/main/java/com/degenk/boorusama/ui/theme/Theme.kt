package com.degenk.boorusama.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.degenk.boorusama.model.Rating
import com.degenk.boorusama.model.TagCategory
import com.degenk.boorusama.model.ThemePreset

/**
 * Port of lib/core/tags/tag/src/types/tag_colors.dart
 */
@Immutable
data class TagColors(
    val artist: Color,
    val character: Color,
    val copyright: Color,
    val general: Color,
    val meta: Color
) {
    fun colorFor(category: TagCategory): Color = when (category) {
        TagCategory.ARTIST -> artist
        TagCategory.CHARACTER -> character
        TagCategory.COPYRIGHT -> copyright
        TagCategory.GENERAL -> general
        TagCategory.META -> meta
    }

    companion object {
        val Dark = TagColors(
            artist = Color(0xFFFF8A8B),
            character = Color(0xFF35C64A),
            copyright = Color(0xFFC797FF),
            general = Color(0xFF009BE6),
            meta = Color(0xFFD9BB62)
        )

        val Light = TagColors(
            artist = Color(0xFFC00004),
            character = Color(0xFF00AB2C),
            copyright = Color(0xFFA800AA),
            general = Color(0xFF00B1F8),
            meta = Color(0xFFFD9200)
        )
    }
}

val LocalTagColors = staticCompositionLocalOf { TagColors.Dark }

fun ratingBadgeColor(rating: Rating): Color = when (rating) {
    Rating.GENERAL -> Color(0xFF2E7D32)
    Rating.SENSITIVE -> Color(0xFF1565C0)
    Rating.QUESTIONABLE -> Color(0xFFEF6C00)
    Rating.EXPLICIT -> Color(0xFFC62828)
}

/**
 * Port of packages/kurumi/lib/src/theme/preset_color_schemes.dart
 */
object KurumiColorSchemes {
    val DanbooruDark: ColorScheme = darkColorScheme(
        primary = Color(0xFF019AE6),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF004B73),
        onPrimaryContainer = Color(0xFFCDE5FF),
        secondary = Color(0xFF019AE6),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF282C3C),
        onSecondaryContainer = Color(0xFFE0E2EE),
        surface = Color(0xFF1F1E2D),
        onSurface = Color(0xFFE4E1E9),
        surfaceVariant = Color(0xFF2E2F42),
        onSurfaceVariant = Color(0xFFA8A8B8),
        surfaceContainerLowest = Color(0xFF13141F),
        surfaceContainerLow = Color(0xFF222433),
        surfaceContainer = Color(0xFF2E2F42),
        surfaceContainerHigh = Color(0xFF36384E),
        surfaceContainerHighest = Color(0xFF42445C),
        background = Color(0xFF1F1E2D),
        onBackground = Color(0xFFE4E1E9),
        error = Color(0xFFC10105),
        onError = Color.White,
        outline = Color(0xFF56586E),
        outlineVariant = Color(0xFF3A3C50)
    )

    val DanbooruLight: ColorScheme = lightColorScheme(
        primary = Color(0xFF0174F9),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD8E6FF),
        onPrimaryContainer = Color(0xFF001D36),
        secondary = Color(0xFF0174F9),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFECEFF7),
        onSecondaryContainer = Color(0xFF1A1C22),
        surface = Color(0xFFFEFEFF),
        onSurface = Color(0xFF191C20),
        surfaceVariant = Color(0xFFECEFF7),
        onSurfaceVariant = Color(0xFF5A5E6B),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF6F8FD),
        surfaceContainer = Color(0xFFECEFF7),
        surfaceContainerHigh = Color(0xFFE3E7F1),
        surfaceContainerHighest = Color(0xFFDADEEB),
        background = Color(0xFFFEFEFF),
        onBackground = Color(0xFF191C20),
        error = Color(0xFFD42B30),
        onError = Color.White,
        outline = Color(0xFFC2C6D4),
        outlineVariant = Color(0xFFE0E3EE)
    )

    val OledBlack: ColorScheme = DanbooruDark.copy(
        surface = Color.Black,
        background = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0D0E14),
        surfaceContainer = Color(0xFF161822),
        surfaceContainerHigh = Color(0xFF202230)
    )

    val GreenLight: ColorScheme = lightColorScheme(
        primary = Color(0xFF3D6938),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFBEF0B3),
        onPrimaryContainer = Color(0xFF002202),
        secondary = Color(0xFF53634E),
        surface = Color(0xFFF8FBF1),
        onSurface = Color(0xFF191D17),
        surfaceContainerLow = Color(0xFFF2F5EB),
        surfaceContainer = Color(0xFFECEFE5),
        background = Color(0xFFF8FBF1)
    )

    val DarkGreen: ColorScheme = darkColorScheme(
        primary = Color(0xFFA3D399),
        onPrimary = Color(0xFF0C390E),
        primaryContainer = Color(0xFF255023),
        onPrimaryContainer = Color(0xFFBEF0B3),
        secondary = Color(0xFFBACCB3),
        surface = Color(0xFF11140F),
        onSurface = Color(0xFFE1E4DA),
        surfaceContainerLow = Color(0xFF191D17),
        surfaceContainer = Color(0xFF1D211B),
        background = Color(0xFF11140F)
    )

    val CoralPink: ColorScheme = lightColorScheme(
        primary = Color(0xFFEA4C89),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD9E2),
        onPrimaryContainer = Color(0xFF3E001D),
        secondary = Color(0xFF74565F),
        surface = Color(0xFFFFF8F8),
        onSurface = Color(0xFF22191C),
        surfaceContainerLow = Color(0xFFFFF0F2),
        surfaceContainer = Color(0xFFFCE8EC),
        background = Color(0xFFFFF8F8)
    )

    val HackerMatrix: ColorScheme = darkColorScheme(
        primary = Color(0xFF00E676),
        onPrimary = Color(0xFF00210E),
        primaryContainer = Color(0xFF00391C),
        onPrimaryContainer = Color(0xFF69F0AE),
        secondary = Color(0xFF00C853),
        surface = Color(0xFF0A0F0D),
        onSurface = Color(0xFFD0F0DC),
        surfaceContainerLow = Color(0xFF111915),
        surfaceContainer = Color(0xFF17231D),
        background = Color(0xFF0A0F0D)
    )

    val Cyberpunk: ColorScheme = darkColorScheme(
        primary = Color(0xFFFF007F),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF5A002C),
        onPrimaryContainer = Color(0xFFFFD8E6),
        secondary = Color(0xFF00F0FF),
        onSecondary = Color(0xFF002022),
        surface = Color(0xFF120B1D),
        onSurface = Color(0xFFF3E5F5),
        surfaceContainerLow = Color(0xFF1B112C),
        surfaceContainer = Color(0xFF25173B),
        background = Color(0xFF120B1D)
    )
}

val BoorusamaTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

val BoorusamaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun BoorusamaTheme(
    themePreset: ThemePreset = ThemePreset.DANBOORU_DARK,
    dynamicColorEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = themePreset.isDark

    val colorScheme = when {
        dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themePreset == ThemePreset.DANBOORU_DARK -> KurumiColorSchemes.DanbooruDark
        themePreset == ThemePreset.DANBOORU_LIGHT -> KurumiColorSchemes.DanbooruLight
        themePreset == ThemePreset.OLED_BLACK -> KurumiColorSchemes.OledBlack
        themePreset == ThemePreset.GREEN -> KurumiColorSchemes.GreenLight
        themePreset == ThemePreset.DARK_GREEN -> KurumiColorSchemes.DarkGreen
        themePreset == ThemePreset.CORAL_PINK -> KurumiColorSchemes.CoralPink
        themePreset == ThemePreset.HACKER -> KurumiColorSchemes.HackerMatrix
        themePreset == ThemePreset.CYBERPUNK -> KurumiColorSchemes.Cyberpunk
        themePreset == ThemePreset.DYNAMIC_DARK -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context)
            else KurumiColorSchemes.DanbooruDark
        }
        themePreset == ThemePreset.DYNAMIC_LIGHT -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context)
            else KurumiColorSchemes.DanbooruLight
        }
        else -> KurumiColorSchemes.DanbooruDark
    }

    val tagColors = if (isDark) TagColors.Dark else TagColors.Light

    CompositionLocalProvider(LocalTagColors provides tagColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BoorusamaTypography,
            shapes = BoorusamaShapes,
            content = content
        )
    }
}
