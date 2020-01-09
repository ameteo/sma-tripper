package sma.tripper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.event.view.*
import sma.tripper.data.Event

class EventRecyclerViewAdapter(
    val events: MutableList<Event>,
    private val onClick: (Event) -> (Unit)
) : RecyclerView.Adapter<EventRecyclerViewAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder{
        return EventViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.event, parent, false))
    }

    override fun getItemCount(): Int {
        return events.size
    }


    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    inner class EventViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(event: Event) {
            view.event_description.text = event.name
            if (event.thumbnailUrl != null)
                RetrieveBitmapTask {
                    view.event_thumbnail.setImageBitmap(it)
                }.execute(event.thumbnailUrl)
            view.event_add.setOnClickListener {
                onClick(events[adapterPosition])
//                    events.remove(event)
//                    notifyDataSetChanged()
            }
        }
    }
}