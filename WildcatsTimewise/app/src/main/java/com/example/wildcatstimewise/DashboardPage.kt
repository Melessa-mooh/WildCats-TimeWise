package com.example.wildcatstimewise
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Canvas
import androidx.core.graphics.ColorUtils
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wildcatstimewise.EventAdapter.OnEventClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs


class DashboardPage : FragmentActivity(), OnEventClickListener, OnMonthFragmentInteractionListener {

    private val TAG = "DashboardPage"

    private lateinit var eventDisplayRecyclerView: RecyclerView
    private lateinit var simpleCalendar: SimpleCalendar
    private lateinit var currentYearTextView: TextView
    private lateinit var currentMonthTextView: TextView
    private lateinit var currentDayTextView: TextView
    private lateinit var addSectionLayout: LinearLayout
    private lateinit var eventSectionLayout: LinearLayout
    private lateinit var eventsHeaderText: TextView
    private lateinit var notificationSectionLayout: LinearLayout

    private lateinit var eventAdapter: EventAdapter
    private var holidaysMap: MutableMap<LocalDate, MutableList<EventInfo>> = mutableMapOf()
    private val userEventsList = mutableListOf<UserEvent>()
    private var combinedEventsMap: Map<LocalDate, List<EventInfo>> = emptyMap()
    private var currentlySelectedDate: LocalDate = LocalDate.now()
    private var currentlyDisplayedYearMonth: YearMonth = YearMonth.now()

    private var isShowingSearchResults = false
    private var currentSearchQuery: String? = null

    private val loadedHolidayYears = mutableSetOf<Int>()
    private var isInitialCalendarSetupDone = false

    private lateinit var recyclerViewGestureDetector: GestureDetector

    private val monthAbbreviationFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    private val monthFullNameFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

    private lateinit var homeSection: LinearLayout
    private lateinit var searchSection: LinearLayout
    private lateinit var notificationSection: LinearLayout
    private lateinit var profileSection: LinearLayout
    private lateinit var homeIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var notificationIcon: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var homeText: TextView
    private lateinit var searchText: TextView
    private lateinit var notificationText: TextView
    private lateinit var profileText: TextView
    private var selectedBottomNavId: Int = R.id.home_section

    private lateinit var searchResultLauncher: ActivityResultLauncher<Intent>


    companion object {
        const val START_YEAR = 1970
        const val END_YEAR = 2100
        const val MONTHS_PER_YEAR = 12
        val TOTAL_MONTHS = (END_YEAR - START_YEAR + 1) * MONTHS_PER_YEAR

        fun yearMonthToPosition(yearMonth: YearMonth): Int {
            if (yearMonth.year < START_YEAR || yearMonth.year > END_YEAR) return -1
            val yearOffset = yearMonth.year - START_YEAR
            val monthOffset = yearMonth.monthValue - 1
            return yearOffset * MONTHS_PER_YEAR + monthOffset
        }

        fun positionToYearMonth(position: Int): YearMonth {
            if (position < 0 || position >= TOTAL_MONTHS) return YearMonth.now()
            val year = START_YEAR + position / MONTHS_PER_YEAR
            val month = (position % MONTHS_PER_YEAR) + 1
            return YearMonth.of(year, month)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard_page)

        findViews()

        currentlyDisplayedYearMonth = YearMonth.from(currentlySelectedDate)
        updateDateHeader(currentlyDisplayedYearMonth.year, currentlyDisplayedYearMonth.monthValue - 1, currentlySelectedDate.dayOfMonth)

        setupRecyclerView()
        setupSearchResultLauncher()

        addInitialSchoolEvents()

        ensureHolidaysLoaded(currentlyDisplayedYearMonth.year) {
            updateCombinedMap()
            if (!isInitialCalendarSetupDone) {
                setupSimpleCalendar()
                isInitialCalendarSetupDone = true
            }
            isShowingSearchResults = false
            showEventsForMonth(currentlyDisplayedYearMonth)
            eventAdapter.setHighlightForDate(currentlySelectedDate)
        }

        setupListeners()
        setupFragmentResultListener()
        updateBottomNavSelection(selectedBottomNavId)
    }

