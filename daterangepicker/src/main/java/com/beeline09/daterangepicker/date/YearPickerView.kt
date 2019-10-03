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
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.beeline09.daterangepicker.R
import java.util.*


/**
 * Displays a selectable list of years.
 */
@SuppressLint("ViewConstructor")
class YearPickerView
/**
 * @param context Bla-bla-bla
 */
(context: Context, private val mController: SmoothDateRangePickerController) : ListView(context), OnItemClickListener, SmoothDateRangePickerFragment.OnDateChangedListener {
    private var mAdapter: YearAdapter? = null
    private val mViewSize: Int
    private val mChildSize: Int
    private var mSelectedView: TextViewWithCircularIndicator? = null
    private var mAccentColor: Int = 0

    val firstPositionOffset: Int
        get() {
            val firstChild = getChildAt(0) ?: return 0
            return firstChild.top
        }

    init {
        mController.registerOnDateChangedListener(this)
        val frame = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT)
        layoutParams = frame
        val res = context.resources
        mViewSize = res.getDimensionPixelOffset(R.dimen.mdtp_date_picker_view_animator_height)
        mChildSize = res.getDimensionPixelOffset(R.dimen.mdtp_year_label_height)
        isVerticalFadingEdgeEnabled = true
        setFadingEdgeLength(mChildSize / 3)
        init(context)
        onItemClickListener = this
        selector = StateListDrawable()
        dividerHeight = 0
        onDateChanged()
    }

    private fun init(context: Context) {
        val years = ArrayList<String>()
        for (year in mController.minSelectableYear..mController.maxYear) {
            years.add(String.format(Locale.getDefault(), "%d", year))
        }
        mAdapter = YearAdapter(context, R.layout.sdrp_year_label_text_view, years)
        adapter = mAdapter
    }

    fun setAccentColor(accentColor: Int) {
        mAccentColor = accentColor
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        mController.tryVibrate()
        val clickedView = view as TextViewWithCircularIndicator?
        if (clickedView != null) {
            if (clickedView !== mSelectedView) {
                if (mSelectedView != null) {
                    mSelectedView!!.drawIndicator(false)
                    mSelectedView!!.requestLayout()
                }
                clickedView.drawIndicator(true)
                clickedView.requestLayout()
                mSelectedView = clickedView
            }
            mController.onYearSelected(getYearFromTextView(clickedView))
            mAdapter!!.notifyDataSetChanged()
        }
    }

    private inner class YearAdapter(context: Context, resource: Int, objects: List<String>) : ArrayAdapter<String>(context, resource, objects) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = super.getView(position, convertView, parent) as TextViewWithCircularIndicator
            v.setAccentColor(mAccentColor, mController.isThemeDark)
            v.requestLayout()
            val year = getYearFromTextView(v)
            val selected = mController.selectedDay.year == year
            v.drawIndicator(selected)
            if (selected) {
                mSelectedView = v
            }
            return v
        }
    }

    fun postSetSelectionCentered(position: Int) {
        postSetSelectionFromTop(position, mViewSize / 2 - mChildSize / 2)
    }

    fun postSetSelectionFromTop(position: Int, offset: Int) {
        post {
            setSelectionFromTop(position, offset)
            requestLayout()
        }
    }

    override fun onDateChanged() {
        mAdapter!!.notifyDataSetChanged()
        postSetSelectionCentered(mController.selectedDay.year - mController.minSelectableYear)
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            event.fromIndex = 0
            event.toIndex = 0
        }
    }

    fun refreshYearAdapter() {
        mAdapter!!.clear()
        for (year in mController.minSelectableYear..mController.maxYear) {
            mAdapter!!.add(String.format(Locale.getDefault(), "%d", year))
        }
        mAdapter!!.notifyDataSetChanged()
    }

    companion object {
        private val TAG = "YearPickerView"

        private fun getYearFromTextView(view: TextView): Int {
            return Integer.parseInt(view.text.toString())
        }
    }
}
