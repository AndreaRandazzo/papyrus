package io.github.andrearandazzo.papyrus.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon

/** Shortest password accepted for a backup. */
private const val MIN_LENGTH = 8

/**
 * Before exporting: protect the backup with a password, or save it without one. The password is
 * typed twice, and the dialog warns that it can't be recovered. Passwords aren't kept across
 * screen rotation on purpose: they never go into saved state.
 */
@Composable
fun ExportPasswordDialog(onExport: (password: CharArray?) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val tooShort = password.isNotEmpty() && password.length < MIN_LENGTH
    val mismatch = repeat.isNotEmpty() && repeat != password
    val valid = password.length >= MIN_LENGTH && repeat == password

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { SymbolIcon(R.drawable.ic_lock, contentDescription = null) },
        title = { Text(stringResource(R.string.backup_password_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.backup_password_body), style = MaterialTheme.typography.bodyMedium)
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.password),
                    error = if (tooShort) pluralStringResource(R.plurals.password_too_short, MIN_LENGTH, MIN_LENGTH) else null,
                    imeAction = ImeAction.Next,
                )
                PasswordField(
                    value = repeat,
                    onValueChange = { repeat = it },
                    label = stringResource(R.string.password_repeat),
                    error = if (mismatch) stringResource(R.string.password_mismatch) else null,
                    imeAction = ImeAction.Done,
                )
                Text(
                    stringResource(R.string.backup_password_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onExport(password.toCharArray()) }, enabled = valid) {
                Text(stringResource(R.string.backup_save_protected))
            }
        },
        dismissButton = {
            TextButton(onClick = { onExport(null) }) { Text(stringResource(R.string.backup_save_plain)) }
        },
    )
}

/** Restoring a protected backup: asks its password; [wrong] shows that the last one didn't work. */
@Composable
fun RestorePasswordDialog(wrong: Boolean, busy: Boolean, onRestore: (CharArray) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { SymbolIcon(R.drawable.ic_lock, contentDescription = null) },
        title = { Text(stringResource(R.string.restore_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.restore_password_body), style = MaterialTheme.typography.bodyMedium)
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.password),
                    error = if (wrong) stringResource(R.string.password_wrong) else null,
                    imeAction = ImeAction.Done,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onRestore(password.toCharArray()) }, enabled = password.isNotEmpty() && !busy) {
                Text(stringResource(R.string.backup_restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String, error: String?, imeAction: ImeAction) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
}
