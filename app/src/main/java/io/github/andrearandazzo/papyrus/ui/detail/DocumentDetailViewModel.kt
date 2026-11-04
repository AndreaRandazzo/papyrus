package io.github.andrearandazzo.papyrus.ui.detail

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.Attachment
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.CategoryRepository
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.DocumentText
import io.github.andrearandazzo.papyrus.data.DocumentTextDao
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-off results shown as a snackbar. */
sealed interface DetailEvent {
    data object FileReplaced : DetailEvent
    data class FilesFailed(val count: Int) : DetailEvent
}

/** The editable fields of a document. */
data class DocumentForm(
    val title: String,
    val categoryId: Long?,
    val note: String,
    val expiresAt: Long?,
) {
    val isValid: Boolean get() = title.isNotBlank()
}

class DocumentDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val documentRepository: DocumentRepository,
    categoryRepository: CategoryRepository,
    documentTexts: DocumentTextDao,
) : ViewModel() {

    private val documentId: Long = checkNotNull(savedStateHandle["documentId"])

    /** The document's extra files, oldest first. */
    val attachments: StateFlow<List<Attachment>> = documentRepository.observeAttachments(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Adding attachments or replacing the file is in progress. */
    var working by mutableStateOf(false)
        private set

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events: Flow<DetailEvent> = _events.receiveAsFlow()

    fun addAttachments(uris: List<Uri>) {
        val current = document ?: return
        if (uris.isEmpty()) return
        launchWork {
            val result = documentRepository.addAttachments(current, uris)
            if (result.failed > 0) _events.send(DetailEvent.FilesFailed(result.failed))
        }
    }

    fun removeAttachment(attachment: Attachment) = launchWork { documentRepository.removeAttachment(attachment) }

    /** Swaps the document's own file; title, category, note, expiry and attachments stay. */
    fun replaceFile(uri: Uri) {
        val current = document ?: return
        launchWork {
            try {
                document = documentRepository.replaceFile(current, uri)
                _events.send(DetailEvent.FileReplaced)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("DocumentDetail", "Replacing the file failed", e)
                _events.send(DetailEvent.FilesFailed(1))
            }
        }
    }

    suspend fun fileForOtherApps(attachment: Attachment): File = documentRepository.exportCopy(attachment)

    private fun launchWork(block: suspend () -> Unit) {
        viewModelScope.launch {
            working = true
            try {
                block()
            } finally {
                working = false
            }
        }
    }

    /** The text inside the document; null while it's still being read (updates by itself). */
    val text: StateFlow<DocumentText?> = documentTexts.observe(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val categories: StateFlow<List<Category>> = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The document as loaded; null while loading or if it no longer exists. */
    var document by mutableStateOf<Document?>(null)
        private set
    var form by mutableStateOf<DocumentForm?>(null)
        private set
    var notFound by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set
    var deleted by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val loaded = documentRepository.observe(documentId).first()
            if (loaded == null) {
                notFound = true
            } else {
                document = loaded
                form = DocumentForm(loaded.title, loaded.categoryId, loaded.note, loaded.expiresAt)
            }
        }
    }

    fun edit(change: (DocumentForm) -> DocumentForm) {
        form = form?.let(change)
    }

    fun save() {
        val original = document ?: return
        val current = form?.takeIf { it.isValid } ?: return
        viewModelScope.launch {
            documentRepository.update(
                original.copy(
                    title = current.title.trim(),
                    categoryId = current.categoryId,
                    note = current.note.trim(),
                    expiresAt = current.expiresAt,
                ),
            )
            saved = true
        }
    }

    /** A copy with a readable file name, to open in or share with another app. */
    suspend fun fileForOtherApps(): File? = document?.let { documentRepository.exportCopy(it) }

    fun delete() {
        val current = document ?: return
        viewModelScope.launch {
            documentRepository.delete(current)
            deleted = true
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PapyrusApp
                DocumentDetailViewModel(createSavedStateHandle(), app.documentRepository, app.categoryRepository, app.documentTexts)
            }
        }
    }
}
