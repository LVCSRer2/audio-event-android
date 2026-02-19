package com.audiolifelog.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtil {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun startOfDay(epochMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun endOfDay(epochMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun formatTime(epochMillis: Long): String {
        return timeFormat.format(Date(epochMillis))
    }

    fun formatDate(epochMillis: Long): String {
        return dateFormat.format(Date(epochMillis))
    }

    fun formatDateTime(epochMillis: Long): String {
        return dateTimeFormat.format(Date(epochMillis))
    }

    fun formatIso8601(epochMillis: Long): String {
        return iso8601Format.format(Date(epochMillis))
    }

    fun daysAgo(days: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        return startOfDay(calendar.timeInMillis)
    }
}
