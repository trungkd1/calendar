package com.trungkieu.mycalendar.helper

import android.content.Context
import android.util.SparseArray
import com.trungkieu.mycalendar.entity.EventEntity
import com.trungkieu.mycalendar.extensions.eventsHandler
import com.trungkieu.mycalendar.extensions.seconds
import com.trungkieu.mycalendar.interfaces.YearlyCalendar
import com.trungkieu.mycalendar.models.DayYearly
import org.joda.time.DateTime

class YearlyCalendarImpl(val callback: YearlyCalendar, val context: Context, val year: Int) {
    fun getEvents(year: Int) {
        val startDateTime = DateTime().withTime(0, 0, 0, 0).withDate(year, 1, 1)
        val startTS = startDateTime.seconds()
        val endTS = startDateTime.plusYears(1).minusSeconds(1).seconds()
        context.eventsHandler.getEvents(startTS, endTS) {
            gotEvents(it)
        }
    }

    private fun gotEvents(events: MutableList<EventEntity>) {
        val arr = SparseArray<ArrayList<DayYearly>>(12)

        events.forEach {
            val startDateTime = Formatter.getDateTimeFromTS(it.startTS)
            markDay(arr, startDateTime, it)

            val startCode = Formatter.getDayCodeFromDateTime(startDateTime)
            val endDateTime = Formatter.getDateTimeFromTS(it.endTS)
            val endCode = Formatter.getDayCodeFromDateTime(endDateTime)
            if (startCode != endCode) {
                var currDateTime = startDateTime
                while (Formatter.getDayCodeFromDateTime(currDateTime) != endCode) {
                    currDateTime = currDateTime.plusDays(1)
                    markDay(arr, currDateTime, it)
                }
            }
        }
        callback.updateYearlyCalendar(arr, events.hashCode())
    }

    private fun markDay(arr: SparseArray<ArrayList<DayYearly>>, dateTime: DateTime, event: EventEntity) {
        val month = dateTime.monthOfYear
        val day = dateTime.dayOfMonth

        if (arr[month] == null) {
            arr.put(month, ArrayList())
            for (i in 1..32)
                arr.get(month).add(DayYearly())
        }

        if (dateTime.year == year) {
            arr.get(month)[day].addColor(event.color)
        }
    }
}