    private fun findViews() {
        simpleCalendar = findViewById(R.id.square_day)
        eventDisplayRecyclerView = findViewById(R.id.eventDisplayRecyclerView)
        currentYearTextView = findViewById(R.id.current_year)
        currentMonthTextView = findViewById(R.id.current_month)
        currentDayTextView = findViewById(R.id.current_day)
        addSectionLayout = findViewById(R.id.add_section)
        eventSectionLayout = findViewById(R.id.event_section)
        eventsHeaderText = findViewById(R.id.events_header_text)
        notificationSectionLayout = findViewById(R.id.notification_section)

        homeSection = findViewById(R.id.home_section)
        searchSection = findViewById(R.id.event_section)
        notificationSection = findViewById(R.id.notification_section)
        profileSection = findViewById(R.id.profile_section)
        homeIcon = findViewById(R.id.home_icon)
        searchIcon = findViewById(R.id.search_icon)
        notificationIcon = findViewById(R.id.notification_icon)
        profileIcon = findViewById(R.id.profile_icon)
        try {
            homeText = homeSection.findViewById<TextView>(R.id.calendartextView)
            searchText = searchSection.findViewById<TextView>(R.id.searchTextView)
            notificationText = notificationSection.findViewById<TextView>(R.id.notificationTextView)
            profileText = profileSection.findViewById<TextView>(R.id.profileTextView)
        } catch (e: Exception) {
            Log.e(TAG, "Could not find specific TextView IDs in bottom nav. Styling might fail.")
            try {
                homeText = homeSection.findViewByType(TextView::class.java)!!
                searchText = searchSection.findViewByType(TextView::class.java)!!
                notificationText = notificationSection.findViewByType(TextView::class.java)!!
                profileText = profileSection.findViewByType(TextView::class.java)!!
            } catch (e2: Exception){
                Log.e(TAG, "Could not find any TextViews in bottom nav layouts.")
                homeText = TextView(this)
                searchText = TextView(this)
                notificationText = TextView(this)
                profileText = TextView(this)
            }
        }
    }

