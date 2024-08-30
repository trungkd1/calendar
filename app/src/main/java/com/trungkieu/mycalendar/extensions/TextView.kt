package com.trungkieu.mycalendar.extensions

import android.graphics.Paint
import android.widget.TextView
import com.simplemobiletools.commons.extensions.addBit
import com.simplemobiletools.commons.extensions.removeBit

fun TextView.checkViewStrikeThrough(isAddFlag: Boolean) {
    paintFlags = if (isAddFlag) {
        paintFlags.addBit(Paint.STRIKE_THRU_TEXT_FLAG)
    } else {
        paintFlags.removeBit(Paint.STRIKE_THRU_TEXT_FLAG)
    }
}