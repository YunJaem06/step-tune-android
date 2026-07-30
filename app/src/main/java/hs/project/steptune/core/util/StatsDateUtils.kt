package hs.project.steptune.core.util

import hs.project.steptune.domain.model.DailyProgress
import hs.project.steptune.domain.model.StatsOverview
import hs.project.steptune.domain.model.StatsPeriod
import hs.project.steptune.domain.model.StatsRecord
import hs.project.steptune.domain.model.StatsSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StatsDateUtils {
    private const val DEFAULT_GOAL = 10_000

    fun queryRange(period: StatsPeriod): Pair<String, String> {
        val today = Calendar.getInstance().normalized()
        val start = when (period) {
            StatsPeriod.DAILY -> today.copy().addDays(-6)
            StatsPeriod.WEEKLY -> today.copy().startOfWeek().addDays(-21)
            StatsPeriod.MONTHLY -> today.copy().startOfMonth().addMonths(-5)
        }
        return formatKey(start.time) to formatKey(today.time)
    }

    fun buildOverview(
        period: StatsPeriod,
        records: List<DailyProgress>
    ): StatsOverview {
        val today = Calendar.getInstance().normalized()
        val recordsByDate = records.associateBy { it.date }

        val dailySnapshots = when (period) {
            StatsPeriod.DAILY -> buildDaySnapshots(today.copy().addDays(-6), today, recordsByDate)
            StatsPeriod.WEEKLY -> buildDaySnapshots(today.copy().startOfWeek().addDays(-21), today, recordsByDate)
            StatsPeriod.MONTHLY -> buildDaySnapshots(today.copy().startOfMonth().addMonths(-5), today, recordsByDate)
        }

        val displayRecords = when (period) {
            StatsPeriod.DAILY -> buildDailyRecords(today, recordsByDate)
            StatsPeriod.WEEKLY -> buildWeeklyRecords(today, recordsByDate)
            StatsPeriod.MONTHLY -> buildMonthlyRecords(today, recordsByDate)
        }

        val summary = StatsSummary(
            averageSteps = if (displayRecords.isEmpty()) 0 else displayRecords.sumOf { it.steps } / displayRecords.size,
            bestSteps = displayRecords.maxOfOrNull { it.steps } ?: 0,
            achievedDays = dailySnapshots.count { it.steps >= it.goal },
            totalSteps = dailySnapshots.sumOf { it.steps }
        )

        return StatsOverview(summary = summary, records = displayRecords)
    }

    private fun buildDailyRecords(
        today: Calendar,
        recordsByDate: Map<String, DailyProgress>
    ): List<StatsRecord> {
        return (6 downTo 0).map { offset ->
            val date = today.copy().addDays(-offset)
            val key = formatKey(date.time)
            val progress = recordsByDate[key]
            StatsRecord(
                label = formatDayLabel(date.time),
                steps = progress?.steps ?: 0
            )
        }
    }

    private fun buildWeeklyRecords(
        today: Calendar,
        recordsByDate: Map<String, DailyProgress>
    ): List<StatsRecord> {
        val currentWeekStart = today.copy().startOfWeek()
        return (3 downTo 0).map { offset ->
            val weekStart = currentWeekStart.copy().addDays(-(offset * 7))
            val weekEnd = weekStart.copy().addDays(6)
            val steps = buildDaySnapshots(weekStart, weekEnd, recordsByDate).sumOf { it.steps }
            StatsRecord(
                label = "${formatDayLabel(weekStart.time)} - ${formatDayLabel(weekEnd.time)}",
                steps = steps
            )
        }
    }

    private fun buildMonthlyRecords(
        today: Calendar,
        recordsByDate: Map<String, DailyProgress>
    ): List<StatsRecord> {
        val currentMonthStart = today.copy().startOfMonth()
        return (5 downTo 0).map { offset ->
            val monthStart = currentMonthStart.copy().addMonths(-offset)
            val monthEnd = monthStart.copy().endOfMonth()
            val steps = buildDaySnapshots(monthStart, monthEnd, recordsByDate).sumOf { it.steps }
            StatsRecord(
                label = formatMonthLabel(monthStart.time),
                steps = steps
            )
        }
    }

    private fun buildDaySnapshots(
        start: Calendar,
        end: Calendar,
        recordsByDate: Map<String, DailyProgress>
    ): List<DailySnapshot> {
        val cursor = start.copy()
        val snapshots = mutableListOf<DailySnapshot>()
        while (!cursor.after(end)) {
            val key = formatKey(cursor.time)
            val progress = recordsByDate[key]
            snapshots += DailySnapshot(
                steps = progress?.steps ?: 0,
                goal = progress?.goal ?: DEFAULT_GOAL
            )
            cursor.addDays(1)
        }
        return snapshots
    }

    private fun Calendar.startOfWeek(): Calendar {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return normalized()
    }

    private fun Calendar.startOfMonth(): Calendar {
        set(Calendar.DAY_OF_MONTH, 1)
        return normalized()
    }

    private fun Calendar.endOfMonth(): Calendar {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        return normalized()
    }

    private fun Calendar.normalized(): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Calendar.copy(): Calendar = clone() as Calendar

    private fun Calendar.addDays(days: Int): Calendar = apply { add(Calendar.DAY_OF_MONTH, days) }

    private fun Calendar.addMonths(months: Int): Calendar = apply { add(Calendar.MONTH, months) }

    private fun formatKey(date: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(date)

    private fun formatDayLabel(date: Date): String = SimpleDateFormat("M/d", Locale.KOREA).format(date)

    private fun formatMonthLabel(date: Date): String = SimpleDateFormat("M월", Locale.KOREA).format(date)

    private data class DailySnapshot(
        val steps: Int,
        val goal: Int
    )
}

