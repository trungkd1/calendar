package com.trungkieu.mycalendar.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import androidx.print.PrintHelper
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.extensions.getFormattedSeconds
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.isUsingSystemDarkTheme
import com.simplemobiletools.commons.extensions.removeBit
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.DAY
import com.simplemobiletools.commons.helpers.EVENT_ID
import com.simplemobiletools.commons.helpers.EVENT_OCCURRENCE_TS
import com.simplemobiletools.commons.helpers.FLAG_TASK_COMPLETED
import com.simplemobiletools.commons.helpers.FRIDAY_BIT
import com.simplemobiletools.commons.helpers.IS_TASK_COMPLETED
import com.simplemobiletools.commons.helpers.MONDAY_BIT
import com.simplemobiletools.commons.helpers.MONTH
import com.simplemobiletools.commons.helpers.NEW_EVENT_START_TS
import com.simplemobiletools.commons.helpers.SATURDAY_BIT
import com.simplemobiletools.commons.helpers.SUNDAY_BIT
import com.simplemobiletools.commons.helpers.THURSDAY_BIT
import com.simplemobiletools.commons.helpers.TUESDAY_BIT
import com.simplemobiletools.commons.helpers.WEDNESDAY_BIT
import com.simplemobiletools.commons.helpers.WEEK
import com.simplemobiletools.commons.helpers.YEAR
import com.simplemobiletools.commons.helpers.getNowSeconds
import com.simplemobiletools.commons.models.RadioItem
import com.trungkieu.mycalendar.R
import com.trungkieu.mycalendar.activity.EventActivity
import com.trungkieu.mycalendar.activity.TaskActivity
import com.trungkieu.mycalendar.database.EventsDatabase
import com.trungkieu.mycalendar.entity.EventEntity
import com.trungkieu.mycalendar.entity.TaskEntity
import com.trungkieu.mycalendar.helper.Config
import com.trungkieu.mycalendar.helper.EventsHandler
import com.trungkieu.mycalendar.helper.Formatter
import com.trungkieu.mycalendar.interfaces.EventTypesDao
import com.trungkieu.mycalendar.interfaces.EventsDao
import com.trungkieu.mycalendar.interfaces.TaskDao
import com.trungkieu.mycalendar.models.ListEvent
import com.trungkieu.mycalendar.models.ListItem
import com.trungkieu.mycalendar.models.ListSectionDay
import com.trungkieu.mycalendar.models.ListSectionMonth
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import java.util.ArrayList
import java.util.Calendar
import java.util.TreeSet
import java.util.UUID


val Context.config: Config get() = Config.newInstance(applicationContext)
val Context.eventsDB: EventsDao get() = EventsDatabase.getInstance(applicationContext).EventsDao()
val Context.eventTypesDB: EventTypesDao get() = EventsDatabase.getInstance(applicationContext).EventTypesDao()
val Context.eventsHandler: EventsHandler get() = EventsHandler(this)
val Context.completedTasksDB: TaskDao get() = EventsDatabase.getInstance(applicationContext).TaskDao()

fun Int.isXWeeklyRepetition() = this != 0 && this % WEEK == 0

fun Int.isXMonthlyRepetition() = this != 0 && this % MONTH == 0

fun Int.isXYearlyRepetition() = this != 0 && this % YEAR == 0

fun Context.getFirstDayOfWeek(date: DateTime): String {
    return getFirstDayOfWeekDt(date).toString()
}

fun Context.isWeekendIndex(dayIndex: Int): Boolean {
    val firstDayOfWeek = config.firstDayOfWeek
    val shiftedIndex = (dayIndex + firstDayOfWeek) % 7
    val dayOfWeek = if (shiftedIndex == 0) {
        DateTimeConstants.SUNDAY
    } else {
        shiftedIndex
    }

    return isWeekend(dayOfWeek)
}

fun isWeekend(dayOfWeek: Int): Boolean {
    val weekendDays = listOf(DateTimeConstants.SATURDAY, DateTimeConstants.SUNDAY)
    return dayOfWeek in weekendDays
}

fun Context.getWeeklyViewItemHeight(): Float {
    val defaultHeight = resources.getDimension(R.dimen.weekly_view_row_height)
    val multiplier = config.weeklyViewItemHeightMultiplier
    return defaultHeight * multiplier
}

