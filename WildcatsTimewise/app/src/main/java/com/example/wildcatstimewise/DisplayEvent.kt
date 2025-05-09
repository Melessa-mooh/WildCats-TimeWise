package com.example.wildcatstimewise

import java.time.LocalDate
import java.time.LocalTime

data class DisplayEvent(
    val id: String,
    val name: String,
    val type: String,
    //val description: String?,
    val date: LocalDate,
    val time: LocalTime?,
    var isHighlighted: Boolean = false
)
