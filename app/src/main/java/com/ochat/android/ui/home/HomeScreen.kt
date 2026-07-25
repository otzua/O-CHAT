package com.ochat.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabPosition
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ochat.android.geohash.ChannelID
import com.ochat.android.ui.ChatViewModel
import com.ochat.android.ui.theme.LocalOChatColors

private const val TAB_CHATS = 0
private const val TAB_CHANNELS = 1
private const val TAB_PEOPLE = 2
private const val TAB_COUNT = 3
private val TAB_LABELS = listOf("CHATS", "CHANNELS", "PEOPLE")

/**
 * Tab underline that tracks the pager offset continuously instead of snapping when the
 * swipe settles, so the indicator stays under your finger mid-drag.
 */
@Composable
private fun PagerTabIndicator(
    tabPositions: List<TabPosition>,
    pagerState: PagerState
) {
    val index = pagerState.currentPage.coerceIn(0, tabPositions.lastIndex)
    val fraction = pagerState.currentPageOffsetFraction
    val current = tabPositions[index]
    val target = tabPositions[(index + if (fraction >= 0) 1 else -1).coerceIn(0, tabPositions.lastIndex)]
    val t = kotlin.math.abs(fraction)

    val left = lerp(current.left, target.left, t)
    val width = lerp(current.width, target.width, t)

    Box(
        Modifier
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = left)
            .width(width)
            .height(3.dp)
            .background(
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
            )
    )
}