fun Context.getDisplayEventTypessAsList() = config.displayEventTypes.map { it.toLong() }.toMutableList() as ArrayList<Long>


// format day bits to strings like "Mon, Tue, Wed"
fun Context.getShortDaysFromBitmask(bitMask: Int): String {
    val dayBits = withFirstDayOfWeekToFront(listOf(MONDAY_BIT, TUESDAY_BIT, WEDNESDAY_BIT, THURSDAY_BIT, FRIDAY_BIT, SATURDAY_BIT, SUNDAY_BIT))
    val weekDays = withFirstDayOfWeekToFront(resources.getStringArray(com.simplemobiletools.commons.R.array.week_days_short).toList())

    var days = ""
    dayBits.forEachIndexed { index, bit ->
        if (bitMask and bit != 0) {
            days += "${weekDays[index]}, "
        }
    }

    return days.trim().trimEnd(',')
}

fun <T> Context.withFirstDayOfWeekToFront(weekItems: Collection<T>): ArrayList<T> {
    val firstDayOfWeek = config.firstDayOfWeek
    if (firstDayOfWeek == DateTimeConstants.MONDAY) {
        return weekItems.toMutableList() as ArrayList<T>
    }

    val firstDayOfWeekIndex = config.firstDayOfWeek - 1
    val rotatedWeekItems = weekItems.drop(firstDayOfWeekIndex) + weekItems.take(firstDayOfWeekIndex)
    return rotatedWeekItems as ArrayList<T>
}

fun Context.getEventListItems(events: List<EventEntity>, addSectionDays: Boolean = true, addSectionMonths: Boolean = true) : ArrayList<ListItem> {
    val listItems = ArrayList<ListItem>(events.size)
    val replaceDescription = config.replaceDescription

    // move all-day events in front of others
    val sorted = events.sortedWith(compareBy<EventEntity> {
        if (it.getIsAllDay()) {
            Formatter.getDayStartTS(Formatter.getDayCodeFromTS(it.startTS)) - 1
        } else {
            it.startTS
        }
    }.thenBy {
        if (it.getIsAllDay()) {
            Formatter.getDayEndTS(Formatter.getDayCodeFromTS(it.endTS))
        } else {
            it.endTS
        }
    }.thenBy { it.title }.thenBy { if (replaceDescription) it.location else it.description })

    var prevCode = ""
    var prevMonthLabel = ""
    val now = getNowSeconds()
    val todayCode = Formatter.getDayCodeFromTS(now)

    sorted.forEach {
        val code = Formatter.getDayCodeFromTS(it.startTS)
        if (addSectionMonths) {
            val monthLabel = Formatter.getLongMonthYear(this, code)
            if (monthLabel != prevMonthLabel) {
                val listSectionMonth = ListSectionMonth(monthLabel)
                listItems.add(listSectionMonth)
                prevMonthLabel = monthLabel
            }
        }

        if (code != prevCode && addSectionDays) {
            val day = Formatter.getDateDayTitle(code)
            val isToday = code == todayCode
            val listSectionDay = ListSectionDay(day, code, isToday, !isToday && it.startTS < now)
            listItems.add(listSectionDay)
            prevCode = code
        }

        val listEvent =
            ListEvent(
                it.id!!,
                it.startTS,
                it.endTS,
                it.title,
                it.description,
                it.getIsAllDay(),
                it.color,
                it.location,
                it.isPastEvent,
                it.repeatInterval > 0,
                it.isTask(),
                it.isTaskCompleted()
            )
        listItems.add(listEvent)
    }
    return listItems
}



fun Context.getDatePickerDialogTheme() = when {
    baseConfig.isUsingSystemTheme -> com.simplemobiletools.commons.R.style.MyDateTimePickerMaterialTheme
    baseConfig.backgroundColor.getContrastColor() == Color.WHITE -> com.simplemobiletools.commons.R.style.MyDialogTheme_Dark
    else -> com.simplemobiletools.commons.R.style.MyDialogTheme
}

fun Context.getFormattedMinutes(minutes: Int, showBefore: Boolean = true) = getFormattedSeconds(if (minutes == -1) minutes else minutes * 60, showBefore)

