package io.github.andrearandazzo.papyrus.data

import java.text.Collator
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** What documents are sorted by. [ascendingFirst] is the direction picked when the field is chosen. */
enum class SortField(val ascendingFirst: Boolean) {
    /** Import date: newest first. */
    ADDED(false),
    /** Title: A–Z. */
    TITLE(true),
    /** Expiry date: soonest first. Documents without one always go last. */
    EXPIRY(true),
}

data class SortOrder(val field: SortField = SortField.ADDED, val ascending: Boolean = field.ascendingFirst) {
    /** The same field in the other direction. */
    fun reversed() = copy(ascending = !ascending)

    companion object {
        val DEFAULT = SortOrder()
    }
}

/**
 * What the user is looking for. Pure Kotlin, so the rules are easy to unit test.
 */
data class DocumentFilter(
    /** Words to find in the title, note or category name. Case and accents are ignored. */
    val query: String = "",
    /** null means every category; [NO_CATEGORY] means documents without one. */
    val categoryId: Long? = null,
    /** Empty means every type. */
    val kinds: Set<DocumentKind> = emptySet(),
    /** Import day range, both ends included; null means open-ended. */
    val addedFrom: LocalDate? = null,
    val addedTo: LocalDate? = null,
    val sort: SortOrder = SortOrder.DEFAULT,
) {
    /** How many filters are set in the filters panel (search and category are shown elsewhere). */
    val panelFilterCount: Int
        get() = (if (kinds.isNotEmpty()) 1 else 0) + (if (addedFrom != null || addedTo != null) 1 else 0)

    val isFiltering: Boolean
        get() = query.isNotBlank() || categoryId != null || panelFilterCount > 0

    fun apply(
        documents: List<Document>,
        categories: List<Category>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Document> {
        val categoryNames = categories.associate { it.id to it.name }
        val terms = normalize(query).split(WHITESPACE).filter { it.isNotEmpty() }

        return documents
            .filter { document ->
                isInCategory(document) &&
                    (kinds.isEmpty() || document.kind in kinds) &&
                    isAddedInRange(document, zone) &&
                    matchesAll(document, categoryNames[document.categoryId], terms)
            }
            .sortedWith(comparator())
    }

    private fun isInCategory(document: Document): Boolean = when (categoryId) {
        null -> true
        NO_CATEGORY -> document.categoryId == null
        else -> document.categoryId == categoryId
    }

    private fun isAddedInRange(document: Document, zone: ZoneId): Boolean {
        if (addedFrom == null && addedTo == null) return true
        val day = Instant.ofEpochMilli(document.importedAt).atZone(zone).toLocalDate()
        return (addedFrom == null || !day.isBefore(addedFrom)) && (addedTo == null || !day.isAfter(addedTo))
    }

    /** Every search word must appear somewhere, in any order. */
    private fun matchesAll(document: Document, categoryName: String?, terms: List<String>): Boolean {
        if (terms.isEmpty()) return true
        val searchable = normalize(listOfNotNull(document.title, document.note, categoryName).joinToString(" "))
        return terms.all { it in searchable }
    }

    private fun comparator(): Comparator<Document> {
        fun <T> directed(order: Comparator<T>) = if (sort.ascending) order else order.reversed()
        val newestFirst = compareByDescending<Document> { it.importedAt }
        return when (sort.field) {
            SortField.ADDED -> directed(compareBy { it.importedAt })
            SortField.TITLE -> compareBy(directed(Collator.getInstance())) { it: Document -> it.title }
                .then(newestFirst)
            // Documents without an expiry date go last in both directions.
            SortField.EXPIRY -> compareBy<Document, Long?>(nullsLast(directed(naturalOrder()))) { it.expiresAt }
                .then(newestFirst)
        }
    }

    companion object {
        /** [categoryId] for documents without a category. Database ids start at 1, so it never clashes. */
        const val NO_CATEGORY = -1L

        private val WHITESPACE = Regex("\\s+")
        private val COMBINING_MARKS = Regex("\\p{Mn}+")

        /** "Città" and "citta" both become "citta". */
        private fun normalize(text: String): String =
            Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(COMBINING_MARKS, "")
    }
}
