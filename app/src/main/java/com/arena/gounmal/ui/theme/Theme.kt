package com.arena.gounmal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 고운글 팔레트.
 *
 * iOS 의 "선택 영역" 느낌을 흉내 내기 위해
 *  - 하이라이트: 아주 옅은 파랑(반투명)
 *  - 하이라이트 양 끝 커서: 짙은 파랑
 *  을 한 쌍으로 쓴다.
 */
@Immutable
data class GounmalPalette(
    val background: Color,
    val backgroundElevated: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val outline: Color,
    val outlineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentStrong: Color,
    val accentSoft: Color,
    val highlightFillTop: Color,
    val highlightFillBottom: Color,
    val highlightStroke: Color,
    val highlightBorder: Color,
    val caret: Color,
    val caretGlow: Color,
    val danger: Color,
    val success: Color,
    val bubble: Color,
    val bubbleShadow: Color,
    val isDark: Boolean,
)

private val LightPalette = GounmalPalette(
    background = Color(0xFFF1F3F9),
    backgroundElevated = Color(0xFFE8ECF6),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF5F7FC),
    outline = Color(0xFFE1E6F0),
    outlineStrong = Color(0xFFC9D2E4),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF5B6579),
    textTertiary = Color(0xFF8C95A8),
    accent = Color(0xFF2F6BFF),
    accentStrong = Color(0xFF1B4BE0),
    accentSoft = Color(0xFFE8EFFF),
    highlightFillTop = Color(0x333B7BFF),
    highlightFillBottom = Color(0x1F2F6BFF),
    highlightStroke = Color(0x4D2F6BFF),
    highlightBorder = Color(0x802F6BFF),
    caret = Color(0xFF1547D8),
    caretGlow = Color(0x551547D8),
    danger = Color(0xFFE05252),
    success = Color(0xFF1F9D74),
    bubble = Color(0xFFFDFEFF),
    bubbleShadow = Color(0x1F1B2A4A),
    isDark = false,
)

private val DarkPalette = GounmalPalette(
    background = Color(0xFF0A0E15),
    backgroundElevated = Color(0xFF10151F),
    surface = Color(0xFF151B26),
    surfaceMuted = Color(0xFF1B222F),
    outline = Color(0xFF232C3B),
    outlineStrong = Color(0xFF33405A),
    textPrimary = Color(0xFFF3F6FC),
    textSecondary = Color(0xFFA6B1C6),
    textTertiary = Color(0xFF7A859B),
    accent = Color(0xFF6E9BFF),
    accentStrong = Color(0xFF8FB3FF),
    accentSoft = Color(0xFF1B2740),
    highlightFillTop = Color(0x3D6E9BFF),
    highlightFillBottom = Color(0x1F4D82FF),
    highlightStroke = Color(0x596E9BFF),
    highlightBorder = Color(0x99749EFF),
    caret = Color(0xFF9DBCFF),
    caretGlow = Color(0x669DBCFF),
    danger = Color(0xFFFF8A8A),
    success = Color(0xFF57D3AC),
    bubble = Color(0xFF1A2230),
    bubbleShadow = Color(0x66000000),
    isDark = true,
)

val LocalGounmalPalette = staticCompositionLocalOf { LightPalette }

/**
 * 반복(숨쉬기) 애니메이션 사용 여부.
 * 스크린샷 테스트·미리보기처럼 화면이 계속 "멈춰" 있어야 하는 곳에서는 끈다.
 */
val LocalGounmalMotion = staticCompositionLocalOf { true }

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2F6BFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EFFF),
    onPrimaryContainer = Color(0xFF12306E),
    secondary = Color(0xFF5B6579),
    background = Color(0xFFF1F3F9),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF5F7FC),
    onSurfaceVariant = Color(0xFF5B6579),
    outline = Color(0xFFE1E6F0),
    error = Color(0xFFE05252),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6E9BFF),
    onPrimary = Color(0xFF0A0E15),
    primaryContainer = Color(0xFF1B2740),
    onPrimaryContainer = Color(0xFFD8E4FF),
    secondary = Color(0xFFA6B1C6),
    background = Color(0xFF0A0E15),
    onBackground = Color(0xFFF3F6FC),
    surface = Color(0xFF151B26),
    onSurface = Color(0xFFF3F6FC),
    surfaceVariant = Color(0xFF1B222F),
    onSurfaceVariant = Color(0xFFA6B1C6),
    outline = Color(0xFF232C3B),
    error = Color(0xFFFF8A8A),
)

private val GounmalTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = FontFamily.SansSerif),
        headlineMedium = headlineMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
        ),
        titleLarge = titleLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
        ),
        titleMedium = titleMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        bodyLarge = bodyLarge.copy(fontFamily = FontFamily.SansSerif, letterSpacing = (-0.1).sp),
        bodyMedium = bodyMedium.copy(fontFamily = FontFamily.SansSerif, letterSpacing = (-0.1).sp),
        labelLarge = labelLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.1).sp,
        ),
        labelMedium = labelMedium.copy(fontFamily = FontFamily.SansSerif),
        labelSmall = labelSmall.copy(fontFamily = FontFamily.SansSerif),
    )
}

/** 본문 편집기에 쓰는 텍스트 스타일 (한국어는 줄 간격을 넉넉히). */
fun editorTextStyle(color: Color): TextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = 17.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.15).sp,
    color = color,
)

@Composable
fun GounmalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    androidx.compose.runtime.CompositionLocalProvider(LocalGounmalPalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = GounmalTypography,
            content = content,
        )
    }
}
