package com.trungkieu.mycalendar.helper

import com.trungkieu.mycalendar.models.MyTimeZone

fun getAllTimeZones() = arrayListOf(
    MyTimeZone("GMT-12", "Etc/GMT+12"),
    MyTimeZone("GMT-11", "Etc/GMT+11")
)