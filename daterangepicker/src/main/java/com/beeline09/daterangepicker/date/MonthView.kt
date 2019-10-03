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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.beeline09.daterangepicker.R
import com.beeline09.daterangepicker.TypefaceHelper
import com.beeline09.daterangepicker.date.MonthAdapter.CalendarDay
import java.security.InvalidParameterException
import java.util.*

/**
 * A calendar-like view displaying a specified month and the appropriate selectable day numbers
 * within the specified month.
 */
abstract class MonthView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null, protected var mController: SmoothDateRangePickerController? = null) : View(context, attr) {

    // affects the padding on the sides of this view
    protected var mEdgePadding = 0

    private val mDayOfWeekTypeface: String
    private val mMonthTitleTypeface: String

    protected var mMonthNumPaint: Paint? = null
    protected var mMonthTitlePaint: Paint? = null
    protected var mSelectedCirclePaint: Paint? = null
    protected var mMonthDayLabelPaint: Paint? = null

    private val mFormatter: Formatter
    private val mStringBuilder: StringBuilder

    // The Julian day of the first day displayed by this item
    protected var mFirstJulianDay = -1
    // The month of the first day in this week
    protected var mFirstMonth = -1
    // The month of the last day in this week
    protected var mLastMonth = -1

    var month: Int = 0
        protected set

    var year: Int = 0
        protected set
    // Quick reference to the width of this view, matches parent
    protected var mWidth: Int = 0
    // The height this view should draw at in pixels, set by height param
    protected var mRowHeight = DEFAULT_HEIGHT
    // If this view contains the today
    protected var mHasToday = false
    // Which day is selected [0-6] or -1 if no day is selected
    protected var mSelectedDay = -1
    // Which day is today [0-6] or -1 if no day is today
    protected var mToday = DEFAULT_SELECTED_DAY
    // Which day of the week to start on [0-6]
    protected var mWeekStart = DEFAULT_WEEK_START
    // How many days to display
    protected var mNumDays = DEFAULT_NUM_DAYS
    // The number of days + a spot for week number if it is displayed
    protected var mNumCells = mNumDays
    // The left edge of the selected day
    protected var mSelectedLeft = -1
    // The right edge of the selected day
    protected var mSelectedRight = -1

    private val mCalendar: Calendar
    protected val mDayLabelCalendar: Calendar
    private val mTouchHelper: MonthViewTouchHelper

    protected var mNumRows = DEFAULT_NUM_ROWS

    // Optional listener for handling day click actions
    protected var mOnDayClickListener: OnDayClickListener? = null

    // Whether to prevent setting the accessibility delegate
    private val mLockAccessibilityDelegate: Boolean

    protected var mDayTextColor: Int = 0
    protected var mSelectedDayTextColor: Int = 0
    protected var mMonthDayTextColor: Int = 0
    protected var mTodayNumberColor: Int = 0
    protected var mHighlightedDayTextColor: Int = 0
    protected var mDisabledDayTextColor: Int = 0
    protected var mMonthTitleColor: Int = 0

    protected val monthViewTouchHelper: MonthViewTouchHelper
        get() = MonthViewTouchHelper(this)

    private var mDayOfWeekStart = 0

    /**
     * A wrapper to the MonthHeaderSize to allow override it in children
     */
    protected val monthHeaderSize: Int
        get() = MONTH_HEADER_SIZE

    private val monthAndYearString: String
        get() {
            val flags = (DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
                    or DateUtils.FORMAT_NO_MONTH_DAY)
            mStringBuilder.setLength(0)
            val millis = mCalendar.timeInMillis
            return DateUtils.formatDateRange(context, mFormatter, millis, millis, flags,
                    null).toString()
        }

    /**
     * @return The date that has accessibility focus, or `null` if no date
     * has focus
     */
    val accessibilityFocus: CalendarDay?
        get() {
            val day = mTouchHelper.focusedVirtualView
            return if (day >= 0) {
                CalendarDay(year, month, day)
            } else null
        }

