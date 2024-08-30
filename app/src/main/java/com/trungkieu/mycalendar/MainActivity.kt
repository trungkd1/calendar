package com.trungkieu.mycalendar

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.helpers.*
import com.trungkieu.mycalendar.helper.*
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.appLaunched
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.fadeIn
import com.simplemobiletools.commons.extensions.fadeOut
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.extensions.getThemeId
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.isVisible
import com.simplemobiletools.commons.extensions.launchActivityIntent
import com.simplemobiletools.commons.extensions.launchMoreAppsFromUsIntent
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.commons.models.RadioItem
import com.trungkieu.mycalendar.databinding.MainActivityLayoutBinding
import com.trungkieu.mycalendar.entity.EventEntity
import com.trungkieu.mycalendar.extensions.config
import com.trungkieu.mycalendar.extensions.getFirstDayOfWeek
import com.trungkieu.mycalendar.extensions.startNewTaskIntent
import com.trungkieu.mycalendar.fragments.DayFragmentsHolder
import com.trungkieu.mycalendar.fragments.EventListFragment
import com.trungkieu.mycalendar.fragments.MonthFragmentsHolder
import com.trungkieu.mycalendar.fragments.MyFragmentHolder
import com.trungkieu.mycalendar.fragments.WeekFragmentsHolder
import com.trungkieu.mycalendar.fragments.YearFragmentsHolder
import com.trungkieu.mycalendar.models.ListItem
import com.trungkieu.mycalendar.ui.theme.MyCalendarTheme
import io.reactivex.android.BuildConfig
import org.joda.time.DateTime

/**
 * Main activity
 *
 * @constructor Create empty Main activity
 */
class MainActivity : BaseSimpleActivity() {

    private var listFragments = ArrayList<MyFragmentHolder>()

    private val binding by viewBinding(MainActivityLayoutBinding::inflate)

    private var showCalDAVRefreshToast = false
    private var mShouldFilterBeVisible = false
    private var mLatestSearchQuery = ""
    private var shouldGoToTodayBeVisible = false
    private var goToTodayButton: MenuItem? = null
    private var currentFragments = java.util.ArrayList<MyFragmentHolder>()
    private var eventTypesToExport = java.util.ArrayList<Long>()


    private var mStoredPrimaryColor = 0
    private var mStoredBackgroundColor = 0
    private var mStoredTextColor = 0
    private var mStoredDayCode = ""

    private var minFetchedSearchTS = 0L
    private var maxFetchedSearchTS = 0L
    private var searchResultEvents = ArrayList<EventEntity>()
    private var bottomItemAtRefresh: ListItem? = null

    override fun getAppIconIDs(): ArrayList<Int> {
        return arrayListOf(

        )
    }

    override fun getAppLauncherName(): String {
        return getString(R.string.app_launcher_name)
    }

