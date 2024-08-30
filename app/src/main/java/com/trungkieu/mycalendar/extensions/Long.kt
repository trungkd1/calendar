package com.trungkieu.mycalendar.extensions

import com.trungkieu.mycalendar.entity.EventEntity
import com.trungkieu.mycalendar.helper.Formatter


fun Long.isTsOnProperDay(event: EventEntity): Boolean {
    val dateTime = Formatter.getDateTimeFromTS(this)
    val power = Math.pow(2.0, (dateTime.dayOfWeek - 1).toDouble()).toInt()
    return event.repeatRule and power != 0
}
