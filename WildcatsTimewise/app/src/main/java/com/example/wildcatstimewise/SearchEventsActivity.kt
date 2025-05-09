package com.example.wildcatstimewise

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import com.example.wildcatstimewise.UserEvent

class SearchEventsActivity : AppCompatActivity(), EventAdapter.OnEventClickListener {

    private lateinit var toolbar: Toolbar
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var eventAdapter: EventAdapter

    private var holidaysMap: MutableMap<LocalDate, MutableList<EventInfo>> = mutableMapOf()
    private val userEventsList = mutableListOf<UserEvent>()
    private var combinedEventsMap: Map<LocalDate, List<EventInfo>> = emptyMap()
    private val loadedHolidayYears = mutableSetOf<Int>()

    private var searchJob: Job? = null
    private val searchDelayMs = 300L

    companion object {
        private const val TAG = "SearchEventsActivity"
        const val EXTRA_SELECTED_DATE = "com.example.wildcatstimewise.SELECTED_DATE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use the layout provided by the user
        setContentView(R.layout.activity_search_events)

        toolbar = findViewById(R.id.toolbar_search_events)
        searchEditText = findViewById(R.id.searchEventsEditText) // ID from user's XML
        recyclerView = findViewById(R.id.searchEventsRecyclerView) // ID from user's XML
        emptyTextView = findViewById(R.id.emptySearchResultsTextView) // ID from user's XML

        setupToolbar()
        setupRecyclerView()
        loadAllEventData()
        setupSearchListener()

        updateSearchResults(emptyList())
        emptyTextView.text = "Enter text to search events"
        emptyTextView.visibility = View.VISIBLE
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = eventAdapter
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener { editable ->
            searchJob?.cancel()
            val query = editable.toString().trim()
            // Perform search immediately if query is empty or after debounce if not
            if (query.isEmpty()) {
                performSearch(query)
            } else if (query.length >= 1) { // Trigger search even on 1 character for faster feedback
                searchJob = lifecycleScope.launch {
                    delay(searchDelayMs)
                    performSearch(query)
                }
            }
        }
    }

    private fun loadAllEventData() {
        Log.d(TAG, "Loading event data for search...")
        synchronized(userEventsList) {
            userEventsList.clear()
            // Assuming UserStore object or similar accessible data source
            // Replace this placeholder logic with your actual UserEvent loading mechanism
            // If UserStore is a global object:
            /*
            UserStore.users.keys.forEach { email ->
                // Logic to create UserEvent objects from UserStore data
            }
            */
            // Using placeholder from DashboardPage for now:
            addInitialSchoolEvents()
            Log.d(TAG, "Loaded ${userEventsList.size} user events (placeholder).")
        }

        val currentYear = YearMonth.now().year
        ensureHolidaysLoaded(currentYear - 1)
        ensureHolidaysLoaded(currentYear)
        ensureHolidaysLoaded(currentYear + 1) {
            Log.d(TAG, "Initial holiday load complete.")
            updateCombinedMap()
        }
    }


    private fun performSearch(query: String) {
        Log.d(TAG, "Executing search logic for query: '$query'")
        if (query.isBlank()) {
            updateSearchResults(emptyList())
            emptyTextView.text = "Enter text to search events"
            emptyTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            return
        }

        if (combinedEventsMap.isEmpty() && (holidaysMap.isNotEmpty() || userEventsList.isNotEmpty())) {
            updateCombinedMap()
        }

        val results = combinedEventsMap.values.flatten()
            .filter { event ->
                event.name.contains(query, ignoreCase = true) ||
                        event.description?.contains(query, ignoreCase = true) == true ||
                        event.type.contains(query, ignoreCase = true)
            }
            .sortedWith(compareBy({ it.date }, { it.time ?: LocalTime.MIN }))

        Log.d(TAG, "Search found ${results.size} results.")
        updateSearchResults(results)
    }


