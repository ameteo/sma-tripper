package sma.tripper

import android.annotation.TargetApi
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
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    var googleSignInClient: GoogleSignInClient? = null
    var account: GoogleSignInAccount? = null

    var ongoingView: View? = null
    var createView: View? = null
    var recommendedView: View? = null
    var tripsView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

        val countries = arrayOf(
            "Belgium", "France", "Italy", "Germany", "Spain"
        )
        val adapter: ArrayAdapter<String> = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, countries)
        val destination = createView?.findViewById<AutoCompleteTextView>(R.id.autocomplete_create_destination)
        destination?.setAdapter(adapter)
        val queue = Volley.newRequestQueue(this)
        destination?.addTextChangedListener {
            val apiKey = getString(R.string.places_api_key)
            val url = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=${it}&types=(cities)&key=${apiKey}"

            val request = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { json ->
                    if ((json.getString("status") == "OK").not()) {
                        print(json.getString("error_message"))
                    } else {
                        val results: ArrayList<String> = ArrayList()
                        val predictions = json.getJSONArray("predictions")
                        for (index in 0 until predictions.length()) {
                            val prediction = predictions.getJSONObject(index)
                            results.add(prediction.getString("description"))
                        }
                        destination.setAdapter(ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, results))
                    }

                },
                Response.ErrorListener {  print("no") }
            )
            queue.add(request)
        }
        val datePickerFrom = DatePickerDialog(this@MainActivity)
        val from = createView?.findViewById<EditText>(R.id.input_create_from)
        var fromDate: LocalDate? = null
        val to = createView?.findViewById<EditText>(R.id.input_create_to)

        from?.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) datePickerFrom.show() }
        datePickerFrom.setOnDateSetListener { view, year, month, dayOfMonth ->
            fromDate = LocalDate.of(year, month + 1, dayOfMonth)
            input_create_from.setText(fromDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
        }
        val datePickerTo = DatePickerDialog(this@MainActivity)
        to?.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) datePickerTo.show() }
        var toDate: LocalDate? = null
        datePickerTo.setOnDateSetListener { view, year, month, dayOfMonth ->
            toDate = LocalDate.of(year, month + 1, dayOfMonth)
            input_create_to.setText(toDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
        }
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
                lbl_select_poi.visibility = View.VISIBLE
                lbl_created.visibility = View.VISIBLE

                if (fromDate != null && toDate != null)
                    lbl_created.text = "${Period.between(fromDate, toDate).days} day trip to ${destination?.text} created!"
                else
                    lbl_created.text = "Trip to ${destination?.text} created!"

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
        val events = (1..10).map { "Point Of Interest #${it}" }.map { Event(it, getString(R.string.temp_thumbnail_url)) }.toList()
        viewAdapter = EventRecyclerViewAdapter(events.toMutableList())
        viewManager = LinearLayoutManager(this)
        recyclerView = rv_events.apply {
            adapter = viewAdapter
            layoutManager = viewManager
        }
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
