package com.olympiades.firststep.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

object DateTimeHelper {
    private const val MS_PER_DAY = 86_400_000L

    fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun nowTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    fun daysBetween(startIsoOrTimestamp: String, endIsoOrTimestamp: String): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        val startStr = startIsoOrTimestamp.take(10)
        val endStr = endIsoOrTimestamp.take(10)
        val startDate = runCatching { fmt.parse(startStr) }.getOrNull() ?: return 0
        val endDate = runCatching { fmt.parse(endStr) }.getOrNull() ?: return 0
        val diffMs = endDate.time - startDate.time
        return floor(diffMs.toDouble() / MS_PER_DAY.toDouble()).toInt()
    }

    fun isOlderThanDays(isoDateOrTimestamp: String, todayIso: String, days: Int): Boolean {
        return daysBetween(isoDateOrTimestamp, todayIso) > days
    }

    fun formatShortDate(isoDateOrTimestamp: String): String {
        val datePart = isoDateOrTimestamp.take(10)
        val parts = datePart.split("-")
        return if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            isoDateOrTimestamp
        }
    }

    fun formatDayMonth(isoDateOrTimestamp: String): String {
        val datePart = isoDateOrTimestamp.take(10)
        val parts = datePart.split("-")
        return if (parts.size == 3) {
            "${parts[2]}/${parts[1]}"
        } else {
            isoDateOrTimestamp
        }
    }
}
