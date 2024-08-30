package com.trungkieu.mycalendar.dialogs

import android.app.Activity
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.commons.views.MyAppCompatCheckbox
import com.trungkieu.mycalendar.R
import com.trungkieu.mycalendar.databinding.DialogVerticalLinearLayoutBinding
import com.trungkieu.mycalendar.databinding.MyCheckboxBinding
import com.trungkieu.mycalendar.extensions.withFirstDayOfWeekToFront

class RepeatRuleWeeklyDialog(val activity: Activity, val curRepeatRule: Int, val callback: (repeate:Int) -> Unit) {
    private val binding by activity.viewBinding(DialogVerticalLinearLayoutBinding::inflate)

    init {
        val days = activity.resources.getStringArray(com.simplemobiletools.commons.R.array.week_days)
        var checkboxes = ArrayList<MyAppCompatCheckbox>(7)
        /**
        * Bước 1: Convert tất cả các ngày trong tuần theo cơ số mũ 2
         * Bước 2 :  chon 2,3 ngày bất kỳ rồi cộng tất cả giá trị lại
         * ví dụ : chọn Thứ 2 và thứ 5 , giá trị sẽ là 9
         * Bước 3 : đổi giá trị vừa chọn sang hệ nhị phân, nếu 9 thì hệ nhị phân là 1001
         * Bước 4 : chạy vòng lặp 7 ngày với i = 0, và bắt đầu với 2^0 = 1
         * Bước 5 :  đổi giá trị vừa chọn sang hệ nhị phân, nếu 2 thì hệ nhị phân là 0001
         * Bước 6 :So sánh giá trị được chọn và giá trị vòng lặp. Ở đây là 1001 & 0001 = 0001 . Vậy giá trị = 2 >0 .
         *
         * mon 1
         * tues 2
         * wed 4
         * thurs 8
         * fri 16
         * sat 32
         * sun 64
         *
         * chay vong lap for
         * i = 0 , 2^0 = 1, vay he nhi phan = 0001 & 1001 = 0001
         * i = 1 , 2^1 = 2, vay he nhi phan = 0010 & 1001 = 0000
         * i = 2 , 2^2 = 4, vay he nhi phan = 0100 & 1001 = 0000
         * i = 3 , 2^3 = 8, vay he nhi phan = 1000 & 1001 = 1000
         * i = 4 , 2^4 = 16, vay he nhi phan = 10000 & 1001 = 00000
         * i = 5 , 2^5 = 32, vay he nhi phan = 100000 & 1001= 00000
         * i = 6 , 2^6 = 64, vay he nhi phan = 1000000 & 1001= 00000
         *
         * */
        for (i in 0 .. 6) {
            val pow = Math.pow(2.0, i.toDouble()).toInt()
            MyCheckboxBinding.inflate(activity.layoutInflater).root.apply {
                isChecked = curRepeatRule and pow != 0
                text = days[i]
                id = pow
                checkboxes.add(this)
            }
        }

        checkboxes = activity.withFirstDayOfWeekToFront(checkboxes)
        checkboxes.forEach {
            binding.dialogVerticalLinearLayout.addView(it)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok) {_,_, ->
                callback(getRepeatRuleSum())
            }
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel,null)
            .apply {
                activity.setupDialogStuff(binding.root,this)
            }
            .setMultiChoiceItems(days,null) { dialog, which, isChecked ->

            }
    }

    private fun getRepeatRuleSum(): Int {
        var sum = 0
        val cnt = binding.dialogVerticalLinearLayout.childCount
        for (i in 0 until cnt) {
            val child = binding.dialogVerticalLinearLayout.getChildAt(i)
            if (child is MyAppCompatCheckbox) {
                if (child.isChecked) {
                    sum += child.id
                }
            }
        }
        return sum
    }
}