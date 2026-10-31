package io.github.andrearandazzo.papyrus.ui.home

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.CategoryRepository
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.DocumentFilter
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.backup.BackupManager
import io.github.andrearandazzo.papyrus.data.backup.InvalidBackupException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import java.io.File

/** One-off results, each shown once as a snackbar. */
sealed interface HomeMessage {
    data class Imported(val imported: Int, val failed: Int) : HomeMessage
    data class BackupSaved(val documents: Int) : HomeMessage
    data object BackupFailed : HomeMessage
    data class Restored(val restored: Int, val alreadyPresent: Int) : HomeMessage
    data object NotABackup : HomeMessage
    data object RestoreFailed : HomeMessage
}

data class HomeState(
    /** Documents matching [filter], in its sort order. */
    val documents: List<Document> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filter: DocumentFilter = DocumentFilter(),
    /** True when there are no documents at all, not just none matching the filter. */
    val libraryEmpty: Boolean = true,
)

class HomeViewModel(
    private val documentRepository: DocumentRepository,
    categoryRepository: CategoryRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    /** Search text. Compose state, so the text field updates without lag while typing. */
    var query by mutableStateOf("")
        private set

    private val filter = MutableStateFlow(DocumentFilter())

    val state: StateFlow<HomeState> = combine(
        documentRepository.documents,
        categoryRepository.categories,
        filter,
        snapshotFlow { query },
    ) { documents, categories, filter, query ->
        // If the selected category was deleted, fall back to "All".
        val effective = filter.copy(
            query = query,
            categoryId = filter.categoryId?.takeIf { id -> categories.any { it.id == id } },
        )
        HomeState(
            documents = effective.apply(documents, categories),
            categories = categories,
            filter = effective,
            libraryEmpty = documents.isEmpty(),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private val _busy = MutableStateFlow(false)
    /** True while importing or backing up/restoring. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _messages = Channel<HomeMessage>(Channel.BUFFERED)
    val messages: Flow<HomeMessage> = _messages.receiveAsFlow()

    fun search(text: String) {
        query = text
    }

    fun selectCategory(category: Category?) {
        filter.update { it.copy(categoryId = category?.id) }
    }

    fun updateFilter(change: (DocumentFilter) -> DocumentFilter) {
        filter.update(change)
    }

    /** A copy with a readable file name, to open in or share with another app. */
    suspend fun fileForOtherApps(document: Document): File = documentRepository.exportCopy(document)

    fun delete(document: Document) {
        viewModelScope.launch { documentRepository.delete(document) }
    }

    /** Clears search and every filter, but keeps the sort order. */
    fun clearFilters() {
        query = ""
        filter.update { DocumentFilter(sort = it.sort) }
    }

    /**
     * Imports into the currently selected category, if any.
     * Runs in the ViewModel's scope, so it keeps going if the screen rotates.
     */
    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val categoryId = state.value.filter.categoryId
        runBusy {
            var failed = 0
            for (uri in uris) {
                try {
                    documentRepository.import(uri, categoryId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Import failed for $uri", e)
                    failed++
                }
            }
            HomeMessage.Imported(imported = uris.size - failed, failed = failed)
        }
    }

    fun exportBackup(target: Uri) = runBusy {
        try {
            HomeMessage.BackupSaved(backupManager.export(target))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Backup failed", e)
            HomeMessage.BackupFailed
        }
    }

    fun restoreBackup(source: Uri) = runBusy {
        try {
            backupManager.restore(source).let { HomeMessage.Restored(it.restored, it.alreadyPresent) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: InvalidBackupException) {
            Log.w(TAG, "Not a backup", e)
            HomeMessage.NotABackup
        } catch (e: Exception) {
            Log.w(TAG, "Restore failed", e)
            HomeMessage.RestoreFailed
        }
    }

    /** Runs [work] in the ViewModel's scope (survives rotation), showing progress, then its message. */
    private fun runBusy(work: suspend () -> HomeMessage) {
        viewModelScope.launch {
            _busy.value = true
            val message = try {
                work()
            } finally {
                _busy.value = false
            }
            _messages.send(message)
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PapyrusApp
                HomeViewModel(app.documentRepository, app.categoryRepository, app.backupManager)
            }
        }
    }
}
