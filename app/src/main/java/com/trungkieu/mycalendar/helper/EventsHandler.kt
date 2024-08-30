package com.trungkieu.mycalendar.helper

import android.content.Context
import android.util.Log
import android.util.LongSparseArray
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.helpers.LOG_TAG
import com.simplemobiletools.commons.helpers.SOURCE_SIMPLE_CALENDAR
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.trungkieu.mycalendar.entity.EventEntity
import com.trungkieu.mycalendar.extensions.completedTasksDB
import com.trungkieu.mycalendar.extensions.config
import com.trungkieu.mycalendar.extensions.eventTypesDB
import com.trungkieu.mycalendar.extensions.eventsDB
import com.trungkieu.mycalendar.extensions.getDisplayEventTypessAsList
import com.trungkieu.mycalendar.extensions.isTsOnProperDay
import com.trungkieu.mycalendar.extensions.isXWeeklyRepetition
import com.trungkieu.mycalendar.extensions.scheduleNextEventReminder
import com.trungkieu.mycalendar.extensions.updateWidgets

class EventsHandler(val context: Context) {

    private val eventsDB = context.eventsDB
    fun getEvents(
        fromTS: Long,
        toTS: Long,
        eventId: Long = -1L,
        applyTypeFilter: Boolean = true,
        searchQuery: String = "",
        callback: (events: ArrayList<EventEntity>) -> Unit
    ) {
        ensureBackgroundThread {
            getEventsSync(fromTS, toTS, eventId, applyTypeFilter, searchQuery, callback)
        }
    }


    /**
     * Get events sync
     * caa gioi thich cach thuc hoat dong cua cach xu ly trong day
     * @param fromTS
     * @param toTS
     * @param eventId
     * @param applyTypeFilter
     * @param searchQuery
     * @param callback
     * @receiver
     */
    fun getEventsSync (
        fromTS: Long,
        toTS: Long,
        eventId: Long = -1L,
        applyTypeFilter: Boolean,
        searchQuery: String = "",
        callback: (events: ArrayList<EventEntity>) -> Unit
    ){

//         　＝-1
        val birthDayEventId = getLocalBirthdaysEventTypeId(createIfNotExists = false)
        // -1
        val anniversaryEventId = getAnniversariesEventTypeId(createIfNotExists = false)

        var events = ArrayList<EventEntity>()
        if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (displayEventTypes.isEmpty()) {
                callback(ArrayList())
                return
            } else {
                try {
                    val typesList = context.config.getDisplayEventTypessAsList()

                    if (searchQuery.isEmpty()) {
                        events.addAll(eventsDB.getOneTimeEventsFromToWithTypes(toTS, fromTS, typesList).toMutableList() as ArrayList<EventEntity>)
                    } else {
                        events.addAll(
                            eventsDB.getOneTimeEventsFromToWithTypesForSearch(toTS, fromTS, typesList, "%$searchQuery%").toMutableList() as ArrayList<EventEntity>
                        )
                    }
                } catch (e: Exception) {
                }
            }
        } else {
            events.addAll(eventsDB.getTasksFromTo(fromTS, toTS, ArrayList()))

            events.addAll(
                if (eventId == -1L) {
                    eventsDB.getOneTimeEventsOrTasksFromTo(toTS, fromTS).toMutableList() as ArrayList<EventEntity>
                } else {
                    eventsDB.getOneTimeEventFromToWithId(eventId, toTS, fromTS).toMutableList() as ArrayList<EventEntity>
                }
            )
        }

        events.addAll(getRepeatableEventsFor(fromTS, toTS, eventId, applyTypeFilter, searchQuery))

        // chỗ này tô màu cho các event
        events = events
            .asSequence()
            .distinct()
            .filterNot { it.repetitionExceptions.contains(Formatter.getDayCodeFromTS(it.startTS)) }
            .toMutableList() as ArrayList<EventEntity>

        val eventTypeColors = LongSparseArray<Int>()
        context.eventTypesDB.getEventTypes().forEach {
            eventTypeColors.put(it.id!!, it.color)
        }

        events.forEach {
            if (it.isTask()) {
                updateIsTaskCompleted(it)
            }

            it.updateIsPastEvent()
            val originalEvent = eventsDB.getEventWithId(it.id!!)
            if (originalEvent != null &&
                (birthDayEventId != -1L && it.eventType == birthDayEventId) or
                (anniversaryEventId != -1L && it.eventType == anniversaryEventId)
            ) {
                val eventStartDate = Formatter.getDateFromTS(it.startTS)
                val originalEventStartDate = Formatter.getDateFromTS(originalEvent.startTS)
                if (it.hasMissingYear().not()) {
                    val years = (eventStartDate.year - originalEventStartDate.year).coerceAtLeast(0)
                    if (years > 0) {
                        it.title = "${it.title} ($years)"
                    }
                }
            }

            if (it.color == 0) {
                it.color = eventTypeColors.get(it.eventType) ?: context.getProperPrimaryColor()
            }
        }

