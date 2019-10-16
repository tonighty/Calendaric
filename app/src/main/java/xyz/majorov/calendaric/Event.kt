package xyz.majorov.calendaric

import androidx.lifecycle.LiveData
import androidx.room.*
import org.threeten.bp.LocalDateTime

@Entity
data class Event (
    @PrimaryKey(autoGenerate = true) val id : Int = 0,
    val ownerId : String,
    var name : String?,
    var details : String?,
    var location : String?,
    var status : String?,
    var startedAt : LocalDateTime?,
    var endedAt : LocalDateTime?,
    var createdAt : LocalDateTime?,
    var updatedAt : LocalDateTime?
)

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: Event)

    @Query("SELECT * FROM event")
    fun getAll(): LiveData<List<Event>>

    @Query("DELETE FROM event")
    fun deleteAll()
}
