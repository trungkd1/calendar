package com.trungkieu.mycalendar.fragments

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.areSystemAnimationsEnabled
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.isGone
import com.simplemobiletools.commons.extensions.onGlobalLayout
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.underlineText
import com.simplemobiletools.commons.helpers.EVENTS_LIST_VIEW
import com.simplemobiletools.commons.helpers.FETCH_INTERVAL
import com.simplemobiletools.commons.helpers.INITIAL_EVENTS
import com.simplemobiletools.commons.helpers.MIN_EVENTS_TRESHOLD
import com.simplemobiletools.commons.helpers.UPDATE_BOTTOM
import com.simplemobiletools.commons.helpers.UPDATE_TOP
import com.simplemobiletools.commons.views.MyLinearLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView
import com.trungkieu.mycalendar.MainActivity
import com.trungkieu.mycalendar.R
import com.trungkieu.mycalendar.adapters.EventListAdapter
import com.trungkieu.mycalendar.databinding.FragmentEventListBinding
import com.trungkieu.mycalendar.entity.EventEntity
import com.trungkieu.mycalendar.extensions.config
import com.trungkieu.mycalendar.extensions.editEvent
import com.trungkieu.mycalendar.extensions.eventsHandler
import com.trungkieu.mycalendar.extensions.getEventListItems
import com.trungkieu.mycalendar.extensions.getViewBitmap
import com.trungkieu.mycalendar.extensions.launchNewEventIntent
import com.trungkieu.mycalendar.extensions.printBitmap
import com.trungkieu.mycalendar.extensions.seconds
import com.trungkieu.mycalendar.helper.Formatter
import com.trungkieu.mycalendar.interfaces.RefreshRecyclerViewListener
import com.trungkieu.mycalendar.models.ListEvent
import com.trungkieu.mycalendar.models.ListItem
import com.trungkieu.mycalendar.models.ListSectionDay
import org.joda.time.DateTime

class EventListFragment : MyFragmentHolder(), RefreshRecyclerViewListener {

    private var updateStatus = 0
    private var mEvents = ArrayList<EventEntity>()
    private var minFetchedTS = 0L
    private var maxFetchedTS = 0L
    private var wereInitialEventsAdded = false
    private var hasBeenScrolled = false
    private var bottomItemAtRefresh: ListItem? = null

    private var use24HourFormat = false

    private lateinit var binding: FragmentEventListBinding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEventListBinding.inflate(inflater,container,false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        binding.calendarEventsListHolder.id = (System.currentTimeMillis() % 1000000).toInt()
        binding.calendarEmptyListPlaceholder2.apply {
            setTextColor(context.getProperPrimaryColor())
            underlineText()
            setOnClickListener{
                activity?.hideKeyboard()
                context.launchNewEventIntent(getNewEventDayCode())
            }
        }
        use24HourFormat = requireContext().config.use24HourFormat

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
        val use24Hour = requireContext().config.use24HourFormat
        if (use24Hour != use24HourFormat) {
            use24HourFormat = use24Hour
            (binding.calendarEventsList.adapter as? EventListAdapter)?.toggle24HourFormat(use24HourFormat)
        }
    }

    private fun checkEvents() {
        if (!wereInitialEventsAdded) {
            minFetchedTS = DateTime().minusMinutes(requireContext().config.displayPastEvents).seconds()
            maxFetchedTS = DateTime().plusMonths(6).seconds()
        }

        requireContext().eventsHandler.getEvents(minFetchedTS, maxFetchedTS) { events ->
            if (events.size >= MIN_EVENTS_TRESHOLD) {
                receivedEvents(events, INITIAL_EVENTS)
            } else {
                if (!wereInitialEventsAdded) {
                    maxFetchedTS += FETCH_INTERVAL
                }

                /**
                 * cho nay hien tai dang query = 0, can check lai gia tri
                 * */
                requireContext().eventsHandler.getEvents(minFetchedTS, maxFetchedTS) {
                    mEvents = it
                    receivedEvents(mEvents, INITIAL_EVENTS, !wereInitialEventsAdded)
                }
            }
            wereInitialEventsAdded = true
        }
    }


