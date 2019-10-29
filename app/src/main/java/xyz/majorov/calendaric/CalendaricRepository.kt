package xyz.majorov.calendaric

import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset

class CalendaricRepository(private val taskDao: TaskDao, private val eventDao: EventDao) {

    val allTasks: LiveData<List<Task>> = taskDao.getAll()
    val allEvents: LiveData<List<Event>> = eventDao.getAll()

    @WorkerThread
    suspend fun insertTask(task: Task) {
        taskDao.insert(task)
    }

    fun getEventByPrimaryKey(key: Long): LiveData<Event> {
        return eventDao.getByPrimaryKey(key)
    }

    suspend fun deleteEvent(event: Event) {
        val eventId = event.id
        val patternId = event.patternId
        eventDao.deleteEvents(event)
        if (eventId !== null) deleteEventWeb(eventId)
    }

    @WorkerThread
    suspend fun updateEvent(event: Event) {
        event.synced = false
        eventDao.update(event)

        if (event.id === null || event.patternId === null) {
            createEventWeb(event)
        } else {
            updateEventWeb(event)
        }
    }

    @WorkerThread
    suspend fun insertEvent(event: Event) {
        eventDao.insert(event)
        createEventWeb(event)
    }

    @WorkerThread
    suspend fun syncEvents() {
        CalendaricApi.create()?.let { api ->
            val eventsResponse = api.listEvents().execute()
            val eventsBody = eventsResponse.body()

            val patternsResponse = api.listPatterns().execute()
            val patternsBody = patternsResponse.body()

            if (eventsResponse.code() == 200 &&
                eventsBody !== null &&
                patternsResponse.code() == 200 &&
                patternsBody !== null) {

                for (e in eventsBody.data) {
                    Log.v("lol", "Got event from web: ${e.id}")
                    val event = eventDao.getById(e.id) ?: Event(e)
                    val pattern = patternsBody.data.find { it.event_id == e.id }

                    event.patternId = pattern?.id
                    event.rrule = pattern?.rrule
                    event.startedAt = fromTimestamp(pattern?.started_at)
                    event.endedAt = fromTimestamp((pattern?.started_at ?: 0) + (pattern?.duration ?: 0))

                    if (event.primaryKey == 0L) eventDao.insert(event) else eventDao.update(event)
                }
            }
            for (event in eventDao.getNotSynced()) {
                createEventWeb(event)
            }
        }
    }

    private fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let {
            LocalDateTime.ofEpochSecond(it / 1000, (it % 1000).toInt(), ZoneOffset.UTC)
        }
    }

    private suspend fun createEventWeb(event: Event) {
        CalendaricApi.create()?.let { api ->
            if (event.id === null) {
                val responseEvent = api.createEvent(EventApiModel(event)).execute()
                val eventBody = responseEvent.body()
                if (responseEvent.code() == 200 && eventBody !== null) {
                    eventBody.data[0].let { receivedEvent ->
                        event.id = receivedEvent.id
                        event.ownerId = receivedEvent.ownerId
                        event.synced = true

                        createPatternWeb(event)

                        Log.v("lol", "Created event: ${receivedEvent.id}")

                        eventDao.update(event)
                    }
                }
            } else if (event.patternId === null) {
                createPatternWeb(event)
            }
        }
    }

    private fun createPatternWeb(event: Event) {
        CalendaricApi.create()?.let { api ->
            val responsePattern = api.createPattern(event.id!!, PatternRequestApiModel(event)).execute()
            val patternBody = responsePattern.body()

            if (responsePattern.code() == 200 && patternBody !== null) {
                event.patternId = patternBody.data[0].id
                Log.v("lol", "Created pattern: ${event.patternId}")
            }
        }
    }

    private suspend fun updateEventWeb(event: Event) {
        CalendaricApi.create()?.let { api ->
            val responseEvent = api.updateEvent(event.id!!, EventApiModel(event)).execute()
            if (responseEvent.code() == 200) {
                val responsePattern = api.updatePattern(event.patternId!!, PatternRequestApiModel(event)).execute()
                if (responsePattern.code() == 200) {
                    Log.v("lol", "Updated pattern: ${event.patternId}")
                } else {
                    Log.e("lol", responsePattern.message())
                }

                Log.v("lol", "Updated event: ${event.id}")

                event.synced = true
                eventDao.update(event)
            } else {
                Log.e("lol", responseEvent.message())
            }
        }
    }

    private fun deleteEventWeb(id: Long) {
        CalendaricApi.create()?.let { api ->
            val responseEvent = api.deleteEvent(id).execute()
            if (responseEvent.code() == 200) {
                Log.v("lol", "Deleted event: $id")
            } else {
                Log.e("lol", responseEvent.message())
            }
        }
    }
}