package xyz.majorov.calendaric

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendaricViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CalendaricRepository

    val allTasks: LiveData<List<Task>>
    val allEvents: LiveData<List<Event>>

    init {
        val database = CalendaricRoomDatabase.getDatabase(application, viewModelScope)
        val taskDao = database.taskDao()
        val eventDao = database.eventDao()

        repository = CalendaricRepository(taskDao, eventDao)
        allTasks = repository.allTasks
        allEvents = repository.allEvents
    }

    fun insertTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertTask(task)
    }

    fun insertEvent(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertEvent(event)
    }
}