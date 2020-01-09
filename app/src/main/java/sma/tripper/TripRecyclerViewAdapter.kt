package sma.tripper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.event.view.*
import kotlinx.android.synthetic.main.trip.view.*
import sma.tripper.data.Event
import sma.tripper.data.Trip
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TripRecyclerViewAdapter(
    val trips: MutableList<Trip>,
    private val onRemove: (Trip) -> (Unit)
) : RecyclerView.Adapter<TripRecyclerViewAdapter.TripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder{
        return TripViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.trip, parent, false))
    }

    override fun getItemCount(): Int {
        return trips.size
    }


    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    inner class TripViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(trip: Trip) {
            view.trip_destination.text = "Trip to ${trip.destination}"
            view.trip_period.text = "${formatDate(trip.from)} - ${formatDate(trip.to)}"
            val today = LocalDate.now()
            if (trip.from.isAfter(today))
                view.trip_until.text = "Coming up in ${ChronoUnit.DAYS.between(today, trip.from)} days..."
            else if (today.isAfter(trip.to))
                view.trip_until.text = "Ended ${ChronoUnit.DAYS.between(trip.to, today)} days ago..."
            else
                view.trip_until.text = "Ongoing..."
            view.trip_remove.setOnClickListener { onRemove(trips[adapterPosition]) }
        }

        private fun formatDate(date: LocalDate) : String {
            return date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
        }
    }
}