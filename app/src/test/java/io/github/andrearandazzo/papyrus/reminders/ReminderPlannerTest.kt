package io.github.andrearandazzo.papyrus.reminders

import io.github.andrearandazzo.papyrus.data.Document
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ReminderPlannerTest {

    private val today = LocalDate.of(2026, 11, 2)

    private fun doc(id: Long, expiresInDays: Long?) = Document(
        id = id, title = "$id", fileName = "$id", mimeType = "application/pdf", sizeBytes = 1, importedAt = 0,
        expiresAt = expiresInDays?.let { today.plusDays(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
    )

    private fun ids(plan: ReminderPlanner.Plan) = plan.reminders.map { it.document.id }

    @Test
    fun remindsAt30And7DaysAndOnTheDay() {
        val docs = listOf(doc(1, 30), doc(2, 7), doc(3, 0), doc(4, 31), doc(5, 12), doc(6, null))
        val plan = ReminderPlanner.plan(docs, today, emptySet())
        assertEquals(listOf(1L, 2L, 3L, 5L), ids(plan)) // 12 days left: the 30-day reminder is due
    }

    @Test
    fun eachReminderIsSentOnce() {
        val docs = listOf(doc(1, 30))
        val first = ReminderPlanner.plan(docs, today, emptySet())
        assertEquals(listOf(1L), ids(first))
        assertEquals(emptyList<Long>(), ids(ReminderPlanner.plan(docs, today, first.sent)))
        // 23 days later the 7-day reminder comes.
        val later = ReminderPlanner.plan(docs, today.plusDays(23), first.sent)
        assertEquals(listOf(1L), ids(later))
        assertEquals(7L, later.reminders.single().daysLeft)
    }

    @Test
    fun afterDaysOffOnlyTheMostUrgentIsShownOnce() {
        // 30-day reminder sent, then the phone stays off until the day after expiry.
        val docs = listOf(doc(1, 30))
        val sent = ReminderPlanner.plan(docs, today, emptySet()).sent
        val plan = ReminderPlanner.plan(docs, today.plusDays(31), sent)
        assertEquals(-1L, plan.reminders.single().daysLeft)
        assertEquals(emptyList<Long>(), ids(ReminderPlanner.plan(docs, today.plusDays(32), plan.sent)))
    }

    @Test
    fun aNewExpiryDateStartsOver() {
        val sent = ReminderPlanner.plan(listOf(doc(1, 5)), today, emptySet()).sent
        assertEquals(listOf(1L), ids(ReminderPlanner.plan(listOf(doc(1, 6)), today, sent)))
    }

    @Test
    fun turningRemindersOnDoesNotFloodWithOldOnes() {
        val docs = listOf(doc(1, -200), doc(2, 3), doc(3, 90))
        val sent = ReminderPlanner.baseline(docs, today)
        assertEquals(emptyList<Long>(), ids(ReminderPlanner.plan(docs, today, sent)))
        // Later reminders still come: document 2's "on the day" one, document 3's 30-day one.
        assertEquals(listOf(2L, 3L), ids(ReminderPlanner.plan(docs, today.plusDays(60), sent)))
    }

    @Test
    fun deletedDocumentsAreForgotten() {
        val sent = ReminderPlanner.plan(listOf(doc(1, 3)), today, emptySet()).sent
        assertEquals(emptySet<String>(), ReminderPlanner.plan(emptyList(), today, sent).sent)
    }
}