    private fun receivedEvents(events: ArrayList<EventEntity>, initialEvents: Int, forceRecreation: Boolean = false) {
        if (context == null || activity == null) {
            return
        }

        mEvents = events
        val listItems = requireContext().getEventListItems(mEvents)

        activity?.runOnUiThread {
            if (activity == null) {
                return@runOnUiThread
            }

            val currAdapter = binding.calendarEventsList.adapter
            if (currAdapter == null || forceRecreation) {
                EventListAdapter(activity as BaseSimpleActivity, listItems, true, this, binding.calendarEventsList) {
                    if (it is ListEvent) {
                        context?.editEvent(it)
                    }
                }.apply {
                    binding.calendarEventsList.adapter = this
                }

                if (requireContext().areSystemAnimationsEnabled) {
                    binding.calendarEventsList.scheduleLayoutAnimation()
                }

                binding.calendarEventsList.endlessScrollListener = object : MyRecyclerView.EndlessScrollListener {
                    override fun updateTop() {
                        fetchPreviousPeriod()
                    }

                    override fun updateBottom() {
                        fetchNextPeriod()
                    }
                }

                binding.calendarEventsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (!hasBeenScrolled) {
                            hasBeenScrolled = true
                            (activity as? MainActivity)?.refreshItems()
                            (activity as? MainActivity)?.refreshMenuItems()
                        }
                    }
                })
            } else {
                (currAdapter as EventListAdapter).updateListItems(listItems)
                if (updateStatus == UPDATE_TOP) {
                    val item = listItems.indexOfFirst { it == bottomItemAtRefresh }
                    if (item != -1) {
                        binding.calendarEventsList.scrollToPosition(item)
                    }
                } else if (updateStatus == UPDATE_BOTTOM) {
                    binding.calendarEventsList.smoothScrollBy(0, requireContext().resources.getDimension(
                        R.dimen.endless_scroll_move_height).toInt())
                }
            }
            checkPlaceholderVisibility()
        }
    }

    private fun checkPlaceholderVisibility() {
        binding.calendarEmptyListPlaceholder.beVisibleIf(mEvents.isEmpty())
        binding.calendarEmptyListPlaceholder2.beVisibleIf(mEvents.isEmpty())
        binding.calendarEventsList.beGoneIf(mEvents.isEmpty())
        if (activity != null) {
            binding.calendarEmptyListPlaceholder.setTextColor(requireActivity().getProperTextColor())
            if (mEvents.isEmpty()) {
                val placeholderTextId = if (requireActivity().config.displayEventTypes.isEmpty()) {
                    R.string.everything_filtered_out
                } else {
                    R.string.no_upcoming_events
                }

                binding.calendarEmptyListPlaceholder.setText(placeholderTextId)
            }
        }
    }

    override val viewType: Int
        get() = EVENTS_LIST_VIEW

    override fun goToToday() {
        val listItems = requireContext().getEventListItems(mEvents)
        val firstNonPastSectionIndex = listItems.indexOfFirst { it is ListSectionDay && !it.isPastSection }
        if (firstNonPastSectionIndex != -1) {
            (binding.calendarEventsList.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(firstNonPastSectionIndex, 0)
            binding.calendarEventsList.onGlobalLayout {
                hasBeenScrolled = false
                (activity as? MainActivity)?.refreshItems()
                (activity as? MainActivity)?.refreshMenuItems()
            }
        }
    }

    private fun fetchPreviousPeriod() {
        val lastPosition = (binding.calendarEventsList.layoutManager as MyLinearLayoutManager).findLastVisibleItemPosition()
        bottomItemAtRefresh = (binding.calendarEventsList.adapter as EventListAdapter).listItems[lastPosition]

        val oldMinFetchedTS = minFetchedTS - 1
        minFetchedTS -= FETCH_INTERVAL
        requireContext().eventsHandler.getEvents(minFetchedTS, oldMinFetchedTS) {
            it.forEach { event ->
                if (mEvents.firstOrNull { it.id == event.id && it.startTS == event.startTS } == null) {
                    mEvents.add(0, event)
                }
            }

            receivedEvents(mEvents, UPDATE_TOP)
        }
    }

    private fun fetchNextPeriod() {
        val oldMaxFetchedTS = maxFetchedTS + 1
        maxFetchedTS += FETCH_INTERVAL
        requireContext().eventsHandler.getEvents(oldMaxFetchedTS, maxFetchedTS) {
            it.forEach { event ->
                if (mEvents.firstOrNull { it.id == event.id && it.startTS == event.startTS } == null) {
                    mEvents.add(0, event)
                }
            }

            receivedEvents(mEvents, UPDATE_BOTTOM)
        }
    }

    override fun showGoToDateDialog() {
//        TODO("Not yet implemented")
    }

    override fun refreshEvents() {
        checkEvents()
    }

    override fun shouldGoToTodayBeVisible(): Boolean {
        return hasBeenScrolled
    }

    override fun getNewEventDayCode(): String {
        return Formatter.getTodayCode()
    }

    override fun printView() {
        binding.apply {
            if (calendarEventsList.isGone()) {
                context?.toast(com.simplemobiletools.commons.R.string.no_items_found)
                return@apply
            }
            (calendarEventsList.adapter as? EventListAdapter)?.togglePrintMode()
            Handler().postDelayed({
                requireContext().printBitmap(calendarEventsList.getViewBitmap())
                Handler().postDelayed({
                    (calendarEventsList.adapter as? EventListAdapter)?.togglePrintMode()
                }, 1000)
            }, 1000)

        }
    }

    override fun getCurrentDate(): DateTime? {
        return null
    }

    override fun refreshItems() {
//        TODO("Not yet implemented")
    }
}