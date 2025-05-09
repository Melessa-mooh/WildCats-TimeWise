package com.example.wildcatstimewise

import android.content.Context
import android.content.res.Resources // Import added
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.example.wildcatstimewise.EventInfo
import com.example.wildcatstimewise.OnMonthFragmentInteractionListener
// Ensure R class is imported from your package
import com.example.wildcatstimewise.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale


class MonthFragment : Fragment() {

    private val TAG = "MonthFragment"

    private var year: Int = 0
    private var month: Int = 0
    private lateinit var yearMonth: YearMonth

    private lateinit var weekLayouts: List<LinearLayout>


    private var interactionListener: OnMonthFragmentInteractionListener? = null


    private var _selectedDate: LocalDate? = null
    var selectedDate: LocalDate?
        get() = _selectedDate
        set(value) {

            if (_selectedDate != value) {
                _selectedDate = value

                val logPrefix = if (::yearMonth.isInitialized) "[$yearMonth]" else "[Pre-Init]"
                Log.d(TAG,"$logPrefix Setter: Selected date updated to $value")

                if (isAdded && view != null) styleDayCells()
            }
        }

    private var _eventData: Map<LocalDate, List<EventInfo>> = emptyMap()
    var eventData: Map<LocalDate, List<EventInfo>>
        get() = _eventData
        set(value) {

            _eventData = value

            val logPrefix = if (::yearMonth.isInitialized) "[$yearMonth]" else "[Pre-Init]"
            Log.d(TAG,"$logPrefix Setter: Event data updated. Map size: ${value.size}")

            if (isAdded && view != null) styleDayCells()
            // }
        }


    private var colorTodayText: Int = Color.WHITE
    private var colorSelectedText: Int = Color.BLACK
    private var colorDefaultText: Int = Color.BLACK
    private var colorOtherMonthText: Int = Color.GRAY
    private var drawableToday: Int = R.drawable.calendary_day_bg_today // Default value
    private var drawableSelected: Int = R.drawable.calendary_day_bg_selected // Default value
    private var drawableEvent: Int = R.drawable.calendary_day_bg_event // Default value
    private var defaultButtonParams: LinearLayout.LayoutParams? = null
    private var dayTextSizePx: Float = 0f


    override fun onAttach(context: Context) {
        super.onAttach(context)

        Log.d(TAG,"onAttach for ${if(::yearMonth.isInitialized) yearMonth else "pending init"}")

        if (context is OnMonthFragmentInteractionListener) {
            interactionListener = context
        } else {

            Log.e(TAG, "$context must implement OnMonthFragmentInteractionListener")
        }

        try {
            colorTodayText = Color.WHITE
            colorSelectedText = Color.BLACK
            colorDefaultText = Color.BLACK
            colorOtherMonthText = ContextCompat.getColor(context, R.color.black)


            drawableToday = R.drawable.calendary_day_bg_today
            drawableSelected = R.drawable.calendary_day_bg_selected
            drawableEvent = R.drawable.calendary_day_bg_event
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Error getting colors/drawables. Check resource names/existence.", e)

            colorOtherMonthText = Color.GRAY
            drawableToday = 0
            drawableSelected = 0
            drawableEvent = 0
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error initializing resources", e)
        }


        val metrics = context.resources.displayMetrics
        defaultButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            weight = 1f
            val marginInPx = (1 * metrics.density).toInt()
            setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
        }

        dayTextSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 12f, metrics
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            year = it.getInt(ARG_YEAR)
            month = it.getInt(ARG_MONTH)

