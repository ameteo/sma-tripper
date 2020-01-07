package sma.tripper

import android.R.attr.end
import android.R.attr.start
import android.app.DatePickerDialog
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeUnit
import android.os.Bundle
import android.provider.Settings.System.DATE_FORMAT
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.tab_create.*
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    var ongoingView: View? = null
    var createView: View? = null
    var recommendedView: View? = null
    var tripsView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ongoingView = layoutInflater.inflate(R.layout.tab_ongoing, null)
        createView = layoutInflater.inflate(R.layout.tab_create, null)
        recommendedView = layoutInflater.inflate(R.layout.tab_recommended, null)
        tripsView = layoutInflater.inflate(R.layout.tab_trips, null)
        nestedScrollView.addView(ongoingView)
        btn_login.setOnClickListener {
            btn_login.visibility = Button.INVISIBLE
            btn_logout.visibility = Button.VISIBLE
            lbl_user.text = "Guest"
        }
        btn_logout.setOnClickListener {
            btn_login.visibility = Button.VISIBLE
            btn_logout.visibility = Button.INVISIBLE
            lbl_user.text = ""
        }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val newView = when(tab?.text) {
                    getString(R.string.tab_title_ongoing) -> ongoingView
                    getString(R.string.tab_title_create) -> createView
                    getString(R.string.tab_title_recommended) -> recommendedView
                    getString(R.string.tab_title_trips) -> tripsView
                    else -> return
                }
                nestedScrollView.removeAllViews()
                nestedScrollView.addView(newView)
            }
            override fun onTabReselected(tab: TabLayout.Tab?) { }
            override fun onTabUnselected(tab: TabLayout.Tab?) { }
        })

        val countries = arrayOf(
            "Belgium", "France", "Italy", "Germany", "Spain"
        )
        val adapter: ArrayAdapter<String> = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, countries)
        val destination = createView?.findViewById<AutoCompleteTextView>(R.id.autocomplete_create_destination)
        destination?.setAdapter(adapter)
        val datePickerFrom = DatePickerDialog(this@MainActivity)
        val from = createView?.findViewById<EditText>(R.id.input_create_from)
        val to = createView?.findViewById<EditText>(R.id.input_create_to)

        from?.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) datePickerFrom.show() }
        datePickerFrom.setOnDateSetListener { view, year, month, dayOfMonth -> input_create_from.setText("${dayOfMonth}/${month + 1}/${year}") }
        val datePickerTo = DatePickerDialog(this@MainActivity)
        to?.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) datePickerTo.show() }
        datePickerTo.setOnDateSetListener { view, year, month, dayOfMonth -> input_create_to.setText("${dayOfMonth}/${month + 1}/${year}") }
        val createButton = createView?.findViewById<Button>(R.id.btn_create_next)
        val createdLabel = createView?.findViewById<TextView>(R.id.lbl_created)
        createButton?.setOnClickListener {
            val invalidFields: ArrayList<String> = ArrayList()
            if (from?.text.isNullOrEmpty()) {
                invalidFields.add("From")
            }
            if (to?.text.isNullOrEmpty()) {
                invalidFields.add("To")
            }
            if (destination?.text.isNullOrEmpty()) {
                invalidFields.add("Destination")
            }
            if (invalidFields.isEmpty()) {
                createButton.visibility = Button.INVISIBLE
                createdLabel?.text = "${100} day trip to ${destination?.text} created!"
                from?.isEnabled = false
                to?.isEnabled = false
                destination?.isEnabled = false
                initList()
            } else
                Toast.makeText(applicationContext, "Invalid value for ${invalidFields.joinToString()}", Toast.LENGTH_SHORT).show()
        }

    }
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: EventRecyclerViewAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private fun initList() {
        val events = listOf(Event("1111111111111111111"), Event("2222222222222222"), Event("3333333333333333"))
        viewAdapter = EventRecyclerViewAdapter(events.toMutableList())
        viewManager = LinearLayoutManager(this)
        recyclerView = rv_events.apply {
            adapter = viewAdapter
            layoutManager = viewManager
        }
    }

}
