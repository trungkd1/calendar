package com.trungkieu.mycalendar.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.dialogs.PermissionRequiredDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.addBitIf
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.checkAppSideloading
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.openNotificationSettings
import com.simplemobiletools.commons.extensions.removeBit
import com.simplemobiletools.commons.extensions.setFillWithStroke
import com.simplemobiletools.commons.extensions.showPickSecondsDialogHelper
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.extensions.value
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import com.trungkieu.mycalendar.R
import com.trungkieu.mycalendar.databinding.TaskActivityBinding
import com.trungkieu.mycalendar.dialogs.RepeatLimitTypePickerDialog
import com.trungkieu.mycalendar.dialogs.RepeatRuleWeeklyDialog
import com.trungkieu.mycalendar.entity.EventEntity
import com.trungkieu.mycalendar.entity.EventTypeEntity
import com.trungkieu.mycalendar.extensions.config
import com.trungkieu.mycalendar.extensions.eventTypesDB
import com.trungkieu.mycalendar.extensions.eventsDB
import com.trungkieu.mycalendar.extensions.eventsHandler
import com.trungkieu.mycalendar.extensions.generateImportId
import com.trungkieu.mycalendar.extensions.getDatePickerDialogTheme
import com.trungkieu.mycalendar.extensions.getFormattedMinutes
import com.trungkieu.mycalendar.extensions.getRepetitionText
import com.trungkieu.mycalendar.extensions.getShortDaysFromBitmask
import com.trungkieu.mycalendar.extensions.getTimePickerDialogTheme
import com.trungkieu.mycalendar.extensions.isXMonthlyRepetition
import com.trungkieu.mycalendar.extensions.isXWeeklyRepetition
import com.trungkieu.mycalendar.extensions.isXYearlyRepetition
import com.trungkieu.mycalendar.extensions.notifyEvent
import com.trungkieu.mycalendar.extensions.seconds
import com.trungkieu.mycalendar.extensions.showEventRepeatIntervalDialog
import com.trungkieu.mycalendar.extensions.updateTaskCompletion
import com.trungkieu.mycalendar.helper.Formatter
import com.trungkieu.mycalendar.models.Reminder
import org.joda.time.DateTime
import kotlin.math.pow

class TaskActivity : BaseSimpleActivity() {

    private var mEventTypeId = REGULAR_EVENT_TYPE_ID
    private lateinit var mTask: EventEntity
    private lateinit var mTaskDateTime: DateTime


    private var mReminder1Minutes = REMINDER_OFF
    private var mReminder2Minutes = REMINDER_OFF
    private var mReminder3Minutes = REMINDER_OFF
    private var mReminder1Type = REMINDER_NOTIFICATION
    private var mReminder2Type = REMINDER_NOTIFICATION
    private var mReminder3Type = REMINDER_NOTIFICATION
    private var mRepeatInterval = 0
    private var mRepeatLimit = 0L
    private var mRepeatRule = 0
    private var mTaskOccurrenceTS = 0L
    private var mOriginalStartTS = 0L
    private var mTaskCompleted = false
    private var mEventColor = 0
    private var mIsNewTask = true
    private val LOG_TAG = "LOG_TAG"

