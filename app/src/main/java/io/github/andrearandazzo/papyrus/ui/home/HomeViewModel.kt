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

data class ImportResult(val imported: Int, val failed: Int)

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

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _importResults = Channel<ImportResult>(Channel.BUFFERED)
    /** One-off results, shown once as a snackbar. */
    val importResults: Flow<ImportResult> = _importResults.receiveAsFlow()

    fun search(text: String) {
        query = text
    }

    fun selectCategory(category: Category?) {
        filter.update { it.copy(categoryId = category?.id) }
    }

    fun updateFilter(change: (DocumentFilter) -> DocumentFilter) {
        filter.update(change)
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
        viewModelScope.launch {
            _importing.value = true
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
            _importing.value = false
            _importResults.send(ImportResult(imported = uris.size - failed, failed = failed))
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PapyrusApp
                HomeViewModel(app.documentRepository, app.categoryRepository)
            }
        }
    }
}
