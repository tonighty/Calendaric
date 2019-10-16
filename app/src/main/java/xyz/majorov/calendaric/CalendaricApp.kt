package xyz.majorov.calendaric

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class CalendaricApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}