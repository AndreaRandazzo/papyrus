package io.github.andrearandazzo.papyrus.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Documents with an expiry date, split by urgency. Documents without one are left out. */
data class ExpiryGroups(
    /** Already expired, most recently expired first. */
    val expired: List<Document> = emptyList(),
    /** Expiring today or within [SOON_DAYS] days, soonest first. */
    val soon: List<Document> = emptyList(),
    /** Expiring later, soonest first. */
    val later: List<Document> = emptyList(),
) {
    /** What needs attention: expired or expiring soon. Shown as the tab badge. */
    val urgentCount: Int get() = expired.size + soon.size

    val isEmpty: Boolean get() = expired.isEmpty() && soon.isEmpty() && later.isEmpty()

    companion object {
        const val SOON_DAYS = 30

        fun of(documents: List<Document>, today: LocalDate): ExpiryGroups {
            val dated = documents.mapNotNull { document ->
                document.expiresAt?.let { document to ChronoUnit.DAYS.between(today, utcDay(it)) }
            }.sortedBy { (_, days) -> days }
            return ExpiryGroups(
                expired = dated.filter { (_, days) -> days < 0 }.map { it.first }.reversed(),
                soon = dated.filter { (_, days) -> days in 0..SOON_DAYS.toLong() }.map { it.first },
                later = dated.filter { (_, days) -> days > SOON_DAYS }.map { it.first },
            )
        }

        /** Expiry dates are stored as UTC midnight of the chosen day. */
        private fun utcDay(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    }
}
