package com.trungkieu.mycalendar.adapters

import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import com.simplemobiletools.commons.helpers.DAY_CODE
import com.trungkieu.mycalendar.fragments.MonthFragment
import com.trungkieu.mycalendar.interfaces.NavigationListener

class MyMonthPagerAdapter(
    fm: FragmentManager,
    private val mCodes: List<String>,
    private val mListener: NavigationListener
) : FragmentStatePagerAdapter(fm) {

    private val listFragments = SparseArray<MonthFragment>()

    override fun getCount(): Int {
        return mCodes.size
    }

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val code = mCodes[position]
        bundle.putString(DAY_CODE, code)

        val fragment = MonthFragment()
        fragment.arguments = bundle
        fragment.mListener = mListener
        listFragments.put(position, fragment)
        return fragment
    }

    fun updateCalendars(pos: Int) {
        for (i in -1 .. 1 ) {
            listFragments[pos + i].updateCalendar()
        }
    }

    fun printCurrentView(pos: Int) {
        listFragments[pos].printCurrentView()
    }

}