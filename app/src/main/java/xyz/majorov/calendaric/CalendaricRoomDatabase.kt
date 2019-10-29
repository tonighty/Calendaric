package xyz.majorov.calendaric

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime

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
                ).addCallback(CalendaricDatabaseCallback(scope)).build()
                INSTANCE = instance
                return instance
            }
        }
    }

    private class CalendaricDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.taskDao(), database.eventDao())
                }
            }
        }

        suspend fun populateDatabase(taskDao: TaskDao, eventDao: EventDao) {
//            taskDao.deleteAll()
//
//            var task = Task(1, "Hello", "Test", "OK", 1, null, 2)
//            taskDao.insert(task)
//            task = Task(2, "World", "Test", "OK", 1, null, 3)
//            taskDao.insert(task)

//            eventDao.deleteAll()
//            for (i in 1..10) {
//                eventDao.insert(Event(
//                    i,
//                    "test",
//                    "Go to hospital $i",
//                    "Do not forget tests",
//                    null,
//                    null,
//                    LocalDateTime.now().plusDays((i - 1).toLong()),
//                    LocalDateTime.now().plusDays((i - 1).toLong()).plusHours(2),
//                    null,
//                    null
//                ))
//            }
        }
    }
}