fun Context.getTimePickerDialogTheme() = when {
    baseConfig.isUsingSystemTheme -> if (isUsingSystemDarkTheme()) {
        com.simplemobiletools.commons.R.style.MyTimePickerMaterialTheme_Dark
    } else {
        com.simplemobiletools.commons.R.style.MyDateTimePickerMaterialTheme
    }
    baseConfig.backgroundColor.getContrastColor() == Color.WHITE -> com.simplemobiletools.commons.R.style.MyDialogTheme_Dark
    else -> com.simplemobiletools.commons.R.style.MyDialogTheme
}

fun Context.getRepetitionText(seconds: Int) = when (seconds) {
    0 -> getString(R.string.no_repetition)
    DAY -> getString(R.string.daily)
    WEEK -> getString(R.string.weekly)
    MONTH -> getString(R.string.monthly)
    YEAR -> getString(R.string.yearly)
    else -> {
        when {
            seconds % YEAR == 0 -> resources.getQuantityString(com.simplemobiletools.commons.R.plurals.years, seconds / YEAR, seconds / YEAR)
            seconds % MONTH == 0 -> resources.getQuantityString(com.simplemobiletools.commons.R.plurals.months, seconds / MONTH, seconds / MONTH)
            seconds % WEEK == 0 -> resources.getQuantityString(com.simplemobiletools.commons.R.plurals.weeks, seconds / WEEK, seconds / WEEK)
            else -> resources.getQuantityString(com.simplemobiletools.commons.R.plurals.days, seconds / DAY, seconds / DAY)
        }
    }
}

fun Context.scheduleCalDAVSync(activate: Boolean) {
//    val syncIntent = Intent(applicationContext, CalDAVSyncReceiver::class.java)
//    val pendingIntent = PendingIntent.getBroadcast(
//        applicationContext,
//        SCHEDULE_CALDAV_REQUEST_CODE,
//        syncIntent,
//        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//    )
//    val alarmManager = getAlarmManager()
//    alarmManager.cancel(pendingIntent)
//
//    if (activate) {
//        val syncCheckInterval = 2 * AlarmManager.INTERVAL_HOUR
//        try {
//            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + syncCheckInterval, syncCheckInterval, pendingIntent)
//        } catch (ignored: Exception) {
//        }
//    }
}

fun notifyEvent(originalEvent: EventEntity) {

}


fun generateImportId(): String {
    return UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis().toString()
}

fun Context.updateTaskCompletion(event: EventEntity, completed: Boolean) {
    if (completed) {
        event.flags = event.flags or FLAG_TASK_COMPLETED
        val task = TaskEntity(null, event.id!!, event.startTS, event.flags)
        completedTasksDB.insertOrUpdate(task)
    } else {
        event.flags = event.flags.removeBit(FLAG_TASK_COMPLETED)
        completedTasksDB.deleteTaskWithIdAndTs(event.id!!, event.startTS)
    }
    // mark event as "incomplete" in the main events db
    eventsDB.updateTaskCompletion(event.id!!, event.flags.removeBit(FLAG_TASK_COMPLETED))
}

fun Context.getFirstDayOfWeekDt(date: DateTime): DateTime {
    val currentDate = date.withTimeAtStartOfDay()
    if (config.startWeekWithCurrentDay) {
        return currentDate
    } else {
        val firstDayOfWeek = config.firstDayOfWeek
        val currentDayOfWeek = currentDate.dayOfWeek
        return if (currentDayOfWeek == firstDayOfWeek) {
            currentDate
        } else {
            // Joda-time's weeks always starts on Monday but user preferred firstDayOfWeek could be any week day
            if (firstDayOfWeek < currentDayOfWeek) {
                currentDate.withDayOfWeek(firstDayOfWeek)
            } else {
                currentDate.minusWeeks(1).withDayOfWeek(firstDayOfWeek)
            }
        }
    }
}

