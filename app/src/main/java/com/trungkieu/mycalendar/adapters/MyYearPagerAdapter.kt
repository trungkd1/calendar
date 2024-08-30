package com.trungkieu.mycalendar.adapters

import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.simplemobiletools.commons.helpers.YEAR_LABEL
import com.trungkieu.mycalendar.fragments.YearFragment
import com.trungkieu.mycalendar.interfaces.NavigationListener

class MyYearPagerAdapter(fm: FragmentManager, val listYears: List<Int>, val mListener: NavigationListener) : FragmentStatePagerAdapter(fm) {

    private val mFragments = SparseArray<YearFragment>()

    override fun getCount(): Int {
        return listYears.size
    }

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val year = listYears[position]
        bundle.putInt(YEAR_LABEL, year)

        val fragment = YearFragment()
        fragment.arguments = bundle
        fragment.mListener = mListener


        mFragments.put(position, fragment)
        return fragment
    }

    fun updateCalendars(pos: Int) {
        for (i in -1..1) {
            mFragments[pos + i]?.updateCalendar()
        }
    }

    fun printCurrentView(pos: Int) {
        mFragments[pos].printCurrentView()
    }
}