        callback(events)

                // 　＝-1
//        val birthDayEventId = getLocalBirthdaysEventTypeId(createIfNotExists = false)
//        // -1
//        val anniversaryEventId = getAnniversariesEventTypeId(createIfNotExists = false)
//
//        var events = ArrayList<EventEntity>()
//        if (applyTypeFilter) {
//            val displayEventTypes = context.config.displayEventTypes
//            if (displayEventTypes.isEmpty()) {
//                callback(ArrayList())
//                return
//            } else {
//                try {
//                    val typesList = context.config.getDisplayEventTypessAsList()
//
//                    if (searchQuery.isEmpty()) {
//                        events.addAll(eventsDB.getOneTimeEventsFromToWithTypes(toTS, fromTS, typesList).toMutableList() as ArrayList<EventEntity>)
//                    } else {
//                        events.addAll(
//                            eventsDB.getOneTimeEventsFromToWithTypesForSearch(toTS, fromTS, typesList, "%$searchQuery%").toMutableList() as ArrayList<EventEntity>
//                        )
//                    }
//                } catch (e: Exception) {
//                }
//            }
//        } else {
//            events.addAll(eventsDB.getTasksFromTo(fromTS, toTS, ArrayList()))
//
//            events.addAll(
//                if (eventId == -1L) {
//                    eventsDB.getOneTimeEventsOrTasksFromTo(toTS, fromTS).toMutableList() as ArrayList<EventEntity>
//                } else {
//                    eventsDB.getOneTimeEventFromToWithId(eventId, toTS, fromTS).toMutableList() as ArrayList<EventEntity>
//                }
//            )
//        }
//
//        events.addAll(getRepeatableEventsFor(fromTS, toTS, eventId, applyTypeFilter, searchQuery))
//
//        events = events
//            .asSequence()
//            .distinct()
//            .filterNot { it.repetitionExceptions.contains(Formatter.getDayCodeFromTS(it.startTS)) }
//            .toMutableList() as ArrayList<EventEntity>
//
//        val eventTypeColors = LongSparseArray<Int>()
//        context.eventTypesDB.getEventTypes().forEach {
//            eventTypeColors.put(it.id!!, it.color)
//        }
//
//        events.forEach {
//            if (it.isTask()) {
//                updateIsTaskCompleted(it)
//            }
//
//            it.updateIsPastEvent()
//            val originalEvent = eventsDB.getEventWithId(it.id!!)
//            if (originalEvent != null &&
//                (birthDayEventId != -1L && it.eventType == birthDayEventId) or
//                (anniversaryEventId != -1L && it.eventType == anniversaryEventId)
//            ) {
//                val eventStartDate = Formatter.getDateFromTS(it.startTS)
//                val originalEventStartDate = Formatter.getDateFromTS(originalEvent.startTS)
//                if (it.hasMissingYear().not()) {
//                    val years = (eventStartDate.year - originalEventStartDate.year).coerceAtLeast(0)
//                    if (years > 0) {
//                        it.title = "${it.title} ($years)"
//                    }
//                }
//            }
//
//            if (it.color == 0) {
//                it.color = eventTypeColors.get(it.eventType) ?: context.getProperPrimaryColor()
//            }
//        }
//
//        callback(events)

    }

