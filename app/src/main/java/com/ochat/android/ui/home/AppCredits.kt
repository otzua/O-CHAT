package com.ochat.android.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ochat.android.R
import com.ochat.android.ui.theme.LocalOChatColors

/**
 * Tracks whether the one-time welcome dialog has been shown.
 *
 * Stored in the same preferences file the theme setting uses. The file name is inherited
 * from bitchat and deliberately not renamed - see docs/BRANDING.md.
 */
object WelcomePreferenceManager {
    private const val PREFS_NAME = "bitchat_settings"
    private const val KEY_WELCOME_SHOWN = "welcome_shown"

    fun hasSeenWelcome(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WELCOME_SHOWN, false)

    fun markWelcomeSeen(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WELCOME_SHOWN, true)
            .apply()
    }
}

/** Opens the author's GitHub profile in a browser, ignoring the case where none exists. */
fun openGitHubProfile(context: Context) {
    val url = context.getString(R.string.credit_github_url)
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/**
 * One-line credit pinned under the conversation list.
 *
 * Kept deliberately small: it sits on the home screen permanently, so it should read as a
 * footer rather than compete with the list above it.
 */
@Composable
fun CreditFooterLine(modifier: Modifier = Modifier) {
    val colors = LocalOChatColors.current
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(
            text = stringResource(R.string.credit_footer_short),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openGitHubProfile(context) }
                .padding(vertical = 10.dp)
        )
    }
}

/**
 * Full credit block for the bottom of the About sheet: author, what this fork is,
 * a tappable GitHub link, and upstream attribution.
 */
@Composable
fun CreditBlock(modifier: Modifier = Modifier) {
    val colors = LocalOChatColors.current
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.credit_author),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.credit_tagline),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clickable { openGitHubProfile(context) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.credit_github_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.OpenInNew,
                contentDescription = stringResource(R.string.cd_open_github),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.credit_upstream),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * First-run welcome dialog. Shown once; dismissing it records the flag so it never
 * reappears, including across app restarts.
 */
@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    val colors = LocalOChatColors.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.welcome_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.credit_author),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { openGitHubProfile(context) }
                ) {
                    Text(
                        text = stringResource(R.string.credit_github_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.cd_open_github),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.credit_upstream),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.welcome_dismiss))
            }
        }
    )
}