    /**
     * On create
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set showTransparentTop is true, I will remove
//        setTheme(getThemeId(showTransparentTop = true))

        setContentView(binding.root)
        appLaunched((BuildConfig.APPLICATION_ID))
        val testCorotines =
        setupOptionMenu()
        refreshMenuItems()

        // Setup a transparent of navigation bar
//        updateMaterialActivityViews(binding.mainCoordinator,binding.mainHolder, true, false)
        binding.btnCalendarFab.beVisibleIf(config.storedView != YEARLY_VIEW && config.storedView != WEEKLY_VIEW)
        binding.apply {
            btnCalendarFab.setOnClickListener {
                if (binding.fabExtendedOverlay.isVisible()) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        hideExtendedFab()
                    }, 300)
                } else {
                    showExtendedFab()
                }

            }
            fabEventLabel.setOnClickListener { openNewEvent() }
            fabTaskIcon.setOnClickListener {
                openNewTask()
                Handler(Looper.getMainLooper()).postDelayed({
                    hideExtendedFab()
                }, 300)
            }
            fabTaskLabel.setOnClickListener {
                openNewTask()
                Handler(Looper.getMainLooper()).postDelayed({
                    hideExtendedFab()
                }, 300)
            }
        }
        updateViewPager()
    }

    override fun onResume() {
        super.onResume()

        binding.apply {

            fabExtendedOverlay.background =
                ColorDrawable(getProperBackgroundColor().adjustAlpha(0.8f))
            fabEventLabel.setTextColor(getProperTextColor())
            fabTaskLabel.setTextColor(getProperTextColor())
            fabTaskIcon.drawable.applyColorFilter(mStoredPrimaryColor.getContrastColor())
            fabTaskIcon.background.applyColorFilter(mStoredPrimaryColor)
        }

    }

    private fun storeStateVariables() {
        mStoredTextColor = getProperTextColor()
        mStoredPrimaryColor = getProperPrimaryColor()
        mStoredBackgroundColor = getProperBackgroundColor()
        config.apply {
//            mStoredFirstDayOfWeek = firstDayOfWeek
//            mStoredUse24HourFormat = use24HourFormat
//            mStoredDimPastEvents = dimPastEvents
//            mStoredDimCompletedTasks = dimCompletedTasks
//            mStoredHighlightWeekends = highlightWeekends
//            mStoredHighlightWeekendsColor = highlightWeekendsColor
//            mStoredMidnightSpan = showMidnightSpanningEventsAtTop
//            mStoredStartWeekWithCurrentDay = startWeekWithCurrentDay
        }
        mStoredDayCode = Formatter.getTodayCode()
    }


    private fun showExtendedFab() {
//        animateFabIcon(false)

        binding.apply {
            arrayOf(fabEventLabel, fabExtendedOverlay, fabTaskIcon, fabTaskLabel).forEach {
                it.fadeIn()
            }
            /** cách viết ở dưới có thể tóm gọn bằng cách viết khác */
//            fabEventLabel.fadeIn()
//            fabExtendedOverlay.fadeIn()
//            fabTaskIcon.fadeIn()
//            fabTaskLabel.fadeIn()


        }
    }


    private fun hideExtendedFab() {
        binding.apply {
            arrayOf(fabEventLabel, fabExtendedOverlay, fabTaskIcon, fabTaskLabel).forEach {
                it.fadeOut()
            }
        }
    }