fun Activity.showEventRepeatIntervalDialog(curSeconds: Int, callback: (minutes: Int) -> Unit) {
    hideKeyboard()
    val seconds = TreeSet<Int>()
    seconds.apply {
        add(0)
        add(DAY)
        add(WEEK)
        add(MONTH)
        add(YEAR)
        add(curSeconds)
    }

    val items = ArrayList<RadioItem>(seconds.size + 1)
    seconds.mapIndexedTo(items) { index, value ->
        RadioItem(index, getRepetitionText(value), value)
    }

    var selectedIndex = 0
    seconds.forEachIndexed { index, value ->
        if (value == curSeconds)
            selectedIndex = index
    }

    items.add(RadioItem(-1, getString(com.simplemobiletools.commons.R.string.custom)))

    RadioGroupDialog(this, items, selectedIndex) {
        if (it == -1) {
//            CustomEventRepeatIntervalDialog(this) {
//                callback(it)
//            }
        } else {
            callback(it as Int)
        }
    }
}

fun getJavaDayOfWeekFromJoda(dayOfWeek: Int): Int {
    return when (dayOfWeek) {
        DateTimeConstants.SUNDAY -> Calendar.SUNDAY
        DateTimeConstants.MONDAY -> Calendar.MONDAY
        DateTimeConstants.TUESDAY -> Calendar.TUESDAY
        DateTimeConstants.WEDNESDAY -> Calendar.WEDNESDAY
        DateTimeConstants.THURSDAY -> Calendar.THURSDAY
        DateTimeConstants.FRIDAY -> Calendar.FRIDAY
        DateTimeConstants.SATURDAY -> Calendar.SATURDAY
        else -> throw IllegalArgumentException("Invalid day: $dayOfWeek")
    }
}

fun Context.scheduleNextEventReminder(event: EventEntity, showToasts: Boolean) {

}

fun Context.editEvent(event: ListEvent) {
    Intent(this, getActivityToOpen(event.isTask)).apply {
        putExtra(EVENT_ID, event.id)
        putExtra(EVENT_OCCURRENCE_TS, event.startTS)
        putExtra(IS_TASK_COMPLETED, event.isTaskCompleted)
        startActivity(this)
    }
}


fun Context.updateWidgets() {
//    val widgetIDs = AppWidgetManager.getInstance(applicationContext)?.getAppWidgetIds(ComponentName(applicationContext, MyWidgetMonthlyProvider::class.java))
//        ?: return
//    if (widgetIDs.isNotEmpty()) {
//        Intent(applicationContext, MyWidgetMonthlyProvider::class.java).apply {
//            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
//            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
//            sendBroadcast(this)
//        }
//    }
//
//    updateListWidget()
//    updateDateWidget()
}

// if the default event start time is set to "Next full hour" and the event is created before midnight, it could change the day
fun Context.launchNewEventIntent(dayCode: String = Formatter.getTodayCode(), allowChangingDay: Boolean = false) {
    Intent(applicationContext, EventActivity::class.java).apply {
        putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode, allowChangingDay))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}

// if the default start time is set to "Next full hour" and the task is created before midnight, it could change the day
fun Context.launchNewTaskIntent(dayCode: String = Formatter.getTodayCode(), allowChangingDay: Boolean = false) {
    Intent(applicationContext, TaskActivity::class.java).apply {
        putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode, allowChangingDay))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}


fun getActivityToOpen(isTask: Boolean) = if (isTask) {
    TaskActivity::class.java
} else {
    EventActivity::class.java
}

fun Context.printBitmap(bitmap: Bitmap) {
    val printHelper = PrintHelper(this)
    printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
    printHelper.orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    printHelper.printBitmap(getString(R.string.app_name), bitmap)
}


fun Context.getProperDayIndexInWeek(date: DateTime): Int {
    val firstDayOfWeek = config.firstDayOfWeek
    val dayOfWeek = date.dayOfWeek
    val dayIndex = if (dayOfWeek >= firstDayOfWeek) {
        dayOfWeek - firstDayOfWeek
    } else {
        dayOfWeek + (7 - firstDayOfWeek)
    }

    return dayIndex
}

// if the default start time is set to "Next full hour" and the task is created before midnight, it could change the day
fun Context.startNewTaskIntent(dayCode: String = com.trungkieu.mycalendar.helper.Formatter.getTodayCode(), allowChangingDay: Boolean = false) {
    Intent(applicationContext, TaskActivity::class.java).apply {
        putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode, allowChangingDay))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}

fun Context.getNewEventTimestampFromCode(dayCode: String, allowChangingDay: Boolean = false): Long {

    return 0L
}



fun getStartTaskActivity() = TaskActivity::class.java

fun getStartEventActivity() = EventActivity::class.java