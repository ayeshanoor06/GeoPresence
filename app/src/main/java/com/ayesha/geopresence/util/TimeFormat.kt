package com.ayesha.geopresence.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormat {

    fun clock(millis: Long): String =
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(millis))

    fun duration(minutes: Int): String {
        if (minutes <= 0) return "—"
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "$m min"
    }

    fun dayTitle(date: LocalDate, today: LocalDate = LocalDate.now()): String {
        val prefix = when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
        }
        return "$prefix, ${date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))}"
    }
}