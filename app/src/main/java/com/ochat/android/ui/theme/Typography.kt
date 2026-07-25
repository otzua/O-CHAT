package com.ochat.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Base font size for consistent scaling across the app
internal const val BASE_FONT_SIZE = com.ochat.android.util.AppConstants.UI.BASE_FONT_SIZE_SP

/**
 * OChat uses the platform sans-serif for everything a normal user reads.
 *
 * The terminal look is not gone, only demoted: surfaces where monospace carries real
 * meaning (fingerprints, geohashes, the debug sheet, the Matrix easter egg) opt in
 * explicitly via [MonospaceTextStyle] rather than inheriting it app-wide.
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = (BASE_FONT_SIZE + 1).sp,
        lineHeight = (BASE_FONT_SIZE + 7).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = BASE_FONT_SIZE.sp,
        lineHeight = (BASE_FONT_SIZE + 5).sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = (BASE_FONT_SIZE - 3).sp,
        lineHeight = (BASE_FONT_SIZE + 1).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (BASE_FONT_SIZE + 6).sp,
        lineHeight = (BASE_FONT_SIZE + 12).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (BASE_FONT_SIZE + 1).sp,
        lineHeight = (BASE_FONT_SIZE + 7).sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = BASE_FONT_SIZE.sp,
        lineHeight = (BASE_FONT_SIZE + 5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (BASE_FONT_SIZE + 3).sp,
        lineHeight = (BASE_FONT_SIZE + 9).sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = (BASE_FONT_SIZE - 1).sp,
        lineHeight = (BASE_FONT_SIZE + 4).sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = (BASE_FONT_SIZE - 2).sp,
        lineHeight = (BASE_FONT_SIZE + 3).sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = (BASE_FONT_SIZE - 4).sp,
        lineHeight = (BASE_FONT_SIZE + 1).sp
    )
)

/** Opt-in monospace for fingerprints, geohashes, debug output and the Matrix animation. */
val MonospaceTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = (BASE_FONT_SIZE - 1).sp,
    lineHeight = (BASE_FONT_SIZE + 4).sp
)
