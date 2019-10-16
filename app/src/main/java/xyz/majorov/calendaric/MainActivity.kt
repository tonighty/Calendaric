package xyz.majorov.calendaric

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.MenuItem
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.model.InDateStyle
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.kizitonwose.calendarview.utils.next
import com.kizitonwose.calendarview.utils.yearMonth
import kotlinx.android.synthetic.main.calendar_day_layout.view.*
import kotlinx.android.synthetic.main.content_main.*
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
    private lateinit var recyclerView : RecyclerView
    private lateinit var eventAdapter: EventListAdapter
    private lateinit var firstMonth: YearMonth
    private lateinit var lastMonth: YearMonth
    private lateinit var auth: FirebaseAuth

    private val titleDateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val titleMonthFormatter = DateTimeFormatter.ofPattern("MMMM")
    private val titleYearFormatter = DateTimeFormatter.ofPattern("yyyy")
    private val selectedDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    companion object {
        const val newTaskActivityRequestCode = 1
        private const val RC_SIGN_IN = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        auth = FirebaseAuth.getInstance()

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, NewEventActivity::class.java)
            startActivityForResult(intent, newTaskActivityRequestCode)
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        recyclerView = findViewById(R.id.eventRecyclerView)
        eventAdapter = EventListAdapter(this)
        recyclerView.adapter = eventAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        calendaricViewModel = ViewModelProviders.of(this).get(CalendaricViewModel::class.java)

        calendaricViewModel.allEvents.observe(this, Observer { tasks ->
            // Update the cached copy of the words in the adapter.
            tasks?.let { it -> eventAdapter.setEvents(it.filter { it.startedAt?.dayOfMonth == today.dayOfMonth }) }
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
                    when (day.date) {
                        today -> {
                            textView.setTextColorResource(R.color.colorPrimaryDark)
                            textView.setBackgroundResource(R.drawable.today_bg)
                            dotView.setBackgroundResource(R.drawable.white_bg)
                            dotView.visibility = VISIBLE
                        }
                        selectedDate -> {
                            textView.setTextColorResource(R.color.colorPrimaryDark)
                            textView.setBackgroundResource(R.drawable.select_bg)
                            dotView.visibility = INVISIBLE
                        }
                        else -> {
                            textView.setTextColorResource(R.color.black)
                            textView.background = null
                            if (calendaricViewModel.allEvents.value?.any { it.startedAt?.toLocalDate() == day.date } == true) {
                                dotView.visibility = VISIBLE
                            } else {
                                dotView.visibility = INVISIBLE
                            }
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
                        toolbar.title = "${titleMonthFormatter.format(firstDate)} - ${titleMonthFormatter.format(lastDate)} ${titleYearFormatter.format(firstDate)}"
                    } else {
                        toolbar.title = "${titleYearFormatter.format(firstDate)} - ${titleYearFormatter.format(lastDate)}"
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

        // Choose authentication providers
        val providers = arrayListOf(AuthUI.IdpConfig.PhoneBuilder().build())

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN)
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
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
        val monthToWeek = item.itemId == R.id.nav_week_view
        val firstDate = calendarView.findFirstVisibleDay()?.date ?: return true
        val lastDate = calendarView.findLastVisibleDay()?.date ?: return true

        val oneWeekHeight = calendarView.dayHeight
        val oneMonthHeight = oneWeekHeight * 6

        val oldHeight = if (monthToWeek) oneMonthHeight else oneWeekHeight
        val newHeight = if (monthToWeek) oneWeekHeight else oneMonthHeight

        // Animate calendar height changes.
        val animator = ValueAnimator.ofInt(oldHeight, newHeight)
        animator.addUpdateListener { animator ->
            calendarView.layoutParams = calendarView.layoutParams.apply {
                height = animator.animatedValue as Int
            }
        }

        // When changing from month to week mode, we change the calendar's
        // config at the end of the animation(doOnEnd) but when changing
        // from week to month mode, we change the calendar's config at
        // the start of the animation(doOnStart). This is so that the change
        // in height is visible. You can do this whichever way you prefer.

        animator.doOnStart {
            if (!monthToWeek) {
                calendarView.inDateStyle = InDateStyle.ALL_MONTHS
                calendarView.maxRowCount = 6
                calendarView.hasBoundaries = true
            }
        }
        animator.doOnEnd {
            if (monthToWeek) {
                calendarView.inDateStyle = InDateStyle.FIRST_MONTH
                calendarView.maxRowCount = 1
                calendarView.hasBoundaries = false
            }

            if (monthToWeek) {
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

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)

        animator.start()

//        when (item.itemId) {
//            R.id.nav_week_view -> {
//                calendarView.maxRowCount = 1
//                calendarView.hasBoundaries = false
//                calendarView.scrollToDate(firstDate)
//            }
//            R.id.nav_month_view -> {
//                calendarView.maxRowCount = 6
//                calendarView.hasBoundaries = true
//                if (firstDate.yearMonth == lastDate.yearMonth) {
//                    calendarView.scrollToMonth(firstDate.yearMonth)
//                } else {
//                    calendarView.scrollToMonth(minOf(firstDate.yearMonth.next, lastMonth))
//                }
//            }
//        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == newTaskActivityRequestCode && resultCode == Activity.RESULT_OK) {
        } else {
            Toast.makeText(
                applicationContext,
                "Not saved",
                Toast.LENGTH_LONG).show()
        }
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = auth.currentUser
                // ...
            } else {
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
        val eventsForToday = calendaricViewModel.allEvents.value?.filter { it.startedAt?.toLocalDate() == date }
        if (eventsForToday !== null) eventAdapter.setEvents(eventsForToday)
    }
}
