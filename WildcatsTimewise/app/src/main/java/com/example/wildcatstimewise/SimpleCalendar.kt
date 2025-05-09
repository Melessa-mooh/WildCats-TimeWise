package com.example.wildcatstimewise

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// Use EventInfo directly
import com.example.wildcatstimewise.EventInfo
// Use the separated interface
import com.example.wildcatstimewise.OnMonthFragmentInteractionListener

class SimpleCalendar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val TAG = "SimpleCalendarRefactored"

    private lateinit var viewPager: ViewPager2
    private lateinit var monthPagerAdapter: MonthPagerAdapter

    // Track internal state
    private var currentYearMonth: YearMonth = YearMonth.now()
    private var selectedDateInternal: LocalDate? = null

    // External listeners
    private var monthChangeListener: ((year: Int, monthIndex: Int) -> Unit)? = null
    private var interactionListenerRef: OnMonthFragmentInteractionListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.activity_simple_calendar, this, true) // Inflate layout with ViewPager2
        orientation = VERTICAL

        if (!isInEditMode) { // Prevent crashing in layout editor
            viewPager = findViewById(R.id.monthViewPager)
            viewPager.offscreenPageLimit = 1 // Keep adjacent months loaded for smoother transition

            if (context is FragmentActivity) {
                monthPagerAdapter = MonthPagerAdapter(context)
                viewPager.adapter = monthPagerAdapter

                // Listen for page changes caused by user swipes
                viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        val newYearMonth = DashboardPage.positionToYearMonth(position)
                        Log.d(TAG, "ViewPager Page Selected: Pos=$position, Month=$newYearMonth")

                        if (newYearMonth != currentYearMonth) {
                            currentYearMonth = newYearMonth
                            // Notify external listener (DashboardPage) about the month change
                            monthChangeListener?.invoke(currentYearMonth.year, currentYearMonth.monthValue - 1) // Pass 0-11 index
                        }
                        // Ensure the newly selected page's fragment has the correct selection state
                        monthPagerAdapter.setSelectedDate(selectedDateInternal)
                        viewPager.requestFocus()
                    }
                })
            } else {
                Log.e(TAG, "Context is NOT FragmentActivity. ViewPager2 adapter cannot be initialized.")
                // Handle error (e.g., hide view)
                visibility = GONE
            }
        }
    }

    // --- Public Methods ---

    fun setMonthYearChangeListener(listener: (year: Int, monthIndex: Int) -> Unit) {
        this.monthChangeListener = listener
    }

    // Called by DashboardPage to provide the interaction listener
    fun setCalendarInteractionListener(listener: OnMonthFragmentInteractionListener) {
        this.interactionListenerRef = listener
        // Note: Fragments get this listener via onAttach in their implementation
    }

    // Called by DashboardPage when event data is updated
    fun setEventData(eventData: Map<LocalDate, List<EventInfo>>) {
        if (::monthPagerAdapter.isInitialized) {
            Log.d(TAG, "Passing event data to adapter.")
            monthPagerAdapter.setEventData(eventData)
        } else {
            Log.w(TAG, "setEventData called but adapter not initialized.")
        }
    }

    // Called by DashboardPage when a day is selected
    fun setSelectedDate(date: LocalDate?) {
        Log.d(TAG, "setSelectedDate called with: $date")
        selectedDateInternal = date // Update internal tracking
        if (::monthPagerAdapter.isInitialized) {
            monthPagerAdapter.setSelectedDate(date) // Pass to adapter to update fragments
        } else {
            Log.w(TAG, "setSelectedDate called but adapter not initialized.")
        }
    }

    // Called by DashboardPage to programmatically change the displayed month
    fun navigateToMonthPosition(position: Int, smoothScroll: Boolean) {
        if (!::viewPager.isInitialized) {
            Log.e(TAG,"ViewPager not initialized in navigateToMonthPosition")
            return
        }
        if (position >= 0 && position < (monthPagerAdapter.itemCount)) {
            Log.d(TAG, "Navigating ViewPager to position: $position, smooth: $smoothScroll")
            if (viewPager.currentItem != position) {
                viewPager.setCurrentItem(position, smoothScroll)
                // If not smooth scrolling, update state and notify listener immediately
                // because onPageSelected might not fire right away.
                if (!smoothScroll) {
                    currentYearMonth = DashboardPage.positionToYearMonth(position)
                    monthChangeListener?.invoke(currentYearMonth.year, currentYearMonth.monthValue - 1)
                    // Ensure selection is passed to adapter for the new page
                    monthPagerAdapter.setSelectedDate(selectedDateInternal)
                }
            } else {
                Log.d(TAG, "Already at target position $position. Refreshing selection.")
                // Ensure selection is visually correct if already on page
                monthPagerAdapter.setSelectedDate(selectedDateInternal)
            }
        } else {
            Log.e(TAG, "Invalid position $position for navigateToMonthPosition. Count: ${monthPagerAdapter.itemCount}")
        }
    }

    // Helper to get current ViewPager position if needed
    fun getCurrentPosition(): Int {
        return if(::viewPager.isInitialized) viewPager.currentItem else -1
    }

    companion object {
        // Provide month names if DashboardPage needs them
        fun getMonthName(monthIndex: Int): String { // monthIndex 0-11
            return try {
                Month.of(monthIndex + 1).getDisplayName(TextStyle.FULL, Locale.getDefault())
            } catch (e: Exception) { "???" }
        }
    }
}
