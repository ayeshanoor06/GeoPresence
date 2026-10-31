package com.ayesha.geopresence.util

import com.ayesha.geopresence.data.model.AttendanceRecord
import com.ayesha.geopresence.data.model.AttendanceSource
import com.ayesha.geopresence.data.model.AttendanceStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.TreeSet
import kotlin.math.roundToInt

object AttendanceStats {

    private const val MAX_LOOKBACK_DAYS = 400L

    data class Summary(val present: Int, val late: Int, val absent: Int, val total: Int) {
        val attended: Int get() = present + late
        val percent: Int get() = if (total == 0) 0 else ((attended * 100.0) / total).roundToInt()
    }

    data class DayEntry(
        val date: LocalDate,
        val status: AttendanceStatus,
        val recorded: Boolean,          // false = auto-generated Absent day
        val entryMillis: Long?,
        val exitMillis: Long?,
        val durationMinutes: Int,
        val note: String?,
        val manual: Boolean
    )

    data class Computed(
        val summary: Summary,
        val history: List<DayEntry>,    // newest first
        val today: DayEntry?
    )

    fun compute(
        records: List<AttendanceRecord>,
        semesterStart: String?,
        holidays: List<String>,
        notBefore: LocalDate?,
        today: LocalDate = LocalDate.now()
    ): Computed {
        val byDate = HashMap<LocalDate, AttendanceRecord>()
        for (r in records) {
            val d = runCatching { LocalDate.parse(r.date) }.getOrNull() ?: continue
            byDate[d] = r
        }

        val holidaySet = holidays.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()

        // Counting starts at the semester start (or first record), never before registration
        val base = semesterStart?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: byDate.keys.minOrNull()
        val lowerBound = today.minusDays(MAX_LOOKBACK_DAYS)
        var start: LocalDate? = base?.let { if (it.isBefore(lowerBound)) lowerBound else it }
        if (start != null && notBefore != null && notBefore.isAfter(start)) start = notBefore

        val days = TreeSet<LocalDate>()
        if (start != null) {
            var d: LocalDate = start
            val last = today.minusDays(1)          // today only counts once there is a record
            while (!d.isAfter(last)) {
                val weekend = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
                if (!weekend && d !in holidaySet) days.add(d)
                d = d.plusDays(1)
            }
        }
        days.addAll(byDate.keys)

        var present = 0
        var late = 0
        var absent = 0
        val history = ArrayList<DayEntry>()

        for (day in days.descendingSet()) {
            val r = byDate[day]
            val status = r?.status ?: AttendanceStatus.ABSENT
            when (status) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.LATE -> late++
                AttendanceStatus.ABSENT -> absent++
            }
            history.add(
                DayEntry(
                    date = day,
                    status = status,
                    recorded = r != null,
                    entryMillis = r?.entryTime?.toDate()?.time,
                    exitMillis = r?.exitTime?.toDate()?.time,
                    durationMinutes = r?.durationMinutes ?: 0,
                    note = r?.note,
                    manual = r?.source == AttendanceSource.MANUAL
                )
            )
        }

        return Computed(
            summary = Summary(present, late, absent, days.size),
            history = history,
            today = history.firstOrNull { it.date == today && it.recorded }
        )
    }
}