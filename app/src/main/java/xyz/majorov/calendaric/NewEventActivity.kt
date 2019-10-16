package xyz.majorov.calendaric

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_new_event.*
import kotlinx.android.synthetic.main.content_new_event.*
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

class NewEventActivity : AppCompatActivity() {

    private var startedAt = LocalDateTime.now()
        set(value) {
            startedAtView.text = formatDate(value)
            startedAtTimeView.text = formatTime(value)
            field = value
        }
    private var endedAt = LocalDateTime.now().plusHours(4)
        set(value) {
            endedAtView.text = formatDate(value)
            endedAtTimeView.text = formatTime(value)

            if (value <= startedAt) {
                endedAtView.setTextColorResource(R.color.colorAccent)
                endedAtTimeView.setTextColorResource(R.color.colorAccent)
            } else {
                endedAtView.setTextColorResource(R.color.black)
                endedAtTimeView.setTextColorResource(R.color.black)
            }

            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_event)
        setSupportActionBar(toolbar)

        val calendaricViewModel = ViewModelProvider(this).get(CalendaricViewModel::class.java)

        startedAt = startedAt
        endedAt = endedAt

        allDaySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startedAtTimeView.visibility = INVISIBLE
                endedAtTimeView.visibility = INVISIBLE
                startedAt = startedAt.withHour(0).withMinute(0)
                endedAt = endedAt.withHour(23).withMinute(59)
            } else {
                startedAtTimeView.visibility = VISIBLE
                endedAtTimeView.visibility = VISIBLE
                val now = LocalTime.now()
                startedAt = startedAt.withHour(now.hour).withMinute(now.minute)
                endedAt = endedAt.withHour(now.hour).plusHours(3).withMinute(now.minute)
            }
        }

        startedAtTimeView.setOnClickListener {
            val now = LocalDateTime.now()
            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                startedAt = startedAt.with(LocalTime.of(hourOfDay, minute))
            }, now.hour, now.minute, true).show()
        }

        endedAtTimeView.setOnClickListener {
            val now = LocalDateTime.now()
            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                endedAt = endedAt.with(LocalTime.of(hourOfDay, minute))
            }, now.hour, now.minute, true).show()
        }

        startedAtView.setOnClickListener {
            val now = LocalDateTime.now()
            DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                startedAt = startedAt.with(LocalDate.of(year, month, dayOfMonth))
            }, now.year, now.monthValue, now.dayOfMonth).show()
        }
        endedAtView.setOnClickListener {
            val now = LocalDateTime.now()
            DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                endedAt = endedAt.with(LocalDate.of(year, month, dayOfMonth))
            }, now.year, now.monthValue, now.dayOfMonth).show()
        }

        fab.setOnClickListener {
            val replyIntent = Intent()
            if (TextUtils.isEmpty(nameText.text)) {
                setResult(Activity.RESULT_CANCELED, replyIntent)
            } else {
                calendaricViewModel.insertEvent(Event(
                    0,
                    "0",
                    nameText.text.toString(),
                    detailsText.text.toString(),
                    locationText.text.toString(),
                    statusText.text.toString(),
                    startedAt,
                    endedAt,
                    null,
                    null
                ))

                setResult(Activity.RESULT_OK, replyIntent)
            }
            finish()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

//    private fun onDatePick(textView: TextView, field: LocalDateTime?): LocalDateTime? {
//        val now = LocalDateTime.now()
//        var pickedDateTime: LocalDateTime? = null
//
//        DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
//            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
//                textView.setText("$dayOfMonth $month, $year - $hourOfDay:$minute")
//                pickedDateTime = LocalDateTime.of(year, month, dayOfMonth, hourOfDay, minute)
//            }, now.hour, now.minute, true).show()
//        }, now.year, now.monthValue, now.dayOfMonth).show()
//
//        return pickedDateTime
//    }

    companion object {
        const val EXTRA_NAME = "xyz.majorov.calendaric.NAME"
        const val EXTRA_STATUS = "xyz.majorov.calendaric.STATUS"
        const val EXTRA_DETAILS = "xyz.majorov.calendaric.DETAILS"
        const val EXTRA_STARTED_AT = "xyz.majorov.calendaric.DETAILS"
        const val EXTRA_ENDED_AT = "xyz.majorov.calendaric.DETAILS"
        const val EXTRA_LOCATION = "xyz.majorov.calendaric.LOCATION"
    }
}
