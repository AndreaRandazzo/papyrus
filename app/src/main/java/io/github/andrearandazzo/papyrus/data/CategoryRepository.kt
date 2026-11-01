package io.github.andrearandazzo.papyrus.data

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {

    val categories: Flow<List<Category>> = dao.observeAll()

    /** [icon] null means "pick from the name". Returns false if the name (ignoring case) already exists. */
    suspend fun add(name: String, icon: String?): Boolean =
        uniqueName { dao.insert(Category(name = name.trim(), icon = icon)) }

    /**
     * Returns false if another category already has that name (ignoring case). Renaming a starter
     * makes it the user's own: it stops following the app's language.
     */
    suspend fun update(category: Category, name: String, icon: String?): Boolean {
        val newName = name.trim()
        val starterKey = category.starterKey.takeIf { newName == category.name }
        return uniqueName { dao.update(category.copy(name = newName, icon = icon, starterKey = starterKey)) }
    }

    /** Its documents are kept and become uncategorized. */
    suspend fun delete(category: Category) = dao.delete(category)

    /**
     * Renames untouched starter categories to [names] (starter key → name in the current language).
     * A starter whose new name is already used by another category keeps its current name.
     */
    suspend fun syncStarterNames(names: Map<String, String>) {
        for (category in dao.getAll()) {
            val wanted = names[category.starterKey ?: continue] ?: continue
            if (wanted != category.name) uniqueName { dao.update(category.copy(name = wanted)) }
        }
    }

    private inline fun uniqueName(write: () -> Unit): Boolean = try {
        write()
        true
    } catch (e: SQLiteConstraintException) {
        false
    }
}