    private val binding by viewBinding(TaskActivityBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()
        refreshMenuitems()
        // check size of application
        if (checkAppSideloading()) {
            return
        }

        val intent = intent ?: return
        updateColors()
        val taskId = intent.getLongExtra(EVENT_ID, 0)
        ensureBackgroundThread {
            val task = eventsDB.getTaskWithId(taskId)

            if (taskId != 0L && task == null) {
                hideKeyboard()
                finish()
                return@ensureBackgroundThread
            }

            val storedEventTypes =
                eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventTypeEntity>
            val localEventType =
                storedEventTypes.firstOrNull { it.id == config.lastUsedLocalEventTypeId }
            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    handleTask(savedInstanceState, localEventType, task)
                }
            }

        }
    }

    private fun setupEditTask() {
        mIsNewTask = false
        val realStart = if (mTaskOccurrenceTS == 0L) mTask.startTS else mTaskOccurrenceTS
        mOriginalStartTS = realStart
        mTaskDateTime = Formatter.getDateTimeFromTS(realStart)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        binding.taskToolbar.title = getString(R.string.edit_task)

        mEventTypeId = mTask.eventType
        mReminder1Minutes = mTask.reminder1Minutes
        mReminder2Minutes = mTask.reminder2Minutes
        mReminder3Minutes = mTask.reminder3Minutes
        mReminder1Type = mTask.reminder1Type
        mReminder2Type = mTask.reminder2Type
        mReminder3Type = mTask.reminder3Type
        mRepeatInterval = mTask.repeatInterval
        mRepeatLimit = mTask.repeatLimit
        mRepeatRule = mTask.repeatRule
        mEventColor = mTask.color

        binding.editTextTitle.setText(mTask.title)
        binding.editTextDescription.setText(mTask.description)
        binding.taskAllDay.isChecked = mTask.getIsAllDay()
        toggleAllDay(mTask.getIsAllDay())
        checkRepeatTexts(mRepeatInterval)

    }

    private fun checkRepeatTexts(limit: Int) {
        binding.taskRepetitionLimitHolder.beGoneIf(limit == 0)
        updateRepetitionLimitText()
//
        binding.taskRepetitionRuleHolder.beVisibleIf(mRepeatInterval.isXWeeklyRepetition() || mRepeatInterval.isXMonthlyRepetition() || mRepeatInterval.isXYearlyRepetition())
        checkRepetitionRuleText()
    }

    private fun checkRepetitionRuleText() {
        when {
            mRepeatInterval.isXWeeklyRepetition() -> {
                binding.taskRepetitionRule.text = if (mRepeatRule == EVERY_DAY_BIT) {
                    getString(com.simplemobiletools.commons.R.string.every_day)
                } else {
                    getShortDaysFromBitmask(mRepeatRule)
                }
            }

            mRepeatInterval.isXMonthlyRepetition() -> {
                val repeatString = if (mRepeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST || mRepeatRule == REPEAT_ORDER_WEEKDAY)
                    R.string.repeat else R.string.repeat_on

                binding.taskRepetitionRuleLabel.text = getString(repeatString)
                binding.taskRepetitionRule.text = getMonthlyRepetitionRuleText(mRepeatRule)
            }

            mRepeatInterval.isXYearlyRepetition() -> {
                val repeatString = if (mRepeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST || mRepeatRule == REPEAT_ORDER_WEEKDAY)
                    R.string.repeat else R.string.repeat_on

                binding.taskRepetitionRuleLabel.text = getString(repeatString)
                binding.taskRepetitionRule.text = getYearlyRepetitionRuleText(mRepeatRule)
            }
        }
    }

    private fun getYearlyRepetitionRuleText(repeatRule: Int) = when (repeatRule) {
        REPEAT_SAME_DAY -> getString(R.string.the_same_day)
        else -> getRepeatXthDayInMonthString(false, mRepeatRule)
    }

    private fun getMonthlyRepetitionRuleText(repeatRule: Int) = when (repeatRule) {
        REPEAT_SAME_DAY -> getString(R.string.the_same_day)
        REPEAT_LAST_DAY -> getString(R.string.the_last_day)
        else -> getRepeatXthDayString(false,mRepeatRule)
    }

    private fun updateRepetitionLimitText() {
        binding.taskRepetitionLimit.text = when {
            /**
             * trường hợp lặp forever khi mRepeatLimit = 0
             * */
            mRepeatLimit == 0L -> {
                binding.taskRepetitionLimitLabel.text = getString(R.string.repeat)
                resources.getString(R.string.forever)
            }

            /**
             * trường hợp lặp có số lần nhất định khi mRepeatLimit = 3,5,6
             * */
            mRepeatLimit > 0 -> {
                binding.taskRepetitionLimitLabel.text = getString(R.string.repeat_till)
                val repeatLimitDateTime = Formatter.getDateTimeFromTS(mRepeatLimit)
                Formatter.getFullDate(this, repeatLimitDateTime)
            }

            /**
             * rường hợp lặp đến thời gian xác định khi mRepeatLimit = 1697602325
             * */
            else -> {
                binding.taskRepetitionLimitLabel.text = getString(R.string.repeat)
                "${-mRepeatLimit} ${getString(R.string.times)}"
            }
        }

    }

    private fun setupNewtask() {
        val startTS = intent.getLongExtra(NEW_EVENT_START_TS, 1697270400)
        val dateTinme = Formatter.getDateTimeFromTS(startTS)
        mTaskDateTime = dateTinme

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        binding.editTextTitle.requestFocus()
        binding.taskToolbar.title = getString(R.string.new_task)

        mTask.apply {
            this.startTS = mTaskDateTime.seconds()
            this.endTS = mTaskDateTime.seconds()
            reminder1Minutes = mReminder1Minutes
            reminder1Type = mReminder1Type
            reminder2Minutes = mReminder2Minutes
            reminder2Type = mReminder2Type
            reminder3Minutes = mReminder3Minutes
            reminder3Type = mReminder3Type
            eventType = mEventTypeId
        }

        updateTexts()
    }

    private fun handleTask(
        savedInstanceState: Bundle?,
        localEventType: EventTypeEntity?,
        task: EventEntity?
    ) {
        if (localEventType == null) {
            config.lastUsedLocalEventTypeId = REGULAR_EVENT_TYPE_ID
        }

        mEventTypeId =
            if (config.defaultEventTypeId == -1L) config.lastUsedLocalEventTypeId else config.defaultEventTypeId

        if (task != null) {
            mTask = task
            mTaskOccurrenceTS = intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L)
            mTaskCompleted = intent.getBooleanExtra(IS_TASK_COMPLETED, false)
            if (savedInstanceState == null) {
                setupEditTask()
            }

            if (intent.getBooleanExtra(IS_DUPLICATE_INTENT, false)) {
                mTask.id = null
                binding.taskToolbar.title = getString(R.string.new_task)
            }
        } else {
            mTask = EventEntity(null)
            config.apply {
                mReminder1Minutes =
                    if (isUsePreviousEventReminders && lastEventReminderMinutes1 >= -1) lastEventReminderMinutes1 else defaultReminder1
                mReminder2Minutes =
                    if (isUsePreviousEventReminders && lastEventReminderMinutes2 >= -1) lastEventReminderMinutes2 else defaultReminder2
                mReminder3Minutes =
                    if (isUsePreviousEventReminders && lastEventReminderMinutes3 >= -1) lastEventReminderMinutes3 else defaultReminder3
            }

            if (savedInstanceState == null) {
                setupNewtask()
            }
        }

        binding.apply {
            taskAllDay.apply {
                setOnCheckedChangeListener { compoundButton, isChecked -> toggleAllDay(isChecked) }
            }

            taskAllDayHolder.apply {
                setOnClickListener { it -> taskAllDay.toggle() }
            }

            taskDate.apply {
                setOnClickListener { pickDate() }
            }

            taskTime.apply {
                setOnClickListener { pickTime() }
            }

            taskTypeHolder.apply {
                setOnClickListener { showEventTypeDialog() }
            }

            taskRepetition.apply {
                setOnClickListener { showRepeatIntervalDialog() }
            }

            taskRepetitionLimitHolder.apply {
                setOnClickListener { showRepetitionTypePicker() }
            }

            taskRepetitionLimitHolder.apply {
                setOnClickListener { showRepetitionTypePicker() }
            }

            taskReminder1.apply {
                setOnClickListener { showReminder1Dialog() }
            }

            taskReminder2.apply {
                setOnClickListener { showReminder2Dialog() }
            }

            taskReminder3.apply {
                setOnClickListener { showReminder3Dialog() }
            }

            taskColorHolder.apply {
                setOnClickListener { showTaskColorDialog() }
            }

            taskRepetitionRuleHolder.apply {
                setOnClickListener {  showRepetitionRuleDialog() }
            }


        }
    }


    private fun updateEventType() {
        TODO("Not yet implemented")
    }

    private fun showRepetitionTypePicker() {
        hideKeyboard()
        RepeatLimitTypePickerDialog(this, mRepeatLimit, mTaskDateTime.seconds()) {
            setRepeatLimit(it)
            Log.e(LOG_TAG,"showRepetitionTypePicker it : $it")
        }
    }

    private fun setRepeatLimit(limit: Long) {
        mRepeatLimit = limit
        Log.e(LOG_TAG,"setRepeatLimit : $mRepeatLimit")
        updateRepetitionLimitText()
    }

    private fun showReminder1Dialog() {
        showPickSecondsDialogHelper(mReminder1Minutes) {
            Log.e("BUG", "showReminder1Dialog : " + it.toString())
            mReminder1Minutes = if (it == -1 || it == 0) it else it / 60
            updateReminder1Text()
        }
    }

    private fun showReminder2Dialog() {
//        TODO("Not yet implemented")
    }

    private fun showReminder3Dialog() {
//        TODO("Not yet implemented")
    }

    private fun showRepeatIntervalDialog() {
        showEventRepeatIntervalDialog(mRepeatInterval) {
            setRepeatInterval(it)
        }
    }

    private fun setRepeatInterval(interval: Int) {
        mRepeatInterval = interval
        updateRepetitionText()
        checkRepeatTexts(interval)

        when {
            mRepeatInterval.isXWeeklyRepetition() -> setRepeatRule(
                2.0.pow((mTaskDateTime.dayOfWeek - 1).toDouble()).toInt()
            )

            mRepeatInterval.isXMonthlyRepetition() -> setRepeatRule(REPEAT_SAME_DAY)
            mRepeatInterval.isXYearlyRepetition() -> setRepeatRule(REPEAT_SAME_DAY)
        }
    }

    private fun setRepeatRule(rule: Int) {
        mRepeatRule = rule
        checkRepetitionRuleText()
        if (rule == 0) {
            setRepeatInterval(0)
        }
    }

    private fun showEventTypeDialog() {
//        TODO("Not yet implemented")
    }


    private fun timeSet(hours: Int, minutes: Int) {
        mTaskDateTime = mTaskDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
        updateTimeText()
    }

    private fun dateSet(year: Int, month: Int, day: Int) {
        mTaskDateTime = mTaskDateTime.withDate(year, month + 1, day)
        updateDateText()
        checkRepeatRule()
    }

    private fun updateTexts() {
        updateDateText()
        updateTimeText()
        updateReminder1Text()
        updateReminder2Text()
        updateReminder3Text()
        updateRepetitionText()
    }

    private fun updateRepetitionText() {
        binding.taskRepetition.text = getRepetitionText(mRepeatInterval)
    }


    private fun updateReminder3Text() {
//        TODO("Not yet implemented")
    }

    private fun updateReminder2Text() {
//        TODO("Not yet implemented")
    }

    private fun updateReminder1Text() {
        binding.taskReminder1.text = getFormattedMinutes(mReminder1Minutes)
    }

    private fun updateTimeText() {
        binding.taskTime.text = Formatter.getTime(this, mTaskDateTime)
    }


    private fun pickDate() {
        hideKeyboard()
        val datePicker = DatePickerDialog(
            this,
            getDatePickerDialogTheme(),
            { _, year, monthOfYear, dayOfMonth ->
                dateSet(year, monthOfYear, dayOfMonth)
            },
            mTaskDateTime.year,
            mTaskDateTime.monthOfYear - 1,
            mTaskDateTime.dayOfMonth
        )
        datePicker.datePicker.firstDayOfWeek = getJavaDayOfWeekFromJoda(config.firstDayOfWeek)
        datePicker.show()
    }

    private fun pickTime() {
        hideKeyboard()
        TimePickerDialog(
            this,
            getTimePickerDialogTheme(),
            { _, hourOfDay, minute ->
                timeSet(hourOfDay, minute)
            },
            mTaskDateTime.hourOfDay,
            mTaskDateTime.minuteOfHour,
            config.use24HourFormat
        ).show()
    }

    private fun showRepetitionRuleDialog() {
        hideKeyboard()
        when {
            mRepeatInterval.isXWeeklyRepetition() -> RepeatRuleWeeklyDialog(this, mRepeatRule) {
                Log.e(LOG_TAG,"showRepetitionRuleDialog() it :$it")
                setRepeatRule(it)
            }

            mRepeatInterval.isXMonthlyRepetition() -> {
                val items = getAvailableMonthlyRepetitionRules()
                RadioGroupDialog(this, items, mRepeatRule) {
                    setRepeatRule(it as Int)
                }
            }

            mRepeatInterval.isXYearlyRepetition() -> {
                val items = getAvailableYearlyRepeatitionRules()
                RadioGroupDialog(this, items, mRepeatRule) {
                    setRepeatRule(it as Int)
                }
            }
        }
    }

    private fun getRepeatXthDayString(includeBase: Boolean, repeatRule: Int): String {
        val dayOfWeek = mTaskDateTime.dayOfWeek
        val base = getString(R.string.repeat_every_m)
        val order = getOrderString(repeatRule)
        val dayString = getDayString(dayOfWeek)
        return if (includeBase) {
            "$base $order $dayString"
        } else {
            val everyString =
                getString(R.string.every_m)
            "$everyString $order $dayString"
        }
    }


    private fun getDayString(day: Int): String {
        return getString(
            when (day) {
                1 -> R.string.monday_alt
                2 -> R.string.tuesday_alt
                3 -> R.string.wednesday_alt
                4 -> R.string.thursday_alt
                5 -> R.string.friday_alt
                6 -> R.string.saturday_alt
                else -> R.string.sunday_alt
            }
        )
    }

    private fun getOrderString(repeatRule: Int): String {
        val dayOfMonth = mTaskDateTime.dayOfMonth
        var order = (dayOfMonth - 1) / 7 + 1
        if (isLastWeekDayOfMonth() && repeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST) {
            order = -1
        }

        val isMale = isMaleGender(mTaskDateTime.dayOfWeek)
        return getString(
            when (order) {
                1 -> if (isMale) R.string.first_m else R.string.first_f
                2 -> if (isMale) R.string.second_m else R.string.second_f
                3 -> if (isMale) R.string.third_m else R.string.third_f
                4 -> if (isMale) R.string.fourth_m else R.string.fourth_f
                5 -> if (isMale) R.string.fifth_m else R.string.fifth_f
                else -> if (isMale) R.string.last_m else R.string.last_f
            }
        )
    }

    private fun getAvailableYearlyRepeatitionRules(): ArrayList<RadioItem> {
        val items = arrayListOf(
            RadioItem(
                REPEAT_SAME_DAY,
                getString(R.string.repeat_on_the_same_day_yearly)
            )
        )
        items.add(
            RadioItem(
                REPEAT_ORDER_WEEKDAY,
                getRepeatXthDayInMonthString(true, REPEAT_ORDER_WEEKDAY)
            )
        )
        if (isLastWeekDayOfMonth()) {
            items.add(
                RadioItem(
                    REPEAT_ORDER_WEEKDAY_USE_LAST,
                    getRepeatXthDayInMonthString(true, REPEAT_ORDER_WEEKDAY_USE_LAST)
                )
            )
        }
        return items
    }

    private fun getRepeatXthDayInMonthString(includeBase: Boolean, repeatRule: Int): String {
        val weekDayString = getRepeatXthDayString(includeBase, repeatRule)
        val monthString = resources.getStringArray(com.simplemobiletools.commons.R.array.in_months)[mTaskDateTime.monthOfYear - 1]
        return "$weekDayString $monthString"
    }

    private fun getAvailableMonthlyRepetitionRules(): ArrayList<RadioItem> {
        val items = arrayListOf(
            RadioItem(
                REPEAT_SAME_DAY,
                getString(R.string.repeat_on_the_same_day_monthly)
            )
        )

        items.add(
            RadioItem(
                REPEAT_ORDER_WEEKDAY,
                getRepeatXthDayString(true, REPEAT_ORDER_WEEKDAY)
            )
        )
        if (isLastWeekDayOfMonth()) {
            items.add(
                RadioItem(
                    REPEAT_ORDER_WEEKDAY_USE_LAST,
                    getRepeatXthDayString(true, REPEAT_ORDER_WEEKDAY_USE_LAST)
                )
            )
        }

        if (isLastDayOfTheMonth()) {
            items.add(
                RadioItem(
                    REPEAT_LAST_DAY,
                    getString(R.string.repeat_on_the_last_day_monthly)
                )
            )
        }
        return items
    }


    private fun checkRepeatRule() {
        if (mRepeatInterval.isXWeeklyRepetition()) {
            val day = mRepeatRule
            if (day == MONDAY_BIT || day == TUESDAY_BIT || day == WEDNESDAY_BIT || day == THURSDAY_BIT || day == FRIDAY_BIT || day == SATURDAY_BIT || day == SUNDAY_BIT) {
                setRepeatRule(2.0.pow((mTaskDateTime.dayOfWeek - 1).toDouble()).toInt())
            }
        } else if (mRepeatInterval.isXMonthlyRepetition() || mRepeatInterval.isXYearlyRepetition()) {
            if (mRepeatRule == REPEAT_LAST_DAY && !isLastDayOfTheMonth()) {
                mRepeatRule = REPEAT_SAME_DAY
            }
            checkRepetitionRuleText()
        }
    }

    private fun updateDateText() {
        binding.taskDate.text = Formatter.getDate(this, mTaskDateTime)
    }

    private fun toggleAllDay(isChecked: Boolean) {

    }

    /**
     * On restore instance state when rotating the screen, we need to save data
     * on the screen before data were destroyed
     *
     * @param savedInstanceState
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }



    private fun refreshMenuitems() {
        if (::mTask.isInitialized) {
            binding.taskToolbar.menu.apply {
                findItem(R.id.delete).isVisible = mTask.id != null
                findItem(R.id.share).isVisible = mTask.id != null
                findItem(R.id.duplicate).isVisible = mTask.id != null
            }
        }
    }

    private fun setupOptionsMenu() {
        binding.taskToolbar.apply {
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.save -> saveCurrentTask()
                    R.id.delete -> deleteTask()
                    R.id.duplicate -> duplicateTask()
                    R.id.share -> shareTask()
                    else -> false
                }
                true
            }
        }
    }

    private fun isLastWeekDayOfMonth(): Boolean {
        return mTaskDateTime.monthOfYear != mTaskDateTime.plusDays(7).monthOfYear
    }


    private fun isLastDayOfTheMonth(): Boolean {
        return mTaskDateTime.dayOfMonth == mTaskDateTime.dayOfMonth().withMaximumValue().dayOfMonth
    }


    private fun shareTask() {
        TODO("Not yet implemented")
    }

    private fun duplicateTask() {
        TODO("Not yet implemented")
    }

    private fun deleteTask() {
        TODO("Not yet implemented")
    }

    private fun saveCurrentTask() {
        if (config.wasAlarmWarningShown || (mReminder1Minutes == REMINDER_OFF && mReminder2Minutes == REMINDER_OFF && mReminder3Minutes == REMINDER_OFF)) {
            ensureBackgroundThread {
                saveTask()
            }
        }else {
            saveTask()
//            ReminderWarningDialog(this) {
//                config.wasAlarmWarningShown = true
//                ensureBackgroundThread {
//                    saveTask()
//                }
//            }
        }

    }

    private fun getReminders(): ArrayList<Reminder> {
        var reminders = arrayListOf(
            Reminder(mReminder1Minutes, mReminder1Type),
            Reminder(mReminder2Minutes, mReminder2Type),
            Reminder(mReminder3Minutes, mReminder3Type)
        )
        reminders = reminders.filter {
            it.minutes != REMINDER_OFF
        }.sortedBy {
            it.minutes
        }.toMutableList() as ArrayList<Reminder>
        return reminders
    }

    private fun saveTask() {
        val newTitle = binding.editTextTitle.value
        if (title.isEmpty()) {
            Toast.makeText(this, getString(R.string.title_empty),Toast.LENGTH_SHORT).show()
            runOnUiThread {
                binding.editTextTitle.requestFocus()
            }
            return
        }

        val reminders = getReminders()
        val wasRepeatable= mTask.repeatInterval > 0
        val newImportId = if (mTask.id != null) {
            mTask.importId
        } else {
            generateImportId()
        }

        if (!binding.taskAllDay.isChecked) {
            if ((reminders.getOrNull(2)?.minutes ?: 0) < -1) {
                reminders.removeAt(2)
            }

            if ((reminders.getOrNull(1)?.minutes ?: 0) < -1) {
                reminders.removeAt(1)
            }

            if ((reminders.getOrNull(0)?.minutes ?: 0) < -1) {
                reminders.removeAt(0)
            }
        }

        val reminder1 = reminders.getOrNull(0) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)
        val reminder2 = reminders.getOrNull(1) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)
        val reminder3 = reminders.getOrNull(2) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)

        config.apply {
            if (isUsePreviousEventReminders) {
                lastEventReminderMinutes1 = reminder1.minutes
                lastEventReminderMinutes2 = reminder2.minutes
                lastEventReminderMinutes3 = reminder3.minutes
            }
        }

        config.lastUsedLocalEventTypeId = mEventTypeId
        mTask.apply {
            startTS = mTaskDateTime.withSecondOfMinute(0).withMinuteOfHour(0).seconds()
            endTS = startTS
            title = newTitle
            description = binding.editTextDescription.value

            // migrate completed task to the new completed tasks db
            if (!wasRepeatable && mTask.isTaskCompleted()) {
                mTask.flags = mTask.flags.removeBit(FLAG_TASK_COMPLETED)
                ensureBackgroundThread {
                    updateTaskCompletion(copy(startTS = mOriginalStartTS),true)
                }
            }
            importId = newImportId
            flags = mTask.flags.addBitIf(binding.taskAllDay.isChecked, FLAG_ALL_DAY)
            lastUpdated = System.currentTimeMillis()
            eventType = mEventTypeId
            type = TYPE_TASK

            reminder1Minutes = reminder1.minutes
            reminder1Type = mReminder1Type
            reminder2Minutes = reminder2.minutes
            reminder2Type = mReminder2Type
            reminder3Minutes = reminder3.minutes
            reminder3Type = mReminder3Type

            repeatInterval = mRepeatInterval
            repeatLimit = if (repeatInterval == 0) 0 else mRepeatLimit
            repeatRule = mRepeatRule
            color = mEventColor
        }

        if (mTask.getReminders().isNotEmpty()) {
            handleNotificationPermission {
                if (it) {
                    ensureBackgroundThread {
                        storeTask(wasRepeatable)
                    }
                } else {
                    PermissionRequiredDialog(this,com.simplemobiletools.commons.R.string.allow_notifications_reminders,{
                        openNotificationSettings()
                    })
                }
            }
        } else {
            storeTask(wasRepeatable)
        }
    }

    private fun storeTask(wasRepeatable: Boolean) {
        if (mTask.id == null) {
            eventsHandler.insertTask(mTask, true) {
                hideKeyboard()

                if (DateTime.now().isAfter(mTaskDateTime.millis)) {
                    if (mTask.repeatInterval == 0 && mTask.getReminders().any { it.type == REMINDER_NOTIFICATION }) {
                        notifyEvent(mTask)
                    }
                }

                finish()
            }
        } else {
//            if (mRepeatInterval > 0 && wasRepeatable) {
//                runOnUiThread {
//                    showEditRepeatingTaskDialog()
//                }
//            } else {
                hideKeyboard()
                eventsHandler.updateEvent(mTask, updateAtCalDAV = false, showToasts = true) {
                    finish()
                }
//            }
        }
    }

    override fun getAppIconIDs(): ArrayList<Int> {
        TODO("Not yet implemented")

    }

    override fun getAppLauncherName(): String {
        TODO("Not yet implemented")
    }

    private fun isMaleGender(day: Int) = day == 1 || day == 2 || day == 4 || day == 5

    private fun showTaskColorDialog() {
        hideKeyboard()
        ensureBackgroundThread {
            val eventType = eventTypesDB.gewtEventTypeWithId(mEventTypeId)
            var currentColor: Int = 0
            if (mEventColor == 0) {
                if (eventType != null) {
                    currentColor = eventType.color
                }
            } else {
                mEventColor
            }

            runOnUiThread {
                ColorPickerDialog(
                    activity = this,
                    color = currentColor,
                    addDefaultColorButton = true
                ) { wasPositivePressed, newColor ->
                    if (wasPositivePressed) {
                        if (newColor != currentColor) {
                            mEventColor = newColor
                            if (eventType != null) {
                                updateTaskColorInfo(eventType.color)
                            }
                        }
                    }
                }

            }
        }
    }

    private fun updateTaskColorInfo(defaultColor: Int) {
        val taskColor = if (mEventColor == 0) {
            defaultColor
        } else {
            mEventColor
        }
        binding.taskColor.setFillWithStroke(taskColor,getProperTextColor())
    }

    /** Update colors tu nhung mau da chon cho theme */
    private fun updateColors() {
        binding.apply {
            updateTextColors(taskNestedScrollview)
            val textColor = getProperTextColor()
            arrayOf(
                taskTimeImage, taskReminderImage, taskRepetitionImage, taskColorImage
            ).forEach {
                it.applyColorFilter(textColor)
            }
        }
    }
}