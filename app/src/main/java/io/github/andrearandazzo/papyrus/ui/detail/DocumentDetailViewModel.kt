package io.github.andrearandazzo.papyrus.ui.detail

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
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.CategoryRepository
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

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
) : ViewModel() {

    private val documentId: Long = checkNotNull(savedStateHandle["documentId"])

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
                DocumentDetailViewModel(createSavedStateHandle(), app.documentRepository, app.categoryRepository)
            }
        }
    }
}
