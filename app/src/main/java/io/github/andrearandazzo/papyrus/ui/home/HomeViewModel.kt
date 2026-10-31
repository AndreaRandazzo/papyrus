package io.github.andrearandazzo.papyrus.ui.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class ImportResult(val imported: Int, val failed: Int)

class HomeViewModel(private val repository: DocumentRepository) : ViewModel() {

    val documents: StateFlow<List<Document>> = repository.documents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _importResults = Channel<ImportResult>(Channel.BUFFERED)
    /** One-off results, shown once as a snackbar. */
    val importResults: Flow<ImportResult> = _importResults.receiveAsFlow()

    /** Runs in the ViewModel's scope, so it keeps going if the screen rotates. */
    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importing.value = true
            var failed = 0
            for (uri in uris) {
                try {
                    repository.import(uri)
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
                HomeViewModel((this[APPLICATION_KEY] as PapyrusApp).documentRepository)
            }
        }
    }
}
