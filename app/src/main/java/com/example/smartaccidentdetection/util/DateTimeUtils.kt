package com.example.smartaccidentdetection.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {

    /**
     * Converts any ISO 8601 UTC timestamp or epoch millis into the device's local timezone (e.g. IST).
     * Example output: "03 Sep 2026, 10:50 AM"
     */
    fun formatToLocalDateTime(isoOrEpoch: String?): String {
        if (isoOrEpoch.isNullOrBlank() || isoOrEpoch == "Just now") {
            return "Today, ${getCurrentLocalTime()}"
        }

        val date = parseToDate(isoOrEpoch) ?: return isoOrEpoch
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(date)
    }

    /**
     * Formats with date, hour, minute, and second in local timezone:
     * Example output: "03 Sep 2026, 10:50:35 AM"
     */
    fun formatToLocalDateTimeWithSeconds(isoOrEpoch: String?): String {
        if (isoOrEpoch.isNullOrBlank() || isoOrEpoch == "Just now") {
            return "Today, ${getCurrentLocalTimeWithSeconds()}"
        }

        val date = parseToDate(isoOrEpoch) ?: return isoOrEpoch
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(date)
    }

    /**
     * Formats to time only in local timezone: "10:50 AM"
     */
    fun formatToLocalTime(isoOrEpoch: String?): String {
        if (isoOrEpoch.isNullOrBlank() || isoOrEpoch == "Just now") {
            return getCurrentLocalTime()
        }
        val date = parseToDate(isoOrEpoch) ?: return isoOrEpoch
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(date)
    }

    fun getCurrentLocalDateTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(Date())
    }

    fun getCurrentLocalTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(Date())
    }

    fun getCurrentLocalTimeWithSeconds(): String {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(Date())
    }

    fun parseToDate(raw: String): Date? {
        val epoch = raw.toLongOrNull()
        if (epoch != null) {
            return Date(epoch)
        }

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(raw)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    fun parseToEpochMillis(raw: String?, fallbackMillis: Long = 0L): Long {
        if (raw.isNullOrBlank()) return fallbackMillis
        val date = parseToDate(raw) ?: return fallbackMillis
        return date.time
    }

    fun formatEpochToLocalTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(Date(epochMillis))
    }

    fun formatEpochToLocalDateTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(Date(epochMillis))
    }
}
