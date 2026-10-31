package io.github.andrearandazzo.papyrus.ui

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

private fun utcDay(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
