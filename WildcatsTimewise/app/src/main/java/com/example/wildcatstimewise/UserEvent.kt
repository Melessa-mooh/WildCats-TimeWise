package com.example.wildcatstimewise

import java.time.LocalDate
import java.time.LocalTime

// Data class to hold user-created events
data class UserEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val date: LocalDate,
    val time: LocalTime?,
    val type: String
)
