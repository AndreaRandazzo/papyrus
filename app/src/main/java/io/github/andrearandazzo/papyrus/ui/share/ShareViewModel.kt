package io.github.andrearandazzo.papyrus.ui.share

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.CategoryRepository
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.ImportResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Files shared from another app, waiting for the user to pick a category and save. */
class ShareViewModel(
    private val uris: List<Uri>,
    private val documents: DocumentRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Names of the shared files, filled in once read; null while loading. */
    var fileNames by mutableStateOf<List<String>?>(null)
        private set

    /** null means "No category". */
    var categoryId by mutableStateOf<Long?>(null)

    var saving by mutableStateOf(false)
        private set

    /** Set once saving is done; the screen then reports it and closes. */
    var result by mutableStateOf<ImportResult?>(null)
        private set

    init {
        viewModelScope.launch { fileNames = uris.map { documents.displayName(it) } }
    }

    fun save() {
        if (saving || result != null) return
        saving = true
        viewModelScope.launch {
            result = documents.importAll(uris, categoryId)
            saving = false
        }
    }

    companion object {
        fun factory(uris: List<Uri>): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PapyrusApp
                ShareViewModel(uris, app.documentRepository, app.categoryRepository)
            }
        }
    }
}
