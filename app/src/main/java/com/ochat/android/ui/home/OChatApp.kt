package com.ochat.android.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ochat.android.model.BitchatMessage
import com.ochat.android.ui.AboutSheet
import com.ochat.android.ui.ChatUserSheet
import com.ochat.android.ui.ChatViewModel
import com.ochat.android.ui.LocationChannelsSheet
import com.ochat.android.ui.LocationNotesSheetPresenter
import com.ochat.android.ui.PasswordPromptDialog
import com.ochat.android.ui.SecurityVerificationSheet
import com.ochat.android.ui.VerificationSheet
import com.ochat.android.ui.debug.DebugSettingsSheet

private const val ROUTE_HOME = "home"
private const val ARG_CONVERSATION = "conversation"
private const val ROUTE_CONVERSATION = "conversation/{$ARG_CONVERSATION}"

/**
 * Root of the OChat UI: a conversation list plus a full-screen conversation route.
 *
 * The sheets and dialogs BitChat already had (about, verification, user actions, location)
 * are hosted here so they remain reachable from either destination.
 */
@Composable
fun OChatApp(viewModel: ChatViewModel) {
    val navController = rememberNavController()

    var showLocationChannels by remember { mutableStateOf(false) }
    var showLocationNotes by remember { mutableStateOf(false) }
    var showDebugSheet by remember { mutableStateOf(false) }
    var userSheetTarget by remember { mutableStateOf<String?>(null) }
    var userSheetMessage by remember { mutableStateOf<BitchatMessage?>(null) }
    var passwordInput by remember { mutableStateOf("") }

    val showAppInfo by viewModel.showAppInfo.collectAsStateWithLifecycle()
    val showVerificationSheet by viewModel.showVerificationSheet.collectAsStateWithLifecycle()
    val showSecurityVerificationSheet by viewModel.showSecurityVerificationSheet.collectAsStateWithLifecycle()
    val showPasswordPrompt by viewModel.showPasswordPrompt.collectAsStateWithLifecycle()
    val passwordPromptChannel by viewModel.passwordPromptChannel.collectAsStateWithLifecycle()

    // A tapped notification asks the ViewModel to open a private chat; mirror that into navigation.
    val privateChatSheetPeer by viewModel.privateChatSheetPeer.collectAsStateWithLifecycle()
    LaunchedEffect(privateChatSheetPeer) {
        privateChatSheetPeer?.let { peerID ->
            navController.navigate("conversation/${ConversationId.Private(peerID).encode()}")
            viewModel.hidePrivateChatSheet()
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                viewModel = viewModel,
                onOpenConversation = { id ->
                    navController.navigate("conversation/${id.encode()}")
                },
                onShowLocationChannels = { showLocationChannels = true },
                onShowAppInfo = { viewModel.showAppInfo() }
            )
        }
        composable(
            route = ROUTE_CONVERSATION,
            arguments = listOf(navArgument(ARG_CONVERSATION) { type = NavType.StringType })
        ) { entry ->
            val raw = entry.arguments?.getString(ARG_CONVERSATION).orEmpty()
            val conversationId = remember(raw) { ConversationId.decode(raw) }
            ConversationScreen(
                conversationId = conversationId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenUserSheet = { nickname, message ->
                    userSheetTarget = nickname
                    userSheetMessage = message
                }
            )
        }
    }

    PasswordPromptDialog(
        show = showPasswordPrompt,
        channelName = passwordPromptChannel,
        passwordInput = passwordInput,
        onPasswordChange = { passwordInput = it },
        onConfirm = {
            val channel = passwordPromptChannel
            if (passwordInput.isNotEmpty() && channel != null) {
                if (viewModel.joinChannel(channel, passwordInput)) passwordInput = ""
            }
        },
        onDismiss = { passwordInput = "" }
    )

    AboutSheet(
        isPresented = showAppInfo,
        onDismiss = { viewModel.hideAppInfo() },
        onShowDebug = { showDebugSheet = true }
    )

    if (showDebugSheet) {
        DebugSettingsSheet(
            isPresented = true,
            onDismiss = { showDebugSheet = false },
            meshService = viewModel.meshService
        )
    }

    if (showLocationChannels) {
        LocationChannelsSheet(
            isPresented = true,
            onDismiss = { showLocationChannels = false },
            viewModel = viewModel
        )
    }

    if (showLocationNotes) {
        LocationNotesSheetPresenter(
            viewModel = viewModel,
            onDismiss = { showLocationNotes = false }
        )
    }

    userSheetTarget?.let { target ->
        ChatUserSheet(
            isPresented = true,
            onDismiss = {
                userSheetTarget = null
                userSheetMessage = null
            },
            targetNickname = target,
            selectedMessage = userSheetMessage,
            viewModel = viewModel
        )
    }

    if (showVerificationSheet) {
        VerificationSheet(
            isPresented = true,
            onDismiss = viewModel::hideVerificationSheet,
            viewModel = viewModel
        )
    }

    if (showSecurityVerificationSheet) {
        SecurityVerificationSheet(
            isPresented = true,
            onDismiss = viewModel::hideSecurityVerificationSheet,
            viewModel = viewModel
        )
    }
}
