package com.trungkieu.mycalendar.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.areSystemAnimationsEnabled
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.helpers.*
import com.trungkieu.mycalendar.MainActivity
import com.trungkieu.mycalendar.adapters.DayEventsAdapter
import com.trungkieu.mycalendar.databinding.FragmentDayBinding
import com.trungkieu.mycalendar.databinding.TopNavigationBinding
import com.trungkieu.mycalendar.entity.EventEntity
import com.trungkieu.mycalendar.extensions.config
import com.trungkieu.mycalendar.extensions.eventsHandler
import com.trungkieu.mycalendar.extensions.getStartEventActivity
import com.trungkieu.mycalendar.extensions.getStartTaskActivity
import com.trungkieu.mycalendar.helper.Formatter
import com.trungkieu.mycalendar.interfaces.NavigationListener
import java.util.ArrayList

class DayFragment : Fragment() {

    private var mTextColor = 0
    private var mDayCode = ""
    private var lastHash = 0

    var mListener: NavigationListener? = null

    private lateinit var binding: FragmentDayBinding
    private lateinit var topNavigationBinding: TopNavigationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDayBinding.inflate(inflater,container,false)
        // liên kết đối tượng binding với một giao diện XML khác là TopNavigationBinding
        topNavigationBinding = TopNavigationBinding.bind(binding.root)
        // Toán tử "!!" được sử dụng để bỏ qua lỗi "null" nếu giá trị không tồn tại
        // taọ ra các đối số đã lưu trong Bundle
        mDayCode = requireArguments().getString(DAY_CODE)!!
        setupButtons()
        return binding.root
    }

    private fun setupButtons() {
        mTextColor = requireContext().getProperTextColor()

        topNavigationBinding.topLeftArrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                mListener?.goLeft()
            }

            val pointerLeft = requireContext().getDrawable(com.simplemobiletools.commons.R.drawable.ic_chevron_left_vector)
            pointerLeft?.isAutoMirrored = true
            setImageDrawable(pointerLeft)
        }

        topNavigationBinding.topRightArrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                mListener?.goRight()
            }

            val pointerRight = requireContext().getDrawable(com.simplemobiletools.commons.R.drawable.ic_chevron_right_vector)
            pointerRight?.isAutoMirrored = true
            setImageDrawable(pointerRight)
        }

        val day = Formatter.getDayTitle(requireContext(), mDayCode)
        topNavigationBinding.topValue.apply {
            text = day
            contentDescription = text
            setOnClickListener {
                (activity as MainActivity).showGoToDateDialog()
            }
            setTextColor(context.getProperTextColor())
        }

    }

    override fun onResume() {
        super.onResume()
        updateCalendar()
    }

    fun updateCalendar() {
        val startTS = Formatter.getDayStartTS(mDayCode)
        val endTS = Formatter.getDayEndTS(mDayCode)
        // dấu "?" là toán tử an toàn, nếu context là null sẽ trả về null mà không truy cập eventHelper
        context?.eventsHandler?.getEvents(startTS,endTS) {
            receivedEvents(it)
        }
    }

    private fun receivedEvents(events: ArrayList<EventEntity>) {
        val newHash = events.hashCode()
        if ( newHash == lastHash || !isAdded ) {
            return
        }

        lastHash = newHash

        val replaceDescription = requireContext().config.replaceDescription
        val sorted = ArrayList(events.sortedWith(compareBy({ !it.getIsAllDay() }, { it.startTS }, { it.endTS }, { it.title }, {
            if (replaceDescription) it.location else it.description
        })))

        activity?.runOnUiThread {
            updateEvents(sorted)
        }

    }

    private fun updateEvents(events: ArrayList<EventEntity>) {
        if (activity == null)
            return

        DayEventsAdapter(activity as BaseSimpleActivity, events, binding.dayEvents, mDayCode) {
            editEvent(it as EventEntity)
        }.apply {
            binding.dayEvents.adapter = this
        }

        if (requireContext().areSystemAnimationsEnabled) {
            binding.dayEvents.scheduleLayoutAnimation()
        }
    }

    private fun editEvent(event: EventEntity) {
        Intent(context, if (event.isTask()) getStartTaskActivity() else getStartEventActivity()).apply {
            putExtra(EVENT_ID, event.id)
            putExtra(EVENT_OCCURRENCE_TS, event.startTS)
            putExtra(IS_TASK_COMPLETED, event.isTaskCompleted())
            startActivity(this)
        }
    }

    fun printCurrentView() {


    }
}