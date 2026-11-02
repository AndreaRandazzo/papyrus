package io.github.andrearandazzo.papyrus.reminders

import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.ExpiryGroups
import java.time.LocalDate

/** A notification to show: [document] expires in [daysLeft] days (0 today, negative already expired). */
data class Reminder(val document: Document, val daysLeft: Long)

/**
 * Which expiry reminders are due. A document gets one 30 days before, one 7 days before and one on
 * the day. Each is sent once: [Plan.sent] remembers them by document, milestone and expiry date, so
 * changing the date starts over. If several are due at once (the phone was off, say), only the most
 * urgent one is shown.
 */
object ReminderPlanner {
    val MILESTONES = listOf(30, 7, 0)

    data class Plan(val reminders: List<Reminder>, val sent: Set<String>)

    fun plan(documents: List<Document>, today: LocalDate, sent: Set<String>): Plan {
        val reminders = mutableListOf<Reminder>()
        val nowSent = mutableSetOf<String>()
        for (document in documents) {
            val expiresAt = document.expiresAt ?: continue
            val daysLeft = ExpiryGroups.daysLeft(expiresAt, today)
            val keys = MILESTONES.map { key(document.id, it, expiresAt) to it }
            // Keep what was already sent for this document and date; forget deleted ones and old dates.
            nowSent += keys.map { it.first }.filter { it in sent }
            val reached = keys.filter { (_, milestone) -> daysLeft <= milestone }
            if (reached.isEmpty()) continue
            if (reached.any { (key, _) -> key !in sent }) reminders += Reminder(document, daysLeft)
            nowSent += reached.map { it.first }
        }
        return Plan(reminders, nowSent)
    }

    /**
     * Turning reminders on: what's already due counts as sent, so the user isn't flooded with
     * notifications for documents that expired long ago. The Expiring tab shows those.
     */
    fun baseline(documents: List<Document>, today: LocalDate): Set<String> = plan(documents, today, emptySet()).sent

    private fun key(id: Long, milestone: Int, expiresAt: Long) = "$id:$milestone:$expiresAt"
}
