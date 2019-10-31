package xyz.majorov.calendarici

import org.dmfs.rfc5545.DateTime
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

fun formatDate(dateTime: LocalDateTime?): String {
    return dateTime?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: ""
}

fun formatTime(dateTime: LocalDateTime?): String {
    return dateTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
}

fun dateToTimestamp(date: LocalDateTime?): Long? {
    return date?.let { it.toEpochSecond(ZoneOffset.UTC) * 1000 + it.nano / 1000000 }
}

fun localDateToDateTime(date: LocalDate): DateTime {
    return DateTime(date.year, date.monthValue - 1, date.dayOfMonth)
}

fun dateTimeToLocalDate(date: DateTime): LocalDate {
    return LocalDate.of(date.year, date.month + 1, date.dayOfMonth)
}

fun fromTimestamp(value: Long?): LocalDateTime? {
    return value?.let {
        LocalDateTime.ofEpochSecond(it / 1000, (it % 1000).toInt(), ZoneOffset.UTC)
    }
}
