/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beeline09.daterangepicker.date

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView.LayoutParams
import android.widget.BaseAdapter

import java.util.Calendar
import java.util.HashMap

/**
 * An adapter for a list of [MonthView] items.
 */
abstract class MonthAdapter(private val mContext: Context,
                            protected val mController: SmoothDateRangePickerController?) : BaseAdapter(), MonthView.OnDayClickListener {

    private var mSelectedDay: CalendarDay? = null

    private var mAccentColor = -1

    /**
     * Updates the selected day and related parameters.
     *
     * @param day The day to highlight
     */
    var selectedDay: CalendarDay?
        get() = mSelectedDay
        set(day) {
            mSelectedDay = day
            notifyDataSetChanged()
        }

    /**
     * A convenience class to represent a specific date.
     */
    class CalendarDay {
        private var calendar: Calendar? = null
        var year: Int = 0
            internal set
        var month: Int = 0
            internal set
        var day: Int = 0
            internal set

        constructor() {
            setTime(System.currentTimeMillis())
        }

        constructor(timeInMillis: Long) {
            setTime(timeInMillis)
        }

        constructor(calendar: Calendar) {
            year = calendar.get(Calendar.YEAR)
            month = calendar.get(Calendar.MONTH)
            day = calendar.get(Calendar.DAY_OF_MONTH)
        }

        constructor(year: Int, month: Int, day: Int) {
            setDay(year, month, day)
        }

        fun set(date: CalendarDay) {
            year = date.year
            month = date.month
            day = date.day
        }

        fun setDay(year: Int, month: Int, day: Int) {
            this.year = year
            this.month = month
            this.day = day
        }

        private fun setTime(timeInMillis: Long) {
            if (calendar == null) {
                calendar = Calendar.getInstance()
            }
            calendar!!.timeInMillis = timeInMillis
            month = calendar!!.get(Calendar.MONTH)
            year = calendar!!.get(Calendar.YEAR)
            day = calendar!!.get(Calendar.DAY_OF_MONTH)
        }
    }

    init {
        init()
        selectedDay = mController?.selectedDay
    }

    fun setAccentColor(color: Int) {
        mAccentColor = color
    }

    /**
     * Set up the gesture detector and selected time
     */
    protected fun init() {
        mSelectedDay = CalendarDay(System.currentTimeMillis())
    }

    override fun getCount(): Int {
        return if (mController != null) {
            (mController.maxYear - mController.minSelectableYear + 1) * MONTHS_IN_YEAR
        } else {
            0
        }

    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    @SuppressLint("NewApi")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v: MonthView?
        var drawingParams: HashMap<String, Int>? = null
        if (convertView != null) {
            v = convertView as MonthView?
            // We store the drawing parameters in the view so it can be recycled
            drawingParams = v?.tag as HashMap<String, Int>
        } else {
            v = createMonthView(mContext)
            // Set up the new view
            val params = LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            v.layoutParams = params
            v.isClickable = true
            v.setOnDayClickListener(this)
            if (mAccentColor != -1) {
                v.setAccentColor(mAccentColor)
            }
        }
        if (drawingParams == null) {
            drawingParams = HashMap()
        }
        drawingParams.clear()

        val month = position % MONTHS_IN_YEAR
        val year = position / MONTHS_IN_YEAR + (mController?.minSelectableYear?:0)

        var selectedDay = -1
        if (isSelectedDayInMonth(year, month)) {
            selectedDay = mSelectedDay!!.day
        }

        // Invokes requestLayout() to ensure that the recycled view is set with the appropriate
        // height/number of weeks before being displayed.
        v.reuse()

        drawingParams[MonthView.VIEW_PARAMS_SELECTED_DAY] = selectedDay
        drawingParams[MonthView.VIEW_PARAMS_YEAR] = year
        drawingParams[MonthView.VIEW_PARAMS_MONTH] = month
        drawingParams[MonthView.VIEW_PARAMS_WEEK_START] = (mController?.firstDayOfWeek?:0)
        v.setMonthParams(drawingParams)
        v.invalidate()
        return v
    }

    abstract fun createMonthView(context: Context): MonthView

    private fun isSelectedDayInMonth(year: Int, month: Int): Boolean {
        return mSelectedDay!!.year == year && mSelectedDay!!.month == month
    }


    override fun onDayClick(view: MonthView?, day: CalendarDay?) {
        if (day != null) {
            onDayTapped(day)
        }
    }

    /**
     * Maintains the same hour/min/sec but moves the day to the tapped day.
     *
     * @param day The day that was tapped
     */
    private fun onDayTapped(day: CalendarDay) {
        mController?.tryVibrate()
        mController?.onDayOfMonthSelected(day.year, day.month, day.day)
        selectedDay = day
    }

    companion object {

        private val TAG = "SimpleMonthAdapter"

        protected var WEEK_7_OVERHANG_HEIGHT = 7
        val MONTHS_IN_YEAR = 12
    }
}
