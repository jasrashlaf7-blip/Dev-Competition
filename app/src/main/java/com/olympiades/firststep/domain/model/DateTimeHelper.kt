package com.olympiades.firststep.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeHelper {
    fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun nowTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

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
