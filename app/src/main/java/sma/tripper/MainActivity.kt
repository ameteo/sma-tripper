package sma.tripper

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.tab_create.*
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private var queue: RequestQueue? = null

    private var googleSignInClient: GoogleSignInClient? = null
    private var account: GoogleSignInAccount? = null

    private var ongoingView: View? = null
    private var createView: View? = null
    private var recommendedView: View? = null
    private var tripsView: View? = null

    private var autocompleteResults: HashMap<String, String> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        queue = Volley.newRequestQueue(this)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        account = GoogleSignIn.getLastSignedInAccount(this)
        updateUI()

        ongoingView = layoutInflater.inflate(R.layout.tab_ongoing, null)
        createView = layoutInflater.inflate(R.layout.tab_create, null)
        recommendedView = layoutInflater.inflate(R.layout.tab_recommended, null)
        tripsView = layoutInflater.inflate(R.layout.tab_trips, null)
        nestedScrollView.addView(ongoingView)
        btn_login.setOnClickListener {
            signIn()
        }
        btn_logout.setOnClickListener {
            signOut()
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


        val destination = createView?.findViewById<AutoCompleteTextView>(R.id.autocomplete_create_destination)


        destination?.addTextChangedListener {
            if (it!!.length >= 3) {
                val apiKey = getString(R.string.places_api_key)
                val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=${it}&types=(cities)&key=${apiKey}"

                val request = JsonObjectRequest(Request.Method.GET, url, null,
                    Response.Listener { json ->
                        if ((json.getString("status") == "OK").not()) {
                            Toast.makeText(applicationContext, json.getString("error_message"), Toast.LENGTH_SHORT).show()
                        } else {
                            autocompleteResults.clear()
//                            val results: ArrayList<String> = ArrayList()
                            val predictions = json.getJSONArray("predictions")
                            for (index in 0 until predictions.length()) {
                                val prediction = predictions.getJSONObject(index)
//                                results.add(prediction.getString("description"))
                                val description = prediction.getString("description")
                                val placeId = prediction.getString("place_id")
                                autocompleteResults[description] = placeId
                            }
                            destination.setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, autocompleteResults.keys.toList()))
                        }

                    },
                    Response.ErrorListener { print("no") }
                )
                queue?.add(request)
            }
        }
        val datePickerFrom = DatePickerDialog(this@MainActivity)
        val datePickerTo = DatePickerDialog(this@MainActivity)
        val from = createView?.findViewById<EditText>(R.id.input_create_from)
        var fromDate: LocalDate? = null
        val to = createView?.findViewById<EditText>(R.id.input_create_to)
        from?.keyListener = null
        to?.keyListener = null
        from?.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) datePickerFrom.show() }
        datePickerFrom.setOnDateSetListener { view, year, month, dayOfMonth ->
            fromDate = LocalDate.of(year, month + 1, dayOfMonth)
            input_create_from.setText(fromDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
            datePickerTo.datePicker.minDate = localDateToDate(fromDate?.plusDays(1)!!).time
        }
        to?.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) datePickerTo.show() }
        var toDate: LocalDate? = null
        datePickerTo.setOnDateSetListener { view, year, month, dayOfMonth ->
            toDate = LocalDate.of(year, month + 1, dayOfMonth)
            input_create_to.setText(toDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
            datePickerFrom.datePicker.maxDate = localDateToDate(toDate?.minusDays(1)!!).time
        }
        val createButton = createView?.findViewById<Button>(R.id.btn_create_next)
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
                lbl_select_poi.visibility = View.VISIBLE
                lbl_created.visibility = View.VISIBLE
                btn_create_done.visibility = Button.VISIBLE

                trip = Trip(fromDate!!, toDate!!, destination?.text.toString())
                lbl_created.text = "${trip!!.tripDays.size} day trip to ${destination?.text} created!"

                from?.isEnabled = false
                to?.isEnabled = false
                destination?.isEnabled = false
                trip = Trip(fromDate!!, toDate!!, destination?.text.toString())
                getDestinationLocationAndInitList()
            } else
                Toast.makeText(applicationContext, "Invalid value for ${invalidFields.joinToString()}", Toast.LENGTH_SHORT).show()
        }
    }
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: EventRecyclerViewAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    private fun localDateToDate(localDate: LocalDate) : Date {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
    }

    private fun getDestinationLocationAndInitList() {
        val destination = autocomplete_create_destination.text.toString()
        val destinationPlaceId = autocompleteResults[destination]
        val apiKey = getString(R.string.places_api_key)

        val placeDetailsUrl = "https://maps.googleapis.com/maps/api/place/details/json?place_id=${destinationPlaceId}&fields=geometry&key=${apiKey}"
        val placeLocationRequest = JsonObjectRequest(Request.Method.GET, placeDetailsUrl, null,
            Response.Listener { json ->
                if ((json.getString("status") == "OK").not()) {
                    Toast.makeText(applicationContext, json.getString("error_message"), Toast.LENGTH_SHORT).show()
                } else {
                    val location = json.getJSONObject("result").getJSONObject("geometry").getJSONObject("location")
                    val lat = location.getString("lat")
                    val lng = location.getString("lng")
                    initList(lat, lng)
                }
            },
            Response.ErrorListener { print("no") }
        )
        queue?.add(placeLocationRequest)
    }

    private fun initList(lat: String, lng: String) {
        val apiKey = getString(R.string.places_api_key)
        val nearbySearchUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${lat},${lng}&radius=10000&type=tourist_attraction&key=${apiKey}"
        val nearbySearchRequest = JsonObjectRequest(Request.Method.GET, nearbySearchUrl, null,
            Response.Listener { json ->
                if ((json.getString("status") == "OK").not()) {
                    val message = if (json.has("error_message")) json.getString("error_message") else "Please try a different location"
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                } else {
                    val events: ArrayList<Event> = ArrayList()
                    val results = json.getJSONArray("results")
                    for (index in 0 until results.length()) {
                        val result = results.getJSONObject(index)

                        val name = result.getString("name")
                        val address = result.getString("vicinity")
                        var photoUrl: String? = null
                        if (result.has("photos")) {
                            val photos = result.getJSONArray("photos")
                            if (photos.length() > 0) {
                                val photo = photos.getJSONObject(0)
                                val photoReference = photo.getString("photo_reference")
                                photoUrl = "https://maps.googleapis.com/maps/api/place/photo?photoreference=${photoReference}&maxwidth=400&key=${apiKey}"
                            }
                        }
                        events.add(Event(name, address, photoUrl))
                    }
                    viewAdapter = EventRecyclerViewAdapter(events.toMutableList()) { event ->

                        val dialogView = View.inflate(this@MainActivity, R.layout.date_time_picker, null)
                        val alertDialog = AlertDialog.Builder(this@MainActivity).create()

                        val spinner = dialogView.findViewById<Spinner>(R.id.spinner_date_time_picker)

                        ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_item, trip!!.tripDays.keys.sorted().toList()).also {
                            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinner.adapter = it
                            spinner.setSelection(0)
                        }

                        val timePicker = dialogView.findViewById<TimePicker>(R.id.time_picker_date_time_picker)
                        dialogView.findViewById<Button>(R.id.btn_date_time_picker_add).setOnClickListener {
                            event.date = trip!!.tripDays[spinner.selectedItem]
                            event.hour = timePicker.hour
                            event.minute = timePicker.minute
                            trip?.addEvent(event)
                            alertDialog.dismiss()
                        }
                        dialogView.findViewById<Button>(R.id.btn_date_time_picker_cancel).setOnClickListener {
                            alertDialog.dismiss()
                        }
                        alertDialog.setView(dialogView)
                        alertDialog.show()

                    }
                    viewManager = LinearLayoutManager(this)
                    recyclerView = rv_events.apply {
                        adapter = viewAdapter
                        layoutManager = viewManager
                    }
                }

            },
            Response.ErrorListener { print("no") }
        )
        queue?.add(nearbySearchRequest)
    }

    var trip: Trip? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SIGN_IN) {
            handleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(data))
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            account = completedTask.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            e.printStackTrace()
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }

    private fun signIn() {
        startActivityForResult(googleSignInClient?.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun signOut() {
        googleSignInClient?.signOut()?.addOnCompleteListener(this) {
            account = null
            updateUI()
        }
    }

    private fun updateUI() {
        if (account == null) {
            btn_login.visibility = Button.VISIBLE
            btn_logout.visibility = Button.INVISIBLE
            lbl_user.text = "Guest"
        } else {
            btn_login.visibility = Button.INVISIBLE
            btn_logout.visibility = Button.VISIBLE
            lbl_user.text = account?.displayName

            val accountPhotoUrl = account?.photoUrl?.path
            if (accountPhotoUrl != null)
                RetrieveBitmapTask {
                    avatar.setImageBitmap(it)
                }.execute(accountPhotoUrl)
            else
                avatar.setImageResource(R.drawable.avatar)
        }
    }

    companion object {
        const val REQUEST_CODE_SIGN_IN = 0xff
    }
}
