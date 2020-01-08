package sma.tripper

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
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private lateinit var apiKey: String
    private val urlAutocompleteFormat = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=%s&types=(cities)&key=%s"
    private val urlPlacesDetails = "https://maps.googleapis.com/maps/api/place/details/json?place_id=%s&fields=geometry&key=%s"
    private val urlPlacesNearby = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%s,%s&radius=10000&type=tourist_attraction&key=%s"
    private val urlPlacesPhoto = "https://maps.googleapis.com/maps/api/place/photo?photoreference=%s&maxwidth=400&key=%s"

    private var queue: RequestQueue? = null

    private var googleSignInClient: GoogleSignInClient? = null
    private var account: GoogleSignInAccount? = null

    private var ongoingView: View? = null
    private var createView: View? = null
    private var recommendedView: View? = null
    private var tripsView: View? = null
    private var trip: Trip? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: EventRecyclerViewAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var autocompleteResults: HashMap<String, String> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiKey = getString(R.string.places_api_key)
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
        btn_login.setOnClickListener { login() }
        btn_logout.setOnClickListener { logout() }
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
                val request = JsonObjectRequest(
                    Request.Method.GET,
                    urlAutocompleteFormat.format(it, apiKey),
                    null,
                    Response.Listener { json -> handleSuccessfulPlacesAutocompleteResponse(json, destination) },
                    Response.ErrorListener { print("no") }
                )
                queue?.add(request)
            }
        }
        val datePickerFrom = DatePickerDialog(this@MainActivity)
        val datePickerTo = DatePickerDialog(this@MainActivity)
        setColors(createView!!)

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
                btn_create_done.setOnClickListener { handleTripDone() }
                from?.isEnabled = false
                to?.isEnabled = false
                destination?.isEnabled = false
                trip = Trip(fromDate!!, toDate!!, destination?.text.toString())
                getDestinationLocationAndInitList()
            } else
                Toast.makeText(applicationContext, "Invalid value for ${invalidFields.joinToString()}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleTripDone() {
        lbl_select_poi.visibility = View.INVISIBLE
        lbl_created.visibility = View.INVISIBLE
        btn_create_done.visibility = View.INVISIBLE
        btn_create_next.visibility = Button.VISIBLE
        input_create_from.isEnabled = true
        input_create_from.text.clear()
        input_create_to.isEnabled = true
        input_create_to.text.clear()
        autocomplete_create_destination.isEnabled = true
        autocomplete_create_destination.text.clear()
        viewAdapter.events.clear()
        viewAdapter.notifyDataSetChanged()
        saveTrip(trip!!)
        Toast.makeText(
            applicationContext,
            "Find your trip in the 'MY TRIPS' tab!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setColors(view: View) {
//        view.findViewById<TextView>(R.id.lbl_create_from)?.setTextColor(resources.getColor(R.color.blacky))
//        view.findViewById<TextView>(R.id.lbl_create_to)?.setTextColor(resources.getColor(R.color.blacky))
//        view.findViewById<TextView>(R.id.lbl_create_destination)?.setTextColor(resources.getColor(R.color.blacky))
//        view.findViewById<EditText>(R.id.input_create_from)?.setTextColor(resources.getColor(R.color.blacky))
//        view.findViewById<EditText>(R.id.input_create_to)?.setTextColor(resources.getColor(R.color.blacky))
//        view.findViewById<AutoCompleteTextView>(R.id.autocomplete_create_destination)?.setTextColor(resources.getColor(R.color.blacky))
//        view.findViewById<Button>(R.id.btn_create_next)?.setTextColor(resources.getColor(R.color.blacky))
    }

    private fun saveTrip(trip: Trip) {

    }

    private fun localDateToDate(localDate: LocalDate) : Date {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
    }

    private fun getDestinationLocationAndInitList() {
        val destination = autocomplete_create_destination.text.toString()
        val destinationPlaceId = autocompleteResults[destination]
        val placeLocationRequest = JsonObjectRequest(
            Request.Method.GET,
            urlPlacesDetails.format(destinationPlaceId, apiKey),
            null,
            Response.Listener { json -> handleSuccessfulPlacesDetailsResponse(json) },
            Response.ErrorListener { print("no") }
        )
        queue?.add(placeLocationRequest)
    }

    private fun findPlacesNearby(lat: String, lng: String) {
        val nearbySearchRequest = JsonObjectRequest(
            Request.Method.GET,
            urlPlacesNearby.format(lat, lng, apiKey),
            null,
            Response.Listener { json -> handleSuccessfulPlacesNearbyResponse(json) },
            Response.ErrorListener { print("no") }
        )
        queue?.add(nearbySearchRequest)
    }

    private fun handleSuccessfulPlacesDetailsResponse(json: JSONObject) {
        if ((json.getString("status") == "OK").not()) {
            Toast.makeText(applicationContext, json.getString("error_message"), Toast.LENGTH_SHORT).show()
        } else {
            val location = json.getJSONObject("result").getJSONObject("geometry").getJSONObject("location")
            val lat = location.getString("lat")
            val lng = location.getString("lng")
            findPlacesNearby(lat, lng)
        }
    }

    private fun handleSuccessfulPlacesAutocompleteResponse(json: JSONObject, destination: AutoCompleteTextView) {
        if ((json.getString("status") == "OK").not()) {
            Toast.makeText(applicationContext, json.getString("error_message"), Toast.LENGTH_SHORT)
                .show()
        } else {
            autocompleteResults.clear()
            val predictions = json.getJSONArray("predictions")
            for (index in 0 until predictions.length()) {
                val prediction = predictions.getJSONObject(index)
                val description = prediction.getString("description")
                val placeId = prediction.getString("place_id")
                autocompleteResults[description] = placeId
            }
            destination.setAdapter(
                ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    autocompleteResults.keys.toList()
                )
            )
        }
    }

    private fun handleSuccessfulPlacesNearbyResponse(json: JSONObject) {
        if ((json.getString("status") == "OK").not()) {
            val message =
                if (json.has("error_message")) json.getString("error_message") else "Please try a different location"
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
                        photoUrl = urlPlacesPhoto.format(photoReference, apiKey)
                    }
                }
                events.add(Event(name, address, photoUrl))
            }
            viewAdapter = EventRecyclerViewAdapter(events.toMutableList()) { event -> onEventAddButtonClicked(event) }
            viewManager = LinearLayoutManager(this@MainActivity)
            recyclerView = rv_events.apply {
                adapter = viewAdapter
                layoutManager = viewManager
            }
        }
    }

    private fun onEventAddButtonClicked(event: Event) {
        EventAddPopup(this@MainActivity, trip!!, event).show()
    }

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

    private fun login() {
        startActivityForResult(googleSignInClient?.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun logout() {
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
