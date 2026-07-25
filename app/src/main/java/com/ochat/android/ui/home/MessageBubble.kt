package com.ochat.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochat.android.model.DeliveryStatus
import com.ochat.android.ui.theme.LocalOChatColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Bubble corner radius; the corner nearest the sender is squared off to form the tail. */
private val BubbleRadius = 14.dp
private val BubbleTailRadius = 3.dp

fun bubbleShape(isSelf: Boolean) = RoundedCornerShape(
    topStart = if (isSelf) BubbleRadius else BubbleTailRadius,
    topEnd = if (isSelf) BubbleTailRadius else BubbleRadius,
    bottomStart = BubbleRadius,
    bottomEnd = BubbleRadius
)

/**
 * Delivery ticks drawn as vector paths rather than characters.
 *
 * The original build used literal check glyphs in strings.xml; drawing them keeps the
 * double-tick shape consistent across fonts and lets the read state take its own colour.
 */
@Composable
fun DeliveryTicks(status: DeliveryStatus, modifier: Modifier = Modifier) {
    val colors = LocalOChatColors.current

    val (tickCount, tint) = when (status) {
        is DeliveryStatus.Sending -> 0 to colors.tickPending
        is DeliveryStatus.Sent -> 1 to colors.tickPending
        is DeliveryStatus.Delivered -> 2 to colors.tickPending
        is DeliveryStatus.PartiallyDelivered -> 1 to colors.tickPending
        is DeliveryStatus.Read -> 2 to colors.tickRead
        is DeliveryStatus.Failed -> -1 to MaterialTheme.colorScheme.error
    }

    when (tickCount) {
        // Sending: a hollow clock-like ring reads as "not yet gone out".
        0 -> Box(
            modifier = modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Transparent)
        ) {
            androidx.compose.foundation.Canvas(Modifier.size(12.dp)) {
                drawCircle(
                    color = tint,
                    radius = size.minDimension / 2f - 1f,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }
        -1 -> Text(
            text = "!",
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier
        )
        else -> androidx.compose.foundation.Canvas(
            modifier = modifier.size(width = if (tickCount == 2) 16.dp else 11.dp, height = 11.dp)
        ) {
            val stroke = Stroke(
                width = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            fun tick(xOffset: Float) {
                val p = Path().apply {
                    moveTo(xOffset + size.height * 0.05f, size.height * 0.55f)
                    lineTo(xOffset + size.height * 0.35f, size.height * 0.85f)
                    lineTo(xOffset + size.height * 0.95f, size.height * 0.18f)
                }
                drawPath(p, color = tint, style = stroke)
            }
            tick(0f)
            if (tickCount == 2) tick(size.height * 0.45f)
        }
    }
}

/**
 * Timestamp (and ticks for own messages) shown on the trailing edge inside a bubble.
 */
@Composable
fun BubbleMeta(
    timestamp: Date,
    isSelf: Boolean,
    deliveryStatus: DeliveryStatus?,
    modifier: Modifier = Modifier
) {
    val colors = LocalOChatColors.current
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = formatter.format(timestamp),
            fontSize = 10.sp,
            color = colors.textMuted
        )
        if (isSelf && deliveryStatus != null) {
            DeliveryTicks(deliveryStatus)
        }
    }
}

/**
 * Centered pill used for system notices ("x joined", "y left"), matching how WhatsApp
 * separates system events from the conversation.
 */
@Composable
fun SystemMessagePill(text: String, modifier: Modifier = Modifier) {
    val colors = LocalOChatColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = colors.textMuted,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceHigh)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
