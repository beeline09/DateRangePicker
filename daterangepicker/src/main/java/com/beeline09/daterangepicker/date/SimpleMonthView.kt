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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet

@SuppressLint("ViewConstructor")
class SimpleMonthView(context: Context, attr: AttributeSet?, controller: SmoothDateRangePickerController?) : MonthView(context, attr, controller) {

    override fun drawMonthDay(canvas: Canvas, year: Int, month: Int, day: Int,
                              x: Int, y: Int, startX: Int, stopX: Int, startY: Int, stopY: Int) {
        if (mSelectedDay == day) {
            canvas.drawCircle(x.toFloat(), (y - MINI_DAY_NUMBER_TEXT_SIZE / 3).toFloat(), DAY_SELECTED_CIRCLE_SIZE.toFloat(),
                    mSelectedCirclePaint ?: Paint())
        }

        if (isHighlighted(year, month, day)) {
            mMonthNumPaint?.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } else {
            mMonthNumPaint?.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // If we have a mindate or maxdate, gray out the day number if it's outside the range.
        if (isOutOfRange(year, month, day)) {
            mMonthNumPaint?.color = mDisabledDayTextColor
        } else if (mSelectedDay == day) {
            mMonthNumPaint?.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            mMonthNumPaint?.color = mSelectedDayTextColor
        } else if (mHasToday && mToday == day) {
            mMonthNumPaint?.color = mTodayNumberColor
        } else {
            mMonthNumPaint?.color = if (isHighlighted(year, month, day)) mHighlightedDayTextColor else mDayTextColor
        }

        canvas.drawText(String.format("%d", day), x.toFloat(), y.toFloat(), mMonthNumPaint
                ?: Paint())
    }
}
