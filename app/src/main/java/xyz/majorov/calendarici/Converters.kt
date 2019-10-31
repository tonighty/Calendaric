package xyz.majorov.calendarici

import androidx.room.TypeConverter
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofEpochSecond(value / 1000, (value % 1000).toInt() * 1000000, ZoneOffset.UTC) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.let { it.toEpochSecond(ZoneOffset.UTC) * 1000 + it.nano / 1000000 }
    }
}