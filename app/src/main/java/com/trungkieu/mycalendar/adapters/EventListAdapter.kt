package com.trungkieu.mycalendar.adapters

import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.helpers.ITEM_EVENT
import com.simplemobiletools.commons.helpers.ITEM_SECTION_DAY
import com.simplemobiletools.commons.helpers.ITEM_SECTION_MONTH
import com.simplemobiletools.commons.helpers.LOG_TAG
import com.simplemobiletools.commons.helpers.MEDIUM_ALPHA
import com.simplemobiletools.commons.helpers.getNowSeconds
import com.simplemobiletools.commons.views.MyRecyclerView
import com.trungkieu.mycalendar.R
import com.trungkieu.mycalendar.databinding.EventItemSectionDayBinding
import com.trungkieu.mycalendar.databinding.EventItemSectionMonthBinding
import com.trungkieu.mycalendar.databinding.EventListItemBinding
import com.trungkieu.mycalendar.extensions.checkViewStrikeThrough
import com.trungkieu.mycalendar.extensions.config
import com.trungkieu.mycalendar.fragments.EventListFragment
import com.trungkieu.mycalendar.helper.Formatter
import com.trungkieu.mycalendar.models.ListEvent
import com.trungkieu.mycalendar.models.ListItem
import com.trungkieu.mycalendar.models.ListSectionDay
import com.trungkieu.mycalendar.models.ListSectionMonth

