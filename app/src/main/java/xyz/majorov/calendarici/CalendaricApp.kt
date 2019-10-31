package xyz.majorov.calendarici

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class CalendaricApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}

class Constants {
    companion object {
        const val LOG_TAG = "mjr"
    }
}