package hs.project.steptune.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {
    private const val MINIMUM_DELAY_MILLIS = 1_000L
    private const val DATE_PATTERN = "yyyy-MM-dd"
    private const val OFFSET_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX"
    private const val OFFSET_DATE_TIME_MILLIS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    private val fractionalSecondsPattern = Regex("^(.+?\\.)(\\d+)(Z|[+-]\\d{2}:\\d{2})$")

    fun today(): String = SimpleDateFormat(DATE_PATTERN, Locale.KOREA).format(Date())

    fun daysAgo(days: Int): String {
        require(days >= 0) { "days must not be negative" }
        val date = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -days)
        }.time
        return SimpleDateFormat(DATE_PATTERN, Locale.KOREA).format(date)
    }

    fun measuredAt(epochMillis: Long): String =
        SimpleDateFormat(OFFSET_DATE_TIME_MILLIS_PATTERN, Locale.US).format(Date(epochMillis))

    fun parseMeasuredAt(value: String): Long? {
        val match = fractionalSecondsPattern.matchEntire(value)
        val normalizedValue = if (match == null) {
            value
        } else {
            val (prefix, fractionalSeconds, offset) = match.destructured
            prefix + fractionalSeconds.padEnd(length = 3, padChar = '0').take(3) + offset
        }
        val pattern = if (match == null) {
            OFFSET_DATE_TIME_PATTERN
        } else {
            OFFSET_DATE_TIME_MILLIS_PATTERN
        }
        return runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
            }.parse(normalizedValue)?.time
        }.getOrNull()
    }

    fun endOfDayEpochMillis(date: String): Long? {
        val parsedDate = runCatching {
            SimpleDateFormat(DATE_PATTERN, Locale.KOREA).apply {
                isLenient = false
            }.parse(date)
        }.getOrNull() ?: return null
        return Calendar.getInstance().apply {
            time = parsedDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun millisUntilNextDay(): Long {
        val now = Calendar.getInstance()
        val nextDay = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (nextDay.timeInMillis - now.timeInMillis).coerceAtLeast(MINIMUM_DELAY_MILLIS)
    }
}

