package io.github.andrearandazzo.papyrus.data

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {

    val categories: Flow<List<Category>> = dao.observeAll()

    /** [icon] null means "pick from the name". Returns false if the name (ignoring case) already exists. */
    suspend fun add(name: String, icon: String?): Boolean =
        uniqueName { dao.insert(Category(name = name.trim(), icon = icon)) }

    /** Returns false if another category already has that name (ignoring case). */
    suspend fun update(category: Category, name: String, icon: String?): Boolean =
        uniqueName { dao.update(category.copy(name = name.trim(), icon = icon)) }

    /** Its documents are kept and become uncategorized. */
    suspend fun delete(category: Category) = dao.delete(category)

    private inline fun uniqueName(write: () -> Unit): Boolean = try {
        write()
        true
    } catch (e: SQLiteConstraintException) {
        false
    }
}
