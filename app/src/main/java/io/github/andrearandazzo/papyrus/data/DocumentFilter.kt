package io.github.andrearandazzo.papyrus.data

import java.text.Collator
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class SortOrder { NEWEST, OLDEST, TITLE, EXPIRY }

/**
 * What the user is looking for. Pure Kotlin, so the rules are easy to unit test.
 */
data class DocumentFilter(
    /** Words to find in the title, note or category name. Case and accents are ignored. */
    val query: String = "",
    /** null means every category. */
    val categoryId: Long? = null,
    /** Empty means every type. */
    val kinds: Set<DocumentKind> = emptySet(),
    /** Import day range, both ends included; null means open-ended. */
    val addedFrom: LocalDate? = null,
    val addedTo: LocalDate? = null,
    val sort: SortOrder = SortOrder.NEWEST,
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
                (categoryId == null || document.categoryId == categoryId) &&
                    (kinds.isEmpty() || document.kind in kinds) &&
                    isAddedInRange(document, zone) &&
                    matchesAll(document, categoryNames[document.categoryId], terms)
            }
            .sortedWith(comparator())
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

    private fun comparator(): Comparator<Document> = when (sort) {
        SortOrder.NEWEST -> compareByDescending { it.importedAt }
        SortOrder.OLDEST -> compareBy { it.importedAt }
        SortOrder.TITLE -> compareBy(Collator.getInstance()) { it.title }
        // Soonest expiry first; documents without one go last.
        SortOrder.EXPIRY -> compareBy<Document, Long?>(nullsLast()) { it.expiresAt }
            .thenByDescending { it.importedAt }
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
        val COMBINING_MARKS = Regex("\\p{Mn}+")

        /** "Città" and "citta" both become "citta". */
        fun normalize(text: String): String =
            Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(COMBINING_MARKS, "")
    }
}
