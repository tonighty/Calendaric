package xyz.majorov.calendarici

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import org.threeten.bp.LocalDateTime
import retrofit2.Call
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit


interface CalendaricApi {

    @GET("events")
    fun listEvents(): Call<CalendaricResponse<EventApiModel>>

    @POST("events")
    fun createEvent(@Body event: EventApiModel): Call<CalendaricResponse<EventApiModel>>

    @PATCH("events/{id}")
    fun updateEvent(@Path("id") id: Long, @Body updates: EventApiModel): Call<CalendaricResponse<EventApiModel>>

    @DELETE("events/{id}")
    fun deleteEvent(@Path("id") id: Long): Call<Void>

    @GET("patterns")
    fun listPatterns(): Call<CalendaricResponse<EventPatternApiModel>>

    @POST("patterns")
    fun createPattern(@Query("event_id") event_id: Long, @Body pattern: PatternRequestApiModel): Call<CalendaricResponse<EventPatternApiModel>>

    @PATCH("patterns/{id}")
    fun updatePattern(@Path("id") id: Long, @Body updates: PatternRequestApiModel): Call<CalendaricResponse<EventPatternApiModel>>

    @DELETE("patterns/{id}")
    fun deletePattern(@Path("id") id: Long): Call<Void>

    companion object {

        private const val BASE_URL = "http://frrcode.com:9040/api/v1/"

        @Volatile
        private var INSTANCE: CalendaricApi? = null
        private var token: String? = null

        fun instance(): CalendaricApi? {
            synchronized(this) {
                val user = FirebaseAuth.getInstance().currentUser ?: return null

                val result: GetTokenResult
                try {
                    result = Tasks.await(user.getIdToken(false), 100000, TimeUnit.MILLISECONDS)
                } catch (e: Exception) {
                    Log.v(Constants.LOG_TAG, "Firebase token exception: ${e.message}")
                    return null
                }

                val tempInstance = INSTANCE
                if (tempInstance != null && token == result.token) {
                    return tempInstance
                }

                val tempToken = result.token ?: return null
                Log.v(Constants.LOG_TAG, "Got firebase token: $tempToken")

                val httpClient = OkHttpClient.Builder()
                httpClient.addInterceptor { chain ->
                    val request = chain.request().newBuilder().addHeader("X-Firebase-Auth", tempToken).build()
                    chain.proceed(request)
                }

                val retrofit = retrofit2.Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build()

                val instance = retrofit.create(CalendaricApi::class.java)

                token = tempToken
                INSTANCE = instance
                return instance
            }
        }
    }
}

class CalendaricResponse<T>(
    val count: Int,
    val data: List<T>,
    val message: String
)

class EventApiModel(
    val id: Long,
    @SerializedName("owner_id") val ownerId: String,
    var name: String?,
    var details: String?,
    var location: String?,
    var status: String?
) {
    constructor(event: Event) : this(
        0,
        event.ownerId,
        event.name,
        event.details,
        event.location,
        event.status
    )
}

class EventPatternApiModel(
    val id: Long,
    val event_id: Long,
    val ended_at: Long?,
    val started_at: Long?,
    val rrule: String?,
    val timezone: String?,
    val duration: Long?
) {
    constructor(event: Event) : this(
        0,
        0,
        dateToTimestamp(LocalDateTime.now().plusYears(1)),
        dateToTimestamp(event.startedAt),
        event.rrule,
        "UTC",
        (dateToTimestamp(event.endedAt) ?: 0) - (dateToTimestamp(event.startedAt) ?: 0)
    )
}

class PatternRequestApiModel(
    val ended_at: Long?,
    val started_at: Long?,
    val rrule: String?,
    val timezone: String?,
    val duration: Long?
) {
    constructor(event: Event) : this(
        dateToTimestamp(LocalDateTime.now().plusYears(10)),
        dateToTimestamp(event.startedAt),
        event.rrule ?: "",
        "UTC",
        (dateToTimestamp(event.endedAt) ?: 0) - (dateToTimestamp(event.startedAt) ?: 0)
    )
}