package xyz.majorov.calendaric

import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

fun formatDate(dateTime: LocalDateTime?): String {
    return dateTime?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: ""
}

fun formatTime(dateTime: LocalDateTime?): String {
    return dateTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
}