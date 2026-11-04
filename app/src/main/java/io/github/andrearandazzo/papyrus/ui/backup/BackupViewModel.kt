package io.github.andrearandazzo.papyrus.ui.backup

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.UserPreferences
import io.github.andrearandazzo.papyrus.data.backup.BackupManager
import io.github.andrearandazzo.papyrus.data.backup.InvalidBackupException
import io.github.andrearandazzo.papyrus.data.backup.PasswordRequiredException
import io.github.andrearandazzo.papyrus.data.backup.WrongPasswordException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/** One-off results, each shown once as a snackbar. */
sealed interface BackupMessage {
    data class Saved(val documents: Int) : BackupMessage
    data object SaveFailed : BackupMessage
    data class Restored(val restored: Int, val alreadyPresent: Int) : BackupMessage
    data object NotABackup : BackupMessage
    data object RestoreFailed : BackupMessage
}

class BackupViewModel(
    private val backupManager: BackupManager,
    private val preferences: UserPreferences,
    documents: DocumentRepository,
) : ViewModel() {

    var busy by mutableStateOf(false)
        private set

    /** A password-protected backup the user picked to restore, waiting for its password. */
    var passwordNeededFor by mutableStateOf<Uri?>(null)
        private set

    /** The last password typed for [passwordNeededFor] didn't open it. */
    var wrongPassword by mutableStateOf(false)
        private set

    /** Typed in the export dialog, kept only until the user picks where to save. */
    private var exportPassword: CharArray? = null

    /** The export dialog was confirmed; [password] null means a plain, unprotected backup. */
    fun prepareExport(password: CharArray?) {
        exportPassword?.fill(' ')
        exportPassword = password
    }

    /** The user cancelled choosing where to save. */
    fun cancelExport() = prepareExport(null)

    fun cancelPassword() {
        passwordNeededFor = null
        wrongPassword = false
    }

    /** When the last backup was saved, or null if never. */
    var lastBackupAt by mutableStateOf(preferences.lastBackupAt)
        private set

    val hasDocuments: StateFlow<Boolean> = documents.documents.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** A backup is due when there are documents and none was saved in the last 30 days. */
    val backupDue: Boolean
        get() = lastBackupAt.let { it == null || System.currentTimeMillis() - it > TimeUnit.DAYS.toMillis(DUE_AFTER_DAYS) }

    private val _messages = Channel<BackupMessage>(Channel.BUFFERED)
    val messages: Flow<BackupMessage> = _messages.receiveAsFlow()

    fun export(target: Uri) = run {
        val password = exportPassword
        exportPassword = null
        try {
            val count = backupManager.export(target, password)
            val now = System.currentTimeMillis()
            preferences.lastBackupAt = now
            lastBackupAt = now
            BackupMessage.Saved(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Backup failed", e)
            BackupMessage.SaveFailed
        }
    }

    /** Restores [source]; a protected backup asks for its [password] first (see [passwordNeededFor]). */
    fun restore(source: Uri, password: CharArray? = null) = run {
        try {
            backupManager.restore(source, password).let {
                cancelPassword()
                BackupMessage.Restored(it.restored, it.alreadyPresent)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: PasswordRequiredException) {
            passwordNeededFor = source
            null
        } catch (e: WrongPasswordException) {
            wrongPassword = true
            null
        } catch (e: InvalidBackupException) {
            Log.w(TAG, "Not a backup", e)
            BackupMessage.NotABackup
        } catch (e: Exception) {
            Log.w(TAG, "Restore failed", e)
            BackupMessage.RestoreFailed
        }
    }

    /**
     * Runs [work] in the ViewModel's scope (survives rotation), showing progress, then its message
     * if any (asking for a password has none).
     */
    private fun run(work: suspend () -> BackupMessage?) {
        viewModelScope.launch {
            busy = true
            val message = try {
                work()
            } finally {
                busy = false
            }
            message?.let { _messages.send(it) }
        }
    }

    override fun onCleared() = prepareExport(null)

    companion object {
        private const val TAG = "BackupViewModel"
        const val DUE_AFTER_DAYS = 30L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PapyrusApp
                BackupViewModel(app.backupManager, app.preferences, app.documentRepository)
            }
        }
    }
}
