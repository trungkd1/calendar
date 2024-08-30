package com.trungkieu.mycalendar.fragments

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.*
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.onPageChangeListener
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.DAY_CODE
import com.simplemobiletools.commons.helpers.LOG_TAG
import com.simplemobiletools.commons.helpers.MONTHLY_VIEW
import com.simplemobiletools.commons.views.MyViewPager
import com.trungkieu.mycalendar.MainActivity
import com.trungkieu.mycalendar.adapters.MyMonthPagerAdapter
import com.trungkieu.mycalendar.databinding.FragmentMonthHolderBinding
import com.trungkieu.mycalendar.extensions.getMonthCode
import com.trungkieu.mycalendar.helper.Formatter
import com.trungkieu.mycalendar.interfaces.NavigationListener
import org.joda.time.DateTime

class MonthFragmentsHolder : MyFragmentHolder(), NavigationListener {
    private val PREFILLED_MONTHS = 251

    private lateinit var viewPager: MyViewPager
    private var defaultMonthlyPage = 0
    private var todayDayCode = ""
    private var currentDayCode = ""
    private var isGoToTodayVisible = false

    override val viewType = MONTHLY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDayCode = arguments?.getString(DAY_CODE) ?: ""
        todayDayCode = Formatter.getTodayCode()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMonthHolderBinding.inflate(inflater,container,false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        viewPager = binding.fragmentDaysViewpager
        /**
         * Các View trong mỗi fragment và activity đều là duy nhất, nên cần set lại id cho cáC VIEW sao cho không bị trùng lặp.
         * Nếu trùng lặp thì các view bên trong sẽ bị conflict và không thực thi được
         * */
        viewPager.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return binding.root
    }

    private fun setupFragment() {
        /**
         * currentDayCode là cái ngày tháng hiện tại, ví dụ hiện tại là ngày 27 tháng 10 năm 2023 thì currentDayCode = 20231027
         * từ ngày tháng hiện tại sẽ lấy codes tháng hiện tại là bắt đầu từ ngày :
         * */
        val codes = getMonths(currentDayCode)
        val adapter = MyMonthPagerAdapter(requireActivity().supportFragmentManager,codes,this)
        defaultMonthlyPage = codes.size / 2

        viewPager.apply {
            this.adapter = adapter
//            onPageChangeListener { position ->
//                currentDayCode = codes[position]
//                val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
//                if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
//                    (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
//                    isGoToTodayVisible = shouldGoToTodayBeVisible
//                }
//            }

            addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
//                    TODO("Not yet implemented")
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

            })
            currentItem = defaultMonthlyPage
        }
    }


    private fun getMonths(code: String): List<String> {
        /**
         * sẽ hiển thị được tổng cộng 251 tháng
         * */
        //Tạo array chứa 3 tháng , size tháng dựa vào PREFILLED_MONTHS, hiện tại PREFILLED_MONTHS =251, có nghĩa sẽ hiển thị được 251 tháng
        val months = ArrayList<String>(PREFILLED_MONTHS)
        // giả sử ngày hiện tại là  code = 20231027  thì dayOfThisMonth = 2023-10-01T00:00:00.000 sẽ làm mốc thời gian
        val timeStartThisMonth = Formatter.getDateTimeFromCode(code).withDayOfMonth(1)
        // Giả sử PREFILLED_MONTHS = 3 mà thời gian mặc định để lấy giá trị là timeStartThisMonth = 2023-10-01T00:00:00.000
        // thì value sẽ có giá trị lần lượt là 20230901 / 20231001 / 20231101
        for (i in -PREFILLED_MONTHS / 2..PREFILLED_MONTHS / 2) {
            val value = Formatter.getDayCodeFromDateTime(timeStartThisMonth.plusMonths(i))
            months.add(value)
        }
        return months
    }

    override fun goToToday() {
        currentDayCode = todayDayCode
        setupFragment()
    }

    override fun showGoToDateDialog() {
        if (activity == null) {
            return
        }

        val datePicker = getDatePickerView()

        datePicker.findViewById<View>(Resources.getSystem().getIdentifier("day", "id", "android")).beGone()

        val dateTime = getCurrentDate()!!
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        activity?.getAlertDialogBuilder()!!
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok) { _, _ -> datePicked(dateTime, datePicker) }
            .apply {
                activity?.setupDialogStuff(datePicker, this)
            }
    }

    private fun datePicked(dateTime: DateTime, datePicker: DatePicker) {
        val month = datePicker.month + 1
        val year = datePicker.year
        val newDateTime = dateTime.withDate(year, month, 1)
        goToDateTime(newDateTime)
    }

    override fun refreshEvents() {
        (viewPager.adapter as? MyMonthPagerAdapter)?.updateCalendars(viewPager.currentItem)
    }

    override fun shouldGoToTodayBeVisible(): Boolean {
        return currentDayCode.getMonthCode() != todayDayCode.getMonthCode()
    }

    override fun getNewEventDayCode(): String {
        return if (shouldGoToTodayBeVisible()) currentDayCode else todayDayCode
    }

    override fun printView() {
        (viewPager.adapter as? MyMonthPagerAdapter)?.printCurrentView(viewPager.currentItem)
    }

    override fun getCurrentDate(): DateTime? {
        return if (currentDayCode != "") {
            DateTime(Formatter.getDateTimeFromCode(currentDayCode).toString())
        } else {
            null
        }
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