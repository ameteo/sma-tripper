package sma.tripper

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.*
import sma.tripper.data.Event
import sma.tripper.data.Trip

class EventAddPopup(
    private val context: Context,
    private val trip: Trip,
    private val event: Event
) {
    fun show() {
        val dialogView = View.inflate(context, R.layout.date_time_picker, null)
        val alertDialog = AlertDialog.Builder(context).create()

        val spinner = dialogView.findViewById<Spinner>(R.id.spinner_date_time_picker)

        ArrayAdapter<String>(
            context,
            android.R.layout.simple_spinner_item,
            trip.tripDays.keys.sorted().toList()
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = it
            spinner.setSelection(0)
        }

        val timePicker = dialogView.findViewById<TimePicker>(R.id.time_picker_date_time_picker)
        dialogView.findViewById<Button>(R.id.btn_date_time_picker_add).setOnClickListener {
            event.date = trip.tripDays[spinner.selectedItem]
            event.hour = timePicker.hour
            event.minute = timePicker.minute
            trip.addEvent(event)
            alertDialog.dismiss()
            Toast.makeText(context, "Event added to your trip!", Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<Button>(R.id.btn_date_time_picker_cancel)
            .setOnClickListener {
                alertDialog.dismiss()
            }
        alertDialog.setView(dialogView)
        alertDialog.show()
    }
}