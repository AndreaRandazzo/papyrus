package io.github.andrearandazzo.papyrus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.andrearandazzo.papyrus.R
import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Formats an instant (e.g. import time) as a date in the phone's time zone. */
fun formatInstantDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

/** Formats a calendar day stored as UTC midnight, as the date picker produces. */
fun formatDay(utcMidnightMillis: Long): String = utcDay(utcMidnightMillis).format(dateFormatter)

fun isPast(utcMidnightMillis: Long): Boolean = utcDay(utcMidnightMillis).isBefore(LocalDate.now())

/** Days from today until the given day: 0 today, negative once past. */
fun daysUntil(utcMidnightMillis: Long): Long = ChronoUnit.DAYS.between(LocalDate.now(), utcDay(utcMidnightMillis))

fun utcDay(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun formatDate(date: LocalDate): String = date.format(dateFormatter)

/** "Expires 12 May 2031", "Expires in 3 weeks", "Expires today", "Expired 15 Oct 2026". */
@Composable
fun expiryText(utcMidnightMillis: Long): String {
    val days = daysUntil(utcMidnightMillis)
    return when {
        days < 0 -> stringResource(R.string.expired_on, formatDay(utcMidnightMillis))
        days == 0L -> stringResource(R.string.expires_today)
        days < 14 -> pluralStringResource(R.plurals.expires_in_days, days.toInt(), days.toInt())
        days < 60 -> (days / 7).toInt().let { pluralStringResource(R.plurals.expires_in_weeks, it, it) }
        else -> stringResource(R.string.expires_on, formatDay(utcMidnightMillis))
    }
}
