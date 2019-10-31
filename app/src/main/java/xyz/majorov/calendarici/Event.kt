package xyz.majorov.calendarici

import androidx.lifecycle.LiveData
import androidx.room.*
import org.threeten.bp.LocalDateTime

@Entity
data class Event(
    @PrimaryKey(autoGenerate = true) val primaryKey: Long = 0,
    var synced: Boolean = false,
    var id: Long? = null,
    var ownerId: String,
    var name: String?,
    var details: String?,
    var location: String?,
    var status: String?,
    var startedAt: LocalDateTime? = null,
    var endedAt: LocalDateTime? = null,
    var rrule: String? = "FREQ=WEEKLY;BYDAY=SU;INTERVAL=1",
    var patternId: Long? = null,
    var timezone: String? = "UTC"
) {
    constructor(eventApi: EventApiModel) : this(
        0,
        true,
        eventApi.id,
        eventApi.ownerId,
        eventApi.name,
        eventApi.details,
        eventApi.location,
        eventApi.status
    )
}

data class EventInstance(
    val primaryKey: Long,
    val id: Long?,
    val patternId: Long?,
    var startedAt: LocalDateTime?,
    var endedAt: LocalDateTime?,
    val name: String?
) {
    constructor(event: Event): this (
        event.primaryKey,
        event.id,
        event.patternId,
        event.startedAt,
        event.endedAt,
        event.name
    )
}

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: Event)

    @Query("SELECT * FROM event")
    fun getAll(): LiveData<List<Event>>

    @Query("SELECT * FROM event WHERE id = :id")
    fun getById(id: Long): Event?

    @Query("SELECT * FROM event WHERE primaryKey = :key")
    fun getByPrimaryKey(key: Long): LiveData<Event>

    @Query("SELECT * FROM event WHERE NOT synced")
    fun getNotSynced(): List<Event>

    @Query("DELETE FROM event")
    fun deleteAll()

    @Update
    suspend fun update(vararg event: Event)

    @Delete
    fun deleteEvents(vararg event: Event)
}
