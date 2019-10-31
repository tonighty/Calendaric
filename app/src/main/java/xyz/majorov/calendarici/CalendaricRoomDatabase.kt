package xyz.majorov.calendarici

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.CoroutineScope

@Database(entities = [Task::class, Event::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CalendaricRoomDatabase : RoomDatabase() {

    abstract fun taskDao() : TaskDao

    abstract fun eventDao() : EventDao

    companion object {
        @Volatile
        private var INSTANCE: CalendaricRoomDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope) : CalendaricRoomDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CalendaricRoomDatabase::class.java,
                    "Calendaric_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}