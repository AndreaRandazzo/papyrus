package io.github.andrearandazzo.papyrus.data

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {

    val categories: Flow<List<Category>> = dao.observeAll()

    /** Returns false if a category with that name (ignoring case) already exists. */
    suspend fun add(name: String): Boolean = uniqueName { dao.insert(Category(name = name.trim())) }

    /** Returns false if another category already has that name (ignoring case). */
    suspend fun rename(category: Category, name: String): Boolean =
        uniqueName { dao.update(category.copy(name = name.trim())) }

    /** Its documents are kept and become uncategorized. */
    suspend fun delete(category: Category) = dao.delete(category)

    private inline fun uniqueName(write: () -> Unit): Boolean = try {
        write()
        true
    } catch (e: SQLiteConstraintException) {
        false
    }
}
