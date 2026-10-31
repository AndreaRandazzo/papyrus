package io.github.andrearandazzo.papyrus.ui.expiring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.CategoryRepository
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.ExpiryGroups
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ExpiringState(
    val groups: ExpiryGroups = ExpiryGroups(),
    val categoryNames: Map<Long, String> = emptyMap(),
)

/** Also used by the navigation bar for the Expiring badge. */
class ExpiringViewModel(documents: DocumentRepository, categories: CategoryRepository) : ViewModel() {

    val state: StateFlow<ExpiringState> = combine(documents.documents, categories.categories) { docs, cats ->
        ExpiringState(ExpiryGroups.of(docs, LocalDate.now()), cats.associate { it.id to it.name })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpiringState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PapyrusApp
                ExpiringViewModel(app.documentRepository, app.categoryRepository)
            }
        }
    }
}