    private fun <T : View> View.findViewByType(viewType: Class<T>): T? {
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (viewType.isAssignableFrom(child.javaClass)) {
                    @Suppress("UNCHECKED_CAST")
                    return child as T
                } else if (child is ViewGroup) {
                    val found = child.findViewByType(viewType)
                    if (found != null) {
                        return found
                    }
                }
            }
        }
        return null
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(this)
        eventDisplayRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DashboardPage)
            adapter = eventAdapter
        }

        val swipeHandler = SwipeToDeleteCallback(this, eventAdapter) { eventInfo, position ->
            showDeleteConfirmationDialog(eventInfo, position)
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(eventDisplayRecyclerView)
    }

    private fun setupSearchResultLauncher() {
        searchResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val selectedDateStr = data?.getStringExtra(SearchEventsActivity.EXTRA_SELECTED_DATE)
                if (selectedDateStr != null) {
                    try {
                        val selectedDate = LocalDate.parse(selectedDateStr)
                        navigateToDate(selectedDate)
                    } catch (e: DateTimeParseException) {
                        Log.e(TAG, "Error parsing date from SearchEventsActivity result: $selectedDateStr", e)
                    }
                }
            }
        }
    }


    private fun setupSimpleCalendar() {
        if (!::simpleCalendar.isInitialized) {
            Log.e(TAG, "SimpleCalendar view is not initialized! Cannot setup.")
            return
        }
        simpleCalendar.setCalendarInteractionListener(this)
        simpleCalendar.setEventData(combinedEventsMap)

        simpleCalendar.setMonthYearChangeListener { year, monthIndex ->
            val newYearMonth = YearMonth.of(year, monthIndex + 1)
            currentlyDisplayedYearMonth = newYearMonth

            ensureHolidaysLoaded(year) {
                updateDateHeader(year, monthIndex, currentlySelectedDate.dayOfMonth)
                updateCombinedMap()
                simpleCalendar.setEventData(combinedEventsMap)
                showEventsForMonth(newYearMonth)
                if(currentlySelectedDate.year == year && currentlySelectedDate.monthValue == newYearMonth.monthValue) {
                    eventAdapter.setHighlightForDate(currentlySelectedDate)
                } else {
                    eventAdapter.clearHighlight()
                }
            }
        }

        val initialPosition = yearMonthToPosition(currentlyDisplayedYearMonth)
        if (initialPosition != -1) {
            simpleCalendar.navigateToMonthPosition(initialPosition, false)
            simpleCalendar.setSelectedDate(currentlySelectedDate)
        } else {
            Log.e(TAG, "Failed to calculate initial position for $currentlyDisplayedYearMonth.")
        }
    }

    private fun setupListeners() {
        currentYearTextView.setOnLongClickListener { showYearPickerDialog(); true }
        currentMonthTextView.setOnLongClickListener { showMonthDayPickerDialog(); true }

        addSectionLayout.setOnClickListener {
            launchAddEventFragment(currentlySelectedDate.year, currentlySelectedDate.monthValue, currentlySelectedDate.dayOfMonth)
        }

        searchSection.setOnClickListener {
            updateBottomNavSelection(R.id.event_section)
            val intent = Intent(this, SearchEventsActivity::class.java)
            searchResultLauncher.launch(intent)
        }

        currentDayTextView.setOnClickListener { navigateToToday() }

        homeSection.setOnClickListener {
            updateBottomNavSelection(R.id.home_section)
            navigateToToday()
            if (isShowingSearchResults) {
                isShowingSearchResults = false
                showEventsForMonth(YearMonth.from(currentlySelectedDate))
                eventAdapter.setHighlightForDate(currentlySelectedDate)
            }
        }
        notificationSection.setOnClickListener {
            updateBottomNavSelection(R.id.notification_section)
            val intent = Intent(this, NotificationHistoryActivity::class.java)
            startActivity(intent)
        }
        profileSection.setOnClickListener {
            updateBottomNavSelection(R.id.profile_section)
            val menuFragment = MenuFragment.newInstance()
            menuFragment.show(supportFragmentManager, MenuFragment.TAG)
        }
    }

    private fun updateBottomNavSelection(selectedId: Int) {
        selectedBottomNavId = selectedId

        val selectedColor = ContextCompat.getColor(this, R.color.crimson)
        val defaultColor = ContextCompat.getColor(this, R.color.black)
        val whiteColor = ContextCompat.getColor(this, R.color.white)

        listOf(homeSection, searchSection, notificationSection, profileSection).forEach { section ->
            section.setBackgroundColor(Color.TRANSPARENT)
            val icon = section.findViewByType(ImageView::class.java)
            val text = section.findViewByType(TextView::class.java)
            when(section.id) {
                R.id.home_section -> homeIcon.setImageResource(R.drawable.solid_calendar)
                R.id.event_section -> searchIcon.setImageResource(R.drawable.solid_search)
                R.id.notification_section -> notificationIcon.setImageResource(R.drawable.solid_notif)
                R.id.profile_section -> profileIcon.setImageResource(R.drawable.solid_profile)
            }
            icon?.clearColorFilter()
            text?.setTextColor(defaultColor)
        }

        when (selectedId) {
            R.id.home_section -> {
                homeSection.setBackgroundResource(R.drawable.rectanglecrimson)
                homeIcon.setImageResource(R.drawable.white_calendar_search)
                homeText.setTextColor(whiteColor)
            }
            R.id.event_section -> {
                searchSection.setBackgroundResource(R.drawable.rectanglecrimson)
                searchIcon.setImageResource(R.drawable.white_solid_search)
                searchText.setTextColor(whiteColor)
            }
            R.id.notification_section -> {
                notificationSection.setBackgroundResource(R.drawable.rectanglecrimson)
                notificationIcon.setImageResource(R.drawable.solid_white_notif)
                notificationText.setTextColor(whiteColor)
            }
            R.id.profile_section -> {
                profileSection.setBackgroundResource(R.drawable.rectanglecrimson)
                profileIcon.setImageResource(R.drawable.white_solid_profile)
                profileText.setTextColor(whiteColor)
            }
        }
    }


    private fun navigateToToday() {
        val today = LocalDate.now()
        navigateToDate(today)
        updateBottomNavSelection(R.id.home_section)
    }

    private fun navigateToDate(date: LocalDate) {
        val currentYearMonth = YearMonth.from(currentlySelectedDate)
        val targetYearMonth = YearMonth.from(date)
        val dateChanged = currentlySelectedDate != date
        val monthChanged = currentYearMonth != targetYearMonth
        val searchWasActive = isShowingSearchResults

        if (searchWasActive) {
            isShowingSearchResults = false
            currentSearchQuery = null
        }

        ensureHolidaysLoaded(date.year) {
            if (monthChanged) {
                val position = yearMonthToPosition(targetYearMonth)
                if (position != -1 && ::simpleCalendar.isInitialized) {
                    val smoothScroll = abs(position - simpleCalendar.getCurrentPosition()) > 1
                    simpleCalendar.navigateToMonthPosition(position, smoothScroll)
                    updateCurrentlySelectedDate(date)
                } else {
                    Log.e(TAG, "Cannot navigate to date $date. Position: $position, Calendar Initialized: ${::simpleCalendar.isInitialized}")
                    updateCurrentlySelectedDate(date)
                    showEventsForMonth(targetYearMonth)
                }
            } else if (dateChanged || searchWasActive) {
                updateCurrentlySelectedDate(date)
                if (searchWasActive) showEventsForMonth(targetYearMonth)
            } else {
                eventAdapter.setHighlightForDate(date)
                if(::simpleCalendar.isInitialized) simpleCalendar.setSelectedDate(date)
            }
        }
    }


    private fun setupFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener(AddEventFragment.REQUEST_KEY, this) { requestKey, bundle ->
            val dateStr = bundle.getString(AddEventFragment.RESULT_KEY_DATE_STR)
            val mode = bundle.getString(AddEventFragment.RESULT_KEY_MODE)
            val id = bundle.getString(AddEventFragment.RESULT_KEY_ID)
            val title = bundle.getString(AddEventFragment.RESULT_KEY_TITLE)
            val desc = bundle.getString(AddEventFragment.RESULT_KEY_DESC)
            val timeStr = bundle.getString(AddEventFragment.RESULT_KEY_TIME_STR)
            val eventType = bundle.getString(AddEventFragment.RESULT_KEY_EVENT_TYPE) ?: "User Event"

            if (dateStr == null || title == null || mode == null) {
                Log.e(TAG, "Fragment result missing essential data. Bundle: $bundle")
                Toast.makeText(this, "Error processing event data.", Toast.LENGTH_SHORT).show()
                return@setFragmentResultListener
            }

            try {
                val date = LocalDate.parse(dateStr)
                val time: LocalTime? = timeStr?.let {
                    try { LocalTime.parse(it) } catch (e: DateTimeParseException) { null }
                }

                ensureHolidaysLoaded(date.year)

                var eventChanged = false
                synchronized(userEventsList) {
                    if (mode == "edit" && id != null) {
                        val index = userEventsList.indexOfFirst { it.id == id }
                        if (index != -1) {
                            userEventsList[index] = userEventsList[index].copy(
                                title = title,
                                description = desc,
                                date = date,
                                time = time,
                                type = eventType
                            )
                            eventChanged = true
                        } else {
                            Log.w(TAG, "Attempted to edit event ID '$id' but it was not found.")
                        }
                    } else if (mode == "add") {
                        val newEvent = UserEvent(
                            title = title,
                            description = desc,
                            date = date,
                            time = time,
                            type = eventType
                        )
                        userEventsList.add(newEvent)
                        eventChanged = true
                    }

                    if (eventChanged) {
                        updateCombinedMap()
                        showEventsForMonth(YearMonth.from(date))
                        if(::simpleCalendar.isInitialized) simpleCalendar.setEventData(combinedEventsMap)
                    }
                }

                navigateToDate(date)

            } catch (e: DateTimeParseException) {
                Log.e(TAG, "Error parsing date/time from fragment result.", e)
                Toast.makeText(this, "Error processing date/time.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Generic error processing fragment result bundle.", e)
                Toast.makeText(this, "An error occurred.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDayClicked(date: LocalDate) {
        navigateToDate(date)
    }

    override fun onDayLongClicked(date: LocalDate) {
        launchAddEventFragment(date.year, date.monthValue, date.dayOfMonth)
    }

    private fun updateCurrentlySelectedDate(newDate: LocalDate) {
        val dateChanged = currentlySelectedDate != newDate
        val monthChanged = YearMonth.from(currentlySelectedDate) != YearMonth.from(newDate)

        currentlySelectedDate = newDate
        updateDateHeader(newDate.year, newDate.monthValue - 1, newDate.dayOfMonth)

        if(::simpleCalendar.isInitialized) simpleCalendar.setSelectedDate(newDate)
        eventAdapter.setHighlightForDate(newDate)


        if (!isShowingSearchResults) {

            if (!monthChanged) {
                val index = eventAdapter.currentList.indexOfFirst { it.date == newDate }
                if (index != -1) {
                    (eventDisplayRecyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 0)
                }
            }
        }
    }


    private fun updateDateHeader(year: Int, monthIndex: Int, day: Int) {
        try {
            val yearMonthObject = YearMonth.of(year, monthIndex + 1)
            currentYearTextView.text = year.toString()
            currentMonthTextView.text = yearMonthObject.format(monthAbbreviationFormatter).uppercase(Locale.getDefault())
            currentDayTextView.text = day.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating date header for Y:$year M:${monthIndex+1} D:$day", e)
            currentYearTextView.text = "----"
            currentMonthTextView.text = "---"
            currentDayTextView.text = "--"
        }
    }

    private fun updateCombinedMap() {
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
                    isHighlighted = false,
                    date = ue.date, time = ue.time, description = ue.description
                )
                newCombinedMap.computeIfAbsent(ue.date) { mutableListOf() }.add(ei)
            }
        }
        newCombinedMap.values.forEach { eventsOnDate ->
            eventsOnDate.sortBy { it.time ?: LocalTime.MIN }
        }
        combinedEventsMap = newCombinedMap

        if(::simpleCalendar.isInitialized) {
            simpleCalendar.setEventData(combinedEventsMap)
        }
        if (!isShowingSearchResults && ::eventAdapter.isInitialized) {
            showEventsForMonth(currentlyDisplayedYearMonth)
            eventAdapter.setHighlightForDate(currentlySelectedDate)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showEventsForMonth(yearMonth: YearMonth?) {
        if (!::eventAdapter.isInitialized) return
        if (yearMonth == null) {
            eventsHeaderText.text = "Events"
            eventAdapter.submitList(emptyList())
            return
        }

        if (isShowingSearchResults) {

            return
        }

        val eventsToShow = combinedEventsMap
            .filterKeys { date ->
                date.year == yearMonth.year && date.monthValue == yearMonth.monthValue
            }
            .values
            .flatten()
            .sortedWith(compareBy({ it.date }, { it.time ?: LocalTime.MIN }))

        eventsHeaderText.text = "Events for ${yearMonth.format(monthFullNameFormatter)} ${yearMonth.year}"

        eventAdapter.submitList(eventsToShow.toList())

        val index = eventsToShow.indexOfFirst { it.date == currentlySelectedDate }
        if (index != -1) {
            (eventDisplayRecyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 0)
        } else if (eventsToShow.isNotEmpty()) {

        }

        currentlySelectedDate.takeIf {
            it.year == yearMonth.year && it.monthValue == yearMonth.monthValue
        }?.let { selectedDate ->
            eventAdapter.setHighlightForDate(selectedDate)
        } ?: eventAdapter.clearHighlight()

    }


    private fun showYearPickerDialog() {
        val currentYear = currentlyDisplayedYearMonth.year
        val minYear = START_YEAR; val maxYear = END_YEAR

        val yearPicker = NumberPicker(this).apply {
            minValue = minYear; maxValue = maxYear
            value = currentYear.coerceIn(minYear, maxYear)
            wrapSelectorWheel = false
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Year")
            .setView(yearPicker)
            .setPositiveButton("OK") { dialog, _ ->
                val selectedYear = yearPicker.value
                val targetDate = LocalDate.of(selectedYear, currentlyDisplayedYearMonth.monthValue, 1)
                navigateToDate(targetDate)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showMonthDayPickerDialog() {
        val currentYear = currentlyDisplayedYearMonth.year
        val currentMonthIndex = currentlyDisplayedYearMonth.monthValue - 1
        val currentDay = currentlySelectedDate.dayOfMonth

        val datePickerDialog = DatePickerDialog(
            this,
            { _, yearFromPicker, monthOfYear, dayOfMonth ->

                val selectedMonth = monthOfYear + 1
                val selectedYear = currentlyDisplayedYearMonth.year

                val daysInMonth = YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
                val validDay = dayOfMonth.coerceAtMost(daysInMonth)
                val selectedDate = LocalDate.of(selectedYear, selectedMonth, validDay)
                navigateToDate(selectedDate)
            },
            currentYear, currentMonthIndex, currentDay
        )

        datePickerDialog.setOnShowListener { dialog ->
            try {
                val dp = (dialog as DatePickerDialog)

                val yearSpinnerId = Resources.getSystem().getIdentifier("year", "id", "android")
                val yearHeaderId = Resources.getSystem().getIdentifier("date_picker_header_year", "id", "android")


                dp.findViewById<View>(yearSpinnerId)?.visibility = View.GONE


                dp.findViewById<View>(yearHeaderId)?.visibility = View.GONE


                val monthDayLayoutId = Resources.getSystem().getIdentifier("month_day_layout", "id", "android")
                dp.findViewById<View>(monthDayLayoutId)?.visibility = View.VISIBLE


            } catch (e: Exception) { Log.e(TAG, "Error trying to hide year spinner/header", e)}
        }
        datePickerDialog.show()
    }


    private fun launchAddEventFragment(year: Int, month: Int, day: Int) {
        val addEventFragment = AddEventFragment.newInstanceForAdd(year, month, day)
        addEventFragment.show(supportFragmentManager, "AddEventDialog")
    }

    private fun launchEditEventFragment(eventInfo: EventInfo) {
        if (!eventInfo.isUserEvent || eventInfo.id == null) {
            Toast.makeText(this, "Only user events can be edited.", Toast.LENGTH_SHORT).show()
            return
        }
        val editEventFragment = EditEventFragment.newInstance(eventInfo)
        editEventFragment.show(supportFragmentManager, "EditEventDialog")
    }

    private fun showDeleteConfirmationDialog(eventInfo: EventInfo, position: Int) {
        if (!eventInfo.isUserEvent || eventInfo.id == null) {
            Toast.makeText(this, "Only user events can be deleted.", Toast.LENGTH_SHORT).show()
            try {
                if (::eventAdapter.isInitialized && position >= 0 && position < eventAdapter.itemCount) {
                    eventAdapter.notifyItemChanged(position)
                }
            } catch(e: Exception) { Log.e(TAG, "Error notifying item change on invalid delete attempt", e)}
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete \"${eventInfo.name}\"?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteEvent(eventInfo)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                try {
                    if (::eventAdapter.isInitialized && position >= 0 && position < eventAdapter.itemCount) {
                        eventAdapter.notifyItemChanged(position)
                    }
                } catch (e: Exception) { Log.e(TAG, "Error notifying item change on cancel", e) }
                dialog.dismiss()
            }
            .setOnDismissListener {
                if (::eventAdapter.isInitialized) {
                    try {
                        if (position >= 0 && position < eventAdapter.itemCount) {
                            eventAdapter.notifyItemChanged(position)
                        }
                    } catch (e: Exception) { Log.e(TAG, "Error notifying item change on dismiss", e) }
                }
            }
            .setOnCancelListener {
                if (::eventAdapter.isInitialized) {
                    try {
                        if (position >= 0 && position < eventAdapter.itemCount) {
                            eventAdapter.notifyItemChanged(position)
                        }
                    } catch (e: Exception) { Log.e(TAG, "Error notifying item change on cancel listener", e) }
                }
            }
            .show()
    }

    private fun deleteEvent(eventInfo: EventInfo) {
        if (eventInfo.id == null || !eventInfo.isUserEvent) {
            Log.w(TAG, "Attempted to delete non-user event or event with null ID in deleteEvent.")
            return
        }

        var eventRemoved = false
        val originalDate = eventInfo.date
        synchronized(userEventsList) {
            val initialSize = userEventsList.size
            userEventsList.removeAll { it.id == eventInfo.id }
            eventRemoved = userEventsList.size < initialSize
        }

        if (eventRemoved) {
            Toast.makeText(this, "\"${eventInfo.name}\" deleted.", Toast.LENGTH_SHORT).show()
            updateCombinedMap()
            showEventsForMonth(currentlyDisplayedYearMonth)
            if (::eventAdapter.isInitialized) {
                eventAdapter.setHighlightForDate(currentlySelectedDate)
            }

        } else {
            Toast.makeText(this, "Error deleting event.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEventLongClick(eventInfo: EventInfo) {
        if (eventInfo.isUserEvent) {
            launchEditEventFragment(eventInfo)
        } else {
            Toast.makeText(this, "Holidays and school events cannot be edited.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEventDeleteClick(eventInfo: EventInfo) {
        Log.d(TAG, "onEventDeleteClick called (likely legacy)")
    }

    override fun onEventItemClick(eventInfo: EventInfo, position: Int) {
        if (eventInfo.date == currentlySelectedDate) {

            eventAdapter.setHighlightForDate(eventInfo.date)
            eventAdapter.toggleHighlight(position)
        } else {

            navigateToDate(eventInfo.date)
        }
    }


    private fun loadHolidays() {
        val currentYear = YearMonth.now().year
        ensureHolidaysLoaded(currentYear -1)
        ensureHolidaysLoaded(currentYear)
        ensureHolidaysLoaded(currentYear + 1)
    }

    private fun ensureHolidaysLoaded(year: Int, onComplete: (() -> Unit)? = null) {
        if (year in loadedHolidayYears) {
            onComplete?.invoke()
            return
        }
        if (year < START_YEAR || year > END_YEAR) {
            onComplete?.invoke()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val holidaysForYear = loadHolidaysForSingleYear(year)
            synchronized(holidaysMap) {
                holidaysForYear.forEach { (date, eventList) ->
                    holidaysMap.computeIfAbsent(date) { mutableListOf() }.addAll(eventList)
                }
                loadedHolidayYears.add(year)
            }
            withContext(Dispatchers.Main) {
                updateCombinedMap()
                onComplete?.invoke()
            }
        }
    }

    private fun loadHolidaysForSingleYear(year: Int): Map<LocalDate, MutableList<EventInfo>> {
        val holidays = mutableMapOf<LocalDate, MutableList<EventInfo>>()
        val addHoliday = { date: LocalDate?, name: String, type: String ->
            date?.let {
                val info = EventInfo(id = null, name = name, type = type, isUserEvent = false, isHighlighted = false, date = it, time = null, description = null)
                holidays.computeIfAbsent(it) { mutableListOf() }.add(info)
            }
        }
        val regular = "Regular Holiday"; val special = "Special Non-Working Day"

        addHoliday(LocalDate.of(year, 1, 1), "New Year's Day", regular)
        addHoliday(LocalDate.of(year, 5, 1), "Labor Day", regular)
        addHoliday(LocalDate.of(year, 6, 12), "Independence Day", regular)
        addHoliday(LocalDate.of(year, 11, 1), "All Saints' Day", special)
        addHoliday(LocalDate.of(year, 11, 30), "Bonifacio Day", regular)
        addHoliday(LocalDate.of(year, 12, 8), "Immaculate Conception", special)
        addHoliday(LocalDate.of(year, 12, 25), "Christmas Day", regular)
        addHoliday(LocalDate.of(year, 12, 30), "Rizal Day", regular)
        addHoliday(LocalDate.of(year, 12, 31), "Last Day of the Year", special)

        var kagitingan = LocalDate.of(year, 4, 9)
        if (kagitingan.dayOfWeek == DayOfWeek.SATURDAY) kagitingan = kagitingan.plusDays(2)
        else if (kagitingan.dayOfWeek == DayOfWeek.SUNDAY) kagitingan = kagitingan.plusDays(1)
        addHoliday(kagitingan, "Araw ng Kagitingan", regular)

        addHoliday(LocalDate.of(year, 8, 1).with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY)), "National Heroes Day", regular)
        addHoliday(LocalDate.of(year, 2, 25), "EDSA Revolution Anniversary", special)
        addHoliday(LocalDate.of(year, 11, 2), "All Souls' Day", special)

        calculateEasterSunday(year)?.let { easter ->
            addHoliday(easter.minusDays(3), "Maundy Thursday", regular)
            addHoliday(easter.minusDays(2), "Good Friday", regular)
            addHoliday(easter.minusDays(1), "Black Saturday", special)
        }
        return holidays
    }

    private fun calculateEasterSunday(year: Int): LocalDate? {
        if (year < 1583) return null
        val a = year % 19; val b = year / 100; val c = year % 100
        val d = b / 4; val e = b % 4; val f = (b + 8) / 25
        val g = (b - f + 1) / 3; val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4; val k = c % 4; val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return try { LocalDate.of(year, month, day) } catch (ex: Exception) { null }
    }

    private fun addInitialSchoolEvents() {
        val currentYear = YearMonth.now().year
        val eventsToAdd = listOf(
            UserEvent(title="Start of Classes", description="First day of the Academic Year", date=LocalDate.of(currentYear, Month.AUGUST, 14), time=null, type="School Event"),
            UserEvent(title="Midterm Exams", description="Examination Period", date=LocalDate.of(currentYear, Month.OCTOBER, 16), time=null, type="School Event"),
            UserEvent(title="Christmas Break Starts", description=null, date=LocalDate.of(currentYear, Month.DECEMBER, 18), time=null, type="School Event"),
            UserEvent(title="Midterm Exams (Sem 2)", description="Examination Period", date=LocalDate.of(currentYear + 1, Month.MARCH, 10), time=null, type="School Event"),
            UserEvent(title="Final Exams (Sem 2)", description="Examination Period", date=LocalDate.of(currentYear + 1, Month.MAY, 19), time=null, type="School Event")
        )
        var addedCount = 0
        synchronized(userEventsList) {
            eventsToAdd.forEach { event ->
                if (userEventsList.none { it.title == event.title && it.date == event.date }) {
                    userEventsList.add(event)
                    addedCount++
                }
            }
        }
        if (addedCount > 0) ensureHolidaysLoaded(currentYear + 1)
    }


    private inner class SwipeToDeleteCallback(
        val context: Context,
        val adapter: EventAdapter,
        val confirmAction: (EventInfo, Int) -> Unit
    ) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

        private val deleteIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.delete_white)
        private val paint: Paint = Paint()
        private val crimsonColor = Color.parseColor("#DC143C")

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                try {
                    val eventInfo = adapter.currentList[position]
                    if (eventInfo.isUserEvent) {
                        confirmAction(eventInfo, position)
                    } else {
                        Toast.makeText(context, "Only user events can be deleted.", Toast.LENGTH_SHORT).show()
                        adapter.notifyItemChanged(position)
                    }
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("SwipeToDeleteCallback", "Error getting item at position $position", e)
                    try { adapter.notifyItemChanged(position) } catch (notifyEx: Exception) { Log.e(TAG, "Error notifying item change after swipe error", notifyEx)}
                } catch (e: Exception) {
                    Log.e("SwipeToDeleteCallback", "Generic error during swipe action at $position", e)
                    try { adapter.notifyItemChanged(position) } catch (notifyEx: Exception) { Log.e(TAG, "Error notifying item change after generic swipe error", notifyEx)}
                }
            }
        }

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
            val itemViewWidth = viewHolder.itemView.width.toFloat()
            if (itemViewWidth <= 0) {
                return 0.5f
            }
            val density = context.resources.displayMetrics.density
            val swipeThresholdPx = 40 * density
            val thresholdFraction = swipeThresholdPx / itemViewWidth
            return thresholdFraction.coerceIn(0.1f, 1.0f)
        }

        override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val position = viewHolder.adapterPosition
            return if (position != RecyclerView.NO_POSITION) {
                try {
                    val eventInfo = adapter.currentList[position]
                    if (eventInfo.isUserEvent) {
                        super.getSwipeDirs(recyclerView, viewHolder)
                    } else {
                        0
                    }
                } catch (e: IndexOutOfBoundsException) {
                    0
                }
            } else {
                0
            }
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView
            val iconMargin = (itemView.height - (deleteIcon?.intrinsicHeight ?: 0)) / 2
            val iconTop = itemView.top + iconMargin
            val iconBottom = iconTop + (deleteIcon?.intrinsicHeight ?: 0)
            val iconLeft: Int
            val iconRight: Int

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                paint.color = crimsonColor
                c.drawRect(
                    itemView.right + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat(),
                    paint
                )

                iconLeft = itemView.right - iconMargin - (deleteIcon?.intrinsicWidth ?: 0)
                iconRight = itemView.right - iconMargin
                deleteIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                deleteIcon?.draw(c)

            } else {
                deleteIcon?.setBounds(0, 0, 0, 0)
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }
}