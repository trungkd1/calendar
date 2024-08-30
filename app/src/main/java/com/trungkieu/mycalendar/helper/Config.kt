package com.trungkieu.mycalendar.helper

import android.content.Context
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.helpers.BaseConfig
import com.trungkieu.mycalendar.R
import com.trungkieu.mycalendar.extensions.scheduleCalDAVSync
import java.util.ArrayList
import java.util.Arrays
import java.util.Calendar
import java.util.HashSet
import java.util.Locale


class Config(context: Context) : BaseConfig(context){
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var displayEventTypes: Set<String>
        get() = prefs.getStringSet(DISPLAY_EVENT_TYPES, HashSet())!!
        set(displayEventTypes) = prefs.edit().remove(DISPLAY_EVENT_TYPES).putStringSet(DISPLAY_EVENT_TYPES, displayEventTypes).apply()

    var storedView: Int
        get() = prefs.getInt(VIEW, MONTHLY_VIEW)
        set(view) = prefs.edit().putInt(VIEW, view).apply()

    var caldavSync: Boolean
        get() = prefs.getBoolean(CALDAV_SYNC, false)
        set(caldavSync) {
            context.scheduleCalDAVSync(caldavSync)
            prefs.edit().putBoolean(CALDAV_SYNC, caldavSync).apply()
        }

    var pullToRefresh: Boolean
        get() = prefs.getBoolean(PULL_TO_REFRESH, false)
        set(pullToRefresh) = prefs.edit().putBoolean(PULL_TO_REFRESH,pullToRefresh).apply()

    var startWeekWithCurrentDay: Boolean
        get() = prefs.getBoolean(START_WEEK_WITH_CURRENT_DAY, false)
        set(startWeekWithCurrentDay) = prefs.edit().putBoolean(START_WEEK_WITH_CURRENT_DAY, startWeekWithCurrentDay).apply()

    var firstDayOfWeek: Int
        get() {
            val defaultFirstDayOfWeek = Calendar.getInstance(Locale.getDefault()).firstDayOfWeek
            return prefs.getInt(FIRST_DAY_OF_WEEK, getJodaDayOfWeekFromJava(defaultFirstDayOfWeek))
        }
        set(firstDayOfWeek) = prefs.edit().putInt(FIRST_DAY_OF_WEEK, firstDayOfWeek).apply()

    var weeklyViewItemHeightMultiplier: Float
        get() = prefs.getFloat(WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER, 1f)
        set(weeklyViewItemHeightMultiplier) = prefs.edit().putFloat(WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER, weeklyViewItemHeightMultiplier).apply()

    var weeklyViewDays: Int
        get() = prefs.getInt(WEEKLY_VIEW_DAYS, 7)
        set(weeklyViewDays) = prefs.edit().putInt(WEEKLY_VIEW_DAYS, weeklyViewDays).apply()

    var replaceDescription: Boolean
        get() = prefs.getBoolean(REPLACE_DESCRIPTION, false)
        set(replaceDescription) = prefs.edit().putBoolean(REPLACE_DESCRIPTION, replaceDescription).apply()

    var displayDescription: Boolean
        get() = prefs.getBoolean(DISPLAY_DESCRIPTION, true)
        set(displayDescription) = prefs.edit().putBoolean(DISPLAY_DESCRIPTION, displayDescription).apply()

    var lastUsedLocalEventTypeId: Long
        get() = prefs.getLong(LAST_USED_LOCAL_EVENT_TYPE_ID, REGULAR_EVENT_TYPE_ID)
        set(lastUsedLocalEventTypeId) = prefs.edit().putLong(LAST_USED_LOCAL_EVENT_TYPE_ID, lastUsedLocalEventTypeId).apply()

    var isUsePreviousEventReminders: Boolean
        get() = prefs.getBoolean(USE_PREVIOUS_EVENT_REMINDERS, true)
        set(isUsePreviousEventReminders) = prefs.edit().putBoolean(USE_PREVIOUS_EVENT_REMINDERS, isUsePreviousEventReminders).apply()

