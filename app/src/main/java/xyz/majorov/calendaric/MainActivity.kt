package xyz.majorov.calendaric

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.model.InDateStyle
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.kizitonwose.calendarview.utils.next
import com.kizitonwose.calendarview.utils.yearMonth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.calendar_day_layout.view.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.WeekFields
import java.util.*

internal fun Context.getColorCompat(@ColorRes color: Int) = ContextCompat.getColor(this, color)

internal fun TextView.setTextColorResource(@ColorRes color: Int) = setTextColor(context.getColorCompat(color))


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


    private val today = LocalDate.now()
    private lateinit var calendaricViewModel: CalendaricViewModel
    private var selectedDate = today
    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventListAdapter
    private lateinit var firstMonth: YearMonth
    private lateinit var lastMonth: YearMonth
    private val daysWithEvents: MutableMap<LocalDate, MutableList<EventInstance>> = mutableMapOf()

    private val titleDateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val titleMonthFormatter = DateTimeFormatter.ofPattern("MMMM")
    private val titleYearFormatter = DateTimeFormatter.ofPattern("yyyy")
    private val selectedDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    companion object {
        private const val newTaskActivityRequestCode = 1
        private const val editTaskActivityRequestCode = 2
        private const val RC_SIGN_IN = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, EventActivity::class.java).apply {
                action = "CREATE"
                putExtra("selectedDate", dateToTimestamp(selectedDate.atStartOfDay()))
            }
            startActivityForResult(intent, newTaskActivityRequestCode)
        }

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        recyclerView = findViewById(R.id.eventRecyclerView)
        eventAdapter = EventListAdapter(this, object : OnItemClickListener {
            override fun onItemClick(itemId: Long?) {
                if (itemId === null) return
                val intent = Intent(this@MainActivity, EventActivity::class.java).apply {
                    action = "EDIT"
                    putExtra("event_primary_key", itemId)
                }
                startActivityForResult(intent, editTaskActivityRequestCode)
            }
        })
        recyclerView.adapter = eventAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        calendaricViewModel = ViewModelProvider(this).get(CalendaricViewModel::class.java)

        calendaricViewModel.allEvents.observe(this, Observer { events ->
            // Update the cached copy of the words in the adapter.
            events?.let { it ->
                val oldDays = daysWithEvents.keys.toSet()
                daysWithEvents.clear()
                val monthAgo = LocalDate.now().minusMonths(1)
                for (event in it) {
                    var start = event.startedAt?.toLocalDate() ?: continue
                    val end = event.endedAt?.toLocalDate() ?: continue
                    val addToList = { date: LocalDate, eventInstance: EventInstance? ->
                        if (daysWithEvents[date] === null)
                            daysWithEvents[date] = mutableListOf()
                        daysWithEvents[date]?.add(eventInstance ?: EventInstance(event))
                        calendarView.notifyDateChanged(date)
                    }
                    var valid = true
                    try {
                        RecurrenceRule(event.rrule)
                    } catch (e: Exception) {
                        valid = false
                    }
                    if (event.rrule.isNullOrBlank() || !valid) {
                        if (start == end) {
                            addToList(start, null)
                        } else while (start <= end) {
                            addToList(start, null)
                            start = start.plusDays(1)
                        }
                    } else {
                        val dif = Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays()
                        val rule = RecurrenceRule(event.rrule)
                        val rStart = localDateToDateTime(if (start < monthAgo) monthAgo else start)
                        val iter = rule.iterator(rStart)

                        var maxInstances = 50
                        while (iter.hasNext() && (!rule.isInfinite || maxInstances-- > 0)) {
                            val localStart = dateTimeToLocalDate(iter.nextDateTime())
                            val instance = EventInstance(event)
                            event.startedAt?.let { instance.startedAt = localStart.atTime(it.toLocalTime()) }
                            event.endedAt?.let { instance.endedAt = localStart.plusDays(dif).atTime(it.toLocalTime()) }
                            for (d in 0..dif) {
                                addToList(localStart.plusDays(d), instance)
                            }
                        }
                    }
                }
                val emptyDays = oldDays - daysWithEvents.keys
                for (d in emptyDays)
                    calendarView.notifyDateChanged(d)
                updateAdapterForDate(selectedDate)
            }
        })

        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay
            val textView = view.calendarDayText
            val dotView = view.calendarDayDotView

            init {
                view.setOnClickListener {
                    if (day.owner == DayOwner.THIS_MONTH) {
                        selectDate(day.date)
                    }
                }
            }
        }

        calendarView.dayBinder = object : DayBinder<DayViewContainer> {
            // Called only when a new container is needed.
            override fun create(view: View) = DayViewContainer(view)

            // Called every time we need to reuse a container.
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day

                val textView = container.textView
                textView.text = day.date.dayOfMonth.toString()

                val dotView = container.dotView

                if (day.owner == DayOwner.THIS_MONTH) {
                    if (daysWithEvents.keys.contains(day.date)) {
                        dotView.visibility = VISIBLE
                    } else {
                        dotView.visibility = INVISIBLE
                    }
                    when (day.date) {
                        today -> {
                            textView.setTextColorResource(R.color.colorPrimaryDark)
                            textView.setBackgroundResource(R.drawable.today_bg)
                            dotView.setBackgroundResource(R.drawable.white_bg)
                        }
                        selectedDate -> {
                            textView.setTextColorResource(R.color.colorPrimaryDark)
                            textView.setBackgroundResource(R.drawable.select_bg)
                        }
                        else -> {
                            textView.setTextColorResource(R.color.black)
                            textView.background = null
                        }
                    }
                } else {
                    textView.setTextColorResource(R.color.muted)
                    textView.background = null
                    dotView.visibility = INVISIBLE
                }
            }
        }

