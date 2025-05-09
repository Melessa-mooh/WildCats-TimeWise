package com.example.wildcatstimewise

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager // Needed for findFragmentByTag
import androidx.lifecycle.Lifecycle // Needed for FragmentStateAdapter constructor
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.time.LocalDate

// Import EventInfo directly
import com.example.wildcatstimewise.EventInfo // Ensure this path is correct

class MonthPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    private val TAG = "MonthPagerAdapter"

    private var eventData: Map<LocalDate, List<EventInfo>> = emptyMap()
    private var selectedDate: LocalDate? = null

    private val fragmentManager: FragmentManager = fragmentActivity.supportFragmentManager
    private val lifecycle: Lifecycle = fragmentActivity.lifecycle

    fun setEventData(newEventData: Map<LocalDate, List<EventInfo>>) {
        Log.d(TAG, "Adapter setEventData. Size: ${newEventData.size}")

        if (eventData != newEventData) {
            eventData = newEventData
            notifyActiveFragmentsDataChanged()
        }
    }

    fun setSelectedDate(newSelectedDate: LocalDate?) {
        Log.d(TAG, "Adapter setSelectedDate: $newSelectedDate")
        if (selectedDate != newSelectedDate) {
            selectedDate = newSelectedDate
            notifyActiveFragmentsSelectionChanged()
        }
    }

    private fun notifyActiveFragmentsDataChanged() {
        Log.d(TAG,"Notifying active fragments of event data change")
        for (i in 0 until itemCount) {
            val fragment = fragmentManager.findFragmentByTag("f${getItemId(i)}") as? MonthFragment
            fragment?.eventData = eventData
        }
    }
    private fun notifyActiveFragmentsSelectionChanged() {
        Log.d(TAG,"Notifying active fragments of selection change")
        for (i in 0 until itemCount) {
            val fragment = fragmentManager.findFragmentByTag("f${getItemId(i)}") as? MonthFragment
            // Update the fragment's selection via its public setter
            fragment?.selectedDate = selectedDate
        }
    }


    override fun getItemCount(): Int {
        return DashboardPage.TOTAL_MONTHS
    }

    override fun createFragment(position: Int): Fragment {
        Log.d(TAG, "createFragment for position: $position")
        val yearMonth = DashboardPage.positionToYearMonth(position)
        val fragment = MonthFragment.newInstance(yearMonth.year, yearMonth.monthValue)
        fragment.eventData = this.eventData
        fragment.selectedDate = this.selectedDate
        return fragment
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return itemId >= 0 && itemId < itemCount
    }

}