//    override fun getAppIconIDs() = arrayListOf(
//        R.mipmap.ic_launcher
//    )
//
//    override fun getAppLauncherName() = getString(R.string.app_launcher_name)


    /** Setup option menu */
    private fun setupOptionMenu() = binding.apply {
        mainMenu.getToolbar().inflateMenu(R.menu.menu_main)
        mainMenu.toggleHideOnScroll(false)
        mainMenu.setupMenu()
        mainMenu.updateColors()
        mainMenu.onSearchTextChangedListener = { text ->
            searchQueryChanged(text)
        }

        mainMenu.getToolbar().setOnMenuItemClickListener { menuItem ->
            if (fabExtendedOverlay.isVisible()) {
                hideExtendedFab()
            }

            when (menuItem.itemId) {
                R.id.change_view -> showViewDialog()
                R.id.go_to_today -> goToToday()
                R.id.go_to_date -> showGoToDateDialog()
                R.id.print -> printView()
                R.id.filter -> showFilterDialog()
                R.id.refresh_caldav_calendars -> refreshCalDaVCalendar()
                R.id.add_holidays -> addHolidays()
                R.id.add_birthdays -> addBirthdays()
                R.id.add_anniversaries -> tryAddAniversaries()
                R.id.import_events -> tryImportEvents()
                R.id.export_events -> tryExportEvents()
                R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
                R.id.settings -> launchSettings()
                R.id.about -> launchAbout()
                else -> false
            }
            true
        }
    }


    private fun launchAbout() {
        TODO("Not yet implemented")
    }

    private fun launchSettings() {
        TODO("Not yet implemented")
    }

    private fun tryExportEvents() {
        TODO("Not yet implemented")
    }

    private fun tryImportEvents() {
        TODO("Not yet implemented")
    }

    private fun tryAddAniversaries() {
        TODO("Not yet implemented")
    }

    private fun addBirthdays() {
        TODO("Not yet implemented")
    }

    private fun addHolidays() {
        TODO("Not yet implemented")
    }

    private fun refreshCalDaVCalendar() {
        TODO("Not yet implemented")
    }

    private fun showFilterDialog() {
        TODO("Not yet implemented")
    }

    private fun printView() {
        TODO("Not yet implemented")
    }

    fun openMonthFromYearly(dateTime: DateTime) {
        if (listFragments.last() is MonthFragmentsHolder) {
            return
        }

        val fragment = MonthFragmentsHolder()
        listFragments.add(fragment)
        val bundle = Bundle()
        bundle.putString(DAY_CODE, Formatter.getDayCodeFromDateTime(dateTime))
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        resetActionBarTitle()
        binding.btnCalendarFab.beVisible()
        showBackNavigationArrow()
        config.storedView = MONTHLY_VIEW
    }

    private fun showBackNavigationArrow() {
        // TODO("Not yet implemented")
    }


    fun showGoToDateDialog() {
//        currentFragments.last().showGoToDateDialog()
    }

    /** Show view dialog when click menu Go to today */
    private fun goToToday() {
        TODO("Not yet implemented")
    }

    /** Show view dialog when click button menu */
    private fun showViewDialog() {
        var items = arrayListOf(
            RadioItem(DAILY_VIEW, getString(R.string.daily_view)),
            RadioItem(WEEKLY_VIEW, getString(R.string.weekly_view)),
            RadioItem(MONTHLY_VIEW, getString(R.string.monthly_view)),
            RadioItem(MONTHLY_DAILY_VIEW, getString(R.string.monthly_daily_view)),
            RadioItem(YEARLY_VIEW, getString(R.string.yearly_view)),
            RadioItem(EVENTS_LIST_VIEW, getString(R.string.simple_event_list))
        )

        /** Show view dialog when click button menu */
        RadioGroupDialog(this, items, config.storedView) {
            Log.e(LOG_TAG, "showViewDialog : $it")
            resetActionBarTitle()
            closeSearch()
            updateView(it as Int)
        }
    }

    private fun updateView(view: Int) {
        binding.btnCalendarFab.beVisibleIf(view != YEARLY_VIEW && view != WEEKLY_VIEW)
        val dateCode = getDateCodeToDIsplay(view)
        config.storedView = view
        checkSwipeRefreshAvailability()
        updateViewPager(dateCode)
        if (goToTodayButton?.isVisible == true) {
            shouldGoToTodayBeVisible = false
            refreshMenuItems()
        }
    }

    private fun getDateCodeToDIsplay(newView: Int): String? {
        val fragment = listFragments.last()
        val currentView = fragment.viewType
        if (newView == EVENTS_LIST_VIEW || currentView == EVENTS_LIST_VIEW) {
            return null
        }

        val fragmentCurrentDate = fragment.getCurrentDate()
        val viewOrder = arrayListOf(DAILY_VIEW, WEEKLY_VIEW, MONTHLY_VIEW, YEARLY_VIEW)
        val currentViewIndex =
            viewOrder.indexOf(if (currentView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else currentView)
        val newViewIndex =
            viewOrder.indexOf(if (newView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else newView)

        return if (fragmentCurrentDate != null && currentViewIndex <= newViewIndex) {
            getDateCodeFormatForView(newView, fragmentCurrentDate)
        } else {
            getDateCodeFormatForView(newView, DateTime())
        }
    }

    private fun getDateCodeFormatForView(view: Int, date: DateTime): String {
        return when (view) {
            WEEKLY_VIEW -> getFirstDayOfWeek(date)
            YEARLY_VIEW -> date.toString()
            else -> Formatter.getDayCodeFromDateTime(date)
        }
    }

    private fun checkSwipeRefreshAvailability() {
        binding.swipeRefreshLayout.isEnabled =
            config.caldavSync && config.pullToRefresh && config.storedView != WEEKLY_VIEW
        if (!binding.swipeRefreshLayout.isEnabled) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun closeSearch() {
        binding.mainMenu.closeSearch()
        minFetchedSearchTS = 0L
        maxFetchedSearchTS = 0L
        searchResultEvents.clear()
        bottomItemAtRefresh = null
    }


    /**
     * Reset action bar title có nghĩa là settext lại hint trong title là từ
     * Search
     */
    private fun resetActionBarTitle() {
//        binding.mainMenu.updateHintText(getString(com.simplemobiletools.commons.R.string.cancel))
    }

    private fun openNewTask() {
        hideKeyboard()
        val fragment = listFragments.last()
        startNewTaskIntent()
    }

    private fun openNewEvent() {
        TODO("Not yet implemented")
    }

    /**
     * Search query changed
     *
     * @param text
     */
    private fun searchQueryChanged(text: String) {

    }

    /** Refresh menu items fdfdfd fdjtrutrngf dfgdfsdfkrtrk dfdfd */
    fun refreshMenuItems() {
        if (binding.fabExtendedOverlay.isVisible()) {
            hideExtendedFab()
        }

        shouldGoToTodayBeVisible =
            currentFragments.lastOrNull()?.shouldGoToTodayBeVisible() ?: false
        binding.mainMenu.getToolbar().menu.apply {
            goToTodayButton = findItem(R.id.go_to_today)
            findItem(R.id.print).isVisible = config.storedView != MONTHLY_DAILY_VIEW
            findItem(R.id.filter).isVisible = mShouldFilterBeVisible
            findItem(R.id.go_to_today).isVisible =
                shouldGoToTodayBeVisible && !binding.mainMenu.isSearchOpen
            findItem(R.id.go_to_date).isVisible = config.storedView != EVENTS_LIST_VIEW
            findItem(R.id.refresh_caldav_calendars).isVisible = config.caldavSync
            findItem(R.id.more_apps_from_us).isVisible =
                !resources.getBoolean(com.simplemobiletools.commons.R.bool.hide_google_relations)
        }
    }

    fun openDayFromMonthly(dateTime: DateTime) {
        Log.e(LOG_TAG, "openDayFromMonthly " + dateTime.toString())
        if (currentFragments == null) {
            return
        }

        val fragment = DayFragmentsHolder()
        currentFragments.add(fragment)
        val bundle = Bundle()
        val dayCode = Formatter.getDayCodeFromDateTime(dateTime)
        Log.e(LOG_TAG, "openDayFromMonthly dayCode : " + dayCode)

        bundle.putString(DAY_CODE, dayCode)
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        showBackNavigationArrow()
        /**
         * Lưu kiểu màn hình vỉew hiện tại vào config,
         *
         * */
        config.storedView = DAILY_VIEW
    }

    fun refreshItems() {
        refreshViewPager()
    }


    private fun refreshViewPager() {
        runOnUiThread {
            if (!isDestroyed) {
                currentFragments.last().refreshEvents()
            }
        }
    }

    /**
     * Update view pager VIew hiển thị ngày, tháng, năm bên dưới khung slide
     * right và left,
     *
     * @param dayCode
     */
    private fun updateViewPager(dayCode: String? = null) {
        val fragement = getFragmentsHolder()
        listFragments.forEach {
            try {
                supportFragmentManager.beginTransaction().remove(it)
            } catch (ex: Exception) {
                return
            }
        }

        Log.e(LOG_TAG, "updateViewPager :: Formatter.getTodayCode() : " + Formatter.getTodayCode())
        listFragments.clear()
        listFragments.add(fragement)
        val bundle = Bundle()
//        val fixedDayCode = dayCode


        val fixedDayCode = fixDayCode(dayCode)

        when (config.storedView) {
            DAILY_VIEW -> bundle.putString(DAY_CODE, fixedDayCode ?: Formatter.getTodayCode())
            WEEKLY_VIEW -> bundle.putString(
                WEEK_START_DATE_TIME, fixedDayCode ?: getFirstDayOfWeek(
                    DateTime()
                )
            )

            MONTHLY_VIEW, MONTHLY_DAILY_VIEW -> bundle.putString(
                DAY_CODE,
                fixedDayCode ?: Formatter.getTodayCode()
            )

            YEARLY_VIEW -> bundle.putString(YEAR_TO_OPEN, fixedDayCode)
        }

        fragement.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragement).commitNow()
        binding.mainMenu.toggleForceArrowBackIcon(false)
    }

    private fun fixDayCode(dayCode: String? = null): String? = when {
        config.storedView == WEEKLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> getFirstDayOfWeek(
            Formatter.getDateTimeFromCode(dayCode)
        )

        config.storedView == YEARLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> Formatter.getYearFromDayCode(
            dayCode
        )

        else -> dayCode
    }

    private fun getFragmentsHolder() = when (config.storedView) {
        DAILY_VIEW -> DayFragmentsHolder()
        MONTHLY_VIEW -> MonthFragmentsHolder()
//        MONTHLY_DAILY_VIEW -> MonthDayFragmentsHolder()
        YEARLY_VIEW -> YearFragmentsHolder()
        EVENTS_LIST_VIEW -> EventListFragment()
        else -> WeekFragmentsHolder()
    }


    fun toggleGoToTodayVisibility(beVisible: Boolean) {
        shouldGoToTodayBeVisible = beVisible
        if (goToTodayButton?.isVisible != beVisible) {
            refreshMenuItems()
        }
    }

}

/**
 * Greeting
 *
 * @param name
 * @param modifier
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

/** Greeting preview */
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyCalendarTheme {
        Greeting("Android")
    }
}