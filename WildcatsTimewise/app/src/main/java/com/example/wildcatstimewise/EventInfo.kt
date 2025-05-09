package com.example.wildcatstimewise

import java.time.LocalDate
import java.time.LocalTime

// Data class for holding event details for display.
// Ensure this definition is accessible by DashboardPage, MonthFragment, MonthPagerAdapter.
data class EventInfo(
    val id: String?,
    val name: String,
    val type: String,
    val isUserEvent: Boolean = true,
    var isHighlighted: Boolean = false,
    val date: LocalDate,
    val time: LocalTime?,
    val description: String? = null
)

