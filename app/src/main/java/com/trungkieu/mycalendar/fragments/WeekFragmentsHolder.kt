package com.trungkieu.mycalendar.fragments

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.extensions.onSeekBarChangeListener
import com.simplemobiletools.commons.helpers.DAILY_VIEW
import com.simplemobiletools.commons.helpers.DAY_CODE
import com.simplemobiletools.commons.helpers.WEEK_START_DATE_TIME
import com.trungkieu.mycalendar.databinding.FragmentDayHolderBinding
import com.trungkieu.mycalendar.databinding.FragmentWeekHolderBinding
import com.trungkieu.mycalendar.databinding.WeeklyViewHourTextviewBinding
import com.trungkieu.mycalendar.extensions.config
import com.trungkieu.mycalendar.extensions.getFirstDayOfWeek
import com.trungkieu.mycalendar.extensions.getWeeklyViewItemHeight
import com.trungkieu.mycalendar.extensions.seconds
import com.trungkieu.mycalendar.helper.Formatter
import com.trungkieu.mycalendar.interfaces.WeekFragmentListener
import org.joda.time.DateTime

class WeekFragmentsHolder : MyFragmentHolder(), WeekFragmentListener {

    private val PREFILLED_DAYS = 151
    private val MAX_SEEKBAR_VALUE = 14


    private lateinit var binding: FragmentWeekHolderBinding
    private lateinit var viewPager: ViewPager
    private var currentWeekCode = 0L
    private var thisWeekTS = 0L


    private var todayDayCode = ""


    override val viewType = DAILY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateTimeString = arguments?.getString(WEEK_START_DATE_TIME) ?: return
        currentWeekCode = (DateTime.parse(dateTimeString) ?: DateTime()).seconds()
        thisWeekTS = DateTime.parse(requireContext().getFirstDayOfWeek(DateTime())).seconds()
        Log.e("getFragmentsHolder", "WeekFragmentsHolder")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val textColor = requireContext().getProperTextColor()
        binding = FragmentWeekHolderBinding.inflate(inflater, container, false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        binding.weekViewMonthLabel.apply {
            setTextColor(textColor)
        }
        binding.weekViewWeekNumber.apply {
            setTextColor(textColor)
        }

        val itemHeight = requireContext().getWeeklyViewItemHeight().toInt()
        binding.weekViewHoursHolder.setPadding(0,0,0,itemHeight)

        viewPager = binding.weekViewViewPager
        viewPager.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return binding.root
    }

    private fun setupFragment() {
        addHours()
//        setupWeeklyViewPager()

        binding.weekViewHoursScrollview.apply {
            setOnTouchListener { view, motionEvent -> true }
        }

        binding.weekViewSeekbar.apply {
            progress = context?.config?.weeklyViewDays ?: 7
            max = MAX_SEEKBAR_VALUE

            onSeekBarChangeListener {it
                if (it == 0) {
                    progress = 1
                }
                updateWeeklyViewDays(progress)
            }
        }

        setupWeeklyActionbarTitle(currentWeekCode)
    }

    private fun setupWeeklyActionbarTitle(timestamp: Long) {
        val startDateTime = Formatter.getDateTimeFromTS(timestamp)
        val month = Formatter.getShortMonthName(requireContext(), startDateTime.monthOfYear)
        binding.weekViewMonthLabel.text = month
        val weekNumber = startDateTime.plusDays(3).weekOfWeekyear
        binding.weekViewWeekNumber.text = "${getString(com.simplemobiletools.commons.R.string.week_number_short)} $weekNumber"
    }

    private fun updateWeeklyViewDays(days: Int) {
        requireContext().config.weeklyViewDays = days
        updateDaysCount(days)
        setupWeeklyViewPager()
    }

    private fun updateDaysCount(count: Int) {
        binding.weekViewDaysCount.text = requireContext().resources.getQuantityString(com.simplemobiletools.commons.R.plurals.days, count, count)
    }

    private fun addHours(textColor: Int = requireContext().getProperTextColor()) {
        var itemheight = requireContext().getWeeklyViewItemHeight().toInt()
        binding.weekViewHoursHolder.removeAllViews()
        val hourDateTime = DateTime().withDate(2000, 1, 1).withTime(0, 0, 0, 0)
        for (i in 1..23) {
            val formattedHours = Formatter.getTime(requireContext(), hourDateTime.withHourOfDay(i))
            WeeklyViewHourTextviewBinding.inflate(layoutInflater).root.apply {
                text = formattedHours
                setTextColor(textColor)
                height = itemheight
                binding.weekViewHoursHolder.addView(this)
            }
        }
    }

    private fun setupWeeklyViewPager() {
        TODO("Not yet implemented")
    }

    override fun goToToday() {
        TODO("Not yet implemented")
    }

    override fun showGoToDateDialog() {
        TODO("Not yet implemented")
    }

    override fun refreshEvents() {
        TODO("Not yet implemented")
    }

    override fun shouldGoToTodayBeVisible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getNewEventDayCode(): String {
        TODO("Not yet implemented")
    }

    override fun printView() {
        TODO("Not yet implemented")
    }

    override fun getCurrentDate(): DateTime? {
        TODO("Not yet implemented")
    }

    private fun getDays(code: String): List<String> {
        val days = ArrayList<String>(PREFILLED_DAYS)
        val today = Formatter.getDateTimeFromCode(code)
        for (i in -PREFILLED_DAYS / 2..PREFILLED_DAYS / 2) {
            days.add(Formatter.getDayCodeFromDateTime(today.plusDays(i)))

        }
        return days
    }

    override fun scrollTo(y: Int) {
        TODO("Not yet implemented")
    }

    override fun updateHoursTopMargin(margin: Int) {
        TODO("Not yet implemented")
    }

    override fun getCurrScrollY(): Int {
        TODO("Not yet implemented")
    }

    override fun updateRowHeight(rowHeight: Int) {
        TODO("Not yet implemented")
    }

    override fun getFullFragmentHeight(): Int {
        TODO("Not yet implemented")
    }
}