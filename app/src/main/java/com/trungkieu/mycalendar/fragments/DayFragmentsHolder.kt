package com.trungkieu.mycalendar.fragments

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.toggleAppIconColor
import com.simplemobiletools.commons.helpers.DAILY_VIEW
import com.simplemobiletools.commons.helpers.DAY_CODE
import com.trungkieu.mycalendar.MainActivity
import com.trungkieu.mycalendar.adapters.MyDayPagerAdapter

import com.trungkieu.mycalendar.databinding.FragmentDayHolderBinding
import com.trungkieu.mycalendar.helper.Formatter
import com.trungkieu.mycalendar.interfaces.NavigationListener
import org.joda.time.DateTime

class DayFragmentsHolder : MyFragmentHolder(), NavigationListener {
    private var defaultDailyPage = 0
    private val PREFILLED_DAYS = 251

    private lateinit var viewPager: ViewPager
    private var currentDayCode = ""
    private var todayDayCode = ""
    private var isGoToTodayVisible = false

    override val viewType = DAILY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDayCode = arguments?.getString(DAY_CODE) ?: ""
        todayDayCode = Formatter.getTodayCode()
        Log.e("getFragmentsHolder","DayFragmentsHolder")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentDayHolderBinding.inflate(inflater, container, false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        viewPager = binding.fragmentDaysViewpager
        viewPager.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return binding.root
    }

    private fun setupFragment() {
        val codes = getDays(currentDayCode)
        val dailyAdapter = MyDayPagerAdapter(requireActivity().supportFragmentManager, codes, this)
        defaultDailyPage = codes.size / 2

        viewPager.apply {
            adapter = dailyAdapter
            addOnAdapterChangeListener(object :  ViewPager.OnPageChangeListener,
                ViewPager.OnAdapterChangeListener {
                override fun onPageScrolled(
                    position: Int, positionOffset: Float, positionOffsetPixels: Int
                ) {
                    //TODO("Not yet implemented")
                }

                override fun onPageSelected(position: Int) {
                    currentDayCode = codes[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {
//                    TODO("Not yet implemented")
                }

                override fun onAdapterChanged(
                    viewPager: ViewPager, oldAdapter: PagerAdapter?, newAdapter: PagerAdapter?
                ) {
//                    TODO("Not yet implemented")
                }

            })
            currentItem = defaultDailyPage

        }

    }

    override fun goToToday() {
        currentDayCode = todayDayCode
        setupFragment()
    }


    override fun showGoToDateDialog() {
        TODO("Not yet implemented")
    }

    override fun refreshEvents() {
        (viewPager.adapter as? MyDayPagerAdapter)?.updateCalendars(viewPager.currentItem)
    }

    override fun shouldGoToTodayBeVisible(): Boolean {
        return currentDayCode != todayDayCode
    }

    override fun getNewEventDayCode(): String {
        return currentDayCode
    }

    override fun printView() {
        (viewPager.adapter as? MyDayPagerAdapter)?.printCurrentView(viewPager.currentItem)
    }

    override fun getCurrentDate(): DateTime? {
        if ( currentDayCode != "") {
            return Formatter.getDateTimeFromCode(currentDayCode)
        } else {
            return null
        }
    }

    private fun getDays(code: String): List<String> {
        val days = ArrayList<String>(PREFILLED_DAYS)
        val today = Formatter.getDateTimeFromCode(code)
        for (i in -PREFILLED_DAYS / 2..PREFILLED_DAYS / 2) {
            days.add(Formatter.getDayCodeFromDateTime(today.plusDays(i)))

        }
        return days
    }

    override fun goLeft() {
        viewPager.currentItem = viewPager.currentItem - 1
    }

    override fun goRight() {
        viewPager.currentItem = viewPager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        currentDayCode = Formatter.getDayCodeFromDateTime(dateTime)
        setupFragment()
    }
}