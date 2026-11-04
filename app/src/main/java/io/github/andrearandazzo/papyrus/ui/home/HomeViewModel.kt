package io.github.andrearandazzo.papyrus.ui.home

import android.net.Uri
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
import io.github.andrearandazzo.papyrus.data.DocumentTextDao
import io.github.andrearandazzo.papyrus.data.Folder
import io.github.andrearandazzo.papyrus.data.ImportResult
import io.github.andrearandazzo.papyrus.data.LibraryView
import io.github.andrearandazzo.papyrus.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class HomeState(
    /** Documents matching [filter], in its sort order. */
    val documents: List<Document> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filter: DocumentFilter = DocumentFilter(),
    /** True when there are no documents at all, not just none matching the filter. */
    val libraryEmpty: Boolean = true,
    /** Every category as a folder, for the Folders view. */
    val folders: List<Folder> = emptyList(),
    /** For results found only by the text inside: the words around the match, by document id. */
    val snippets: Map<Long, String> = emptyMap(),
    /** How many attachments each document has, by id (absent means none). */
    val attachmentCounts: Map<Long, Int> = emptyMap(),
) {
    /** The folder open in the Folders view, if any. */
    val openFolder: Folder? get() = filter.categoryId?.let { id -> folders.firstOrNull { it.filterId == id } }
}

/** The text inside documents, as read and in the form search compares against, by document id. */
private class Contents(
    val text: Map<Long, String> = emptyMap(),
    val searchable: Map<Long, String> = emptyMap(),
    val attachmentCounts: Map<Long, Int> = emptyMap(),
)

class HomeViewModel(
    private val documentRepository: DocumentRepository,
    categoryRepository: CategoryRepository,
    private val preferences: UserPreferences,
    documentTexts: DocumentTextDao,
) : ViewModel() {

    /** List, grid or folders; remembered across app restarts. */
    var view by mutableStateOf(preferences.libraryView)
        private set

    fun showView(view: LibraryView) {
        if (view == this.view) return
        this.view = view
        preferences.libraryView = view
        // Folders starts from the folder overview, not inside the category picked as a chip.
        if (view == LibraryView.FOLDERS) filter.update { it.copy(categoryId = null) }
    }

    /** Search text. Compose state, so the text field updates without lag while typing. */
    var query by mutableStateOf("")
        private set

    private val filter = MutableStateFlow(DocumentFilter(sort = preferences.sortOrder))

    // Prepared once whenever a text is read, not on every keystroke.
    private val contents = documentTexts.observeAll()
        .map { rows ->
            val text = rows.filter { it.text.isNotBlank() }.associate { it.documentId to it.text }
            text to text.mapValues { DocumentFilter.searchable(it.value) }
        }
        .combine(documentRepository.attachmentCounts) { (text, searchable), counts -> Contents(text, searchable, counts) }
        .flowOn(Dispatchers.Default)

    val state: StateFlow<HomeState> = combine(
        documentRepository.documents,
        categoryRepository.categories,
        filter,
        snapshotFlow { query },
        contents,
    ) { documents, categories, filter, query, contents ->
        // If the selected category was deleted, fall back to "All".
        val effective = filter.copy(
            query = query,
            categoryId = filter.categoryId?.takeIf { id ->
                id == DocumentFilter.NO_CATEGORY || categories.any { it.id == id }
            },
        )
        val found = effective.apply(documents, categories, contents = contents.searchable)
        // Results the title, note or category alone wouldn't find show where the words are inside.
        val snippets = if (effective.query.isBlank()) {
            emptyMap()
        } else {
            val byDetails = effective.apply(documents, categories).map { it.id }.toSet()
            found.filter { it.id !in byDetails }
                .mapNotNull { document -> contents.text[document.id]?.let(effective::snippet)?.let { document.id to it } }
                .toMap()
        }
        HomeState(
            documents = found,
            snippets = snippets,
            attachmentCounts = contents.attachmentCounts,
            categories = categories,
            filter = effective,
            libraryEmpty = documents.isEmpty(),
            folders = Folder.of(documents, categories, LocalDate.now()),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _importResults = Channel<ImportResult>(Channel.BUFFERED)
    val importResults: Flow<ImportResult> = _importResults.receiveAsFlow()

    /** How many originals were deleted, out of how many asked. */
    private val _originalsDeleted = Channel<Pair<Int, Int>>(Channel.BUFFERED)
    val originalsDeleted: Flow<Pair<Int, Int>> = _originalsDeleted.receiveAsFlow()

    /** Deletes the source files of an import the user confirmed; Papyrus keeps its copies. */
    fun deleteOriginals(uris: List<Uri>) {
        viewModelScope.launch {
            _originalsDeleted.send(documentRepository.deleteOriginals(uris) to uris.size)
        }
    }

    fun search(text: String) {
        query = text
    }

    fun selectCategory(category: Category?) {
        filter.update { it.copy(categoryId = category?.id) }
    }

    /** Opens a folder of the Folders view, or goes back to the overview with null. */
    fun openFolder(folder: Folder?) {
        filter.update { it.copy(categoryId = folder?.filterId) }
    }

    /** Applies a change from the filters panel or the sort menu; the sort order is remembered. */
    fun updateFilter(change: (DocumentFilter) -> DocumentFilter) {
        val updated = filter.updateAndGet(change)
        if (updated.sort != preferences.sortOrder) preferences.sortOrder = updated.sort
    }

    /** A copy with a readable file name, to open in or share with another app. */
    suspend fun fileForOtherApps(document: Document): File = documentRepository.exportCopy(document)

    fun delete(document: Document) {
        viewModelScope.launch { documentRepository.delete(document) }
    }

    /** Clears search and every filter, but keeps the sort order and the open folder. */
    fun clearFilters() {
        query = ""
        filter.update {
            DocumentFilter(sort = it.sort, categoryId = it.categoryId.takeIf { view == LibraryView.FOLDERS })
        }
    }

    /** The category new documents go into: the selected one or the open folder, else none. */
    val targetCategoryId: Long?
        get() = state.value.filter.categoryId?.takeIf { it != DocumentFilter.NO_CATEGORY }

    /**
     * Imports into the currently selected category, if any.
     * Runs in the ViewModel's scope, so it keeps going if the screen rotates.
     */
    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val categoryId = targetCategoryId
        viewModelScope.launch {
            _importing.value = true
            val result = try {
                documentRepository.importAll(uris, categoryId)
            } finally {
                _importing.value = false
            }
            _importResults.send(result)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PapyrusApp
                HomeViewModel(app.documentRepository, app.categoryRepository, app.preferences, app.documentTexts)
            }
        }
    }
}
