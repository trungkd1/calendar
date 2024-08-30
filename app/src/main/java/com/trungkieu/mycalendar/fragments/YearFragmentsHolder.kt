package com.trungkieu.mycalendar.fragments

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.onPageChangeListener
import com.simplemobiletools.commons.helpers.LOG_TAG
import com.simplemobiletools.commons.helpers.YEARLY_VIEW
import com.simplemobiletools.commons.helpers.YEAR_TO_OPEN
import com.simplemobiletools.commons.views.MyViewPager
import com.trungkieu.mycalendar.MainActivity
import com.trungkieu.mycalendar.adapters.MyYearPagerAdapter
import com.trungkieu.mycalendar.databinding.FragmentYearsHolderBinding
import com.trungkieu.mycalendar.helper.Formatter
import com.trungkieu.mycalendar.interfaces.NavigationListener
import org.joda.time.DateTime

class YearFragmentsHolder : MyFragmentHolder(), NavigationListener {

    private var currentYear = 0
    private var todayYear = 0
    private var defaultYearlyPage = 0
    private var isGoToTodayVisible = false


    private val PREFILLED_YEARS = 61

    private lateinit var viewPager: MyViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateTimeString = arguments?.getString(YEAR_TO_OPEN)
        currentYear =
            (if (dateTimeString != null) DateTime.parse(dateTimeString) else DateTime()).toString(
                Formatter.YEAR_PATTERN
            ).toInt()
        todayYear = DateTime().toString(Formatter.YEAR_PATTERN).toInt()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentYearsHolderBinding.inflate(inflater, container, false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        viewPager = binding.fragmentYearsViewpager
        viewPager.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return binding.root
    }

    private fun setupFragment() {
        val years = getYears(currentYear)
        val adapter = MyYearPagerAdapter(requireActivity().supportFragmentManager, years, this)
        defaultYearlyPage = years.size / 2

        viewPager.apply {
            this.adapter = adapter
            onPageChangeListener { position ->
                currentYear = years[position]
                val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                    (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                    isGoToTodayVisible = shouldGoToTodayBeVisible
                }
            }
            currentItem = defaultYearlyPage
        }
    }

    private fun getYears(currentYear: Int): List<Int> {
        val years = ArrayList<Int>(PREFILLED_YEARS)
        for (i in currentYear - PREFILLED_YEARS / 2..currentYear + PREFILLED_YEARS / 2) {
            years.add(i)
        }
        return years
    }


    override val viewType: Int
        get() = YEARLY_VIEW

    override fun goToToday() {
        currentYear = todayYear
        setupFragment()
    }

    override fun showGoToDateDialog() {
//        TODO("Not yet implemented")
    }

    override fun refreshEvents() {
//        TODO("Not yet implemented")
    }

    override fun shouldGoToTodayBeVisible(): Boolean {
        return currentYear != todayYear
    }

    override fun getNewEventDayCode(): String {
        return Formatter.getTodayCode()
    }

    override fun printView() {
        (viewPager.adapter as? MyYearPagerAdapter)?.printCurrentView(viewPager.currentItem)
    }

    override fun getCurrentDate(): DateTime? {
        return null
    }

    override fun goLeft() {
        viewPager.currentItem = viewPager.currentItem - 1
    }

    override fun goRight() {
        viewPager.currentItem = viewPager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
//        TODO("Not yet implemented")
        Log.e(LOG_TAG,"goToDateTime")
    }
}