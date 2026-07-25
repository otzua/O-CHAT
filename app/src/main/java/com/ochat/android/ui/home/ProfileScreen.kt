package com.ochat.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ochat.android.ui.ChatViewModel
import com.ochat.android.ui.theme.LocalOChatColors
import com.ochat.android.ui.theme.MonospaceTextStyle
import com.ochat.android.ui.theme.ThemePreference
import com.ochat.android.ui.theme.ThemePreferenceManager

/**
 * Profile and settings.
 *
 * Restores two things that became unreachable when the original chat screen was replaced:
 * editing your nickname, and the emergency data wipe. Both previously lived in the old
 * header (the wipe as an undocumented triple-tap), and neither had any entry point in the
 * rebuilt interface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val colors = LocalOChatColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val themePref by ThemePreferenceManager.themeFlow.collectAsStateWithLifecycle(
        initialValue = ThemePreference.System
    )

    var draftNickname by remember(nickname) { mutableStateOf(nickname) }
    var showWipeConfirm by remember { mutableStateOf(false) }
    var copiedLabel by remember { mutableStateOf<String?>(null) }

    val trimmed = draftNickname.trim()
    val isDirty = trimmed != nickname && trimmed.isNotEmpty()

    // setNickname persists and re-broadcasts an announce to the mesh, so it is committed
    // deliberately rather than on every keystroke - the original header re-announced on
    // each character typed.
    fun commitNickname() {
        if (isDirty) viewModel.setNickname(trimmed)
        focusManager.clearFocus()
    }

    // Don't silently discard an edit if the user navigates away with the keyboard open.
    val latestCommit = rememberUpdatedState(::commitNickname)
    DisposableEffect(Unit) { onDispose { latestCommit.value() } }

    // Keyed on the peer ID, which is a live getter over the mesh service. Clearing all data
    // recreates the identity in place without leaving this screen, so unkeyed remembers
    // would keep displaying the old fingerprint and Nostr key after a wipe - actively
    // dangerous, since a fingerprint is what someone reads out to verify you in person.
    val myPeerID = viewModel.myPeerID
    val fingerprint = remember(myPeerID) { runCatching { viewModel.getMyFingerprint() }.getOrNull() }
    val npub = remember(myPeerID) { runCatching { viewModel.getCurrentNpub() }.getOrNull() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { commitNickname(); onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = { Text("Profile", style = MaterialTheme.typography.titleMedium) },
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
                .verticalScroll(rememberScrollState())
        ) {
            // -- Identity header ------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ConversationAvatar(
                    title = nickname.ifEmpty { "?" },
                    seed = myPeerID,
                    size = 88.dp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = nickname.ifEmpty { "no nickname set" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (val n = connectedPeers.count { it != myPeerID }) {
                        0 -> "no one nearby right now"
                        1 -> "connected to 1 person"
                        else -> "connected to $n people"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }

            SectionHeader("Your nickname")
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = draftNickname,
                    onValueChange = { draftNickname = it },
                    singleLine = true,
                    label = { Text("Nickname") },
                    supportingText = {
                        Text(
                            "This is what people nearby see. It is not reserved or unique - " +
                                "others can pick the same name, which is why messages also show " +
                                "a short identity suffix."
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = isDirty,
                            enter = fadeIn(tween(ANIM_FAST)),
                            exit = fadeOut(tween(ANIM_FAST))
                        ) {
                            IconButton(onClick = { commitNickname() }) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Save nickname",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitNickname() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Changing this announces the new name to people nearby.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Appearance")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeChoice("System", Icons.Filled.SettingsBrightness, themePref.isSystem) {
                    ThemePreferenceManager.set(context, ThemePreference.System)
                }
                ThemeChoice("Light", Icons.Filled.LightMode, themePref.isLight) {
                    ThemePreferenceManager.set(context, ThemePreference.Light)
                }
                ThemeChoice("Dark", Icons.Filled.DarkMode, themePref.isDark) {
                    ThemePreferenceManager.set(context, ThemePreference.Dark)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Identity")
            IdentityRow(
                label = "Peer ID",
                value = myPeerID,
                explanation = "Identifies this device on the mesh. It changes if you wipe your data.",
                onCopy = {
                    clipboard.setText(AnnotatedString(myPeerID))
                    copiedLabel = "Peer ID"
                }
            )
            if (!fingerprint.isNullOrBlank()) {
                IdentityRow(
                    label = "Fingerprint",
                    value = fingerprint,
                    explanation = "Your encryption key fingerprint. Compare it with someone in " +
                        "person to be certain you are talking to them and not an impostor.",
                    onCopy = {
                        clipboard.setText(AnnotatedString(fingerprint))
                        copiedLabel = "Fingerprint"
                    }
                )
            }
            if (!npub.isNullOrBlank()) {
                IdentityRow(
                    label = "Nostr key",
                    value = npub,
                    explanation = "Used for location channels and for reaching people over the " +
                        "internet when they are out of Bluetooth range.",
                    onCopy = {
                        clipboard.setText(AnnotatedString(npub))
                        copiedLabel = "Nostr key"
                    }
                )
            }

            AnimatedVisibility(
                visible = copiedLabel != null,
                enter = fadeIn(tween(ANIM_FAST)),
                exit = fadeOut(tween(ANIM_MEDIUM))
            ) {
                Text(
                    text = "${copiedLabel.orEmpty()} copied",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(28.dp))
            SectionHeader("Danger zone")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWipeConfirm = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Clear all data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Erases messages, identity keys and settings from this device.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            CreditBlock()
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear all data?") },
            text = {
                Text(
                    "This erases every message, your identity keys, your nickname and all " +
                        "settings from this device.\n\nYour identity cannot be recovered - " +
                        "people who verified you will see you as a new, unverified person. " +
                        "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showWipeConfirm = false
                    viewModel.panicClearAllData()
                }) {
                    Text("Erase everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun ThemeChoice(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * One identity value: monospace, wrapped, copyable. Monospace genuinely helps here -
 * these are compared character by character.
 */
@Composable
private fun IdentityRow(
    label: String,
    value: String,
    explanation: String,
    onCopy: () -> Unit
) {
    val colors = LocalOChatColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopy)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MonospaceTextStyle,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = "Copy $label",
                tint = colors.textMuted,
                modifier = Modifier.padding(start = 12.dp).size(18.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline
        )
    }
}
