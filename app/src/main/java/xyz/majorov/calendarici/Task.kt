package xyz.majorov.calendarici

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class Task(
    @PrimaryKey var id : Int,
    var name: String?,
    var details: String?,
    var status : String?,
    var eventId : Int?,
    var parentId : Int?,
    var deadlineAt : Int
)

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: Task)

    @Query("SELECT * FROM task")
    fun getAll(): LiveData<List<Task>>

    @Query("DELETE FROM task")
    fun deleteAll()
}