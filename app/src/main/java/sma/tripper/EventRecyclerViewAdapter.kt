package sma.tripper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.event.view.*
import java.net.URL

class EventRecyclerViewAdapter(
    private val events: MutableList<Event>
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
            view.event_title.text = event.name
            RetrieveImageTask().execute("https://blindfacts.com/wp-content/uploads/2018/01/The-Color-Green.png")
        }

        fun setImage(bitmap: Bitmap) {
            view.event_thumbnail.setImageBitmap(bitmap)
        }

        inner class RetrieveImageTask : AsyncTask<String, Void, Bitmap>() {
            override fun doInBackground(vararg sources: String?): Bitmap {
                val source = URL(sources[0])
                return BitmapFactory.decodeStream(source.openConnection().getInputStream())
            }

            override fun onPostExecute(bitmap: Bitmap?) {
                setImage(bitmap!!)
            }
        }
    }
}