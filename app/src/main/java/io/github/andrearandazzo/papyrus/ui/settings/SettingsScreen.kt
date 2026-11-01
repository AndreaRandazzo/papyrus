package io.github.andrearandazzo.papyrus.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.ThemeMode
import io.github.andrearandazzo.papyrus.ui.components.SectionLabel
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = (context.applicationContext as PapyrusApp).preferences
    val themeMode by preferences.themeMode.collectAsStateWithLifecycle()
    val info = remember { deviceInfo(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val noBrowser = stringResource(R.string.no_app_to_open_link)
    var dialog by remember { mutableStateOf<String?>(null) }

    val open: (String) -> Unit = { url ->
        if (!openLink(context, url)) scope.launch { snackbarHostState.showSnackbar(noBrowser) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        SymbolIcon(R.drawable.ic_arrow_back, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Section(stringResource(R.string.settings_appearance))
            SettingsRow(R.drawable.ic_palette, stringResource(R.string.theme), stringResource(themeMode.label())) {
                dialog = "theme"
            }
            if (AppLanguageSetting.isSupported) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val language = remember { AppLanguageSetting.current(context) }
                SettingsRow(R.drawable.ic_translate, stringResource(R.string.language), stringResource(language.label)) {
                    dialog = "language"
                }
            }

            Section(stringResource(R.string.settings_feedback))
            SettingsRow(
                R.drawable.ic_bug,
                stringResource(R.string.report_bug),
                stringResource(R.string.report_bug_body),
                trailing = R.drawable.ic_open_in_new,
            ) { open(FeedbackLinks.bugReport(info)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsRow(
                R.drawable.ic_lightbulb,
                stringResource(R.string.request_feature),
                stringResource(R.string.request_feature_body),
                trailing = R.drawable.ic_open_in_new,
            ) { open(FeedbackLinks.featureRequest(info)) }
            if (FeedbackLinks.EMAIL.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val subject = stringResource(R.string.feedback_email_subject)
                SettingsRow(R.drawable.ic_mail, stringResource(R.string.send_email), FeedbackLinks.EMAIL) {
                    if (!sendEmail(context, subject, FeedbackLinks.emailBody(info))) {
                        scope.launch { snackbarHostState.showSnackbar(noBrowser) }
                    }
                }
            }
            Text(
                stringResource(R.string.feedback_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )

            Section(stringResource(R.string.about_papyrus))
            SettingsRow(R.drawable.ic_info, stringResource(R.string.about_version), info.appVersion)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsRow(
                R.drawable.ic_license,
                stringResource(R.string.about_license),
                stringResource(R.string.about_license_value),
                trailing = R.drawable.ic_chevron_right,
            ) { open(FeedbackLinks.LICENSE) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsRow(
                R.drawable.ic_code,
                stringResource(R.string.about_source),
                stringResource(R.string.about_source_body),
                trailing = R.drawable.ic_open_in_new,
            ) { open(FeedbackLinks.REPOSITORY) }
        }
    }

    when (dialog) {
        "theme" -> ChoiceDialog(
            title = stringResource(R.string.theme),
            options = ThemeMode.entries,
            selected = themeMode,
            label = { stringResource(it.label()) },
            onSelect = { preferences.setThemeMode(it); dialog = null },
            onDismiss = { dialog = null },
        )
        "language" -> if (AppLanguageSetting.isSupported) ChoiceDialog(
            title = stringResource(R.string.language),
            options = AppLanguage.entries,
            selected = AppLanguageSetting.current(context),
            label = { stringResource(it.label) },
            onSelect = { dialog = null; AppLanguageSetting.set(context, it) },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun Section(title: String) {
    SectionLabel(title, Modifier.padding(start = 4.dp, top = 20.dp, bottom = 4.dp))
}

@Composable
private fun SettingsRow(icon: Int, title: String, value: String, trailing: Int? = null, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 14.dp),
    ) {
        SymbolIcon(icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.let { SymbolIcon(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, role = Role.RadioButton) { onSelect(option) }
                            .padding(vertical = 10.dp),
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun deviceInfo(context: Context) = DeviceInfo(
    appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
    androidVersion = "Android ${Build.VERSION.RELEASE}",
    device = "${Build.MANUFACTURER} ${Build.MODEL}",
)

/** Opens a web page in the user's browser; Papyrus itself never goes online. */
private fun openLink(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (e: ActivityNotFoundException) {
    false
}

private fun sendEmail(context: Context, subject: String, body: String): Boolean = try {
    context.startActivity(
        Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(FeedbackLinks.EMAIL))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body),
    )
    true
} catch (e: ActivityNotFoundException) {
    false
}
