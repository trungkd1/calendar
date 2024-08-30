package com.trungkieu.mycalendar.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.helpers.MEDIUM_ALPHA
import com.simplemobiletools.commons.views.MyRecyclerView
import com.trungkieu.mycalendar.R
import com.trungkieu.mycalendar.databinding.EventListItemBinding
import com.trungkieu.mycalendar.entity.EventEntity
import com.trungkieu.mycalendar.extensions.checkViewStrikeThrough
import com.trungkieu.mycalendar.extensions.config
import com.trungkieu.mycalendar.helper.Formatter

class DayEventsAdapter(activity: BaseSimpleActivity, val events: ArrayList<EventEntity>, recyclerView: MyRecyclerView, var dayCode: String, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity,recyclerView,itemClick){

    private val allDayString = resources.getString(R.string.all_day)
    private val displayDescription = activity.config.displayDescription
    private val replaceDescriptionWithLocation = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private val dimCompletedTasks = activity.config.dimCompletedTasks
    private var isPrintVersion = false
    private val mediumMargin = activity.resources.getDimension(com.simplemobiletools.commons.R.dimen.medium_margin).toInt()

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId(): Int {
        return R.menu.cab_day
    }

    override fun prepareActionMode(menu: Menu) {
        TODO("Not yet implemented")
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()

        }
    }

    override fun getSelectableItemCount(): Int {
        return events.size
    }

    override fun getIsItemSelectable(position: Int): Boolean {
        return true
    }

    override fun getItemSelectionKey(position: Int): Int? {
        return events.getOrNull(position)?.id?.toInt()
    }

    override fun getItemKeyPosition(key: Int): Int {
        return events.indexOfFirst { it.id?.toInt() == key }
    }

    override fun onActionModeCreated() {
//        TODO("Not yet implemented")
    }

    override fun onActionModeDestroyed() {
//        TODO("Not yet implemented")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(
            view = EventListItemBinding.inflate(activity.layoutInflater, parent,false).root
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.bindView(event, allowSingleClick = true, allowLongClick = true) { itemView, adapterPosition ->
            setupView(itemView,event)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount(): Int {
        return events.size
    }

    private fun setupView(view: View, event: EventEntity) {
        EventListItemBinding.bind(view).apply {
            eventItemHolder.isSelected = selectedKeys.contains(event.id?.toInt())
            eventItemHolder.background.applyColorFilter(textColor)
            eventItemTitle.text = event.title
            eventItemTitle.checkViewStrikeThrough(event.isTaskCompleted())
            eventItemTime.text = if (event.getIsAllDay()) allDayString else Formatter.getTimeFromTS(activity, event.startTS)
            if (event.startTS != event.endTS) {
                val startDayCode = Formatter.getDayCodeFromTS(event.startTS)
                val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
                val startDate = Formatter.getDayTitle(activity, startDayCode, false)
                val endDate = Formatter.getDayTitle(activity, endDayCode, false)
                val startDayString = if (startDayCode != dayCode) " ($startDate)" else ""
                if (!event.getIsAllDay()) {
                    val endTimeString = Formatter.getTimeFromTS(activity, event.endTS)
                    val endDayString = if (endDayCode != dayCode) " ($endDate)" else ""
                    eventItemTime.text = "${eventItemTime.text}$startDayString - $endTimeString$endDayString"
                } else {
                    val endDayString = if (endDayCode != dayCode) " - ($endDate)" else ""
                    eventItemTime.text = "${eventItemTime.text}$startDayString$endDayString"
                }
            }

            eventItemDescription.text = if (replaceDescriptionWithLocation) event.location else event.description.replace("\n", " ")
            eventItemDescription.beVisibleIf(displayDescription && eventItemDescription.text.isNotEmpty())
            eventItemColorBar.background.applyColorFilter(event.color)

            var newTextColor = textColor

            val adjustAlpha = if (event.isTask()) {
                dimCompletedTasks && event.isTaskCompleted()
            } else {
                dimPastEvents && event.isPastEvent && !isPrintVersion
            }

            if (adjustAlpha) {
                newTextColor = newTextColor.adjustAlpha(MEDIUM_ALPHA)
            }

            eventItemTime.setTextColor(newTextColor)
            eventItemTitle.setTextColor(newTextColor)
            eventItemDescription.setTextColor(newTextColor)
            eventItemTaskImage.applyColorFilter(newTextColor)
            eventItemTaskImage.beVisibleIf(event.isTask())

            val startMargin = if (event.isTask()) {
                0
            } else {
                mediumMargin
            }

            (eventItemTitle.layoutParams as ConstraintLayout.LayoutParams).marginStart = startMargin
        }
    }

    private fun shareEvents() {

    }

    private fun askConfirmDelete() {
//        TODO("Not yet implemented")
    }

}