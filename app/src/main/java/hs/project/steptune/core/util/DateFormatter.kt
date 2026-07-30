package hs.project.steptune.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {
    private const val MINIMUM_DELAY_MILLIS = 1_000L

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())

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