//    private fun getRepeatableEventsFor(
//        fromTS: Long,
//        toTS: Long,
//        eventId: Long,
//        applyTypeFilter: Boolean,
//        searchQuery: String
//    ): Collection<EventEntity> {
////        TODO("Not yet implemented")
//        val newEvents = ArrayList<EventEntity>()
//
//        return newEvents
//    }

    fun getRepeatableEventsFor(fromTS: Long, toTS: Long, eventId: Long = -1L, applyTypeFilter: Boolean = false, searchQuery: String = ""): List<EventEntity> {
        val events = if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (!displayEventTypes.isEmpty()) {
                /**
                 * đang sửa chỗ này
                 * */
//                return ArrayList()
                eventsDB.getRepeatableEventsOrTasksWithTypes(toTS, context.config.getDisplayEventTypessAsList()).toMutableList() as ArrayList<EventEntity>
            } else if (searchQuery.isEmpty()) {
                eventsDB.getRepeatableEventsOrTasksWithTypes(toTS, context.config.getDisplayEventTypessAsList()).toMutableList() as ArrayList<EventEntity>
            } else {
                eventsDB.getRepeatableEventsOrTasksWithTypesForSearch(toTS, context.config.getDisplayEventTypessAsList(), "%$searchQuery%")
                    .toMutableList() as ArrayList<EventEntity>
            }
        } else {
            if (eventId == -1L) {
                eventsDB.getRepeatableEventsOrTasksWithTypes(toTS).toMutableList() as ArrayList<EventEntity>
            } else {
                eventsDB.getRepeatableEventsOrTasksWithId(eventId, toTS).toMutableList() as ArrayList<EventEntity>
            }
        }

        val startTimes = androidx.collection.LongSparseArray<Long>()
        val newEvents = ArrayList<EventEntity>()
        events.forEach {
            startTimes.put(it.id!!, it.startTS)
            if (it.repeatLimit >= 0) {
                newEvents.addAll(getEventsRepeatingTillDateOrForever(fromTS, toTS, startTimes, it))
            } else {
                newEvents.addAll(getEventsRepeatingXTimes(fromTS, toTS, startTimes, it))
            }
        }

        return newEvents
    }

    private fun getEventsRepeatingXTimes(
        fromTS: Long,
        toTS: Long,
        startTimes: androidx.collection.LongSparseArray<Long>,
        it: Any
    ): Collection<EventEntity> {
        TODO("Not yet implemented")
    }

    private fun getEventsRepeatingTillDateOrForever(fromTS: Long, toTS: Long, startTimes: androidx.collection.LongSparseArray<Long>, event: EventEntity): ArrayList<EventEntity> {
        val original = event.copy()
        val events = ArrayList<EventEntity>()
        while (event.startTS <= toTS && (event.repeatLimit == 0L || event.repeatLimit >= event.startTS)) {
            if (event.endTS >= fromTS) {
                if (event.repeatInterval.isXWeeklyRepetition()) {
                    if (event.startTS.isTsOnProperDay(event)) {
                        if (event.isOnProperWeek(startTimes)) {
                            event.copy().apply {
                                updateIsPastEvent()
                                color = event.color
                                events.add(this)
                            }
                        }
                    }
                } else {
                    event.copy().apply {
                        updateIsPastEvent()
                        color = event.color
                        events.add(this)
                    }
                }
            }

            if (event.getIsAllDay()) {
                if (event.repeatInterval.isXWeeklyRepetition()) {
                    if (event.endTS >= toTS && event.startTS.isTsOnProperDay(event)) {
                        if (event.isOnProperWeek(startTimes)) {
                            event.copy().apply {
                                updateIsPastEvent()
                                color = event.color
                                events.add(this)
                            }
                        }
                    }
                } else {
                    val dayCode = Formatter.getDayCodeFromTS(fromTS)
                    val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
                    if (dayCode == endDayCode) {
                        event.copy().apply {
                            updateIsPastEvent()
                            color = event.color
                            events.add(this)
                        }
                    }
                }
            }
            event.addIntervalTime(original)
        }
        return events
    }

    private fun getAnniversariesEventTypeId(createIfNotExists: Boolean): Long {
//        TODO("Not ye//t implemented")
        return 0L
    }

    private fun getLocalBirthdaysEventTypeId(createIfNotExists: Boolean): Long {
//        TODO("Not yet implemented")

        return 0L
    }

    fun updateIsTaskCompleted(event: EventEntity) {
        val task = context.completedTasksDB.getTaskWithIdAndTs(event.id!!, startTs = event.startTS)
        event.flags = task?.flags ?: event.flags
        if (task != null) {
            event.flags = task.flags
        } else {
            event.flags = event.flags
        }
    }

    fun insertTask(task: EventEntity, showToasts: Boolean, enableEventType: Boolean = true, callback: () -> Unit) {
        maybeUpdateParentExceptions(task)
        task.id = eventsDB.insertOrUpdate(task)
        ensureEventTypeVisibility(task, enableEventType)
        context.updateWidgets()
        context.scheduleNextEventReminder(task, showToasts)
        callback()
    }

    fun updateEvent(event: EventEntity, updateAtCalDAV: Boolean, showToasts: Boolean, enableEventType: Boolean = true, callback: (() -> Unit)? = null) {
        eventsDB.insertOrUpdate(event)
        ensureEventTypeVisibility(event, enableEventType)
        context.updateWidgets()
        context.scheduleNextEventReminder(event, showToasts)
//        if (updateAtCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && context.config.caldavSync) {
//            context.calDAVHelper.updateCalDAVEvent(event)
//        }
        callback?.invoke()
    }


    private fun ensureEventTypeVisibility(event: EventEntity, enableEventType: Boolean) {
        if (enableEventType) {
            val eventType = event.eventType.toString()
            val displayEventTypes = context.config.displayEventTypes
            if (!displayEventTypes.contains(eventType)) {
                context.config.displayEventTypes = displayEventTypes.plus(eventType)
            }
        }
    }

    private fun maybeUpdateParentExceptions(task: Any) {
       // TODO("Not yet implemented")
    }
}