//        class MonthViewContainer(view: View) : ViewContainer(view) {
//            val textView = view.calendarHeaderText
//        }
//
//        calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
//            override fun create(view: View) = MonthViewContainer(view)
//            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
//                container.textView.text = "${month.yearMonth.month.name.toLowerCase().capitalize()} ${month.year}"
//
//                if (month.month == today.monthValue) {
//                    container.textView.setTextColorResource(R.color.colorAccent)
//                }
//            }
//        }

        calendarView.monthScrollListener = {
            if (calendarView.maxRowCount == 6) {
                toolbar.title = titleDateFormatter.format(it.yearMonth)
            } else {
                // In week mode, we show the header a bit differently.
                // We show indices with dates from different months since
                // dates overflow and cells in one index can belong to different
                // months/years.
                val firstDate = it.weekDays.first().first().date
                val lastDate = it.weekDays.last().last().date
                if (firstDate.yearMonth == lastDate.yearMonth) {
                    toolbar.title = titleDateFormatter.format(firstDate)
                } else {
                    if (firstDate.year == lastDate.year) {
                        toolbar.title =
                            "${titleMonthFormatter.format(firstDate)} - ${titleMonthFormatter.format(lastDate)} ${titleYearFormatter.format(
                                firstDate
                            )}"
                    } else {
                        toolbar.title =
                            "${titleYearFormatter.format(firstDate)} - ${titleYearFormatter.format(lastDate)}"
                    }
                }
            }
        }

        currentDayView.text = selectedDateFormatter.format(today)

        val currentMonth = YearMonth.now()
        firstMonth = currentMonth.minusMonths(10)
        lastMonth = currentMonth.plusMonths(10)
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        calendarView.setup(firstMonth, lastMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        if (FirebaseAuth.getInstance().currentUser === null) {
            setDrawerMenuAuth(false)
        } else {
            setDataForUser()
        }
    }

    private fun signIn() {
        // Choose authentication providers
        val providers = arrayListOf(AuthUI.IdpConfig.PhoneBuilder().build())

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    private fun setDataForUser() {
        setDrawerMenuAuth(true)
        calendaricViewModel.syncEvents()

        FirebaseAuth.getInstance().currentUser?.let {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName("Vyacheslav Majorov")
                .build()

            it.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("lol", "User profile updated.")
                        userNameView.text = it.displayName ?: "No name"
                        it.phoneNumber?.let { phone ->
                            userEmailView.text = PhoneNumberUtils.formatNumber(phone, Locale.getDefault().country)
                        }
//                        userImageView.setImageURI(it.photoUrl)
                    }
                }
        }
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_week_view -> toggleCalendarView(true)
            R.id.nav_month_view -> toggleCalendarView(false)
            R.id.log_out_view -> {
                FirebaseAuth.getInstance().signOut()
                setDrawerMenuAuth(false)
            }
            R.id.sign_in_view -> signIn()
        }

        drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    private fun toggleCalendarView(toMonth: Boolean) {
        val firstDate = calendarView.findFirstVisibleDay()?.date ?: return
        val lastDate = calendarView.findLastVisibleDay()?.date ?: return

        val oneWeekHeight = calendarView.dayHeight
        val oneMonthHeight = oneWeekHeight * 6

        val oldHeight = if (toMonth) oneMonthHeight else oneWeekHeight
        val newHeight = if (toMonth) oneWeekHeight else oneMonthHeight

        // Animate calendar height changes.
        val animator = ValueAnimator.ofInt(oldHeight, newHeight)
        animator.addUpdateListener {
            calendarView.layoutParams = calendarView.layoutParams.apply {
                height = it.animatedValue as Int
            }
        }

        // When changing from month to week mode, we change the calendar's
        // config at the end of the animation(doOnEnd) but when changing
        // from week to month mode, we change the calendar's config at
        // the start of the animation(doOnStart). This is so that the change
        // in height is visible. You can do this whichever way you prefer.

        animator.doOnStart {
            if (!toMonth) {
                calendarView.inDateStyle = InDateStyle.ALL_MONTHS
                calendarView.maxRowCount = 6
                calendarView.hasBoundaries = true
            }
        }
        animator.doOnEnd {
            if (toMonth) {
                calendarView.inDateStyle = InDateStyle.FIRST_MONTH
                calendarView.maxRowCount = 1
                calendarView.hasBoundaries = false
            }

            if (toMonth) {
                // We want the first visible day to remain
                // visible when we change to week mode.
                calendarView.scrollToDate(firstDate)
                calendarView.notifyMonthChanged(firstDate.yearMonth)
            } else {
                // When changing to month mode, we choose current
                // month if it is the only one in the current frame.
                // if we have multiple months in one frame, we prefer
                // the second one unless it's an outDate in the last index.
                if (firstDate.yearMonth == lastDate.yearMonth) {
                    calendarView.scrollToMonth(firstDate.yearMonth)
                } else {
                    calendarView.scrollToMonth(minOf(firstDate.yearMonth.next, lastMonth))
                }
            }
        }
        animator.duration = 250

        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        drawerLayout.closeDrawer(GravityCompat.START)

        animator.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == newTaskActivityRequestCode && resultCode != Activity.RESULT_OK) {
            Toast.makeText(
                applicationContext,
                "Not saved",
                Toast.LENGTH_LONG
            ).show()
        } else if (requestCode == RC_SIGN_IN) {
//            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                setDataForUser()
            } else {
                setDrawerMenuAuth(false)

                Toast.makeText(
                    applicationContext,
                    "Sign in failed",
                    Toast.LENGTH_LONG
                ).show()

                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

    private fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            oldDate?.let { calendarView.notifyDateChanged(it) }
            calendarView.notifyDateChanged(date)
            updateAdapterForDate(date)
            currentDayView.text = selectedDateFormatter.format(date)
        }
    }

    private fun updateAdapterForDate(date: LocalDate) {
        eventAdapter.setEvents(daysWithEvents[date]?.sortedBy { it.startedAt } ?: emptyList<EventInstance>(), selectedDate)
    }

    private fun setDrawerMenuAuth(signed: Boolean) {
        navView.menu.apply {
            findItem(R.id.sign_in_view).isVisible = !signed
            findItem(R.id.log_out_view).isVisible = signed
        }

    }
}
