package com.ochat.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * OChat palette: amber accent on a warm near-black, with a matching light counterpart.
 *
 * Material's ColorScheme does not have slots for chat-specific surfaces (message bubbles,
 * delivery ticks, the chat wallpaper). Those live in [OChatColors] and are supplied through
 * [LocalOChatColors] so composables can read them the same way they read MaterialTheme.
 */

// -- Dark palette ------------------------------------------------------------

val AmberAccent = Color(0xFFFFA726)
val AmberAccentPressed = Color(0xFFE0891B)

internal val DarkBackground = Color(0xFF0F0D0A)   // app background / chat wallpaper
internal val DarkSurface = Color(0xFF1A1713)      // top bar, list rows, input bar
internal val DarkSurfaceHigh = Color(0xFF241F1A)  // menus, sheets, elevated cards
internal val DarkBubbleSent = Color(0xFF4A3316)   // own messages
internal val DarkBubbleRecv = Color(0xFF23201B)   // other people's messages
internal val DarkTextPrimary = Color(0xFFF2EDE6)
internal val DarkTextMuted = Color(0xFF9A8F80)
internal val DarkDivider = Color(0xFF2E2823)

// -- Light palette -----------------------------------------------------------

/** Darker than [AmberAccent] so it keeps contrast against light surfaces. */
val AmberAccentLight = Color(0xFFB26A00)

internal val LightBackground = Color(0xFFF7F2EA)  // warm off-white wallpaper
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceHigh = Color(0xFFFFFFFF)
internal val LightBubbleSent = Color(0xFFFFE3B8)  // own messages
internal val LightBubbleRecv = Color(0xFFFFFFFF)  // other people's messages
internal val LightTextPrimary = Color(0xFF1B1813)
internal val LightTextMuted = Color(0xFF7A7065)
internal val LightDivider = Color(0xFFE4DCD0)

// -- Shared semantic colors --------------------------------------------------

/** Read receipts stay blue in both themes; that reading is near-universal in messengers. */
val TickRead = Color(0xFF4FA3FF)
val ErrorRed = Color(0xFFE5534B)

/**
 * Chat-specific colors that Material's ColorScheme has no slot for.
 */
@Immutable
data class OChatColors(
    val isDark: Boolean,
    val chatWallpaper: Color,
    val bubbleSent: Color,
    val bubbleReceived: Color,
    val bubbleTextPrimary: Color,
    val textMuted: Color,
    val divider: Color,
    val surfaceHigh: Color,
    /** Ticks for Sent/Delivered; Read uses [TickRead]. */
    val tickPending: Color,
    val tickRead: Color,
    val unreadBadge: Color,
    val onUnreadBadge: Color
)

internal val DarkOChatColors = OChatColors(
    isDark = true,
    chatWallpaper = DarkBackground,
    bubbleSent = DarkBubbleSent,
    bubbleReceived = DarkBubbleRecv,
    bubbleTextPrimary = DarkTextPrimary,
    textMuted = DarkTextMuted,
    divider = DarkDivider,
    surfaceHigh = DarkSurfaceHigh,
    tickPending = DarkTextMuted,
    tickRead = TickRead,
    unreadBadge = AmberAccent,
    onUnreadBadge = Color(0xFF1B1207)
)

internal val LightOChatColors = OChatColors(
    isDark = false,
    chatWallpaper = LightBackground,
    bubbleSent = LightBubbleSent,
    bubbleReceived = LightBubbleRecv,
    bubbleTextPrimary = LightTextPrimary,
    textMuted = LightTextMuted,
    divider = LightDivider,
    surfaceHigh = LightSurfaceHigh,
    tickPending = LightTextMuted,
    tickRead = TickRead,
    unreadBadge = AmberAccentLight,
    onUnreadBadge = Color.White
)

/**
 * Defaults to the dark set so a composable that renders outside [OChatTheme]
 * (previews, detached dialogs) still gets sane colors instead of throwing.
 */
val LocalOChatColors = staticCompositionLocalOf { DarkOChatColors }
