package com.ochat.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochat.android.ui.theme.LocalOChatColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Timestamp shown on a conversation row, using the same escalation as WhatsApp:
 * time of day today, "Yesterday", weekday within the last week, then a short date.
 */
fun formatConversationTimestamp(date: Date?, now: Date = Date()): String {
    if (date == null) return ""
    val cal = Calendar.getInstance().apply { time = date }
    val nowCal = Calendar.getInstance().apply { time = now }

    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    if (sameDay(cal, nowCal)) {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }
    val yesterday = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (sameDay(cal, yesterday)) return "Yesterday"

    val weekAgo = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
    if (date.after(weekAgo.time)) {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
    }
    return SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
}

/**
 * Deterministic avatar colour derived from a stable seed, so the same peer keeps the
 * same colour across the chat list, the people list and the conversation header.
 */
fun avatarColorFor(seed: String, isDark: Boolean): Color {
    var hash = 0
    for (c in seed) hash = c.code + ((hash shl 5) - hash)
    val hue = ((hash % 360) + 360) % 360
    return Color.hsl(
        hue = hue.toFloat(),
        saturation = if (isDark) 0.42f else 0.55f,
        lightness = if (isDark) 0.42f else 0.62f
    )
}

private fun initialsFor(title: String): String {
    val cleaned = title.trimStart('#', '@').trim()
    if (cleaned.isEmpty()) return "?"
    return cleaned.first().uppercase()
}

/**
 * Circular avatar: an icon when one is supplied (channels, mesh), otherwise the
 * conversation's first letter on a seeded colour.
 */
@Composable
fun ConversationAvatar(
    title: String,
    seed: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    val colors = LocalOChatColors.current
    val bg = avatarColorFor(seed, colors.isDark)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.5f)
            )
        } else {
            Text(
                text = initialsFor(title),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}

/** Amber pill showing the unread count, capped like WhatsApp's badge. */
@Composable
fun UnreadCountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val colors = LocalOChatColors.current
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.unreadBadge)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = colors.onUnreadBadge,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * A single conversation-list row: avatar, title, preview, timestamp and unread badge.
 */
@Composable
fun ConversationRowItem(
    row: ConversationRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarIcon: ImageVector? = null,
    trailingTitleIcon: ImageVector? = null
) {
    val colors = LocalOChatColors.current
    val hasUnread = row.unreadCount > 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            ConversationAvatar(title = row.title, seed = row.avatarSeed, icon = avatarIcon)
            if (row.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(colors.chatWallpaper)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (trailingTitleIcon != null) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = trailingTitleIcon,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.size(2.dp))
            val secondary = row.participantCount
                ?.let { c -> if (c == 1) "1 person here" else "$c people here" }
                ?: row.preview
            if (secondary.isNotEmpty()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasUnread) MaterialTheme.colorScheme.onSurface else colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = formatConversationTimestamp(row.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = if (hasUnread) colors.unreadBadge else colors.textMuted
            )
            UnreadCountBadge(row.unreadCount)
        }
    }
}