            if (isValidYearMonth(year, month)) {

                yearMonth = YearMonth.of(year, month)
                Log.d(TAG, "onCreate retrieved valid args for $yearMonth")
            } else {
                Log.e(TAG,"Invalid year/month in arguments: Y=$year, M=$month. Using current.")
                yearMonth = YearMonth.now()
                this.year = yearMonth.year
                this.month = yearMonth.monthValue
            }
        } ?: run {
            Log.e(TAG,"Fragment created without arguments! Using current month.")
            yearMonth = YearMonth.now()
            this.year = yearMonth.year
            this.month = yearMonth.monthValue
        }
    }

    private fun isValidYearMonth(y: Int, m: Int): Boolean {
        return y >= DashboardPage.START_YEAR && y <= DashboardPage.END_YEAR && m in 1..12
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, b: Bundle?): View? {
        Log.d(TAG, "onCreateView for $yearMonth")
        return inflater.inflate(R.layout.activity_month_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated for $yearMonth")
        weekLayouts = listOfNotNull(
            view.findViewById(R.id.month_week_0), view.findViewById(R.id.month_week_1),
            view.findViewById(R.id.month_week_2), view.findViewById(R.id.month_week_3),
            view.findViewById(R.id.month_week_4), view.findViewById(R.id.month_week_5)
        )
        if (weekLayouts.size == 6 && ::yearMonth.isInitialized) {
            populateMonthGrid() // Create and add the day buttons
        } else {
            Log.e(TAG, "Could not populate grid: Layouts found=${weekLayouts.size}, YearMonth Initialized=${::yearMonth.isInitialized}")
        }
    }

    override fun onResume() {
        super.onResume()
        if (::yearMonth.isInitialized && weekLayouts.isNotEmpty()) {
            Log.d(TAG, "onResume for $yearMonth - refreshing style")
            styleDayCells()
        }
    }

    override fun onDetach() {
        super.onDetach()
        interactionListener = null
        Log.d(TAG,"onDetach for $yearMonth")
    }

    // --- Grid Population and Styling ---

    private fun populateMonthGrid() {
        if (!isAdded || context == null || defaultButtonParams == null || weekLayouts.size != 6 || !::yearMonth.isInitialized) {
            Log.w(TAG, "Cannot populate grid, fragment not fully initialized or prerequisites missing.")
            return
        }
        Log.d(TAG, "[$yearMonth] Populating month grid.")
        val context = requireContext() // Safe to call as isAdded is true

        val firstOfMonth = yearMonth.atDay(1)
        val firstDayOfWeekSetting = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        var currentDisplayDate = firstOfMonth

        while (currentDisplayDate.dayOfWeek != firstDayOfWeekSetting) {
            currentDisplayDate = currentDisplayDate.minusDays(1)
        }


        for (weekLayout in weekLayouts) {
            weekLayout.removeAllViews()
            for (i in 0..6) {
                val dateForButton = currentDisplayDate
                val button = Button(context, null, android.R.attr.buttonStyleSmall).apply {
                    layoutParams = defaultButtonParams
                    text = dateForButton.dayOfMonth.toString()
                    tag = dateForButton
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, dayTextSizePx)
                    minHeight = 0; minWidth = 0
                    isClickable = true; isFocusable = true

                    setBackgroundColor(Color.TRANSPARENT)
                    val paddingInPx = (4 * resources.displayMetrics.density).toInt()
                    setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)

                    setOnClickListener {
                        Log.d(TAG, "Day clicked: $dateForButton")
                        interactionListener?.onDayClicked(dateForButton)
                    }
                    setOnLongClickListener {
                        Log.d(TAG, "Day long clicked: $dateForButton")
                        interactionListener?.onDayLongClicked(dateForButton)
                        true
                    }
                }
                weekLayout.addView(button)
                currentDisplayDate = currentDisplayDate.plusDays(1)
            }
        }
        styleDayCells() // Apply initial styling after all buttons are added
    }

    // Applies styling (backgrounds, text colors) to all day buttons based on state
    private fun styleDayCells() {
        // Check if fragment is ready and view exists
        if (!isAdded || view == null || weekLayouts.isEmpty() || !::yearMonth.isInitialized) {
            Log.w(TAG, "Attempted to style cells when fragment not ready.")
            return
        }
        Log.d(TAG,"[$yearMonth] Styling day cells. Current Selected Date = $selectedDate")
        val today = LocalDate.now()

        // Iterate through each week layout and then each button within it
        weekLayouts.forEach { weekLayout ->
            weekLayout.children.filterIsInstance<Button>().forEach { button ->
                // Retrieve the date stored in the button's tag
                val date = button.tag as? LocalDate ?: return@forEach // Skip if tag is not a LocalDate

                // Determine the state of the button/date
                val isToday = date.equals(today)
                val isSelected = date.equals(selectedDate) // Use the selectedDate property
                val isCurrentMonth = date.year == yearMonth.year && date.monthValue == yearMonth.monthValue
                // Check if there are events for this date using the eventData property
                val hasEvents = eventData.containsKey(date) && eventData[date]?.isNotEmpty() == true

                // --- Apply Styling ---
                // 1. Reset to default/base style first
                button.background = null // Remove any previously set background resource
                button.setBackgroundColor(Color.TRANSPARENT) // Ensure transparent background initially
                button.setTextColor(if (isCurrentMonth) colorDefaultText else colorOtherMonthText)
                button.alpha = if (isCurrentMonth) 1.0f else 0.5f // Fade out days from other months

                // 2. Apply state-specific backgrounds (Event -> Today -> Selected)
                // Event marker (applied first, can be overlaid by today/selected)
                if (isCurrentMonth && hasEvents && drawableEvent != 0) {
                    try { button.setBackgroundResource(drawableEvent) }
                    catch (e: Exception) { Log.e(TAG, "Error setting event drawable for $date", e) }
                }

                // Today marker (overrides event marker if necessary, applied before selected)
                if (isToday && drawableToday != 0) {
                    try { button.setBackgroundResource(drawableToday) }
                    catch (e: Exception) { Log.e(TAG, "Error setting today drawable for $date", e) }
                    button.setTextColor(colorTodayText) // Today text color
                }

                // Selected marker (applied last, overrides text color, potentially background)
                if (isSelected) {
                    if (!isToday && drawableSelected != 0) {
                        // Apply selected background only if it's not today
                        try { button.setBackgroundResource(drawableSelected) }
                        catch (e: Exception) { Log.e(TAG, "Error setting selected drawable for $date", e) }
                    } else if (isToday && drawableToday != 0) {
                        // If it is today AND selected, keep today's background visually
                        try { button.setBackgroundResource(drawableToday) }
                        catch (e: Exception) { Log.e(TAG, "Error setting today(selected) drawable for $date", e) }
                    }
                    // Selected text color always takes precedence
                    button.setTextColor(colorSelectedText)
                }
            }
        }
    }

    // Companion object for creating new instances of the fragment
    companion object {
        private const val ARG_YEAR = "year"
        private const val ARG_MONTH = "month" // Month as 1-12

        @JvmStatic
        fun newInstance(year: Int, month: Int): MonthFragment {
            Log.d("MonthFragmentCompanion", "Creating new instance for Year=$year, Month=$month")
            return MonthFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_YEAR, year)
                    putInt(ARG_MONTH, month)
                }
            }
        }
    }
}
