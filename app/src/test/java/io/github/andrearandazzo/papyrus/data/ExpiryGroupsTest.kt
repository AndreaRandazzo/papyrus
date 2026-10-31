package io.github.andrearandazzo.papyrus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ExpiryGroupsTest {

    private val today = LocalDate.of(2026, 10, 31)

    private fun doc(id: Long, expires: LocalDate?) = Document(
        id = id,
        title = "Doc $id",
        fileName = "$id.pdf",
        mimeType = "application/pdf",
        sizeBytes = 1,
        importedAt = 0,
        expiresAt = expires?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )

    @Test
    fun groupsByUrgencyAndSkipsDocumentsWithoutExpiry() {
        val groups = ExpiryGroups.of(
            listOf(
                doc(1, today.minusDays(20)),
                doc(2, today.minusDays(1)),
                doc(3, today),
                doc(4, today.plusDays(30)),
                doc(5, today.plusDays(31)),
                doc(6, today.plusYears(5)),
                doc(7, null),
            ),
            today,
        )
        assertEquals(listOf(2L, 1L), groups.expired.map { it.id }) // most recently expired first
        assertEquals(listOf(3L, 4L), groups.soon.map { it.id }) // today and day 30 count as soon
        assertEquals(listOf(5L, 6L), groups.later.map { it.id })
        assertEquals(4, groups.urgentCount)
    }

    @Test
    fun emptyWhenNothingHasAnExpiry() {
        assertTrue(ExpiryGroups.of(listOf(doc(1, null)), today).isEmpty)
    }
}
