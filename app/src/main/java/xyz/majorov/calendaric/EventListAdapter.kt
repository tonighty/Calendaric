package xyz.majorov.calendaric

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.content_new_event.view.*
import kotlinx.android.synthetic.main.event_recycler_view_item.view.*
import org.threeten.bp.LocalTime

class EventListAdapter internal constructor(
    context: Context
) : RecyclerView.Adapter<EventListAdapter.TaskViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var events = emptyList<Event>() // Cached copy of words

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventItemView: TextView = itemView.eventView
        val eventStartedAtView: TextView = itemView.startTimeView
        val eventEndedAtView: TextView = itemView.endTimeView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = inflater.inflate(R.layout.event_recycler_view_item, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val current = events[position]
        holder.eventItemView.text = current.name

        if (current.startedAt?.hour == 0 && current.startedAt?.minute == 0 &&
            current.endedAt?.hour == 23 && current.endedAt?.minute == 59) {
            holder.eventStartedAtView.text = "All-day";
            holder.eventEndedAtView.text = "";
        } else {
            holder.eventStartedAtView.text = formatTime(current.startedAt)
            holder.eventEndedAtView.text =formatTime(current.endedAt)
        }
    }

    internal fun setEvents(events: List<Event>) {
        this.events = events
        notifyDataSetChanged()
    }

    override fun getItemCount() = events.size
}