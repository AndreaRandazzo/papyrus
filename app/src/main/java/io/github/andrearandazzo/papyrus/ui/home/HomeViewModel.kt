package io.github.andrearandazzo.papyrus.ui.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.CategoryRepository
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class ImportResult(val imported: Int, val failed: Int)

data class HomeState(
    val documents: List<Document> = emptyList(),
    val categories: List<Category> = emptyList(),
    /** null means "All". */
    val selectedCategory: Category? = null,
    /** True when there are no documents at all, not just none in the selected category. */
    val libraryEmpty: Boolean = true,
)

class HomeViewModel(
    private val documentRepository: DocumentRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<Long?>(null)

    val state: StateFlow<HomeState> = combine(
        documentRepository.documents,
        categoryRepository.categories,
        selectedCategoryId,
    ) { documents, categories, selectedId ->
        // If the selected category was deleted, fall back to "All".
        val selected = categories.find { it.id == selectedId }
        HomeState(
            documents = if (selected == null) documents else documents.filter { it.categoryId == selected.id },
            categories = categories,
            selectedCategory = selected,
            libraryEmpty = documents.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _importResults = Channel<ImportResult>(Channel.BUFFERED)
    /** One-off results, shown once as a snackbar. */
    val importResults: Flow<ImportResult> = _importResults.receiveAsFlow()

    fun selectCategory(category: Category?) {
        selectedCategoryId.value = category?.id
    }

    /**
     * Imports into the currently selected category, if any.
     * Runs in the ViewModel's scope, so it keeps going if the screen rotates.
     */
    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val categoryId = state.value.selectedCategory?.id
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