    init {
        val res = context.resources

        mDayLabelCalendar = Calendar.getInstance()
        mCalendar = Calendar.getInstance()

        mDayOfWeekTypeface = res.getString(R.string.mdtp_day_of_week_label_typeface)
        mMonthTitleTypeface = res.getString(R.string.mdtp_sans_serif)

        val darkTheme = mController != null && mController!!.isThemeDark
        if (darkTheme) {
            mDayTextColor = res.getColor(R.color.mdtp_date_picker_text_normal_dark_theme)
            mMonthDayTextColor = res.getColor(R.color.mdtp_date_picker_month_day_dark_theme)
            mDisabledDayTextColor = res.getColor(R.color.mdtp_date_picker_text_disabled_dark_theme)
            mHighlightedDayTextColor = res.getColor(R.color.mdtp_date_picker_text_highlighted_dark_theme)
        } else {
            mDayTextColor = res.getColor(R.color.mdtp_date_picker_text_normal)
            mMonthDayTextColor = res.getColor(R.color.mdtp_date_picker_month_day)
            mDisabledDayTextColor = res.getColor(R.color.mdtp_date_picker_text_disabled)
            mHighlightedDayTextColor = res.getColor(R.color.mdtp_date_picker_text_highlighted)
        }
        mSelectedDayTextColor = res.getColor(R.color.mdtp_white)
        mTodayNumberColor = res.getColor(R.color.mdtp_accent_color)
        mMonthTitleColor = res.getColor(R.color.mdtp_white)

        mStringBuilder = StringBuilder(50)
        mFormatter = Formatter(mStringBuilder, Locale.getDefault())

        MINI_DAY_NUMBER_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.mdtp_day_number_size)
        MONTH_LABEL_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.mdtp_month_label_size)
        MONTH_DAY_LABEL_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.mdtp_month_day_label_text_size)
        MONTH_HEADER_SIZE = res.getDimensionPixelOffset(R.dimen.mdtp_month_list_item_header_height)
        DAY_SELECTED_CIRCLE_SIZE = res
                .getDimensionPixelSize(R.dimen.mdtp_day_number_select_circle_radius)

        mRowHeight = (res.getDimensionPixelOffset(R.dimen.mdtp_date_picker_view_animator_height) - monthHeaderSize) / MAX_NUM_ROWS

        // Set up accessibility components.
        mTouchHelper = monthViewTouchHelper
        ViewCompat.setAccessibilityDelegate(this, mTouchHelper)
        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
        mLockAccessibilityDelegate = true

        // Sets up any standard paints that will be used
        initView()
    }

    fun setDatePickerController(controller: SmoothDateRangePickerController) {
        mController = controller
    }

    override fun setAccessibilityDelegate(delegate: View.AccessibilityDelegate?) {
        // Workaround for a JB MR1 issue where accessibility delegates on
        // top-level ListView items are overwritten.
        if (!mLockAccessibilityDelegate) {
            super.setAccessibilityDelegate(delegate)
        }
    }

    fun setOnDayClickListener(listener: OnDayClickListener) {
        mOnDayClickListener = listener
    }

    public override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        // First right-of-refusal goes the touch exploration helper.
        return if (mTouchHelper.dispatchHoverEvent(event)) {
            true
        } else super.dispatchHoverEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                val day = getDayFromLocation(event.x, event.y)
                if (day >= 0) {
                    onDayClick(day)
                }
            }
        }
        return true
    }

    /**
     * Sets up the text and style properties for painting. Override this if you
     * want to use a different paint.
     */
    protected fun initView() {
        mMonthTitlePaint = Paint()
        mMonthTitlePaint?.isFakeBoldText = true
        mMonthTitlePaint?.isAntiAlias = true
        mMonthTitlePaint?.textSize = MONTH_LABEL_TEXT_SIZE.toFloat()
        mMonthTitlePaint?.typeface = Typeface.create(mMonthTitleTypeface, Typeface.BOLD)
        mMonthTitlePaint?.color = mDayTextColor
        mMonthTitlePaint?.textAlign = Align.CENTER
        mMonthTitlePaint?.style = Style.FILL

        mSelectedCirclePaint = Paint()
        mSelectedCirclePaint?.isFakeBoldText = true
        mSelectedCirclePaint?.isAntiAlias = true
        mSelectedCirclePaint?.color = mTodayNumberColor
        mSelectedCirclePaint?.textAlign = Align.CENTER
        mSelectedCirclePaint?.style = Style.FILL
        mSelectedCirclePaint?.alpha = SELECTED_CIRCLE_ALPHA

        mMonthDayLabelPaint = Paint()
        mMonthDayLabelPaint?.isAntiAlias = true
        mMonthDayLabelPaint?.textSize = MONTH_DAY_LABEL_TEXT_SIZE.toFloat()
        mMonthDayLabelPaint?.color = mMonthDayTextColor
        mMonthDayLabelPaint?.typeface = TypefaceHelper[context, "Roboto-Medium"]
        mMonthDayLabelPaint?.style = Style.FILL
        mMonthDayLabelPaint?.textAlign = Align.CENTER
        mMonthDayLabelPaint?.isFakeBoldText = true

        mMonthNumPaint = Paint()
        mMonthNumPaint?.isAntiAlias = true
        mMonthNumPaint?.textSize = MINI_DAY_NUMBER_TEXT_SIZE.toFloat()
        mMonthNumPaint?.style = Style.FILL
        mMonthNumPaint?.textAlign = Align.CENTER
        mMonthNumPaint?.isFakeBoldText = false
    }

    fun setAccentColor(color: Int) {
        mTodayNumberColor = color
        mSelectedCirclePaint?.color = color
    }

    override fun onDraw(canvas: Canvas) {
        drawMonthTitle(canvas)
        drawMonthDayLabels(canvas)
        drawMonthNums(canvas)
    }

    /**
     * Sets all the parameters for displaying this week. The only required
     * parameter is the week number. Other parameters have a default value and
     * will only update if a new value is included, except for focus month,
     * which will always default to no focus month if no value is passed in. See
     * [.VIEW_PARAMS_HEIGHT] for more info on parameters.
     *
     * @param params A viewMap of the new parameters, see
     * [.VIEW_PARAMS_HEIGHT]
     */
    fun setMonthParams(params: HashMap<String, Int>) {
        if (!params.containsKey(VIEW_PARAMS_MONTH) && !params.containsKey(VIEW_PARAMS_YEAR)) {
            throw InvalidParameterException("You must specify month and year for this view")
        }
        tag = params
        // We keep the current value for any params not present
        if (params.containsKey(VIEW_PARAMS_HEIGHT)) {
            mRowHeight = params[VIEW_PARAMS_HEIGHT]!!
            if (mRowHeight < MIN_HEIGHT) {
                mRowHeight = MIN_HEIGHT
            }
        }
        if (params.containsKey(VIEW_PARAMS_SELECTED_DAY)) {
            mSelectedDay = params[VIEW_PARAMS_SELECTED_DAY]!!
        }

        // Allocate space for caching the day numbers and focus values
        month = params[VIEW_PARAMS_MONTH]!!
        year = params[VIEW_PARAMS_YEAR]!!

        // Figure out what day today is
        //final Time today = new Time(Time.getCurrentTimezone());
        //today.setToNow();
        val today = Calendar.getInstance()
        mHasToday = false
        mToday = -1

        mCalendar.set(Calendar.MONTH, month)
        mCalendar.set(Calendar.YEAR, year)
        mCalendar.set(Calendar.DAY_OF_MONTH, 1)
        mDayOfWeekStart = mCalendar.get(Calendar.DAY_OF_WEEK)

        if (params.containsKey(VIEW_PARAMS_WEEK_START)) {
            mWeekStart = params[VIEW_PARAMS_WEEK_START]!!
        } else {
            mWeekStart = mCalendar.firstDayOfWeek
        }

        mNumCells = mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 0 until mNumCells) {
            val day = i + 1
            if (sameDay(day, today)) {
                mHasToday = true
                mToday = day
            }
        }
        mNumRows = calculateNumRows()

        // Invalidate cached accessibility information.
        mTouchHelper.invalidateRoot()
    }

    fun setSelectedDay(day: Int) {
        mSelectedDay = day
    }

    fun reuse() {
        mNumRows = DEFAULT_NUM_ROWS
        requestLayout()
    }

    private fun calculateNumRows(): Int {
        val offset = findDayOffset()
        val dividend = (offset + mNumCells) / mNumDays
        val remainder = (offset + mNumCells) % mNumDays
        return dividend + if (remainder > 0) 1 else 0
    }

    private fun sameDay(day: Int, today: Calendar): Boolean {
        return year == today.get(Calendar.YEAR) &&
                month == today.get(Calendar.MONTH) &&
                day == today.get(Calendar.DAY_OF_MONTH)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), mRowHeight * mNumRows
                + monthHeaderSize + 5)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mWidth = w

        // Invalidate cached accessibility information.
        mTouchHelper.invalidateRoot()
    }

    protected fun drawMonthTitle(canvas: Canvas) {
        val x = (mWidth + 2 * mEdgePadding) / 2
        val y = (monthHeaderSize - MONTH_DAY_LABEL_TEXT_SIZE) / 2
        canvas.drawText(monthAndYearString, x.toFloat(), y.toFloat(), mMonthTitlePaint ?: Paint())
    }

    protected fun drawMonthDayLabels(canvas: Canvas) {
        val y = monthHeaderSize - MONTH_DAY_LABEL_TEXT_SIZE / 2
        val dayWidthHalf = (mWidth - mEdgePadding * 2) / (mNumDays * 2)

        for (i in 0 until mNumDays) {
            val x = (2 * i + 1) * dayWidthHalf + mEdgePadding

            val calendarDay = (i + mWeekStart) % mNumDays
            mDayLabelCalendar.set(Calendar.DAY_OF_WEEK, calendarDay)
            val locale = Locale.getDefault()
            val localWeekDisplayName = mDayLabelCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale)
            var weekString = localWeekDisplayName!!.toUpperCase(locale).substring(0, 1)

            if (locale == Locale.CHINA || locale == Locale.CHINESE || locale == Locale.SIMPLIFIED_CHINESE || locale == Locale.TRADITIONAL_CHINESE) {
                val len = localWeekDisplayName.length
                weekString = localWeekDisplayName.substring(len - 1, len)
            }

            if (locale.language == "he" || locale.language == "iw") {
                if (mDayLabelCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                    val len = localWeekDisplayName.length
                    weekString = localWeekDisplayName.substring(len - 2, len - 1)
                } else {
                    // I know this is duplication, but it makes the code easier to grok by
                    // having all hebrew code in the same block
                    weekString = localWeekDisplayName.toUpperCase(locale).substring(0, 1)
                }
            }
            canvas.drawText(weekString, x.toFloat(), y.toFloat(), mMonthDayLabelPaint ?: Paint())
        }
    }

    /**
     * Draws the week and month day numbers for this week. Override this method
     * if you need different placement.
     *
     * @param canvas The canvas to draw on
     */
    protected fun drawMonthNums(canvas: Canvas) {
        var y = (mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2 - DAY_SEPARATOR_WIDTH + monthHeaderSize
        val dayWidthHalf = (mWidth - mEdgePadding * 2) / (mNumDays * 2.0f)
        var j = findDayOffset()
        for (dayNumber in 1..mNumCells) {
            val x = ((2 * j + 1) * dayWidthHalf + mEdgePadding).toInt()

            val yRelativeToDay = (mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2 - DAY_SEPARATOR_WIDTH

            val startX = (x - dayWidthHalf).toInt()
            val stopX = (x + dayWidthHalf).toInt()
            val startY = y - yRelativeToDay
            val stopY = startY + mRowHeight

            drawMonthDay(canvas, year, month, dayNumber, x, y, startX, stopX, startY, stopY)

            j++
            if (j == mNumDays) {
                j = 0
                y += mRowHeight
            }
        }
    }

    /**
     * This method should draw the month day.  Implemented by sub-classes to allow customization.
     *
     * @param canvas  The canvas to draw on
     * @param year  The year of this month day
     * @param month  The month of this month day
     * @param day  The day number of this month day
     * @param x  The default x position to draw the day number
     * @param y  The default y position to draw the day number
     * @param startX  The left boundary of the day number rect
     * @param stopX  The right boundary of the day number rect
     * @param startY  The top boundary of the day number rect
     * @param stopY  The bottom boundary of the day number rect
     */
    abstract fun drawMonthDay(canvas: Canvas, year: Int, month: Int, day: Int,
                              x: Int, y: Int, startX: Int, stopX: Int, startY: Int, stopY: Int)

    protected fun findDayOffset(): Int {
        return (if (mDayOfWeekStart < mWeekStart) mDayOfWeekStart + mNumDays else mDayOfWeekStart) - mWeekStart
    }


    /**
     * Calculates the day that the given x position is in, accounting for week
     * number. Returns the day or -1 if the position wasn't in a day.
     *
     * @param x The x position of the touch event
     * @return The day number, or -1 if the position wasn't in a day
     */
    fun getDayFromLocation(x: Float, y: Float): Int {
        val day = getInternalDayFromLocation(x, y)
        return if (day < 1 || day > mNumCells) {
            -1
        } else day
    }

    /**
     * Calculates the day that the given x position is in, accounting for week
     * number.
     *
     * @param x The x position of the touch event
     * @return The day number
     */
    protected fun getInternalDayFromLocation(x: Float, y: Float): Int {
        val dayStart = mEdgePadding
        if (x < dayStart || x > mWidth - mEdgePadding) {
            return -1
        }
        // Selection is (x - start) / (pixels/day) == (x -s) * day / pixels
        val row = (y - monthHeaderSize).toInt() / mRowHeight
        val column = ((x - dayStart) * mNumDays / (mWidth - dayStart - mEdgePadding)).toInt()

        var day = column - findDayOffset() + 1
        day += row * mNumDays
        return day
    }

    /**
     * Called when the user clicks on a day. Handles callbacks to the
     * [OnDayClickListener] if one is set.
     *
     *
     * If the day is out of the range set by minDate and/or maxDate, this is a no-op.
     *
     * @param day The day that was clicked
     */
    private fun onDayClick(day: Int) {
        // If the min / max date are set, only process the click if it's a valid selection.
        if (isOutOfRange(year, month, day)) {
            return
        }


        if (mOnDayClickListener != null) {
            mOnDayClickListener!!.onDayClick(this, CalendarDay(year, month, day))
        }

        // This is a no-op if accessibility is turned off.
        mTouchHelper.sendEventForVirtualView(day, AccessibilityEvent.TYPE_VIEW_CLICKED)
    }

    /**
     * @return true if the specified year/month/day are within the selectable days or the range set by minDate and maxDate.
     * If one or either have not been set, they are considered as Integer.MIN_VALUE and
     * Integer.MAX_VALUE.
     */
    protected fun isOutOfRange(year: Int, month: Int, day: Int): Boolean {
        if (mController!!.selectableDays != null) {
            return !isSelectable(year, month, day)
        }

        if (isBeforeMin(year, month, day)) {
            return true
        } else if (isAfterMax(year, month, day)) {
            return true
        }

        return false
    }

    private fun isSelectable(year: Int, month: Int, day: Int): Boolean {
        val selectableDays = mController!!.selectableDays
        for (c in selectableDays) {
            if (year < c.get(Calendar.YEAR)) break
            if (year > c.get(Calendar.YEAR)) continue
            if (month < c.get(Calendar.MONTH)) break
            if (month > c.get(Calendar.MONTH)) continue
            if (day < c.get(Calendar.DAY_OF_MONTH)) break
            if (day > c.get(Calendar.DAY_OF_MONTH)) continue
            return true
        }
        return false
    }

    private fun isBeforeMin(year: Int, month: Int, day: Int): Boolean {
        if (mController == null) {
            return false
        }
        //        Calendar minDate = mController.getMinDate();
        val minDate = mController!!.minSelectableDate ?: return false

        if (year < minDate.get(Calendar.YEAR)) {
            return true
        } else if (year > minDate.get(Calendar.YEAR)) {
            return false
        }

        if (month < minDate.get(Calendar.MONTH)) {
            return true
        } else if (month > minDate.get(Calendar.MONTH)) {
            return false
        }

        return if (day < minDate.get(Calendar.DAY_OF_MONTH)) {
            true
        } else {
            false
        }
    }

    private fun isAfterMax(year: Int, month: Int, day: Int): Boolean {
        if (mController == null) {
            return false
        }
        val maxDate = mController!!.maxDate ?: return false

        if (year > maxDate.get(Calendar.YEAR)) {
            return true
        } else if (year < maxDate.get(Calendar.YEAR)) {
            return false
        }

        if (month > maxDate.get(Calendar.MONTH)) {
            return true
        } else if (month < maxDate.get(Calendar.MONTH)) {
            return false
        }

        return if (day > maxDate.get(Calendar.DAY_OF_MONTH)) {
            true
        } else {
            false
        }
    }

    /**
     * @param year
     * @param month
     * @param day
     * @return true if the given date should be highlighted
     */
    protected fun isHighlighted(year: Int, month: Int, day: Int): Boolean {
        val highlightedDays = mController!!.highlightedDays ?: return false
        for (c in highlightedDays) {
            if (year < c.get(Calendar.YEAR)) break
            if (year > c.get(Calendar.YEAR)) continue
            if (month < c.get(Calendar.MONTH)) break
            if (month > c.get(Calendar.MONTH)) continue
            if (day < c.get(Calendar.DAY_OF_MONTH)) break
            if (day > c.get(Calendar.DAY_OF_MONTH)) continue
            return true
        }
        return false
    }

    /**
     * Clears accessibility focus within the view. No-op if the view does not
     * contain accessibility focus.
     */
    fun clearAccessibilityFocus() {
        mTouchHelper.clearFocusedVirtualView()
    }

    /**
     * Attempts to restore accessibility focus to the specified date.
     *
     * @param day The date which should receive focus
     * @return `false` if the date is not valid for this month view, or
     * `true` if the date received focus
     */
    fun restoreAccessibilityFocus(day: CalendarDay): Boolean {
        if (day.year != year || day.month != month || day.day > mNumCells) {
            return false
        }
        mTouchHelper.focusedVirtualView = day.day
        return true
    }

    /**
     * Provides a virtual view hierarchy for interfacing with an accessibility
     * service.
     */
    protected open inner class MonthViewTouchHelper(host: View) : ExploreByTouchHelper(host) {

        private val mTempRect = Rect()
        private val mTempCalendar = Calendar.getInstance()

        fun setFocusedVirtualView(virtualViewId: Int) {
            getAccessibilityNodeProvider(this@MonthView).performAction(
                    virtualViewId, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, null)
        }

        fun clearFocusedVirtualView() {
            val focusedVirtualView = focusedVirtualView
            if (focusedVirtualView != ExploreByTouchHelper.INVALID_ID) {
                getAccessibilityNodeProvider(this@MonthView).performAction(
                        focusedVirtualView,
                        AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null)
            }
        }

        override fun getVirtualViewAt(x: Float, y: Float): Int {
            val day = getDayFromLocation(x, y)
            return if (day >= 0) {
                day
            } else ExploreByTouchHelper.INVALID_ID
        }

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            for (day in 1..mNumCells) {
                virtualViewIds.add(day)
            }
        }

        override fun onPopulateEventForVirtualView(virtualViewId: Int, event: AccessibilityEvent) {
            event.contentDescription = getItemDescription(virtualViewId)
        }

        override fun onPopulateNodeForVirtualView(virtualViewId: Int,
                                                  node: AccessibilityNodeInfoCompat) {
            getItemBounds(virtualViewId, mTempRect)

            node.contentDescription = getItemDescription(virtualViewId)
            node.setBoundsInParent(mTempRect)
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK)

            if (virtualViewId == mSelectedDay) {
                node.isSelected = true
            }

        }

        override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int,
                                                   arguments: Bundle?): Boolean {
            when (action) {
                AccessibilityNodeInfo.ACTION_CLICK -> {
                    onDayClick(virtualViewId)
                    return true
                }
            }

            return false
        }

        /**
         * Calculates the bounding rectangle of a given time object.
         *
         * @param day The day to calculate bounds for
         * @param rect The rectangle in which to store the bounds
         */
        protected fun getItemBounds(day: Int, rect: Rect) {
            val offsetX = mEdgePadding
            val offsetY = monthHeaderSize
            val cellHeight = mRowHeight
            val cellWidth = (mWidth - 2 * mEdgePadding) / mNumDays
            val index = day - 1 + findDayOffset()
            val row = index / mNumDays
            val column = index % mNumDays
            val x = offsetX + column * cellWidth
            val y = offsetY + row * cellHeight

            rect.set(x, y, x + cellWidth, y + cellHeight)
        }

        /**
         * Generates a description for a given time object. Since this
         * description will be spoken, the components are ordered by descending
         * specificity as DAY MONTH YEAR.
         *
         * @param day The day to generate a description for
         * @return A description of the time object
         */
        private fun getItemDescription(day: Int): CharSequence {
            mTempCalendar.set(year, month, day)
            val date = DateFormat.format("dd MMMM yyyy",
                    mTempCalendar.timeInMillis)

            return if (day == mSelectedDay) {
                context.getString(R.string.mdtp_item_is_selected, date)
            } else date

        }
    }

    /**
     * Handles callbacks when the user clicks on a time object.
     */
    interface OnDayClickListener {
        fun onDayClick(view: MonthView?, day: CalendarDay?)
    }

    companion object {
        private val TAG = "MonthView"

        /**
         * These params can be passed into the view to control how it appears.
         * [.VIEW_PARAMS_WEEK] is the only required field, though the default
         * values are unlikely to fit most layouts correctly.
         */
        /**
         * This sets the height of this week in pixels
         */
        val VIEW_PARAMS_HEIGHT = "height"
        /**
         * This specifies the position (or weeks since the epoch) of this week.
         */
        val VIEW_PARAMS_MONTH = "month"
        /**
         * This specifies the position (or weeks since the epoch) of this week.
         */
        val VIEW_PARAMS_YEAR = "year"
        /**
         * This sets one of the days in this view as selected [Calendar.SUNDAY]
         * through [Calendar.SATURDAY].
         */
        val VIEW_PARAMS_SELECTED_DAY = "selected_day"
        /**
         * Which day the week should start on. [Calendar.SUNDAY] through
         * [Calendar.SATURDAY].
         */
        val VIEW_PARAMS_WEEK_START = "week_start"
        /**
         * How many days to display at a time. Days will be displayed starting with
         * [.mWeekStart].
         */
        val VIEW_PARAMS_NUM_DAYS = "num_days"
        /**
         * Which month is currently in focus, as defined by [Calendar.MONTH]
         * [0-11].
         */
        val VIEW_PARAMS_FOCUS_MONTH = "focus_month"
        /**
         * If this month should display week numbers. false if 0, true otherwise.
         */
        val VIEW_PARAMS_SHOW_WK_NUM = "show_wk_num"

        protected var DEFAULT_HEIGHT = 32
        protected var MIN_HEIGHT = 10
        protected val DEFAULT_SELECTED_DAY = -1
        protected val DEFAULT_WEEK_START = Calendar.SUNDAY
        protected val DEFAULT_NUM_DAYS = 7
        protected val DEFAULT_SHOW_WK_NUM = 0
        protected val DEFAULT_FOCUS_MONTH = -1
        protected val DEFAULT_NUM_ROWS = 6
        protected val MAX_NUM_ROWS = 6

        private val SELECTED_CIRCLE_ALPHA = 255

        protected var DAY_SEPARATOR_WIDTH = 1
        var MINI_DAY_NUMBER_TEXT_SIZE: Int = 0
        protected var MONTH_LABEL_TEXT_SIZE: Int = 0
        protected var MONTH_DAY_LABEL_TEXT_SIZE: Int = 0
        protected var MONTH_HEADER_SIZE: Int = 0
        var DAY_SELECTED_CIRCLE_SIZE: Int = 0

        // used for scaling to the device density
        protected var mScale = 0f
    }
}
