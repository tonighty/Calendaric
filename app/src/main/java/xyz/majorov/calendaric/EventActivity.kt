package xyz.majorov.calendaric

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_new_event.*
import kotlinx.android.synthetic.main.content_new_event.*
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime

class EventActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var calendaricViewModel: CalendaricViewModel
    private var event: Event? = null
    private var freq: RecurrenceRule.Freq? = null
    private val days = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")

    private var startedAt = LocalDateTime.now()
        set(value) {
            startedAtView.text = formatDate(value)
            startedAtTimeView.text = formatTime(value)
            validateDate(value, endedAt)
            field = value

        }
    private var endedAt = LocalDateTime.now().plusHours(4)
        set(value) {
            endedAtView.text = formatDate(value)
            endedAtTimeView.text = formatTime(value)
            validateDate(startedAt, value)
            field = value
        }

    private fun validateDate(start: LocalDateTime, end: LocalDateTime) {
        if (end <= start) {
            endedAtView.setTextColorResource(R.color.colorAccent)
            endedAtTimeView.setTextColorResource(R.color.colorAccent)
        } else {
            endedAtView.setTextColorResource(R.color.black)
            endedAtTimeView.setTextColorResource(R.color.black)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_event)
        setSupportActionBar(toolbar)

        ArrayAdapter.createFromResource(
            this,
            R.array.recur_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinnerRecur.adapter = adapter
        }
        spinnerRecur.onItemSelectedListener = this

        calendaricViewModel = ViewModelProvider(this).get(CalendaricViewModel::class.java)

        if (intent.action === ACTION_EDIT) {
            this.title = getString(R.string.edit_event)
            calendaricViewModel.getEventByPrimaryKey(intent.getLongExtra(MainActivity.EXTRA_PRIMARY_KEY, -1))
                .observe(this, Observer { eventFromDb ->
                    eventFromDb?.let {
                        event = it
                        nameText.setText(it.name)
                        detailsText.setText(it.details)
                        locationText.setText(it.location)
                        statusText.setText(it.status)
                        startedAt = it.startedAt
                        endedAt = it.endedAt
                        if (it.rrule !== null && it.rrule != "FREQ=DAILY;INTERVAL=1;COUNT=1")
                            when (RecurrenceRule(it.rrule).freq) {
                                RecurrenceRule.Freq.DAILY -> spinnerRecur.setSelection(1)
                                RecurrenceRule.Freq.WEEKLY -> spinnerRecur.setSelection(2)
                                RecurrenceRule.Freq.MONTHLY -> spinnerRecur.setSelection(3)
                                RecurrenceRule.Freq.YEARLY -> spinnerRecur.setSelection(4)
                                else -> spinnerRecur.setSelection(0)
                            }
                        else spinnerRecur.setSelection(0)

                        if (it.startedAt?.hour == 0 && it.startedAt?.minute == 0 &&
                            it.endedAt?.hour == 23 && it.endedAt?.minute == 59
                        ) allDaySwitch.isChecked = true
                    }
                })
        } else if (intent.action == ACTION_CREATE) {
            this.title = getString(R.string.create_event)
            val day = intent.getLongExtra(MainActivity.EXTRA_SELECTED_DATE, -1)
            if (day != -1L) {
                startedAt = fromTimestamp(day)?.plusHours(12)
                endedAt = startedAt.plusHours(2)
            }
        }

        startedAt = startedAt
        endedAt = endedAt


        allDaySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startedAtTimeView.visibility = INVISIBLE
                endedAtTimeView.visibility = INVISIBLE
                startedAt = startedAt.withHour(0).withMinute(0).withNano(0)
                endedAt = endedAt.withHour(23).withMinute(59).withNano(999999999)
            } else {
                startedAtTimeView.visibility = VISIBLE
                endedAtTimeView.visibility = VISIBLE
                val now = LocalTime.now()
                startedAt = startedAt.withHour(now.hour).withMinute(now.minute)
                endedAt = endedAt.withHour(now.hour).plusHours(3).withMinute(now.minute)
            }
        }

        startedAtTimeView.setOnClickListener {
            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                startedAt = startedAt.with(LocalTime.of(hourOfDay, minute))
            }, startedAt.hour, startedAt.minute, true).show()
        }

        endedAtTimeView.setOnClickListener {
            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                endedAt = endedAt.with(LocalTime.of(hourOfDay, minute))
            }, endedAt.hour, endedAt.minute, true).show()
        }

        startedAtView.setOnClickListener {
            DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                startedAt = startedAt.with(LocalDate.of(year, month + 1, dayOfMonth))
            }, startedAt.year, startedAt.monthValue - 1, startedAt.dayOfMonth).show()
        }
        endedAtView.setOnClickListener {
            DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                endedAt = endedAt.with(LocalDate.of(year, month + 1, dayOfMonth))
            }, endedAt.year, endedAt.monthValue - 1, endedAt.dayOfMonth).show()
        }

        fab.setOnClickListener {
            if (startedAt > endedAt || nameText.text.toString().isBlank()) {
                AlertDialog.Builder(this@EventActivity)
                    .setTitle("Error")
                    .setMessage("Please fill correct name and/or date")
                    .setPositiveButton("OK") { _, _ -> }
                    .create()
                    .show()
                return@setOnClickListener
            }

            val replyIntent = Intent()
            if (TextUtils.isEmpty(nameText.text)) {
                setResult(Activity.RESULT_CANCELED, replyIntent)
            } else {
                var rule = RecurrenceRule(freq).apply { interval = 1 }.toString()
                when (freq) {
                    RecurrenceRule.Freq.MONTHLY -> {
                        rule = "$rule;BYMONTHDAY=${startedAt.dayOfMonth}"
                    }
                    RecurrenceRule.Freq.YEARLY -> {
                        rule = "$rule;BYMONTH=${startedAt.monthValue}"
                        rule = "$rule;BYMONTHDAY=${startedAt.dayOfMonth}"
                    }
                    RecurrenceRule.Freq.WEEKLY -> {
                        rule = "$rule;BYDAY=${days[startedAt.dayOfWeek.value - 1]}"
                    }
                    else -> {}
                }
                if (intent.action === ACTION_CREATE) {
                    calendaricViewModel.insertEvent(
                        Event(
                            0,
                            false,
                            null,
                            "0",
                            nameText.text.toString(),
                            detailsText.text.toString(),
                            locationText.text.toString(),
                            statusText.text.toString(),
                            startedAt,
                            endedAt,
                            if (freq !== null) rule else "FREQ=DAILY;INTERVAL=1;COUNT=1"
                        )
                    )
                } else if (intent.action === ACTION_EDIT) {
                    event?.let {
                        it.name = nameText.text.toString()
                        it.details = detailsText.text.toString()
                        it.location = locationText.text.toString()
                        it.status = statusText.text.toString()
                        it.startedAt = startedAt
                        it.endedAt = endedAt
                        it.rrule = if (freq !== null) rule else "FREQ=DAILY;INTERVAL=1;COUNT=1"
                        calendaricViewModel.updateEvent(it)
                    }
                }

                setResult(Activity.RESULT_OK, replyIntent)
            }
            finish()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (intent.action === ACTION_EDIT) menuInflater.inflate(R.menu.event, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_delete -> {
                AlertDialog.Builder(this@EventActivity)
                    .setTitle("Deleting this event")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes") { _, _ ->
                        event?.let { calendaricViewModel.deleteEvent(it) }
                        val replyIntent = Intent()
                        setResult(Activity.RESULT_OK, replyIntent)
                        finish()
                    }
                    .setNegativeButton("No") { _, _ -> }
                    .create()
                    .show()

                true
            }
            android.R.id.home -> {
                AlertDialog.Builder(this@EventActivity)
                    .setTitle("Discard changes?")
                    .setPositiveButton("Yes") { _, _ ->
                        super.onBackPressed()
                    }
                    .setNegativeButton("No") { _, _ -> }
                    .create()
                    .show()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this@EventActivity)
            .setTitle("Discard changes?")
            .setPositiveButton("Yes") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("No") { _, _ -> }
            .create()
            .show()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
        when (pos) {
            0 -> freq = null
            1 -> freq = RecurrenceRule.Freq.DAILY
            2 -> freq = RecurrenceRule.Freq.WEEKLY
            3 -> freq = RecurrenceRule.Freq.MONTHLY
            4 -> freq = RecurrenceRule.Freq.YEARLY
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {}

    companion object {
        const val ACTION_CREATE = "create"
        const val ACTION_EDIT = "edit"
    }
}
