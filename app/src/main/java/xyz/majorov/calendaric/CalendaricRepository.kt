package xyz.majorov.calendaric

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData

class CalendaricRepository(private val taskDao: TaskDao, private val eventDao: EventDao) {

    val allTasks: LiveData<List<Task>> = taskDao.getAll()
    val allEvents: LiveData<List<Event>> = eventDao.getAll()

    @WorkerThread
    suspend fun insertTask(task: Task) {
        taskDao.insert(task)
    }

    @WorkerThread
    suspend fun insertEvent(event: Event) {
        eventDao.insert(event)
    }
}