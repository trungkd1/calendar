package com.trungkieu.mycalendar.models

data class DayYearly(var eventColors: HashSet<Int> = HashSet()) {
    fun addColor(color: Int) = eventColors.add(color)
}
