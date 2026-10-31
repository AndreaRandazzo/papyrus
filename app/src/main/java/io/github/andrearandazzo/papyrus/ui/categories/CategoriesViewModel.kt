package io.github.andrearandazzo.papyrus.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(private val repository: CategoryRepository) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Returns false if the name is already taken. */
    suspend fun add(name: String): Boolean = repository.add(name)

    /** Returns false if the name is already taken. */
    suspend fun rename(category: Category, name: String): Boolean = repository.rename(category, name)

    fun delete(category: Category) {
        viewModelScope.launch { repository.delete(category) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CategoriesViewModel((this[APPLICATION_KEY] as PapyrusApp).categoryRepository)
            }
        }
    }
}
