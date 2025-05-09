package com.example.wildcatstimewise

import java.time.LocalDate

// Listener interface for fragment interactions back to the Activity/Parent
interface OnMonthFragmentInteractionListener {
    fun onDayClicked(date: LocalDate)
    fun onDayLongClicked(date: LocalDate)
}