    var lastEventReminderMinutes1: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES, 10)
        set(lastEventReminderMinutes) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES, lastEventReminderMinutes).apply()

    var lastEventReminderMinutes2: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES_2, REMINDER_OFF)
        set(lastEventReminderMinutes2) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES_2, lastEventReminderMinutes2).apply()

    var lastEventReminderMinutes3: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES_3, REMINDER_OFF)
        set(lastEventReminderMinutes3) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES_3, lastEventReminderMinutes3).apply()

    var defaultReminder1: Int
        get() = prefs.getInt(DEFAULT_REMINDER_1, 10)
        set(defaultReminder1) = prefs.edit().putInt(DEFAULT_REMINDER_1, defaultReminder1).apply()

    var defaultReminder2: Int
        get() = prefs.getInt(DEFAULT_REMINDER_2, REMINDER_OFF)
        set(defaultReminder2) = prefs.edit().putInt(DEFAULT_REMINDER_2, defaultReminder2).apply()

    var defaultReminder3: Int
        get() = prefs.getInt(DEFAULT_REMINDER_3, REMINDER_OFF)
        set(defaultReminder3) = prefs.edit().putInt(DEFAULT_REMINDER_3, defaultReminder3).apply()

    var highlightWeekendsColor: Int
        get() = prefs.getInt(HIGHLIGHT_WEEKENDS_COLOR, context.resources.getColor(com.simplemobiletools.commons.R.color.md_light_green))
        set(highlightWeekendsColor) = prefs.edit().putInt(HIGHLIGHT_WEEKENDS_COLOR, highlightWeekendsColor).apply()

    var defaultEventTypeId: Long
        get() = prefs.getLong(DEFAULT_EVENT_TYPE_ID, -1L)
        set(defaultEventTypeId) = prefs.edit().putLong(DEFAULT_EVENT_TYPE_ID, defaultEventTypeId).apply()

    fun addDisplayEventType(type: String) {
        addDisplayEventTypes(HashSet(Arrays.asList(type)))
    }

    fun getDisplayEventTypessAsList() = displayEventTypes.map { it.toLong() }.toMutableList() as ArrayList<Long>


    var displayPastEvents: Int
        get() = prefs.getInt(DISPLAY_PAST_EVENTS, DAY_MINUTES)
        set(displayPastEvents) = prefs.edit().putInt(DISPLAY_PAST_EVENTS, displayPastEvents).apply()


    var showGrid: Boolean
        get() = prefs.getBoolean(SHOW_GRID, false)
        set(showGrid) = prefs.edit().putBoolean(SHOW_GRID, showGrid).apply()

    var highlightWeekends: Boolean
        get() = prefs.getBoolean(HIGHLIGHT_WEEKENDS, true)
        set(highlightWeekends) = prefs.edit().putBoolean(HIGHLIGHT_WEEKENDS, highlightWeekends).apply()

    var showWeekNumbers: Boolean
        get() = prefs.getBoolean(WEEK_NUMBERS, false)
        set(showWeekNumbers) = prefs.edit().putBoolean(WEEK_NUMBERS, showWeekNumbers).apply()

    var dimPastEvents: Boolean
        get() = prefs.getBoolean(DIM_PAST_EVENTS, true)
        set(dimPastEvents) = prefs.edit().putBoolean(DIM_PAST_EVENTS, dimPastEvents).apply()

    var dimCompletedTasks: Boolean
        get() = prefs.getBoolean(DIM_COMPLETED_TASKS, true)
        set(dimCompletedTasks) = prefs.edit().putBoolean(DIM_COMPLETED_TASKS, dimCompletedTasks).apply()

    private fun addDisplayEventTypes(types: Set<String>) {
        val currDisplayEventTypes = HashSet(displayEventTypes)
        currDisplayEventTypes.addAll(types)
        displayEventTypes = currDisplayEventTypes
    }
}