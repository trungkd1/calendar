package com.trungkieu.mycalendar.views

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.SparseIntArray
import android.view.View
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.COLUMN_COUNT
import com.simplemobiletools.commons.helpers.HIGHER_ALPHA
import com.simplemobiletools.commons.helpers.LOG_TAG
import com.simplemobiletools.commons.helpers.LOWER_ALPHA
import com.simplemobiletools.commons.helpers.MEDIUM_ALPHA
import com.simplemobiletools.commons.helpers.ROW_COUNT
import com.trungkieu.mycalendar.R
import com.trungkieu.mycalendar.entity.EventEntity
import com.trungkieu.mycalendar.extensions.config
import com.trungkieu.mycalendar.extensions.getProperDayIndexInWeek
import com.trungkieu.mycalendar.extensions.isWeekendIndex
import com.trungkieu.mycalendar.extensions.seconds
import com.trungkieu.mycalendar.extensions.withFirstDayOfWeekToFront
import com.trungkieu.mycalendar.helper.Formatter
import com.trungkieu.mycalendar.models.DayMonthly
import com.trungkieu.mycalendar.models.MonthViewEvent
import org.joda.time.DateTime
import org.joda.time.Days
import kotlin.math.max
import kotlin.math.min

// used in the Monthly view fragment, 1 view per screen
class MonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private val BG_CORNER_RADIUS = 8f

    private var textPaint: Paint
    // Màu sắc của kkhung chứa các chữ M,t,W,T
    private var letterPaint: Paint
    private var eventTitlePaint: TextPaint
    // gridPaint là màu viền của từng ô gridview, để hiển thị gridview thì line 194 phải xoá điều kiện
    private var gridPaint: Paint
    // Vòng tròn hiển thị ngày hiện tại
    private var circleStrokePaint: Paint
    private var config = context.config
    // Chieu cao va chieu dai cac o trong Grid
    private var dayWidth = 0f
    private var dayHeight = 0f

    // Màu sắc cho ngày hiện tại
    private var primaryColor = 0
    // màu sắc cho các chữ trong monthview
    private var textColor = 0
    // màu sắc cho các số trong monthview
    private var numberColor = 0
    private var weekendsTextColor = 0

    // Chiều cao của các ô hiển thị ký tự : M, T,W...
    private var weekDaysLetterHeight = 0

    // CHieu cao cua chu trong event
    private var eventTitleHeight = 0

    // ?
    private var currDayOfWeek = 0

    // padding cho cac event xuất hiện trong ô
    private var smallPadding = 0

    // so event co the xuat hien trong moi o vuong
    private var maxEventsPerDay = 0

    // khoảng cách giữa 2 mép màn hình bên trái với kkhung
    private var horizontalOffset = 0

    // ?
    private var showWeekNumbers = false

    // ?
    private var dimPastEvents = true
    private var dimCompletedTasks = true

    // Hien thi mau sac 2 ngay cuoi tuan, neu = true thi se lien quan den weekendsTextColor
    private var highlightWeekends = false
    private var isPrintVersion = false
    private var isMonthDayView = false
    private var allEvents = ArrayList<MonthViewEvent>()
    private var bgRectF = RectF()
    private var dayTextRect = Rect()
    private var dayLetters = ArrayList<String>()
    private var days = ArrayList<DayMonthly>()
    private var dayVerticalOffsets = SparseIntArray()
    private var selectedDayCoords = Point(-1, -1)

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
//        primaryColor = context.getProperPrimaryColor()
        primaryColor = context.getColor(com.simplemobiletools.commons.R.color.md_orange_100)
        /**done*/
//        textColor = context.getProperTextColor()
        textColor = context.getColor(R.color.white)
        /**done*/
        numberColor = context.getColor(R.color.black)

//        weekendsTextColor = config.highlightWeekendsColor
        weekendsTextColor = context.getColor(com.simplemobiletools.commons.R.color.md_light_green)

        showWeekNumbers = config.showWeekNumbers
        /** done */
//        dimPastEvents = config.dimPastEvents
        dimCompletedTasks = config.dimCompletedTasks
        highlightWeekends = config.highlightWeekends

        /** done */
        smallPadding = resources.displayMetrics.density.toInt()

        val normalTextSize = resources.getDimensionPixelSize(com.simplemobiletools.commons.R.dimen.normal_text_size)
        /** done */
//        weekDaysLetterHeight = normalTextSize * 2
        weekDaysLetterHeight = normalTextSize * 3

        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            /** RPR */
            color = textColor
            textSize = normalTextSize.toFloat()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(com.simplemobiletools.commons.R.color.md_light_green)
        }

        /**Done*/