/**
 * OChat home: a WhatsApp-shaped shell over BitChat's existing state.
 *
 * Every row here is derived from flows the ChatViewModel already exposes; opening a row
 * delegates straight back to the ViewModel, so navigation carries no chat state of its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onOpenConversation: (ConversationId) -> Unit,
    onShowLocationChannels: () -> Unit,
    onShowAppInfo: () -> Unit
) {
    val colors = LocalOChatColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(initialPage = TAB_CHATS) { TAB_COUNT }
    val scope = rememberCoroutineScope()

    // settledPage lags the swipe until it snaps, so the tab row and FAB do not flicker
    // between states while a drag is in progress.
    val settledPage = pagerState.currentPage

    // Unread counts, surfaced on the tab labels so activity in a tab you are not looking
    // at is still visible. Derived so the top bar only recomposes when a count changes.
    val unreadPrivate by viewModel.unreadPrivateMessages.collectAsStateWithLifecycle()
    val unreadChannels by viewModel.unreadChannelMessages.collectAsStateWithLifecycle()
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val myPeerID = viewModel.myPeerID

    val chatsBadge = unreadPrivate.size
    val channelsBadge = remember(unreadChannels) { unreadChannels.values.sum() }
    val peopleCount = remember(connectedPeers, myPeerID) {
        connectedPeers.count { it != myPeerID }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "OChat",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            // Live mesh status. Crossfaded rather than swapped so the label
                            // does not pop as peers come and go.
                            Crossfade(
                                targetState = peopleCount,
                                animationSpec = tween(ANIM_MEDIUM),
                                label = "meshStatus"
                            ) { count ->
                                Text(
                                    text = when (count) {
                                        0 -> "searching for people nearby"
                                        1 -> "1 person nearby"
                                        else -> "$count people nearby"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (count > 0) colors.textMuted else colors.textMuted.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    actions = {
                        Box {
                            androidx.compose.material3.IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Location channels") },
                                    leadingIcon = { Icon(Icons.Filled.Place, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onShowLocationChannels()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("About OChat") },
                                    leadingIcon = { Icon(Icons.Filled.Info, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onShowAppInfo()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                TabRow(
                    selectedTabIndex = settledPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        // Indicator follows the drag continuously rather than jumping on
                        // settle, so the underline tracks your finger.
                        if (settledPage < tabPositions.size) {
                            PagerTabIndicator(tabPositions, pagerState)
                        }
                    }
                ) {
                    TAB_LABELS.forEachIndexed { index, label ->
                        val badge = when (index) {
                            TAB_CHATS -> chatsBadge
                            TAB_CHANNELS -> channelsBadge
                            else -> 0
                        }
                        Tab(
                            selected = settledPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    AnimatedVisibility(
                                        visible = badge > 0,
                                        enter = fadeIn(tween(ANIM_FAST)) + scaleIn(tween(ANIM_FAST), initialScale = 0.6f),
                                        exit = fadeOut(tween(ANIM_FAST)) + scaleOut(tween(ANIM_FAST), targetScale = 0.6f)
                                    ) {
                                        Row {
                                            Spacer(Modifier.size(6.dp))
                                            UnreadCountBadge(badge)
                                        }
                                    }
                                }
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = colors.textMuted
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = settledPage != TAB_PEOPLE,
                enter = fadeIn(tween(ANIM_FAST)) + scaleIn(tween(ANIM_FAST), initialScale = 0.8f),
                exit = fadeOut(tween(ANIM_FAST)) + scaleOut(tween(ANIM_FAST), targetScale = 0.8f)
            ) {
                FloatingActionButton(
                    onClick = {
                        // Both remaining tabs start a new conversation from the people list.
                        scope.launch { pagerState.animateScrollToPage(TAB_PEOPLE) }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New chat")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                // Only the visible page is composed. Pre-composing neighbours would build
                // three lists at once on a cold start, which is exactly the kind of cost a
                // low-end device feels.
                beyondViewportPageCount = 0,
                key = { it }
            ) { page ->
                when (page) {
                    TAB_CHATS -> ChatsTab(viewModel, onOpenConversation)
                    TAB_CHANNELS -> ChannelsTab(viewModel, onOpenConversation, onShowLocationChannels)
                    else -> PeopleTab(viewModel, onOpenConversation)
                }
            }
            CreditFooterLine()
        }
    }
}

@Composable
private fun ChatsTab(viewModel: ChatViewModel, onOpen: (ConversationId) -> Unit) {
    val privateChats by viewModel.privateChats.collectAsStateWithLifecycle()
    val unread by viewModel.unreadPrivateMessages.collectAsStateWithLifecycle()
    val peerNicknames by viewModel.peerNicknames.collectAsStateWithLifecycle()
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()

    val rows = privateChats.map { (peerID, messages) ->
        val last = messages.lastOrNull()
        ConversationRow(
            id = ConversationId.Private(peerID),
            title = peerNicknames[peerID] ?: peerID.take(12),
            preview = previewFor(last, includeSender = false, selfNickname = nickname),
            timestamp = last?.timestamp,
            // BitChat tracks unread per peer as a flag, not a count; show a dot-equivalent of 1.
            unreadCount = if (unread.contains(peerID)) 1 else 0,
            avatarSeed = peerID,
            isOnline = connectedPeers.contains(peerID)
        )
    }.sortedForDisplay()

    if (rows.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Lock,
            title = "No chats yet",
            message = "Private chats are end-to-end encrypted. Open the PEOPLE tab to message someone nearby."
        )
    } else {
        ConversationList(rows, onOpen)
    }
}

@Composable
private fun ChannelsTab(
    viewModel: ChatViewModel,
    onOpen: (ConversationId) -> Unit,
    onShowLocationChannels: () -> Unit
) {
    val joinedChannels by viewModel.joinedChannels.collectAsStateWithLifecycle()
    val channelMessages by viewModel.channelMessages.collectAsStateWithLifecycle()
    val unreadChannels by viewModel.unreadChannelMessages.collectAsStateWithLifecycle()
    val meshMessages by viewModel.messages.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val passwordProtected by viewModel.passwordProtectedChannels.collectAsStateWithLifecycle()
    val selectedLocation by viewModel.selectedLocationChannel.collectAsStateWithLifecycle()
    val participantCounts by viewModel.geohashParticipantCounts.collectAsStateWithLifecycle()

    val meshRow = ConversationRow(
        id = ConversationId.MeshPublic,
        title = "Mesh Public",
        preview = previewFor(meshMessages.lastOrNull(), includeSender = true, selfNickname = nickname),
        timestamp = meshMessages.lastOrNull()?.timestamp,
        unreadCount = 0,
        avatarSeed = "mesh-public"
    )

    val channelRows = joinedChannels.map { channel ->
        val msgs = channelMessages[channel].orEmpty()
        ConversationRow(
            id = ConversationId.Channel(channel),
            title = channel,
            preview = previewFor(msgs.lastOrNull(), includeSender = true, selfNickname = nickname),
            timestamp = msgs.lastOrNull()?.timestamp,
            unreadCount = unreadChannels[channel] ?: 0,
            avatarSeed = channel,
            isFavorite = passwordProtected.contains(channel)
        )
    }

    // Only the currently selected location channel has a live timeline to show.
    val geoRows = (selectedLocation as? ChannelID.Location)?.let { loc ->
        val geohash = loc.channel.geohash
        val msgs = channelMessages["geo:$geohash"].orEmpty()
        listOf(
            ConversationRow(
                id = ConversationId.Geohash(geohash),
                title = "#$geohash",
                preview = previewFor(msgs.lastOrNull(), includeSender = true, selfNickname = nickname),
                timestamp = msgs.lastOrNull()?.timestamp,
                unreadCount = unreadChannels["geo:$geohash"] ?: 0,
                avatarSeed = "geo-$geohash",
                participantCount = if (msgs.isEmpty()) participantCounts[geohash] else null
            )
        )
    }.orEmpty()

    val rows = (channelRows + geoRows).sortedForDisplay()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
        item {
            ConversationRowItem(
                row = meshRow,
                onClick = { onOpen(meshRow.id) },
                avatarIcon = Icons.Filled.Podcasts
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp),
                color = MaterialTheme.colorScheme.outline
            )
        }
        items(rows, key = { it.id.encode() }) { row ->
            ConversationRowItem(
                row = row,
                onClick = { onOpen(row.id) },
                avatarIcon = when (row.id) {
                    is ConversationId.Geohash -> Icons.Filled.Place
                    else -> Icons.Filled.Group
                },
                trailingTitleIcon = if (row.isFavorite) Icons.Filled.Lock else null
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp),
                color = MaterialTheme.colorScheme.outline
            )
        }
        item {
            DropdownStyleAction(
                label = "Browse location channels",
                icon = Icons.Filled.Place,
                onClick = onShowLocationChannels
            )
        }
    }
}

@Composable
private fun PeopleTab(viewModel: ChatViewModel, onOpen: (ConversationId) -> Unit) {
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val peerNicknames by viewModel.peerNicknames.collectAsStateWithLifecycle()
    val favorites by viewModel.favoritePeers.collectAsStateWithLifecycle()
    val fingerprints by viewModel.peerFingerprints.collectAsStateWithLifecycle()
    val myPeerID = viewModel.myPeerID

    val rows = connectedPeers
        .filter { it != myPeerID }
        .map { peerID ->
            val fp = fingerprints[peerID]
            ConversationRow(
                id = ConversationId.Private(peerID),
                title = peerNicknames[peerID] ?: peerID.take(12),
                preview = "",
                timestamp = null,
                unreadCount = 0,
                avatarSeed = peerID,
                isOnline = true,
                isFavorite = fp != null && favorites.contains(fp)
            )
        }
        .sortedWith(compareByDescending<ConversationRow> { it.isFavorite }.thenBy { it.title.lowercase() })

    if (rows.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Group,
            title = "Nobody nearby yet",
            message = "OChat finds people over Bluetooth mesh. Keep the app open and stay near other users."
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
            items(rows, key = { it.id.encode() }) { row ->
                ConversationRowItem(
                    row = row,
                    onClick = { onOpen(row.id) },
                    trailingTitleIcon = if (row.isFavorite) Icons.Filled.Star else null
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 78.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ConversationList(rows: List<ConversationRow>, onOpen: (ConversationId) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
        items(rows, key = { it.id.encode() }) { row ->
            ConversationRowItem(
                row = row,
                onClick = { onOpen(row.id) },
                // Rows reorder as conversations become most-recent; animate the move so the
                // list does not visibly jump under the finger.
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(ANIM_MEDIUM),
                    placementSpec = tween(ANIM_MEDIUM),
                    fadeOutSpec = null
                )
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun DropdownStyleAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    val colors = LocalOChatColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
