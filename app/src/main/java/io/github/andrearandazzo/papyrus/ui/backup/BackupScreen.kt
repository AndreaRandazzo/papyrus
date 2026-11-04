package io.github.andrearandazzo.papyrus.ui.backup

import android.content.res.Resources
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.ui.components.PrivacyNote
import io.github.andrearandazzo.papyrus.ui.components.SectionLabel
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon
import io.github.andrearandazzo.papyrus.ui.formatInstantDate
import io.github.andrearandazzo.papyrus.ui.theme.Paper
import java.time.LocalDate


/** Some file providers label ZIP files differently, so accept all the common types. */
private val BACKUP_TYPES = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(viewModel: BackupViewModel = viewModel(factory = BackupViewModel.Factory)) {
    // Follows language changes, unlike LocalContext.current.resources.
    val resources = LocalResources.current
    val hasDocuments by viewModel.hasDocuments.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showExportDialog by remember { mutableStateOf(false) }

    // The system pickers need no storage permission: the user chooses exactly where we read or write.
    val onTarget = { uri: Uri? -> if (uri == null) viewModel.cancelExport() else viewModel.export(uri) }
    val plainTarget = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip"), onTarget)
    // A protected backup isn't a ZIP any more: its own extension, so nothing tries to open it as one.
    val protectedTarget =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream"), onTarget)
    val source = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::restore)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.text(resources)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_backup), style = MaterialTheme.typography.titleLarge) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (viewModel.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Hero()
                val due = hasDocuments && viewModel.backupDue
                LastBackupCard(viewModel.lastBackupAt, due)
                if (due) Reminder(neverBackedUp = viewModel.lastBackupAt == null)
                ActionCard(
                    icon = R.drawable.ic_duo_upload,
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    title = stringResource(R.string.backup_export),
                    body = stringResource(R.string.backup_export_body),
                    enabled = hasDocuments && !viewModel.busy,
                    onClick = { showExportDialog = true },
                )
                ActionCard(
                    icon = R.drawable.ic_duo_restore,
                    iconBackground = Paper,
                    title = stringResource(R.string.backup_restore),
                    body = stringResource(R.string.backup_restore_body),
                    enabled = !viewModel.busy,
                    onClick = { source.launch(BACKUP_TYPES) },
                )
                PrivacyNote(
                    stringResource(R.string.privacy_note),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showExportDialog) {
        ExportPasswordDialog(
            onExport = { password ->
                showExportDialog = false
                viewModel.prepareExport(password)
                if (password == null) {
                    plainTarget.launch("papyrus-backup-${LocalDate.now()}.zip")
                } else {
                    protectedTarget.launch("papyrus-backup-${LocalDate.now()}.papyrus")
                }
            },
            onDismiss = { showExportDialog = false },
        )
    }

    viewModel.passwordNeededFor?.let { uri ->
        RestorePasswordDialog(
            wrong = viewModel.wrongPassword,
            busy = viewModel.busy,
            onRestore = { password -> viewModel.restore(uri, password) },
            onDismiss = viewModel::cancelPassword,
        )
    }
}

@Composable
private fun Hero() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(R.drawable.ic_duo_shield, contentDescription = null)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(stringResource(R.string.backup_hero_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.backup_hero_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LastBackupCard(lastBackupAt: Long?, due: Boolean) {
    OutlinedCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f)) {
                SectionLabel(stringResource(R.string.last_backup))
                Text(
                    lastBackupAt?.let(::formatInstantDate) ?: stringResource(R.string.never),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (due) {
                StatusPill(R.drawable.ic_notifications, stringResource(R.string.backup_due), Paper, Color(0xFF5A3A12))
            } else if (lastBackupAt != null) {
                StatusPill(
                    R.drawable.ic_check,
                    stringResource(R.string.backup_up_to_date),
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(icon: Int, text: String, background: Color, content: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(background, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
private fun Reminder(neverBackedUp: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Paper, MaterialTheme.shapes.small)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(painterResource(R.drawable.ic_lightbulb), contentDescription = null, tint = Color(0xFF5A3A12))
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(if (neverBackedUp) R.string.backup_reminder_never else R.string.backup_reminder_old),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF3A2A10),
        )
    }
}

@Composable
private fun ActionCard(
    icon: Int,
    iconBackground: Color,
    title: String,
    body: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(onClick = onClick, enabled = enabled) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                Modifier.size(52.dp).background(iconBackground, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = Color(0xFF1E2B1D))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SymbolIcon(R.drawable.ic_chevron_right, contentDescription = null)
        }
    }
}

/** Rounded, outlined container; clickable when [onClick] is given. Dimmed when disabled. */
@Composable
private fun OutlinedCard(
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Box(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier),
    ) {
        content()
    }
}

private fun BackupMessage.text(resources: Resources): String = when (this) {
    is BackupMessage.Saved -> resources.getQuantityString(R.plurals.backup_saved, documents, documents)
    BackupMessage.SaveFailed -> resources.getString(R.string.backup_failed)
    is BackupMessage.Restored -> buildString {
        append(resources.getQuantityString(R.plurals.restore_done, restored, restored))
        if (alreadyPresent > 0) {
            append(" · ")
            append(resources.getQuantityString(R.plurals.restore_skipped, alreadyPresent, alreadyPresent))
        }
    }
    BackupMessage.NotABackup -> resources.getString(R.string.restore_not_a_backup)
    BackupMessage.RestoreFailed -> resources.getString(R.string.restore_failed)
}
