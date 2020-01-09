package sma.tripper

import android.app.DatePickerDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import kotlinx.android.synthetic.main.tab_ongoing.*

import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
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
import sma.tripper.data.Event
import sma.tripper.data.Trip
import sma.tripper.room.AppDatabaseProvider
import sma.tripper.room.RoomTripRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.tab_recommended.*
import kotlinx.android.synthetic.main.tab_trips.*
import kotlinx.android.synthetic.main.trip.view.*
import kotlinx.android.synthetic.main.trip_details.*
import kotlinx.android.synthetic.main.trip_details.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import sma.tripper.firebase.FirebaseTripRepository
import java.time.temporal.ChronoUnit
import kotlin.random.Random

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
    private var tripView: View? = null
    private var tripRepository: TripRepository? = null
    private val tripsLiveData = MutableLiveData<List<Trip>>()

    private lateinit var createRecyclerView: RecyclerView
    private lateinit var createViewAdapter: EventRecyclerViewAdapter
    private lateinit var createViewManager: RecyclerView.LayoutManager

    private lateinit var recommendedCreateRecyclerView: RecyclerView
    private lateinit var recommendedCreateViewAdapter: EventRecyclerViewAdapter
    private lateinit var recommendedCreateViewManager: RecyclerView.LayoutManager

    private lateinit var tripsRecyclerView: RecyclerView
    private lateinit var tripsViewAdapter: TripRecyclerViewAdapter
    private lateinit var tripsViewManager: RecyclerView.LayoutManager

    private lateinit var tripDaysRecyclerView: RecyclerView
    private lateinit var tripDaysViewAdapter: EventRecyclerViewAdapter
    private lateinit var tripDaysViewManager: RecyclerView.LayoutManager

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
        tripView = layoutInflater.inflate(R.layout.trip_details, null)

        nestedScrollView.addView(ongoingView)
        nestedView.addView(tripView)
        btn_login.setOnClickListener { login() }
        btn_logout.setOnClickListener { logout() }


        OngoingUpdater().execute(tripRepository, this::updateOngoing)

        tripsLiveData.observe(this, Observer { trips ->
            tripsViewAdapter = TripRecyclerViewAdapter(
                trips.toMutableList(),
                { trip ->
                    GlobalScope.launch {
                        tripRepository?.removeTrip(trip)
                        tripsLiveData.postValue(tripRepository?.getAllTrips())
                    }
                }
            )
            tripsViewManager = LinearLayoutManager(this@MainActivity)
            tripsView?.findViewById<RecyclerView>(R.id.rv_trips)?.apply {
                adapter = tripsViewAdapter
                layoutManager = tripsViewManager
            }
        })

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val newView = when(tab?.text) {
                    getString(R.string.tab_title_ongoing) -> {
                        OngoingUpdater().execute(tripRepository, this@MainActivity::updateOngoing)
                        ongoingView
                    }
                    getString(R.string.tab_title_create) -> createView
                    getString(R.string.tab_title_recommended) -> {
                        GlobalScope.launch { findRecommended(tripRepository!!.getAllTrips()) }
                        recommendedView
                    }
                    getString(R.string.tab_title_trips) -> {
                        GlobalScope.launch { tripsLiveData.postValue(tripRepository?.getAllTrips()) }
                        tripsView
                    }
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
                lbl_select_poi.visibility = VISIBLE
                lbl_created.visibility = VISIBLE
                btn_create_done.visibility = Button.VISIBLE

                val trip = Trip(
                    Random.nextLong(),
                    fromDate!!,
                    toDate!!,
                    destination?.text.toString()
                )
                lbl_created.text = "${trip!!.tripDays.size} day trip to ${destination?.text} created!"
                btn_create_done.setOnClickListener { handleTripDone(trip) }
                from?.isEnabled = false
                to?.isEnabled = false
                destination?.isEnabled = false

                getDestinationLocationAndInitList(trip)
            } else
                Toast.makeText(applicationContext, "Invalid value for ${invalidFields.joinToString()}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findRecommended(trips: List<Trip>) {
        trips.filter{t -> tripIsOngoingOrUpcoming(t) }.forEach{t -> findPlacesNearby(t.lat!!, t.lng!!, t, true)}
    }

    private fun tripIsOngoingOrUpcoming(t: Trip) : Boolean {
        val today = LocalDate.now()
        return today.isBefore(t.from) || (t.from.minusDays(1).isBefore(today) && t.to.plusDays(1).isAfter(today))
    }

    private fun handleTripDone(trip: Trip) {
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
        createViewAdapter.events.clear()
        createViewAdapter.notifyDataSetChanged()
        saveTrip(trip!!)
        Toast.makeText(
            applicationContext,
            "Find your trip in the MY TRIPS tab!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveTrip(trip: Trip) {
        GlobalScope.launch {
            tripRepository?.addTrip(trip)
        }
    }

    private fun localDateToDate(localDate: LocalDate) : Date {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
    }

    private fun getDestinationLocationAndInitList(trip: Trip) {
        val destination = autocomplete_create_destination.text.toString()
        val destinationPlaceId = autocompleteResults[destination]
        val placeLocationRequest = JsonObjectRequest(
            Request.Method.GET,
            urlPlacesDetails.format(destinationPlaceId, apiKey),
            null,
            Response.Listener { json -> handleSuccessfulPlacesDetailsResponse(json, trip) },
            Response.ErrorListener { print("no") }
        )
        queue?.add(placeLocationRequest)
    }

    private fun findPlacesNearby(lat: String, lng: String, trip: Trip, isRecommended: Boolean) {
        val nearbySearchRequest = JsonObjectRequest(
            Request.Method.GET,
            urlPlacesNearby.format(lat, lng, apiKey),
            null,
            Response.Listener { json -> handleSuccessfulPlacesNearbyResponse(json, trip, isRecommended) },
            Response.ErrorListener { print("no") }
        )
        queue?.add(nearbySearchRequest)
    }

    private fun handleSuccessfulPlacesDetailsResponse(json: JSONObject, trip: Trip) {
        if ((json.getString("status") == "OK").not()) {
            Toast.makeText(applicationContext, json.getString("error_message"), Toast.LENGTH_SHORT).show()
        } else {
            val location = json.getJSONObject("result").getJSONObject("geometry").getJSONObject("location")
            val lat = location.getString("lat")
            val lng = location.getString("lng")
            trip?.lat = lat
            trip?.lng = lng
            findPlacesNearby(lat, lng, trip, false)
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

    private fun handleSuccessfulPlacesNearbyResponse(json: JSONObject, trip: Trip, isRecommended: Boolean) {
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
                events.add(Event(Random.nextLong(), name, address, photoUrl))
            }
            addEventsToRecycleView(events, trip, isRecommended)
        }
    }

    private fun addEventsToRecycleView(events: ArrayList<Event>, trip: Trip, isRecommended: Boolean) {
        if(isRecommended.not()) {
            createViewAdapter = EventRecyclerViewAdapter(events.toMutableList()) { event ->
                onEventAddButtonClicked(
                    event,
                    trip
                )
            }
            createViewManager = LinearLayoutManager(this@MainActivity)
            createRecyclerView =  createView?.findViewById<RecyclerView>(R.id.rv_events)?.apply {
                adapter = createViewAdapter
                layoutManager = createViewManager
            }!!
        } else {
            recommendedCreateViewAdapter = EventRecyclerViewAdapter(events.toMutableList()) { event ->
                onEventAddButtonClicked(
                    event,
                    trip
                )
            }
            recommendedCreateViewManager = LinearLayoutManager(this@MainActivity)
            recommendedCreateRecyclerView = recommendedView?.findViewById<RecyclerView>(R.id.rv_recommended_events)?.apply {
                adapter = recommendedCreateViewAdapter
                layoutManager = recommendedCreateViewManager
            }!!
        }
    }

    private fun onEventAddButtonClicked(event: Event, trip: Trip) {
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
        GlobalScope.launch { tripsLiveData.postValue(tripRepository?.getAllTrips()) }
    }

    private fun login() {
        startActivityForResult(googleSignInClient?.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun logout() {
        googleSignInClient?.signOut()?.addOnCompleteListener(this) {
            account = null
            avatar.setImageResource(R.drawable.avatar)
            updateUI()
            GlobalScope.launch { tripsLiveData.postValue(tripRepository?.getAllTrips()) }
        }

    }

    private fun updateUI() {
        if (account == null) {
            btn_login.visibility = Button.VISIBLE
            btn_logout.visibility = Button.INVISIBLE
            lbl_user.text = "Guest"
            tripRepository = RoomTripRepository(AppDatabaseProvider.getDb(this@MainActivity))
        } else {
            btn_login.visibility = Button.INVISIBLE
            btn_logout.visibility = Button.VISIBLE
            lbl_user.text = account?.displayName

            var accountPhotoUrl = account?.photoUrl?.path
            if (accountPhotoUrl != null) {
                if (accountPhotoUrl.startsWith("http").not())
                    accountPhotoUrl = "https://lh3.googleusercontent.com/${accountPhotoUrl}"
                RetrieveBitmapTask {
                    avatar.setImageBitmap(it)
                }.execute(accountPhotoUrl)
            }
            else
                avatar.setImageResource(R.drawable.avatar)

            tripRepository = FirebaseTripRepository(account?.id!!)
        }
    }

    companion object {
        const val REQUEST_CODE_SIGN_IN = 0xff
    }

    fun updateOngoing(trips: List<Trip>) {
        val today = LocalDate.now()
        val ongoingTrip = trips.filter {
            it.from.minusDays(1).isBefore(today) && it.to.plusDays(1).isAfter(today)
        }.firstOrNull()
        if (ongoingTrip == null) {
            ongoingView?.findViewById<TextView>(R.id.tv_no_ongoing_trip)?.visibility = VISIBLE
            ongoingView?.findViewById<View>(R.id.nestedView)?.visibility = INVISIBLE
        } else {
            ongoingView?.findViewById<TextView>(R.id.tv_no_ongoing_trip)?.visibility = INVISIBLE
            ongoingView?.findViewById<View>(R.id.nestedView)?.visibility = VISIBLE
            ongoingView?.trip_destination2?.text = "Trip to ${ongoingTrip.destination}"
            ongoingView?.trip_period2?.text = "${formatDate(ongoingTrip.from)} - ${formatDate(ongoingTrip.to)}"
            val today = LocalDate.now()
            if (ongoingTrip.from.isAfter(today))
                ongoingView?.trip_until2?.text = "Coming up in ${ChronoUnit.DAYS.between(today, ongoingTrip.from)} days..."
            else if (today.isAfter(ongoingTrip.to))
                ongoingView?.trip_until2?.text = "Ended ${ChronoUnit.DAYS.between(ongoingTrip.to, today)} days ago..."
            else
                ongoingView?.trip_until2?.text = "Ongoing..."

            val dailyEvents: HashMap<LocalDate, ArrayList<Event>> = HashMap()
            ongoingTrip.events.forEach {
                dailyEvents.putIfAbsent(it.date!!, ArrayList())
                dailyEvents[it.date!!]?.add(it)
            }

            tripDaysViewManager = LinearLayoutManager(this@MainActivity)
            nestedView.rv_trip_days.apply {
                adapter = TripDayRecyclerViewAdapter(dailyEvents.toSortedMap().values.toMutableList())
                layoutManager = tripDaysViewManager
            }
        }
    }

    private fun formatDate(date: LocalDate) : String {
        return date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    }

    class OngoingUpdater: AsyncTask<Any, Void?, Void>() {
        var onPost: ((List<Trip>) -> Unit)? = null
        var trips: List<Trip>? = null

        override fun doInBackground(vararg params: Any?): Void? {
            onPost = params[1] as (List<Trip>) -> Unit
            runBlocking {
                trips = (params[0] as TripRepository).getAllTrips()
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            onPost?.invoke(trips!!)
        }

    }
}
