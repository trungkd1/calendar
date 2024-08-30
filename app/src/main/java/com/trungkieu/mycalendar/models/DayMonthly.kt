package com.trungkieu.mycalendar.models

import com.trungkieu.mycalendar.entity.EventEntity

data class DayMonthly(
    val value: Int,
    val isThisMonth: Boolean,
    val isToday: Boolean,
    val code: String,
    val weekOfYear: Int,
    var dayEvents: ArrayList<EventEntity>,
    var indexOnMonthView: Int,
    var isWeekend: Boolean
)