    private fun updateSearchResults(results: List<EventInfo>) {
        eventAdapter.submitList(results.toList())
        if (results.isEmpty()) {
            emptyTextView.text = "No events found" // Update empty text
            emptyTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateCombinedMap() {
        Log.d(TAG, "Rebuilding combined event map...")
        val newCombinedMap = mutableMapOf<LocalDate, MutableList<EventInfo>>()
        synchronized(holidaysMap) {
            holidaysMap.forEach { (date, eventList) ->
                newCombinedMap.computeIfAbsent(date) { mutableListOf() }.addAll(eventList)
            }
        }
        synchronized(userEventsList) {
            userEventsList.forEach { ue ->
                val ei = EventInfo(
                    id = ue.id, name = ue.title, type = ue.type, isUserEvent = true,
                    isHighlighted = false, date = ue.date, time = ue.time, description = ue.description
                )
                newCombinedMap.computeIfAbsent(ue.date) { mutableListOf() }.add(ei)
            }
        }
        newCombinedMap.values.forEach { it.sortBy { ev -> ev.time ?: LocalTime.MIN } }
        combinedEventsMap = newCombinedMap
        Log.d(TAG, "Combined event map updated. Total dates: ${combinedEventsMap.size}")
        // Perform search again if query exists after data updates
        val currentQuery = searchEditText.text?.toString()?.trim()
        if (!currentQuery.isNullOrEmpty()) {
            performSearch(currentQuery)
        }
    }

    private fun ensureHolidaysLoaded(year: Int, onComplete: (() -> Unit)? = null) {
        if (year in loadedHolidayYears || year < DashboardPage.START_YEAR || year > DashboardPage.END_YEAR) {
            onComplete?.invoke(); return
        }
        Log.i(TAG, "Loading holidays for year $year...")
        lifecycleScope.launch(Dispatchers.IO) {
            val holidaysForYear = loadHolidaysForSingleYear(year)
            synchronized(holidaysMap) {
                holidaysForYear.forEach { (date, eventList) ->
                    holidaysMap.computeIfAbsent(date) { mutableListOf() }.addAll(eventList)
                }
                loadedHolidayYears.add(year)
            }
            Log.i(TAG, "Finished loading holidays for year: $year")
            withContext(Dispatchers.Main) {
                updateCombinedMap() // Update map once holidays are loaded
                onComplete?.invoke()
            }
        }
    }

    private fun loadHolidaysForSingleYear(year: Int): Map<LocalDate, MutableList<EventInfo>> {
        val holidays = mutableMapOf<LocalDate, MutableList<EventInfo>>()
        val addHoliday = { date: LocalDate?, name: String, type: String ->
            date?.let { holidays.computeIfAbsent(it) { mutableListOf() }.add(EventInfo(id=null, name=name, type=type, isUserEvent=false, isHighlighted=false, date=it, time=null, description=null)) } }
        val regular = "Regular Holiday"; val special = "Special Non-Working Day"
        addHoliday(LocalDate.of(year, 1, 1), "New Year's Day", regular); addHoliday(LocalDate.of(year, 5, 1), "Labor Day", regular); addHoliday(LocalDate.of(year, 6, 12), "Independence Day", regular)
        addHoliday(LocalDate.of(year, 11, 1), "All Saints' Day", special); addHoliday(LocalDate.of(year, 11, 30), "Bonifacio Day", regular); addHoliday(LocalDate.of(year, 12, 8), "Immaculate Conception", special)
        addHoliday(LocalDate.of(year, 12, 25), "Christmas Day", regular); addHoliday(LocalDate.of(year, 12, 30), "Rizal Day", regular); addHoliday(LocalDate.of(year, 12, 31), "Last Day of the Year", special)
        var kagitingan = LocalDate.of(year, 4, 9); if (kagitingan.dayOfWeek == DayOfWeek.SATURDAY) kagitingan = kagitingan.plusDays(2) else if (kagitingan.dayOfWeek == DayOfWeek.SUNDAY) kagitingan = kagitingan.plusDays(1)
        addHoliday(kagitingan, "Araw ng Kagitingan", regular)
        addHoliday(LocalDate.of(year, 8, 1).with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY)), "National Heroes Day", regular)
        addHoliday(LocalDate.of(year, 2, 25), "EDSA Revolution Anniversary", special); addHoliday(LocalDate.of(year, 11, 2), "All Souls' Day", special)
        calculateEasterSunday(year)?.let { easter -> addHoliday(easter.minusDays(3), "Maundy Thursday", regular); addHoliday(easter.minusDays(2), "Good Friday", regular); addHoliday(easter.minusDays(1), "Black Saturday", special) }
        return holidays
    }

    private fun calculateEasterSunday(year: Int): LocalDate? {
        if (year < 1583) return null; val a = year % 19; val b = year / 100; val c = year % 100; val d = b / 4; val e = b % 4; val f = (b + 8) / 25; val g = (b - f + 1) / 3; val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4; val k = c % 4; val l = (32 + 2 * e + 2 * i - h - k) % 7; val m = (a + 11 * h + 22 * l) / 451; val month = (h + l - 7 * m + 114) / 31; val day = ((h + l - 7 * m + 114) % 31) + 1
        return try { LocalDate.of(year, month, day) } catch (ex: Exception) { null }
    }

    private fun addInitialSchoolEvents() {
        val currentYear = YearMonth.now().year
        val eventsToAdd = listOf(
            UserEvent(title="Start of Classes", description="First day", date=LocalDate.of(currentYear, Month.AUGUST, 14), time=null, type="School Event"),
            UserEvent(title="Midterm Exams", description="Exams", date=LocalDate.of(currentYear, Month.OCTOBER, 16), time=null, type="School Event"),
            UserEvent(title="Christmas Break Starts", description=null, date=LocalDate.of(currentYear, Month.DECEMBER, 18), time=null, type="School Event"),
            UserEvent(title="Midterm Exams (Sem 2)", description="Exams", date=LocalDate.of(currentYear + 1, Month.MARCH, 10), time=null, type="School Event"),
            UserEvent(title="Final Exams (Sem 2)", description="Exams", date=LocalDate.of(currentYear + 1, Month.MAY, 19), time=null, type="School Event")
        )
        synchronized(userEventsList) {
            eventsToAdd.forEach { event -> if (userEventsList.none { it.title == event.title && it.date == event.date }) { userEventsList.add(event) } }
        }
    }


    override fun onEventLongClick(eventInfo: EventInfo) {
        Toast.makeText(this, "Long Click: ${eventInfo.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onEventDeleteClick(eventInfo: EventInfo) {
        Toast.makeText(this, "Cannot delete from search results.", Toast.LENGTH_SHORT).show()
    }

    override fun onEventItemClick(eventInfo: EventInfo, position: Int) {
        Log.d(TAG, "Search result clicked: ${eventInfo.name}")
        val resultIntent = Intent().apply {
            putExtra(EXTRA_SELECTED_DATE, eventInfo.date.toString())
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}