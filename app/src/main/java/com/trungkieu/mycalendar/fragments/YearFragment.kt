package com.trungkieu.mycalendar.fragments

import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.YEAR_LABEL
import com.trungkieu.mycalendar.MainActivity
import com.trungkieu.mycalendar.R
import com.trungkieu.mycalendar.databinding.FragmentYearBinding
import com.trungkieu.mycalendar.databinding.SmallMonthViewHolderBinding
import com.trungkieu.mycalendar.databinding.TopNavigationBinding
import com.trungkieu.mycalendar.extensions.getProperDayIndexInWeek
import com.trungkieu.mycalendar.extensions.getViewBitmap
import com.trungkieu.mycalendar.extensions.printBitmap
import com.trungkieu.mycalendar.helper.YearlyCalendarImpl
import com.trungkieu.mycalendar.interfaces.NavigationListener
import com.trungkieu.mycalendar.interfaces.YearlyCalendar
import com.trungkieu.mycalendar.models.DayYearly
import org.joda.time.DateTime
import java.util.ArrayList

class YearFragment : Fragment(), YearlyCalendar {

    private var mYears = 0
    private var mFirstDayOfWeek = 0
    private var isPrintVersion = false
    private var lastHash = 0

    private lateinit var binding: FragmentYearBinding
    private lateinit var topNavigationBinding: TopNavigationBinding
    private lateinit var listMonthHolders: List<SmallMonthViewHolderBinding>
    private var mCalendar: YearlyCalendarImpl? = null

    var mListener: NavigationListener? = null



    private val monthResIds = arrayOf(
        com.simplemobiletools.commons.R.string.january,
        com.simplemobiletools.commons.R.string.february,
        com.simplemobiletools.commons.R.string.march,
        com.simplemobiletools.commons.R.string.april,
        com.simplemobiletools.commons.R.string.may,
        com.simplemobiletools.commons.R.string.june,
        com.simplemobiletools.commons.R.string.july,
        com.simplemobiletools.commons.R.string.august,
        com.simplemobiletools.commons.R.string.september,
        com.simplemobiletools.commons.R.string.october,
        com.simplemobiletools.commons.R.string.november,
        com.simplemobiletools.commons.R.string.december
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentYearBinding.inflate(inflater,container,false)
        topNavigationBinding = TopNavigationBinding.bind(binding.root)
        listMonthHolders = arrayListOf(
            binding.month1Holder, binding.month2Holder, binding.month3Holder, binding.month4Holder, binding.month5Holder, binding.month6Holder,
            binding.month7Holder, binding.month8Holder, binding.month9Holder, binding.month10Holder, binding.month11Holder, binding.month12Holder
        ).apply {
            forEachIndexed { index, smallMonthViewHolderBinding ->
                smallMonthViewHolderBinding.monthLabel.text = getString(monthResIds[index])
//                smallMonthViewHolderBinding.monthLabel.setTextColor(requireContext().getColor(com.simplemobiletools.commons.R.color.md_green))

            }
        }
        mYears = requireArguments().getInt(YEAR_LABEL)
        requireContext().updateTextColors(binding.calendarWrapper)
        setupMonths()
        setupButtons()

        mCalendar = YearlyCalendarImpl(this, requireContext(), mYears)

        return binding.root
    }

    private fun setupButtons() {
        val textColor = requireContext().getProperTextColor()
        topNavigationBinding.topLeftArrow.apply {
            applyColorFilter(textColor)
            background = null
            setOnClickListener {
                mListener?.goLeft()
            }

            val pointerLeft = requireContext().getDrawable(com.simplemobiletools.commons.R.drawable.ic_chevron_left_vector)
            pointerLeft?.isAutoMirrored = true
            setImageDrawable(pointerLeft)
        }

        topNavigationBinding.topRightArrow.apply {
            applyColorFilter(textColor)
            background = null
            setOnClickListener {
                mListener?.goRight()
            }

            val pointerRight = requireContext().getDrawable(com.simplemobiletools.commons.R.drawable.ic_chevron_right_vector)
            pointerRight?.isAutoMirrored = true
            setImageDrawable(pointerRight)
        }

        topNavigationBinding.topValue.apply {
            setTextColor(requireContext().getProperTextColor())
            setOnClickListener {
                (activity as MainActivity).showGoToDateDialog()
            }
            topNavigationBinding.topValue.text = mYears.toString()
        }
    }

    private fun setupMonths() {
        val dateTime = DateTime().withYear(mYears).withHourOfDay(12)
        listMonthHolders.forEachIndexed { index, monthHolder ->
            val monthOfYear = index + 1
            val monthView = monthHolder.smallMonthView
            // Màu sắc của các text như January, Feruary...
            val curTextColor = when {
                isPrintVersion -> resources.getColor(com.simplemobiletools.commons.R.color.md_red)
//                else -> requireContext().getProperTextColor()
                /**RPR*/
                else -> requireContext().getColor(com.simplemobiletools.commons.R.color.md_green)

            }
            /**Day la cho set  cho cac thang trong nam*/
            monthHolder.monthLabel.setTextColor(curTextColor)
            monthView.firstDay = requireContext().getProperDayIndexInWeek(dateTime.withMonthOfYear(monthOfYear))
            val numberOfDays = dateTime.withMonthOfYear(monthOfYear).dayOfMonth().maximumValue
            monthView.setDays(numberOfDays)
            monthView.setOnClickListener {
                (activity as MainActivity).openMonthFromYearly(DateTime().withDate(mYears, monthOfYear, 1))
            }
        }

        if (!isPrintVersion) {
            val now = DateTime()
            markCurrentMonth(now)
        }

    }

    fun updateCalendar() {
        mCalendar?.getEvents(mYears)
    }

    private fun markCurrentMonth(now: DateTime) {
        if (now.year == mYears) {
            val monthOfYear = now.monthOfYear
            val monthHolder = listMonthHolders[monthOfYear - 1]
            monthHolder.monthLabel.setTextColor(requireContext().getProperPrimaryColor())
            monthHolder.smallMonthView.todaysId = now.dayOfMonth
        }
    }

    override fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int) {
        if (!isAdded) {
            return
        }

        if (hashCode == lastHash) {
            return
        }

        lastHash = hashCode
        listMonthHolders.forEachIndexed { index, monthHolder ->
            val monthView = monthHolder.smallMonthView
            val monthOfYear = index + 1
            monthView.setEvents(events.get(monthOfYear))
        }

        topNavigationBinding.topValue.post {
            topNavigationBinding.topValue.text = mYears.toString()
        }
    }

    fun printCurrentView() {
        isPrintVersion = true
        setupMonths()
        toggleSmallMonthPrintModes()

        requireContext().printBitmap(binding.calendarWrapper.getViewBitmap())

        isPrintVersion = false
        setupMonths()
        toggleSmallMonthPrintModes()
    }

    private fun toggleSmallMonthPrintModes() {
        listMonthHolders.forEach {
            it.smallMonthView.togglePrintMode()
        }
    }
}