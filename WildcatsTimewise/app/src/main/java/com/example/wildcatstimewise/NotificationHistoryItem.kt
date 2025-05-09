package com.example.wildcatstimewise

import kotlinx.serialization.Serializable

@Serializable
data class NotificationHistoryItem(
    val title: String,
    val message: String,
    val timestamp: Long
)