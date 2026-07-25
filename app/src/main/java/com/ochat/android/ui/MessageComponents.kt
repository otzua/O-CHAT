package com.ochat.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
 

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import android.content.Intent
import android.net.Uri
import com.ochat.android.model.BitchatMessage
import com.ochat.android.model.DeliveryStatus
import com.ochat.android.mesh.MeshService
import java.text.SimpleDateFormat
import java.util.*
import com.ochat.android.ui.media.VoiceNotePlayer
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import com.ochat.android.ui.media.FileMessageItem
import com.ochat.android.model.BitchatMessageType
import com.ochat.android.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.core.tween
import com.ochat.android.ui.home.ANIM_MEDIUM
import com.ochat.android.ui.home.BubbleMeta
import com.ochat.android.ui.home.DeliveryTicks
import com.ochat.android.ui.home.SystemMessagePill
import com.ochat.android.ui.home.bubbleShape
import com.ochat.android.ui.theme.LocalOChatColors


// VoiceNotePlayer moved to com.ochat.android.ui.media.VoiceNotePlayer

/**
 * Message display components for ChatScreen
 * Extracted from ChatScreen.kt for better organization
 */

@Composable
fun MessagesList(
    messages: List<BitchatMessage>,
    currentUserNickname: String,
    meshService: MeshService,
    modifier: Modifier = Modifier,
    forceScrollToBottom: Boolean = false,
    onScrolledUpChanged: ((Boolean) -> Unit)? = null,
    onNicknameClick: ((String) -> Unit)? = null,
    onMessageLongPress: ((BitchatMessage) -> Unit)? = null,
    onCancelTransfer: ((BitchatMessage) -> Unit)? = null,
    onImageClick: ((String, List<String>, Int) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    
    // Track if this is the first time messages are being loaded
    var hasScrolledToInitialPosition by remember { mutableStateOf(false) }
    var followIncomingMessages by remember { mutableStateOf(true) }
    
    // Smart scroll: auto-scroll to bottom for initial load, then follow unless user scrolls away
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val isFirstLoad = !hasScrolledToInitialPosition
            if (isFirstLoad || followIncomingMessages) {
                listState.scrollToItem(0)
                if (isFirstLoad) {
                    hasScrolledToInitialPosition = true
                }
            }
        }
    }
    
    // Track whether user has scrolled away from the latest messages
    val isAtLatest by remember {
        derivedStateOf {
            val firstVisibleIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: -1
            firstVisibleIndex <= 2
        }
    }
    LaunchedEffect(isAtLatest) {
        followIncomingMessages = isAtLatest
        onScrolledUpChanged?.invoke(!isAtLatest)
    }
    
    // Force scroll to bottom when requested (e.g., when user sends a message)
    LaunchedEffect(forceScrollToBottom) {
        if (messages.isNotEmpty()) {
            // With reverseLayout=true and reversed data, latest is at index 0
            followIncomingMessages = true
            listState.scrollToItem(0)
        }
    }
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
        reverseLayout = true
    ) {
        items(
            items = messages.asReversed(),
            key = { it.id }
        ) { message ->
                MessageItem(
                    message = message,
                    messages = messages,
                    currentUserNickname = currentUserNickname,
                    meshService = meshService,
                    onNicknameClick = onNicknameClick,
                    onMessageLongPress = onMessageLongPress,
                    onCancelTransfer = onCancelTransfer,
                    onImageClick = onImageClick,
                    // Arriving messages settle into place instead of appearing instantly.
                    // Placement-only, so it costs a translation and nothing is re-laid out.
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(ANIM_MEDIUM),
                        placementSpec = tween(ANIM_MEDIUM),
                        fadeOutSpec = null
                    )
                )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: BitchatMessage,
    currentUserNickname: String,
    meshService: MeshService,
    messages: List<BitchatMessage> = emptyList(),
    onNicknameClick: ((String) -> Unit)? = null,
    onMessageLongPress: ((BitchatMessage) -> Unit)? = null,
    onCancelTransfer: ((BitchatMessage) -> Unit)? = null,
    onImageClick: ((String, List<String>, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val ochatColors = LocalOChatColors.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // System notices are conversation events, not messages - they get a centered pill.
    if (message.sender == "system") {
        SystemMessagePill(
            text = message.content,
            modifier = modifier.padding(vertical = 4.dp)
        )
        return
    }

    val isSelf = message.senderPeerID == meshService.myPeerID ||
        message.sender == currentUserNickname ||
        message.sender.startsWith("$currentUserNickname#")

    // Media items bring their own internal layout; wrapping them in a padded text bubble
    // would double up their chrome, so they sit in a bare aligned container instead.
    val isMedia = message.type != BitchatMessageType.Message

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(bubbleShape(isSelf))
                .background(if (isSelf) ochatColors.bubbleSent else ochatColors.bubbleReceived)
                .padding(
                    start = if (isMedia) 4.dp else 10.dp,
                    end = if (isMedia) 4.dp else 10.dp,
                    top = if (isMedia) 4.dp else 6.dp,
                    bottom = 4.dp
                ),
            horizontalAlignment = Alignment.Start
        ) {
            // Sender label, shown only for other people's messages in multi-party timelines.
            if (!isSelf && !message.isPrivate) {
                val (baseName, suffix) = splitSuffix(message.sender)
                Text(
                    text = truncateNickname(baseName) + suffix,
                    color = getPeerColor(message, ochatColors.isDark),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .clickable {
                            onNicknameClick?.invoke(message.originalSender ?: message.sender)
                        }
                )
            }

            MessageTextWithClickableNicknames(
                message = message,
                messages = messages,
                currentUserNickname = currentUserNickname,
                meshService = meshService,
                colorScheme = colorScheme,
                timeFormatter = timeFormatter,
                onNicknameClick = onNicknameClick,
                onMessageLongPress = onMessageLongPress,
                onCancelTransfer = onCancelTransfer,
                onImageClick = onImageClick,
                modifier = Modifier
            )

            BubbleMeta(
                timestamp = message.timestamp,
                isSelf = isSelf,
                deliveryStatus = message.deliveryStatus,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
    private fun MessageTextWithClickableNicknames(
        message: BitchatMessage,
        messages: List<BitchatMessage>,
        currentUserNickname: String,
        meshService: MeshService,
        colorScheme: ColorScheme,
        timeFormatter: SimpleDateFormat,
        onNicknameClick: ((String) -> Unit)?,
        onMessageLongPress: ((BitchatMessage) -> Unit)?,
        onCancelTransfer: ((BitchatMessage) -> Unit)?,
        onImageClick: ((String, List<String>, Int) -> Unit)?,
        modifier: Modifier = Modifier
    ) {
    // Image special rendering
    if (message.type == BitchatMessageType.Image) {
        com.ochat.android.ui.media.ImageMessageItem(
            message = message,
            messages = messages,
            currentUserNickname = currentUserNickname,
            meshService = meshService,
            colorScheme = colorScheme,
            timeFormatter = timeFormatter,
            onNicknameClick = onNicknameClick,
            onMessageLongPress = onMessageLongPress,
            onCancelTransfer = onCancelTransfer,
            onImageClick = onImageClick,
            modifier = modifier
        )
        return
    }

    // Voice note special rendering
    if (message.type == BitchatMessageType.Audio) {
        com.ochat.android.ui.media.AudioMessageItem(
            message = message,
            currentUserNickname = currentUserNickname,
            meshService = meshService,
            colorScheme = colorScheme,
            timeFormatter = timeFormatter,
            onNicknameClick = onNicknameClick,
            onMessageLongPress = onMessageLongPress,
            onCancelTransfer = onCancelTransfer,
            modifier = modifier
        )
        return
    }

    // File special rendering
    if (message.type == BitchatMessageType.File) {
        val path = message.content.trim()
        // Derive sending progress if applicable
        val (overrideProgress, _) = when (val st = message.deliveryStatus) {
            is com.ochat.android.model.DeliveryStatus.PartiallyDelivered -> {
                if (st.total > 0 && st.reached < st.total) {
                    (st.reached.toFloat() / st.total.toFloat()) to Color(0xFF1E88E5) // blue while sending
                } else null to null
            }
            else -> null to null
        }
        Column(modifier = modifier.fillMaxWidth()) {
            // Header: nickname + timestamp line above the file, identical styling to text messages
            val headerText = formatMessageHeaderAnnotatedString(
                message = message,
                currentUserNickname = currentUserNickname,
                meshService = meshService,
                colorScheme = colorScheme,
                timeFormatter = timeFormatter
            )
            val haptic = LocalHapticFeedback.current
            var headerLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
            Text(
                text = headerText,
                fontFamily = FontFamily.Monospace,
                color = colorScheme.onSurface,
                modifier = Modifier.pointerInput(message.id) {
                    detectTapGestures(onTap = { pos ->
                        val layout = headerLayout ?: return@detectTapGestures
                        val offset = layout.getOffsetForPosition(pos)
                        val ann = headerText.getStringAnnotations("nickname_click", offset, offset)
                        if (ann.isNotEmpty() && onNicknameClick != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNicknameClick.invoke(ann.first().item)
                        }
                    }, onLongPress = { onMessageLongPress?.invoke(message) })
                },
                onTextLayout = { headerLayout = it }
            )

            // Try to load the file packet from the path
            val packet = try {
                val file = java.io.File(path)
                if (file.exists()) {
                    // Create a temporary BitchatFilePacket for display
                    // In a real implementation, this would be stored with the packet metadata
                    com.ochat.android.model.BitchatFilePacket(
                        fileName = file.name,
                        fileSize = file.length(),
                        mimeType = com.ochat.android.features.file.FileUtils.getMimeTypeFromExtension(file.name),
                        content = file.readBytes()
                    )
                } else null
            } catch (e: Exception) {
                null
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Box {
                    if (packet != null) {
                        if (overrideProgress != null) {
                            // Show sending animation while in-flight
                            com.ochat.android.ui.media.FileSendingAnimation(
                                fileName = packet.fileName,
                                progress = overrideProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // Static file display with open/save dialog
                            FileMessageItem(
                                packet = packet,
                                onFileClick = {
                                    // handled inside FileMessageItem via dialog
                                }
                            )
                        }

                        // Cancel button overlay during sending
                        val showCancel = message.sender == currentUserNickname && (message.deliveryStatus is DeliveryStatus.PartiallyDelivered)
                        if (showCancel) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .background(Color.Gray.copy(alpha = 0.6f), CircleShape)
                                    .clickable { onCancelTransfer?.invoke(message) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.cd_cancel), tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    } else {
                        Text(text = stringResource(R.string.file_unavailable), fontFamily = FontFamily.Monospace, color = Color.Gray)
                    }
                }
            }
        }
        return
    }

    // Check if this message should be animated during PoW mining
    val shouldAnimate = shouldAnimateMessage(message.id)
    
    // If animation is needed, use the matrix animation component for content only
    if (shouldAnimate) {
        // Display message with matrix animation for content
        MessageWithMatrixAnimation(
            message = message,
            messages = messages,
            currentUserNickname = currentUserNickname,
            meshService = meshService,
            colorScheme = colorScheme,
            timeFormatter = timeFormatter,
            onNicknameClick = onNicknameClick,
            onMessageLongPress = onMessageLongPress,
            onImageClick = onImageClick,
            modifier = modifier
        )
    } else {
        // Normal message display. The bubble draws the sender label, timestamp and ticks,
        // so only the body is formatted here.
        val ochatColors = LocalOChatColors.current
        val isSelfMessage = message.senderPeerID == meshService.myPeerID ||
            message.sender == currentUserNickname ||
            message.sender.startsWith("$currentUserNickname#")
        val annotatedText = formatMessageContentAsAnnotatedString(
            message = message,
            currentUserNickname = currentUserNickname,
            isSelf = isSelfMessage,
            contentColor = ochatColors.bubbleTextPrimary,
            isDark = ochatColors.isDark
        )

        // Own nickname is not a tap target
        val isSelf = isSelfMessage

        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        Text(
            text = annotatedText,
            modifier = modifier.pointerInput(message) {
                detectTapGestures(
                    onTap = { position ->
                        val layout = textLayoutResult ?: return@detectTapGestures
                        val offset = layout.getOffsetForPosition(position)
                        // Nickname click only when not self
                        if (!isSelf && onNicknameClick != null) {
                            val nicknameAnnotations = annotatedText.getStringAnnotations(
                                tag = "nickname_click",
                                start = offset,
                                end = offset
                            )
                            if (nicknameAnnotations.isNotEmpty()) {
                                val nickname = nicknameAnnotations.first().item
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNicknameClick.invoke(nickname)
                                return@detectTapGestures
                            }
                        }
                        // Geohash teleport (all messages)
                        val geohashAnnotations = annotatedText.getStringAnnotations(
                            tag = "geohash_click",
                            start = offset,
                            end = offset
                        )
                        if (geohashAnnotations.isNotEmpty()) {
                            val geohash = geohashAnnotations.first().item
                            try {
                                val locationManager = com.ochat.android.geohash.LocationChannelManager.getInstance(
                                    context
                                )
                                val level = when (geohash.length) {
                                    in 0..2 -> com.ochat.android.geohash.GeohashChannelLevel.REGION
                                    in 3..4 -> com.ochat.android.geohash.GeohashChannelLevel.PROVINCE
                                    5 -> com.ochat.android.geohash.GeohashChannelLevel.CITY
                                    6 -> com.ochat.android.geohash.GeohashChannelLevel.NEIGHBORHOOD
                                    else -> com.ochat.android.geohash.GeohashChannelLevel.BLOCK
                                }
                                val channel = com.ochat.android.geohash.GeohashChannel(level, geohash.lowercase())
                                locationManager.setTeleported(true)
                                locationManager.select(com.ochat.android.geohash.ChannelID.Location(channel))
                            } catch (_: Exception) { }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            return@detectTapGestures
                        }
                        // URL open (all messages)
                        val urlAnnotations = annotatedText.getStringAnnotations(
                            tag = "url_click",
                            start = offset,
                            end = offset
                        )
                        if (urlAnnotations.isNotEmpty()) {
                            val raw = urlAnnotations.first().item
                            val resolved = if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) raw else "https://$raw"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resolved))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            return@detectTapGestures
                        }
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMessageLongPress?.invoke(message)
                    }
                )
            },
            fontFamily = FontFamily.Monospace,
            softWrap = true,
            overflow = TextOverflow.Visible,
            style = androidx.compose.ui.text.TextStyle(
                color = colorScheme.onSurface
            ),
            onTextLayout = { result -> textLayoutResult = result }
        )
    }
}

/**
 * Delivery indicator.
 *
 * Now delegates to [DeliveryTicks], which draws the ticks as vector paths. The previous
 * implementation rendered check characters from strings.xml; those string values are gone,
 * so any remaining caller routes here to keep a single source of truth for the shapes.
 */
@Composable
fun DeliveryStatusIcon(status: DeliveryStatus) {
    DeliveryTicks(status = status)
}
