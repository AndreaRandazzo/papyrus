package io.github.andrearandazzo.papyrus.data

import java.time.LocalDate

/**
 * One folder of the Library's Folders view: a category, or "No category" when [category] is null.
 * [urgentCount] is how many of its documents are expired or expiring soon (see [ExpiryGroups]).
 */
data class Folder(val category: Category?, val documentCount: Int, val urgentCount: Int) {
    /** The value of [DocumentFilter.categoryId] that shows this folder's documents. */
    val filterId: Long get() = category?.id ?: DocumentFilter.NO_CATEGORY

    companion object {
        /**
         * A folder per category, in the categories' order, empty ones included so documents can be
         * added to them; then "No category", only if some documents have none.
         */
        fun of(documents: List<Document>, categories: List<Category>, today: LocalDate): List<Folder> {
            val byCategory = documents.groupBy { it.categoryId }
            fun folder(category: Category?, inside: List<Document>) =
                Folder(category, inside.size, ExpiryGroups.of(inside, today).urgentCount)

            val known = categories.map { folder(it, byCategory[it.id].orEmpty()) }
            // Deleting a category leaves its documents without one (the database sets it to null).
            val uncategorized = byCategory[null].orEmpty()
            return if (uncategorized.isEmpty()) known else known + folder(null, uncategorized)
        }
    }
}
