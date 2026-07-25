package com.ochat.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ochat.android.model.BitchatMessage
import com.ochat.android.ui.ChatInputSection
import com.ochat.android.ui.ChatViewModel
import com.ochat.android.ui.CommandSuggestion
import com.ochat.android.ui.MessagesList
import com.ochat.android.ui.media.FullScreenImageViewer
import com.ochat.android.ui.splitSuffix
import com.ochat.android.ui.theme.LocalOChatColors

/**
 * Full-screen conversation, replacing BitChat's bottom-sheet private chat.
 *
 * This screen owns no chat state. Opening it tells the ViewModel which conversation is
 * active - exactly as the old sheet did - so sends, receipts, notification clearing and
 * command handling keep working through their existing paths.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationId: ConversationId,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenUserSheet: (String, BitchatMessage?) -> Unit
) {
    val colors = LocalOChatColors.current
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val privateChats by viewModel.privateChats.collectAsStateWithLifecycle()
    val channelMessages by viewModel.channelMessages.collectAsStateWithLifecycle()
    val meshMessages by viewModel.messages.collectAsStateWithLifecycle()
    val peerNicknames by viewModel.peerNicknames.collectAsStateWithLifecycle()
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val showCommandSuggestions by viewModel.showCommandSuggestions.collectAsStateWithLifecycle()
    val commandSuggestions by viewModel.commandSuggestions.collectAsStateWithLifecycle()
    val showMentionSuggestions by viewModel.showMentionSuggestions.collectAsStateWithLifecycle()
    val mentionSuggestions by viewModel.mentionSuggestions.collectAsStateWithLifecycle()

    var messageText by remember { mutableStateOf(TextFieldValue("")) }
    var forceScrollToBottom by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var viewerPaths by remember { mutableStateOf(emptyList<String>()) }
    var viewerIndex by remember { mutableStateOf(0) }

    // Bind the ViewModel's active-conversation context to this screen's lifetime, and release
    // it on exit so background messages are not misrouted into a closed chat.
    DisposableEffect(conversationId) {
        when (conversationId) {
            is ConversationId.Private -> viewModel.startPrivateChat(conversationId.peerID)
            is ConversationId.Channel -> viewModel.switchToChannel(conversationId.name)
            is ConversationId.MeshPublic, is ConversationId.Geohash -> viewModel.switchToChannel(null)
        }
        onDispose {
            when (conversationId) {
                is ConversationId.Private -> viewModel.endPrivateChat()
                is ConversationId.Channel -> viewModel.switchToChannel(null)
                else -> Unit
            }
        }
    }

    val messages: List<BitchatMessage> = when (conversationId) {
        is ConversationId.Private -> privateChats[conversationId.peerID].orEmpty()
        is ConversationId.Channel -> channelMessages[conversationId.name].orEmpty()
        is ConversationId.Geohash -> channelMessages["geo:${conversationId.geohash}"].orEmpty()
        is ConversationId.MeshPublic -> meshMessages
    }

    val title = when (conversationId) {
        is ConversationId.Private ->
            peerNicknames[conversationId.peerID] ?: conversationId.peerID.take(12)
        is ConversationId.Channel -> conversationId.name
        is ConversationId.Geohash -> "#${conversationId.geohash}"
        is ConversationId.MeshPublic -> "Mesh Public"
    }

    val subtitle = when (conversationId) {
        is ConversationId.Private ->
            if (connectedPeers.contains(conversationId.peerID)) "online" else "offline"
        is ConversationId.MeshPublic -> "${connectedPeers.size} nearby"
        else -> null
    }

    val avatarIcon = when (conversationId) {
        is ConversationId.MeshPublic -> Icons.Filled.Podcasts
        is ConversationId.Channel -> Icons.Filled.Group
        is ConversationId.Geohash -> Icons.Filled.Place
        is ConversationId.Private -> null
    }

    // Geohash timelines are relayed over Nostr and do not carry mesh media transfers.
    val showMediaButtons = conversationId !is ConversationId.Geohash

    Scaffold(
        containerColor = colors.chatWallpaper,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (conversationId is ConversationId.Private) {
                                onOpenUserSheet(title.substringBefore('#'), null)
                            }
                        }
                    ) {
                        ConversationAvatar(
                            title = title,
                            seed = when (conversationId) {
                                is ConversationId.Private -> conversationId.peerID
                                else -> conversationId.encode()
                            },
                            icon = avatarIcon,
                            size = 36.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.chatWallpaper)
                .windowInsetsPadding(WindowInsets.ime)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            MessagesList(
                messages = messages,
                currentUserNickname = nickname,
                meshService = viewModel.meshServiceFacade,
                modifier = Modifier.weight(1f),
                forceScrollToBottom = forceScrollToBottom,
                onNicknameClick = { fullSenderName ->
                    val (baseName, hashSuffix) = splitSuffix(fullSenderName)
                    val mention = if (conversationId is ConversationId.Geohash && hashSuffix.isNotEmpty()) {
                        "@$baseName$hashSuffix"
                    } else {
                        "@$baseName"
                    }
                    val current = messageText.text
                    val newText = when {
                        current.isEmpty() -> "$mention "
                        current.endsWith(" ") -> "$current$mention "
                        else -> "$current $mention "
                    }
                    messageText = TextFieldValue(newText, TextRange(newText.length))
                },
                onMessageLongPress = { message ->
                    val (baseName, _) = splitSuffix(message.sender)
                    onOpenUserSheet(baseName, message)
                },
                onCancelTransfer = { msg -> viewModel.cancelMediaSend(msg.id) },
                onImageClick = { _, allPaths, index ->
                    viewerPaths = allPaths
                    viewerIndex = index
                    showImageViewer = true
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            ChatInputSection(
                messageText = messageText,
                onMessageTextChange = { newText ->
                    messageText = newText
                    viewModel.updateCommandSuggestions(newText.text)
                    viewModel.updateMentionSuggestions(newText.text)
                },
                onSend = {
                    if (messageText.text.trim().isNotEmpty()) {
                        viewModel.sendMessage(messageText.text.trim())
                        messageText = TextFieldValue("")
                        forceScrollToBottom = !forceScrollToBottom
                    }
                },
                onSendVoiceNote = { peer, channel, path -> viewModel.sendVoiceNote(peer, channel, path) },
                onSendImageNote = { peer, channel, path -> viewModel.sendImageNote(peer, channel, path) },
                onSendFileNote = { peer, channel, path -> viewModel.sendFileNote(peer, channel, path) },
                showCommandSuggestions = showCommandSuggestions,
                commandSuggestions = commandSuggestions,
                showMentionSuggestions = showMentionSuggestions,
                mentionSuggestions = mentionSuggestions,
                onCommandSuggestionClick = { suggestion: CommandSuggestion ->
                    val commandText = viewModel.selectCommandSuggestion(suggestion)
                    messageText = TextFieldValue(commandText, TextRange(commandText.length))
                },
                onMentionSuggestionClick = { mention: String ->
                    val mentionText = viewModel.selectMentionSuggestion(mention, messageText.text)
                    messageText = TextFieldValue(mentionText, TextRange(mentionText.length))
                },
                selectedPrivatePeer = (conversationId as? ConversationId.Private)?.peerID,
                currentChannel = (conversationId as? ConversationId.Channel)?.name,
                nickname = nickname,
                colorScheme = MaterialTheme.colorScheme,
                showMediaButtons = showMediaButtons
            )
        }
    }

    if (showImageViewer) {
        FullScreenImageViewer(
            imagePaths = viewerPaths,
            initialIndex = viewerIndex,
            onClose = { showImageViewer = false }
        )
    }
}
