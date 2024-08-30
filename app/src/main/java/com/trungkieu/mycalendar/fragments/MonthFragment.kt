package com.trungkieu.mycalendar.fragments

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.helpers.DAY_CODE
import com.trungkieu.mycalendar.MainActivity
import com.trungkieu.mycalendar.R
import com.trungkieu.mycalendar.databinding.FragmentDayBinding
import com.trungkieu.mycalendar.databinding.FragmentMonthBinding
import com.trungkieu.mycalendar.databinding.TopNavigationBinding
import com.trungkieu.mycalendar.extensions.config
import com.trungkieu.mycalendar.extensions.getViewBitmap
import com.trungkieu.mycalendar.extensions.printBitmap
import com.trungkieu.mycalendar.helper.Config
import com.trungkieu.mycalendar.helper.Formatter
import com.trungkieu.mycalendar.helper.MonthlyCalendarImpl
import com.trungkieu.mycalendar.interfaces.MonthlyCalendar
import com.trungkieu.mycalendar.interfaces.NavigationListener
import com.trungkieu.mycalendar.models.DayMonthly
import org.joda.time.DateTime

class MonthFragment : Fragment(), MonthlyCalendar {
    var mListener: NavigationListener ?= null
    private var mCalendar: MonthlyCalendarImpl? = null
    private var mTextColor = 0
    private var mLastHash = 0L

    private var mShowWeekNumbers = false
    private var mPackageName = ""
    private var mDayCode = ""
    private lateinit var mRes: Resources
    private lateinit var mConfig: Config
    private lateinit var binding: FragmentMonthBinding
    private lateinit var topNavigationBinding: TopNavigationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMonthBinding.inflate(inflater,container,false)
        topNavigationBinding = TopNavigationBinding.bind(binding.root)
        mRes = resources

        mPackageName = requireActivity().packageName
        mDayCode = requireArguments().getString(DAY_CODE)!!
        mConfig = requireContext().config
        storeStateVariables()

        setupButtons()
        mCalendar = MonthlyCalendarImpl(this, requireContext())

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (mConfig.showWeekNumbers != mShowWeekNumbers) {
            mLastHash = -1L
        }

        mCalendar!!.apply {
            mTargetDate = Formatter.getDateTimeFromCode(mDayCode)
            // đổ dữ liệu vào khung thời gian
            getDays(false)    // prefill the screen asap, even if without events
        }

        storeStateVariables()
        updateCalendar()
    }

    private fun storeStateVariables() {
        mConfig.apply {
            mShowWeekNumbers = showWeekNumbers
        }
    }

    override fun updateMonthlyCalendar(
        context: Context,
        month: String,
        days: ArrayList<DayMonthly>,
        checkedEvents: Boolean,
        currTargetDate: DateTime
    ) {
        val newHash = month.hashCode() + days.hashCode().toLong()
        if ((mLastHash != 0L && !checkedEvents) || mLastHash == newHash) {
            return
        }

        mLastHash = newHash
        activity?.runOnUiThread {
            topNavigationBinding.topValue.apply {
                text = month
                contentDescription = text

                if (activity != null) {
                    setTextColor(requireActivity().getProperTextColor())
                }
            }
            updateDays(days)

        }
    }

    private fun updateDays(days: ArrayList<DayMonthly>) {
        binding.monthViewWrapper.updateDays(days, true) {
            (activity as MainActivity).openDayFromMonthly(Formatter.getDateTimeFromCode(it.code))
        }
    }

    private fun setupButtons() {
        mTextColor = requireContext().getProperTextColor()

        topNavigationBinding.topLeftArrow.apply {
            applyColorFilter(mTextColor)
            background = context.getDrawable(com.simplemobiletools.commons.R.color.md_green_900)
            setOnClickListener {
                mListener?.goLeft()
            }

            val pointerLeft = requireContext().getDrawable(com.simplemobiletools.commons.R.drawable.ic_chevron_left_vector)
            pointerLeft?.isAutoMirrored = true
            setImageDrawable(pointerLeft)
        }

        topNavigationBinding.topRightArrow.apply {
            applyColorFilter(mTextColor)
            background = context.getDrawable(com.simplemobiletools.commons.R.color.md_green_900)
            setOnClickListener {
                mListener?.goRight()
            }

            val pointerRight = requireContext().getDrawable(com.simplemobiletools.commons.R.drawable.ic_chevron_right_vector)
            pointerRight?.isAutoMirrored = true
            setImageDrawable(pointerRight)
        }

        topNavigationBinding.topValue.apply {
            background = context.getDrawable(com.simplemobiletools.commons.R.color.md_green_900)
            setTextColor(requireContext().getProperTextColor())
            setOnClickListener {
                (activity as MainActivity).showGoToDateDialog()
            }
        }
    }

    fun updateCalendar() {

        mCalendar?.updateMonthlyCalendar(Formatter.getDateTimeFromCode(mDayCode))
    }

    fun printCurrentView() {
        topNavigationBinding.apply {
            topLeftArrow.beGone()
            topRightArrow.beGone()
            topValue.setTextColor(resources.getColor(com.simplemobiletools.commons.R.color.theme_light_text_color))
            binding.monthViewWrapper.togglePrintMode()

            requireContext().printBitmap(binding.monthCalendarHolder.getViewBitmap())

            topLeftArrow.beVisible()
            topRightArrow.beVisible()
            topValue.setTextColor(requireContext().getProperTextColor())
            binding.monthViewWrapper.togglePrintMode()
        }
    }


}