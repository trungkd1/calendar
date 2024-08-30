package com.trungkieu.mycalendar.interfaces

import android.util.SparseArray
import com.trungkieu.mycalendar.models.DayYearly
import java.util.ArrayList

interface YearlyCalendar {
    fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int)

}