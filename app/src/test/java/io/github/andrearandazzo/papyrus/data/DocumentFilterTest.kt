package io.github.andrearandazzo.papyrus.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class DocumentFilterTest {

    private val car = Category(id = 1, name = "Auto")
    private val health = Category(id = 2, name = "Salute")
    private val categories = listOf(car, health)

    private fun doc(
        id: Long,
        title: String,
        mime: String = "application/pdf",
        day: Int = 1,
        categoryId: Long? = null,
        note: String = "",
        expiresAt: Long? = null,
    ) = Document(
        id = id,
        title = title,
        fileName = "$id",
        mimeType = mime,
        sizeBytes = 1,
        importedAt = LocalDate.of(2026, 10, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        categoryId = categoryId,
        note = note,
        expiresAt = expiresAt,
    )

    private val insurance = doc(1, "Polizza assicurazione", day = 5, categoryId = 1, note = "Scade a marzo", expiresAt = 300)
    private val bloodTest = doc(2, "Analisi del sangue", mime = "image/jpeg", day = 10, categoryId = 2, expiresAt = 100)
    private val contract = doc(3, "Contratto affitto città", mime = "application/msword", day = 20)
    private val all = listOf(insurance, bloodTest, contract)

    private fun ids(filter: DocumentFilter) = filter.apply(all, categories, ZoneOffset.UTC).map { it.id }

    @Test
    fun noFilterReturnsEverythingNewestFirst() {
        assertEquals(listOf(3L, 2L, 1L), ids(DocumentFilter()))
    }

    @Test
    fun searchIgnoresCaseAndAccents() {
        assertEquals(listOf(3L), ids(DocumentFilter(query = "CITTA")))
    }

    @Test
    fun searchLooksInNoteAndCategoryName() {
        assertEquals(listOf(1L), ids(DocumentFilter(query = "marzo")))
        assertEquals(listOf(2L), ids(DocumentFilter(query = "salute")))
    }

    @Test
    fun everySearchWordMustMatch() {
        assertEquals(listOf(1L), ids(DocumentFilter(query = "auto polizza")))
        assertEquals(emptyList<Long>(), ids(DocumentFilter(query = "auto sangue")))
    }

    @Test
    fun filtersByCategoryAndType() {
        assertEquals(listOf(2L), ids(DocumentFilter(categoryId = 2)))
        assertEquals(listOf(3L, 2L), ids(DocumentFilter(kinds = setOf(DocumentKind.WORD, DocumentKind.IMAGE))))
    }

    @Test
    fun dateRangeIncludesBothEnds() {
        val filter = DocumentFilter(addedFrom = LocalDate.of(2026, 10, 5), addedTo = LocalDate.of(2026, 10, 10))
        assertEquals(listOf(2L, 1L), ids(filter))
        assertEquals(listOf(3L), ids(DocumentFilter(addedFrom = LocalDate.of(2026, 10, 11))))
    }

    private fun sorted(field: SortField, ascending: Boolean) =
        ids(DocumentFilter(sort = SortOrder(field, ascending)))

    @Test
    fun sortsByDateAddedBothWays() {
        assertEquals(listOf(3L, 2L, 1L), sorted(SortField.ADDED, ascending = false))
        assertEquals(listOf(1L, 2L, 3L), sorted(SortField.ADDED, ascending = true))
    }

    @Test
    fun sortsByTitleBothWays() {
        assertEquals(listOf(2L, 3L, 1L), sorted(SortField.TITLE, ascending = true))
        assertEquals(listOf(1L, 3L, 2L), sorted(SortField.TITLE, ascending = false))
    }

    @Test
    fun sortsByExpiryBothWaysWithMissingExpiryAlwaysLast() {
        assertEquals(listOf(2L, 1L, 3L), sorted(SortField.EXPIRY, ascending = true))
        assertEquals(listOf(1L, 2L, 3L), sorted(SortField.EXPIRY, ascending = false))
    }

    @Test
    fun choosingAFieldStartsInItsNaturalDirection() {
        assertEquals(false, SortOrder(SortField.ADDED).ascending) // newest first
        assertEquals(true, SortOrder(SortField.TITLE).ascending) // A–Z
        assertEquals(true, SortOrder(SortField.EXPIRY).ascending) // soonest first
        assertEquals(SortOrder(SortField.TITLE, ascending = false), SortOrder(SortField.TITLE).reversed())
    }

    @Test
    fun searchLooksInsideDocuments() {
        val contents = mapOf(2L to DocumentFilter.searchable("Emocromo: Ematocrito 41%, Glicemia 88 mg/dL"))
        fun found(query: String) = DocumentFilter(query = query).apply(all, categories, ZoneOffset.UTC, contents).map { it.id }
        assertEquals(listOf(2L), found("glicemia"))
        assertEquals(listOf(2L), found("salute EMATOCRITO")) // one word in the category, one inside
        assertEquals(emptyList<Long>(), found("glicemia polizza"))
    }

    @Test
    fun snippetShowsTheWordInContext() {
        val text = "BOLLETTA LUCE\nIntestatario: Mario Rossi\nCodice cliente: 4815162342\nScadenza pagamento: 15 dicembre 2026"
        assertEquals("…Rossi Codice cliente: 4815162342 Scadenza…", DocumentFilter(query = "4815162342").snippet(text, radius = 25))
        // Accents and case don't matter, and the original text is shown.
        assertEquals("Città di Torino", DocumentFilter(query = "CITTA").snippet("Città di Torino", radius = 40))
        assertEquals(null, DocumentFilter(query = "assente").snippet(text))
    }
}