class EventListAdapter(
    activity: BaseSimpleActivity,
    var listItems: ArrayList<ListItem>,
    val allowLongClick: Boolean,
    val listener: EventListFragment,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private val allDayString = resources.getString(R.string.all_day)
    private val displayDescription = activity.config.displayDescription
    private val replaceDescription = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private val dimCompletedTasks = activity.config.dimCompletedTasks
    private val now = getNowSeconds()
    private var use24HourFormat = activity.config.use24HourFormat
    private var currentItemsHash = listItems.hashCode()
    private var isPrintVersion = false
    private val mediumMargin =
        activity.resources.getDimension(com.simplemobiletools.commons.R.dimen.medium_margin).toInt()

    init {
        setupDragListener(true)
        val firstNonPastSectionIndex =
            listItems.indexOfFirst { it is ListSectionDay && !it.isPastSection }
        if (firstNonPastSectionIndex != -1) {
            activity.runOnUiThread {
                recyclerView.scrollToPosition(firstNonPastSectionIndex)
            }
        }
    }

    override fun getActionMenuId(): Int {
        return R.menu.cab_event_list
    }

    override fun prepareActionMode(menu: Menu) {
        //TODO("Not yet implemented")
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    private fun askConfirmDelete() {
        TODO("Not yet implemented")
    }

    private fun shareEvents() {
        TODO("Not yet implemented")
    }

    override fun getSelectableItemCount(): Int {
        return listItems.filter {
            it is ListItem
        }.size
    }

    override fun getIsItemSelectable(position: Int): Boolean {
        return listItems[position] is ListEvent
    }

    override fun getItemSelectionKey(position: Int): Int? {
        return (listItems.getOrNull(position) as? ListEvent)?.hashCode()
    }

    override fun getItemKeyPosition(key: Int): Int {
        return listItems.indexOfFirst { (it as? ListEvent)?.hashCode() == key }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            listItems[position] is ListEvent -> ITEM_EVENT
            listItems[position] is ListSectionDay -> ITEM_SECTION_DAY
            else -> ITEM_SECTION_MONTH
        }
    }

    override fun onActionModeCreated() {
        TODO("Not yet implemented")
    }

    override fun onActionModeDestroyed() {
        TODO("Not yet implemented")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = activity.layoutInflater
        val binding = when (viewType) {
            ITEM_SECTION_DAY -> EventItemSectionDayBinding.inflate(layoutInflater, parent, false)
            ITEM_SECTION_MONTH -> EventItemSectionMonthBinding.inflate(
                layoutInflater,
                parent,
                false
            )

            else -> EventListItemBinding.inflate(layoutInflater, parent, false)
        }

        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listItem = listItems[position]
        holder.bindView(
            listItem,
            allowSingleClick = true,
            allowLongClick = allowLongClick && listItem is ListEvent
        ) { item, position ->
            when (listItem) {
                is ListSectionDay -> setupListSectionDay(item, listItem)
                is ListSectionMonth -> setupListSectionMonth(item, listItem)
                is ListEvent -> setupListEvent(item, listItem)
            }
        }
        bindViewHolder(holder)
    }

    private fun setupListEvent(view: View, listEvent: ListEvent) {
        EventListItemBinding.bind(view).apply {
            eventItemHolder.isSelected = selectedKeys.contains(listEvent.hashCode())
            eventItemHolder.background.applyColorFilter(textColor)
            eventItemTitle.text = listEvent.title
            eventItemTitle.checkViewStrikeThrough(listEvent.isTaskCompleted)
            eventItemTime.text = if (listEvent.isAllDay) allDayString else Formatter.getTimeFromTS(
                activity,
                listEvent.startTS
            )
            if (listEvent.startTS != listEvent.endTS) {
                if (!listEvent.isAllDay) {
                    eventItemTime.text = "${eventItemTime.text} - ${
                        Formatter.getTimeFromTS(
                            activity,
                            listEvent.endTS
                        )
                    }"
                }

                val startCode = Formatter.getDayCodeFromTS(listEvent.startTS)
                val endCode = Formatter.getDayCodeFromTS(listEvent.endTS)
                if (startCode != endCode) {
                    eventItemTime.text =
                        "${eventItemTime.text} (${Formatter.getDateDayTitle(endCode)})"
                }
            }

            eventItemDescription.text =
                if (replaceDescription) listEvent.location else listEvent.description.replace(
                    "\n",
                    " "
                )
            eventItemDescription.beVisibleIf(displayDescription && eventItemDescription.text.isNotEmpty())
            eventItemColorBar.background.applyColorFilter(listEvent.color)

            var newTextColor = textColor
            if (listEvent.isAllDay || listEvent.startTS <= now && listEvent.endTS <= now) {
                if (listEvent.isAllDay && Formatter.getDayCodeFromTS(listEvent.startTS) == Formatter.getDayCodeFromTS(
                        now
                    ) && !isPrintVersion
                ) {
                    newTextColor = properPrimaryColor
                }

                val adjustAlpha = if (listEvent.isTask) {
                    dimCompletedTasks && listEvent.isTaskCompleted
                } else {
                    dimPastEvents && listEvent.isPastEvent && !isPrintVersion
                }
                if (adjustAlpha) {
                    newTextColor = newTextColor.adjustAlpha(MEDIUM_ALPHA)
                }
            } else if (listEvent.startTS <= now && listEvent.endTS >= now && !isPrintVersion) {
                newTextColor = properPrimaryColor
            }

            eventItemTime.setTextColor(newTextColor)
            eventItemTitle.setTextColor(newTextColor)
            eventItemDescription.setTextColor(newTextColor)
            eventItemTaskImage.applyColorFilter(newTextColor)
            eventItemTaskImage.beVisibleIf(listEvent.isTask)

            val startMargin = if (listEvent.isTask) {
                0
            } else {
                mediumMargin
            }

            (eventItemTitle.layoutParams as ConstraintLayout.LayoutParams).marginStart = startMargin

        }
    }

    /** bi loi doan nay */
    private fun setupListSectionMonth(view: View, listItem: ListSectionMonth) {
        val binding = EventItemSectionMonthBinding.bind(view)
        binding.eventSectionTitle.text = listItem.title
        binding.eventSectionTitle.setTextColor(properPrimaryColor)

        /**
         * code theo cach tren hay duoi deu duoc
         * */
//        EventItemSectionMonthBinding.bind(view).apply {
//            eventSectionTitle.apply {
//                text = listItem.title
//                setTextColor(properPrimaryColor)
//            }
//        }

    }

    private fun setupListSectionDay(view: View, listItem: ListSectionDay) {
        EventItemSectionDayBinding.bind(view).eventSectionTitle.apply {
            text = listItem.title
            val dayColor = if (listItem.isToday) properPrimaryColor else textColor
            setTextColor(dayColor)
        }
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    fun toggle24HourFormat(use24HourFormat: Boolean) {
        this.use24HourFormat = use24HourFormat
        notifyDataSetChanged()
    }

    fun updateListItems(newListItems: ArrayList<ListItem>) {
        if (newListItems.hashCode() != currentItemsHash) {
            currentItemsHash = newListItems.hashCode()
            listItems = newListItems.clone() as ArrayList<ListItem>
            recyclerView.resetItemCount()
            notifyDataSetChanged()
            finishActMode()
        }
    }

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(com.simplemobiletools.commons.R.color.theme_light_text_color)
        } else {
            activity.getProperTextColor()
        }
        notifyDataSetChanged()
    }
}