//        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//            color = textColor.adjustAlpha(LOWER_ALPHA)
//        }

        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(com.simplemobiletools.commons.R.color.md_green)
        }

        circleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.getDimension(R.dimen.circle_stroke_width)
            color = primaryColor
        }

        val smallerTextSize = resources.getDimensionPixelSize(com.simplemobiletools.commons.R.dimen.smaller_text_size)
        /**RPR*/
//        eventTitleHeight = smallerTextSize
        eventTitleHeight = smallerTextSize

        eventTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = smallerTextSize.toFloat()
            textAlign = Paint.Align.LEFT
        }

        initWeekDayLetters()
        setupCurrentDayOfWeekIndex()
    }

    fun updateDays(newDays: ArrayList<DayMonthly>, isMonthDayView: Boolean) {
        this.isMonthDayView = isMonthDayView
        days = newDays
        showWeekNumbers = config.showWeekNumbers
        horizontalOffset = if (showWeekNumbers) eventTitleHeight * 2 else 0
        initWeekDayLetters()
        setupCurrentDayOfWeekIndex()
        groupAllEvents()
        invalidate()
    }

    private fun groupAllEvents() {
        days.forEach { day ->
            val dayIndexOnMonthView = day.indexOnMonthView

            day.dayEvents.forEach { event ->
                // make sure we properly handle events lasting multiple days and repeating ones
                val validDayEvent = isDayValid(event, day.code)
                val lastEvent = allEvents.lastOrNull { it.id == event.id }
                val notYetAddedOrIsRepeatingEvent = lastEvent == null || lastEvent.endTS <= event.startTS

                // handle overlapping repeating events e.g. an event that lasts 3 days, but repeats every 2 days has a one day overlap
                val canOverlap = event.endTS - event.startTS > event.repeatInterval
                val shouldAddEvent = notYetAddedOrIsRepeatingEvent || canOverlap && (lastEvent!!.startTS < event.startTS)

                if (shouldAddEvent && !validDayEvent) {
                    val daysCnt = getEventLastingDaysCount(event)

                    val monthViewEvent = MonthViewEvent(
                        event.id!!, event.title, event.startTS, event.endTS, event.color, dayIndexOnMonthView,
                        daysCnt, dayIndexOnMonthView, event.getIsAllDay(), event.isPastEvent, event.isTask(), event.isTaskCompleted()
                    )
                    allEvents.add(monthViewEvent)
                }
            }
        }

        allEvents = allEvents.asSequence().sortedWith(
            compareBy({ -it.daysCnt }, { !it.isAllDay }, { it.startTS }, { it.endTS }, { it.startDayIndex }, { it.title })
        ).toMutableList() as ArrayList<MonthViewEvent>
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        dayVerticalOffsets.clear()
        measureDaySize(canvas)
        /**RPR*/
//        if (config.showGrid && !isMonthDayView) {
            drawGrid(canvas)
//        }

        addWeekDayLetters(canvas)
        if (showWeekNumbers && days.isNotEmpty()) {
            addWeekNumbers(canvas)
        }

        var curId = 0
        for (y in 0 until ROW_COUNT) {
            for (x in 0 until COLUMN_COUNT) {
                val day = days.getOrNull(curId)
                if (day != null) {
                    dayVerticalOffsets.put(day.indexOnMonthView, dayVerticalOffsets[day.indexOnMonthView] + weekDaysLetterHeight)
                    val verticalOffset = dayVerticalOffsets[day.indexOnMonthView]
                    val xPos = x * dayWidth + horizontalOffset
                    val yPos = y * dayHeight + verticalOffset + 15
                    val xPosCenter = xPos + dayWidth / 2
                    val dayNumber = day.value.toString()

                    val textPaint = getTextPaint(day)
                    if (selectedDayCoords.x != -1 && x == selectedDayCoords.x && y == selectedDayCoords.y) {
                        canvas.drawCircle(xPosCenter, yPos + textPaint.textSize * 0.7f, textPaint.textSize * 0.8f, circleStrokePaint)
                        if (day.isToday) {
                            textPaint.color = textColor

                        }
                    } else if (day.isToday && !isPrintVersion) {
                        canvas.drawCircle(xPosCenter, yPos + textPaint.textSize * 0.7f, textPaint.textSize * 0.8f, getCirclePaint(day))
                    }

                    // mark days with events with a dot
                    if (isMonthDayView && day.dayEvents.isNotEmpty()) {
                        getCirclePaint(day).getTextBounds(dayNumber, 0, dayNumber.length, dayTextRect)
                        val height = dayTextRect.height() * 1.25f
                        canvas.drawCircle(
                            xPosCenter,
                            yPos + height + textPaint.textSize / 2,
                            textPaint.textSize * 0.2f,
                            getDayEventColor(day.dayEvents.first())
                        )
                    }
                    /**day la cho to mau cho text*/
                    canvas.drawText(dayNumber, xPosCenter, yPos + textPaint.textSize, textPaint)
                    dayVerticalOffsets.put(day.indexOnMonthView, (verticalOffset + textPaint.textSize * 2).toInt())
                }
                curId++
            }
        }

        if (!isMonthDayView) {
            for (event in allEvents) {
                drawEvent(event, canvas)
            }
        }
    }

    private fun drawGrid(canvas: Canvas) {
        // vertical lines
        for (i in 0 until COLUMN_COUNT) {
            var lineX = i * dayWidth
            if (showWeekNumbers) {
                lineX += horizontalOffset
            }
            canvas.drawLine(lineX, 0f, lineX, canvas.height.toFloat(), gridPaint)

        }
        canvas.drawRect(0f, 5 * dayHeight + weekDaysLetterHeight, canvas.width.toFloat(), 5 * dayHeight + weekDaysLetterHeight, circleStrokePaint.apply {
            style = Paint.Style.FILL })

        // horizontal lines
        canvas.drawLine(0f, 0f, canvas.width.toFloat(), 0f, gridPaint)
        for (i in 0 until ROW_COUNT) {

            canvas.drawLine(0f, i * dayHeight + weekDaysLetterHeight, canvas.width.toFloat(), i * dayHeight + weekDaysLetterHeight, gridPaint)
        }
        canvas.drawLine(0f, canvas.height.toFloat(), canvas.width.toFloat(), canvas.height.toFloat(), gridPaint)

        /**hinh 1 với toạ độ từ trên xuống, tại góc trái trên cùng màn hình toạ độ, left 0 và top 0*/
//        canvas.drawRect(
//            0f, 0f, 108.0f, 132.5120f,
//            circleStrokePaint.apply {
//                style = Paint.Style.FILL
//            }
//        )
        /**hinh 2*/
//        canvas.drawRect(
//            0f, 100f, 108.0f, 132.5120f,
//            circleStrokePaint.apply {
//                style = Paint.Style.FILL
//            }
//        )

        canvas.drawRect(
            0f, 0f, canvas.width.toFloat(), weekDaysLetterHeight.toFloat(),
            letterPaint.apply {
                style = Paint.Style.FILL
            }
        )
    }


    /**
     * Add week day letters
     * Tạo khung có các ký tự, như M,T,W,TH,
     * @param canvas
     */
    private fun addWeekDayLetters(canvas: Canvas) {
        for (i in 0 until COLUMN_COUNT) {
            val xPos = horizontalOffset + (i + 1) * dayWidth - dayWidth / 2
            var weekDayLetterPaint = textPaint

            // Thiết lập màu cho khung day letter, là khung chứa các ký tự :  M,T,W,TH,
            if (i == currDayOfWeek && !isPrintVersion) {
                weekDayLetterPaint = getColoredPaint(primaryColor)
            } else if (highlightWeekends && context.isWeekendIndex(i)) {
                weekDayLetterPaint = getColoredPaint(textColor)
//                canvas.drawColor(context.getColor(R.color.purple_200))
            }
            canvas.drawText(dayLetters[i], xPos, weekDaysLetterHeight * 0.7f, weekDayLetterPaint)
        }
    }

    private fun addWeekNumbers(canvas: Canvas) {
        val weekNumberPaint = Paint(textPaint)
        weekNumberPaint.textAlign = Paint.Align.RIGHT

        for (i in 0 until ROW_COUNT) {
            val weekDays = days.subList(i * 7, i * 7 + 7)
            weekNumberPaint.color = if (weekDays.any { it.isToday && !isPrintVersion }) primaryColor else textColor

            // fourth day of the week determines the week of the year number
            val weekOfYear = days.getOrNull(i * 7 + 3)?.weekOfYear ?: 1
            val id = "$weekOfYear:"
            val yPos = i * dayHeight + weekDaysLetterHeight
            canvas.drawText(id, horizontalOffset.toFloat() * 0.9f, yPos + textPaint.textSize, weekNumberPaint)
        }
    }

    private fun measureDaySize(canvas: Canvas) {
        /**RPR*/
        // dayWidth = 0, dayHeight = 13.3
        dayWidth = (canvas.width - horizontalOffset) / 7f
        dayHeight = (canvas.height - weekDaysLetterHeight) / ROW_COUNT.toFloat()
        val availableHeightForEvents = dayHeight.toInt() - weekDaysLetterHeight
        maxEventsPerDay = availableHeightForEvents / eventTitleHeight
    }

    private fun drawEvent(event: MonthViewEvent, canvas: Canvas) {
        var verticalOffset = 0
        for (i in 0 until min(event.daysCnt, 7 - event.startDayIndex % 7)) {
            verticalOffset = max(verticalOffset, dayVerticalOffsets[event.startDayIndex + i])
        }
        val xPos = event.startDayIndex % 7 * dayWidth + horizontalOffset
        val yPos = (event.startDayIndex / 7) * dayHeight + 30
        val xPosCenter = xPos + dayWidth / 2

        if (verticalOffset - eventTitleHeight * 2 > dayHeight) {
            val paint = getTextPaint(days[event.startDayIndex])
            paint.color = textColor
            canvas.drawText("...", xPosCenter, yPos + verticalOffset - eventTitleHeight / 2, paint)
            return
        }

        // event background rectangle
        val backgroundY = yPos + verticalOffset
        val bgLeft = xPos + smallPadding
        val bgTop = backgroundY + smallPadding - eventTitleHeight
        var bgRight = xPos - smallPadding + dayWidth * event.daysCnt
        val bgBottom = backgroundY + smallPadding * 2
        if (bgRight > canvas.width.toFloat()) {
            bgRight = canvas.width.toFloat() - smallPadding
            val newStartDayIndex = (event.startDayIndex / 7 + 1) * 7
            if (newStartDayIndex < 42) {
                val newEvent = event.copy(startDayIndex = newStartDayIndex, daysCnt = event.daysCnt - (newStartDayIndex - event.startDayIndex))
                drawEvent(newEvent, canvas)
            }
        }

        val startDayIndex = days[event.originalStartDayIndex]
        val endDayIndex = days[min(event.startDayIndex + event.daysCnt - 1, 41)]
        bgRectF.set(bgLeft, bgTop, bgRight, bgBottom)
        canvas.drawRoundRect(bgRectF, BG_CORNER_RADIUS, BG_CORNER_RADIUS, getEventBackgroundColor(event, startDayIndex, endDayIndex))

        val specificEventTitlePaint = getEventTitlePaint(event, startDayIndex, endDayIndex)
        var taskIconWidth = 0
        if (event.isTask) {
            val taskIcon = resources.getColoredDrawableWithColor(R.drawable.ic_task_vector, specificEventTitlePaint.color).mutate()
            val taskIconY = yPos.toInt() + verticalOffset - eventTitleHeight + smallPadding * 2
            taskIcon.setBounds(xPos.toInt() + smallPadding * 2, taskIconY, xPos.toInt() + eventTitleHeight + smallPadding * 2, taskIconY + eventTitleHeight)
            taskIcon.draw(canvas)
            taskIconWidth += eventTitleHeight + smallPadding
        }

        drawEventTitle(event, canvas, xPos + taskIconWidth, yPos + verticalOffset, bgRight - bgLeft - smallPadding - taskIconWidth, specificEventTitlePaint)

        for (i in 0 until min(event.daysCnt, 7 - event.startDayIndex % 7)) {
            dayVerticalOffsets.put(event.startDayIndex + i, verticalOffset + eventTitleHeight + smallPadding * 2)
        }
    }

    private fun drawEventTitle(event: MonthViewEvent, canvas: Canvas, x: Float, y: Float, availableWidth: Float, paint: Paint) {
        val ellipsized = TextUtils.ellipsize(event.title, eventTitlePaint, availableWidth - smallPadding, TextUtils.TruncateAt.END)
        canvas.drawText(event.title, 0, ellipsized.length, x + smallPadding * 2, y, paint)
    }


    /**
     * Get text paint, hiển thị màu sắc cho các số trong tháng
     *
     * @param startDay
     * @return
     */
    private fun getTextPaint(startDay: DayMonthly): Paint {
        /**RPR*/
        var paintColor = numberColor
        if (!isPrintVersion) {
            if (startDay.isToday) {
                paintColor = primaryColor.getContrastColor()
            } else if (highlightWeekends && startDay.isWeekend) {
                paintColor = weekendsTextColor
            }
        }

        // Kiểm tra những ngày của tháng hiện tại, nếu ngày  không thuộc tháng hiện tại sẽ bị làm mờ đi
        if (!startDay.isThisMonth) {
//            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
            paintColor = context.getColor(com.simplemobiletools.commons.R.color.md_light_green_100)

        }
        return getColoredPaint(paintColor)
    }

    private fun getColoredPaint(color: Int): Paint {
        val curPaint = Paint(textPaint)
        curPaint.color = color
        return curPaint
    }

    private fun getEventBackgroundColor(event: MonthViewEvent, startDay: DayMonthly, endDay: DayMonthly): Paint {
        var paintColor = event.color

        val adjustAlpha = when {
            event.isTask -> dimCompletedTasks && event.isTaskCompleted
            !startDay.isThisMonth && !endDay.isThisMonth -> true
            else -> dimPastEvents && event.isPastEvent && !isPrintVersion
        }

        if (adjustAlpha) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
        }

        return getColoredPaint(paintColor)
    }

    private fun getEventTitlePaint(event: MonthViewEvent, startDay: DayMonthly, endDay: DayMonthly): Paint {
        var paintColor = event.color.getContrastColor()
        val adjustAlpha = when {
            event.isTask -> dimCompletedTasks && event.isTaskCompleted
            !startDay.isThisMonth && !endDay.isThisMonth -> true
            else -> dimPastEvents && event.isPastEvent && !isPrintVersion
        }

        if (adjustAlpha) {
            paintColor = paintColor.adjustAlpha(HIGHER_ALPHA)
        }

        val curPaint = Paint(eventTitlePaint)
        curPaint.color = paintColor
        curPaint.isStrikeThruText = event.isTaskCompleted
        return curPaint
    }

    private fun getCirclePaint(day: DayMonthly): Paint {
        val curPaint = Paint(textPaint)
        var paintColor = primaryColor
        if (!day.isThisMonth) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
        }
        curPaint.color = paintColor
        return curPaint
    }

    private fun getDayEventColor(event: EventEntity): Paint {
        val curPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        curPaint.color = event.color
        return curPaint
    }


    /** Init week day letters : có nghĩa là lấy các ký tự thứ trong tháng, vd: M,T,W...*/
    private fun initWeekDayLetters() {
        dayLetters = context.withFirstDayOfWeekToFront(context.resources.getStringArray(com.simplemobiletools.commons.R.array.week_day_letters).toList())
    }

    private fun setupCurrentDayOfWeekIndex() {
        if (days.firstOrNull { it.isToday && it.isThisMonth } == null) {
            currDayOfWeek = -1
            return
        }

        /** RPR */
        currDayOfWeek = context.getProperDayIndexInWeek(DateTime())
//        currDayOfWeek = 10
    }

    // take into account cases when an event starts on the previous screen, subtract those days
    private fun getEventLastingDaysCount(event: EventEntity): Int {
        val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
        val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
        val code = days.first().code
        val screenStartDateTime = Formatter.getDateTimeFromCode(code).toLocalDate()
        var eventStartDateTime = Formatter.getDateTimeFromTS(startDateTime.seconds()).toLocalDate()
        val eventEndDateTime = Formatter.getDateTimeFromTS(endDateTime.seconds()).toLocalDate()
        val diff = Days.daysBetween(screenStartDateTime, eventStartDateTime).days
        if (diff < 0) {
            eventStartDateTime = screenStartDateTime
        }

        val isMidnight = Formatter.getDateTimeFromTS(endDateTime.seconds()) == Formatter.getDateTimeFromTS(endDateTime.seconds()).withTimeAtStartOfDay()
        val numDays = Days.daysBetween(eventStartDateTime, eventEndDateTime).days
        val daysCnt = if (numDays == 1 && isMidnight) 0 else numDays
        return daysCnt + 1
    }

    private fun isDayValid(event: EventEntity, code: String): Boolean {
        val date = Formatter.getDateTimeFromCode(code)
        return event.startTS != event.endTS && Formatter.getDateTimeFromTS(event.endTS) == Formatter.getDateTimeFromTS(date.seconds()).withTimeAtStartOfDay()
    }

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(com.simplemobiletools.commons.R.color.theme_light_text_color)
        } else {
            context.getProperTextColor()
        }

        textPaint.color = textColor
        /**RPR*/
        gridPaint.color = textColor.adjustAlpha(LOWER_ALPHA)
        invalidate()
        initWeekDayLetters()
    }

    fun updateCurrentlySelectedDay(x: Int, y: Int) {
        selectedDayCoords = Point(x, y)
        invalidate()
    }
}
