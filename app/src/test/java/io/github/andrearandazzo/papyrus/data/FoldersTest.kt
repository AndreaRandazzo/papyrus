package io.github.andrearandazzo.papyrus.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class FoldersTest {

    private val today = LocalDate.of(2026, 11, 2)
    private val car = Category(id = 1, name = "Car")
    private val tickets = Category(id = 2, name = "Tickets")

    private fun day(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun doc(id: Long, categoryId: Long?, expires: LocalDate? = null) = Document(
        id = id, title = "$id", fileName = "$id", mimeType = "application/pdf", sizeBytes = 1,
        importedAt = 0, categoryId = categoryId, expiresAt = expires?.let(::day),
    )

    @Test
    fun countsDocumentsAndUrgentOnesPerCategory() {
        val documents = listOf(
            docIn(1, car, expires = today.minusDays(3)), // expired
            docIn(2, car, expires = today.plusDays(10)), // soon
            docIn(3, car, expires = today.plusYears(2)), // later: not urgent
        )
        assertEquals(
            listOf(Folder(car, 3, 2), Folder(tickets, 0, 0)),
            Folder.of(documents, listOf(car, tickets), today),
        )
    }

    @Test
    fun noCategoryFolderOnlyWhenNeeded() {
        val folders = Folder.of(listOf(doc(1, null), doc(2, 1)), listOf(car), today)
        assertEquals(listOf(Folder(car, 1, 0), Folder(null, 1, 0)), folders)
        assertEquals(DocumentFilter.NO_CATEGORY, folders.last().filterId)
        assertEquals(listOf(Folder(car, 1, 0)), Folder.of(listOf(doc(2, 1)), listOf(car), today))
    }

    @Test
    fun noCategoryFilterShowsOnlyDocumentsWithoutOne() {
        val documents = listOf(doc(1, null), doc(2, 1))
        val shown = DocumentFilter(categoryId = DocumentFilter.NO_CATEGORY).apply(documents, listOf(car))
        assertEquals(listOf(1L), shown.map { it.id })
    }

    private fun docIn(id: Long, category: Category, expires: LocalDate? = null) = doc(id, category.id, expires)
}
