package com.trungkieu.mycalendar.fragments

import android.graphics.Color
import android.widget.DatePicker
import androidx.fragment.app.Fragment
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.trungkieu.mycalendar.databinding.DatePickerDarkBinding
import org.joda.time.DateTime

abstract class MyFragmentHolder : Fragment() {
    abstract  val viewType: Int

    abstract fun goToToday()

    abstract fun showGoToDateDialog()

    abstract fun refreshEvents()

    abstract fun shouldGoToTodayBeVisible(): Boolean

    abstract fun getNewEventDayCode(): String

    abstract fun printView()

    abstract fun getCurrentDate(): DateTime?

    fun getDatePickerView(): DatePicker {
        return if (requireActivity().getProperBackgroundColor().getContrastColor() == Color.WHITE) {
            DatePickerDarkBinding.inflate(layoutInflater).datePicker
        } else {
            DatePickerDarkBinding.inflate(layoutInflater).datePicker
        }
    }
}