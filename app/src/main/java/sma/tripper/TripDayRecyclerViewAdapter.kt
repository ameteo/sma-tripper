package sma.tripper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.event.view.*
import kotlinx.android.synthetic.main.ongoing_event.view.*
import kotlinx.android.synthetic.main.trip.view.*
import sma.tripper.data.Event
import sma.tripper.data.Trip
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TripDayRecyclerViewAdapter(
    val dailyEvents: MutableList<ArrayList<Event>>
) : RecyclerView.Adapter<TripDayRecyclerViewAdapter.TripDayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripDayViewHolder{
        return TripDayViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.trip_day, parent, false))
    }

    override fun getItemCount(): Int {
        return dailyEvents.size
    }


    override fun onBindViewHolder(holder: TripDayViewHolder, position: Int) {
        holder.bind(dailyEvents[position])
    }

    inner class TripDayViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(events: ArrayList<Event>) {
            view.findViewById<TextView>(R.id.day_text_view).text = events[0].date.toString()

            view.findViewById<RecyclerView>(R.id.event_list_view).apply {
                adapter = EventInfoRecyclerViewAdapter(events)
                layoutManager = LinearLayoutManager(view.context)
            }
        }
    }

    inner class EventInfoRecyclerViewAdapter(
        val events: MutableList<Event>
    ): RecyclerView.Adapter<EventInfoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventInfoViewHolder {
            return EventInfoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.ongoing_event, parent, false))
        }

        override fun getItemCount(): Int {
            return events.size
        }

        override fun onBindViewHolder(holder: EventInfoViewHolder, position: Int) {
            holder.bind(events[position])
        }
    }

    inner class EventInfoViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(event: Event) {
            if (event.thumbnailUrl != null)
                RetrieveBitmapTask {
                    view.ongoing_event_thumbnail.setImageBitmap(it)
                }.execute(event.thumbnailUrl)
            view.ongoing_event_description.text = event.name
            if (event.minute!! < 10)
                view.ongoing_event_time.text = "${event.hour}:0${event.minute}"
            else
                view.ongoing_event_time.text = "${event.hour}:${event.minute}"
        }
    }
}