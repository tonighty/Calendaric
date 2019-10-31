package xyz.majorov.calendarici

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.event_recycler_view_item.view.*
import org.threeten.bp.LocalDate

class EventListAdapter internal constructor(
    private val context: Context, private val listener: OnItemClickListener
) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>() {

    private var selectedDate: LocalDate = LocalDate.now()
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var events = emptyList<EventInstance>()

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view = itemView
        val eventItemView: TextView = itemView.eventView
        val eventStartedAtView: TextView = itemView.startTimeView
        val eventEndedAtView: TextView = itemView.endTimeView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val itemView = inflater.inflate(R.layout.event_recycler_view_item, parent, false)
        return EventViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val current = events[position]

        holder.view.setOnClickListener {
            listener.onItemClick(current.primaryKey)
        }

        holder.eventItemView.text = current.name

        current.startedAt?.let { start ->
            current.endedAt?.let { end ->
                val startDate = start.toLocalDate()
                val endDate = end.toLocalDate()
                if (startDate < selectedDate && endDate > selectedDate ||
                    startDate == selectedDate && endDate == selectedDate &&
                    start.hour == 0 && start.minute == 0 &&
                    end.hour == 23 && end.minute == 59
                ) {
                    holder.eventStartedAtView.text = context.getString(R.string.all_day)
                    holder.eventEndedAtView.text = ""
                } else if (startDate < selectedDate && endDate == selectedDate) {
                    holder.eventStartedAtView.text = context.getString(R.string.before)
                    holder.eventEndedAtView.text = formatTime(end)
                } else if (startDate == selectedDate && endDate > selectedDate) {
                    holder.eventStartedAtView.text = formatTime(start)
                    holder.eventEndedAtView.text = context.getString(R.string.after)
                } else {
                    holder.eventStartedAtView.text = formatTime(start)
                    holder.eventEndedAtView.text = formatTime(end)
                }
            }
        }
    }

    internal fun setEvents(events: List<EventInstance>, selectedDate: LocalDate) {
        this.events = events
        this.selectedDate = selectedDate
        notifyDataSetChanged()
    }

    override fun getItemCount() = events.size
}