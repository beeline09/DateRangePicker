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

import android.content.DialogInterface
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.InputType
import android.text.format.DateUtils
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.view.animation.AlphaAnimation
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import com.beeline09.daterangepicker.HapticFeedbackController
import com.beeline09.daterangepicker.R
import com.beeline09.daterangepicker.TypefaceHelper
import com.beeline09.daterangepicker.Utils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog allowing users to select a date.
 */
class DateRangePickerFragment : DialogFragment(), OnClickListener, DateRangePickerController {

    private val mCalendar = Calendar.getInstance()
    private val mCalendarEnd = Calendar.getInstance()
    private var mCallBack: OnDateRangeSetListener? = null
    private val mListeners = HashSet<OnDateChangedListener>()
    private var mOnCancelListener: DialogInterface.OnCancelListener? = null
    private var mOnDismissListener: DialogInterface.OnDismissListener? = null

    private var mAnimator: AccessibleDateAnimator? = null

    private var mDayOfWeekView: MaterialTextView? = null
    private var mMonthAndDayView: LinearLayout? = null
    private var mSelectedMonthTextView: MaterialTextView? = null
    private var mSelectedDayTextView: MaterialTextView? = null
    private var mYearView: MaterialTextView? = null
    private var mDayPickerView: DayPickerView? = null
    private var mYearPickerView: YearPickerView? = null

    private var mDayOfWeekViewEnd: MaterialTextView? = null
    private var mMonthAndDayViewEnd: LinearLayout? = null
    private var mSelectedMonthTextViewEnd: MaterialTextView? = null
    private var mSelectedDayTextViewEnd: MaterialTextView? = null
    private var mYearViewEnd: MaterialTextView? = null
    private var mDayPickerViewEnd: SimpleDayPickerView? = null
    private var mYearPickerViewEnd: YearPickerView? = null

    private var viewList: MutableList<View?>? = null

    private var mDurationView: LinearLayout? = null
    private var mDurationTextView: MaterialTextView? = null
    private var mDurationEditText: AppCompatEditText? = null
    private var mDurationDayTextView: MaterialTextView? = null
    private var mDurationArrow: MaterialTextView? = null
    private var mDurationArrowEnd: MaterialTextView? = null
    private var mNumberPadView: NumberPadView? = null

    private var mCurrentView = UNINITIALIZED

    private var mWeekStart = mCalendar.firstDayOfWeek
    private var mMinYear = DEFAULT_START_YEAR
    private var mMaxYear = DEFAULT_END_YEAR
    private var mMinDate: Calendar? = null
    private var mMinSelectableDate: Calendar? = null
    private var mMaxDate: Calendar? = null
    private var highlightedDays: Array<Calendar>? = null
    private var selectableDays: Array<Calendar>? = null

    private var mDuration: Int = 0

    private var mThemeDark: Boolean = false
    /**
     * Get the accent color of this dialog
     *
     * @return accent color
     */
    /**
     * Set the accent color of this dialog
     *
     * @param accentColor the accent color you want
     */
    var accentColor = -1
    private var mVibrate: Boolean = false
    private var mDismissOnPause: Boolean = false

    private var mHapticFeedbackController: HapticFeedbackController? = null

    private var mDelayAnimation = true

    // Accessibility strings.
    private var mDayPickerDescription: String? = null
    private var mSelectDay: String? = null
    private var mYearPickerDescription: String? = null
    private var mSelectYear: String? = null


    /**
     * The callback used to indicate the user is done filling in the date.
     */
    interface OnDateRangeSetListener {

        /**
         * @param view             The view associated with this listener.
         * @param yearStart        The start year that was set.
         * @param monthStart The start month that was set (0-11) for compatibility
         * with [Calendar].
         * @param dayStart  The start day of the month that was set.
         * @param yearEnd          The end year that was set.
         * @param monthEnd   The end month that was set (0-11) for compatibility
         * with [Calendar].
         * @param dayEnd    The end day of the month that was set.
         */
        fun onDateRangeSet(view: DateRangePickerFragment, yearStart: Int, monthStart: Int,
                           dayStart: Int, yearEnd: Int, monthEnd: Int, dayEnd: Int)
    }

    /**
     * The callback used to notify other date picker components of a change in selected date.
     */
    interface OnDateChangedListener {
        fun onDateChanged()
    }

    fun initialize(callBack: OnDateRangeSetListener, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        mCallBack = callBack
        mCalendar.set(Calendar.YEAR, year)
        mCalendar.set(Calendar.MONTH, monthOfYear)
        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        mCalendarEnd.set(Calendar.YEAR, year)
        mCalendarEnd.set(Calendar.MONTH, monthOfYear)
        mCalendarEnd.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        mThemeDark = false
        accentColor = -1
        mVibrate = true
        mDismissOnPause = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = activity
        activity?.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        if (savedInstanceState != null) {
            mCalendar.set(Calendar.YEAR, savedInstanceState.getInt(KEY_SELECTED_YEAR))
            mCalendar.set(Calendar.MONTH, savedInstanceState.getInt(KEY_SELECTED_MONTH))
            mCalendar.set(Calendar.DAY_OF_MONTH, savedInstanceState.getInt(KEY_SELECTED_DAY))
            mCalendarEnd.set(Calendar.YEAR, savedInstanceState.getInt(KEY_SELECTED_YEAR_END))
            mCalendarEnd.set(Calendar.MONTH, savedInstanceState.getInt(KEY_SELECTED_MONTH_END))
            mCalendarEnd.set(Calendar.DAY_OF_MONTH, savedInstanceState.getInt(KEY_SELECTED_DAY_END))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_YEAR, mCalendar.get(Calendar.YEAR))
        outState.putInt(KEY_SELECTED_MONTH, mCalendar.get(Calendar.MONTH))
        outState.putInt(KEY_SELECTED_DAY, mCalendar.get(Calendar.DAY_OF_MONTH))
        outState.putInt(KEY_SELECTED_YEAR_END, mCalendarEnd.get(Calendar.YEAR))
        outState.putInt(KEY_SELECTED_MONTH_END, mCalendarEnd.get(Calendar.MONTH))
        outState.putInt(KEY_SELECTED_DAY_END, mCalendarEnd.get(Calendar.DAY_OF_MONTH))
        outState.putInt(KEY_YEAR_START, mMinYear)
        outState.putInt(KEY_YEAR_END, mMaxYear)

        outState.putInt(KEY_WEEK_START, mWeekStart)
        outState.putInt(KEY_CURRENT_VIEW, mCurrentView)
        var listPosition = -1
        var listPositionEnd = -1
        if (mCurrentView == MONTH_AND_DAY_VIEW) {
            listPosition = mDayPickerView!!.mostVisiblePosition
        } else if (mCurrentView == YEAR_VIEW) {
            listPosition = mYearPickerView!!.firstVisiblePosition
            outState.putInt(KEY_LIST_POSITION_OFFSET, mYearPickerView!!.firstPositionOffset)
        } else if (mCurrentView == MONTH_AND_DAY_VIEW_END) {
            listPositionEnd = mDayPickerViewEnd!!.mostVisiblePosition
        } else if (mCurrentView == YEAR_VIEW_END) {
            listPositionEnd = mYearPickerViewEnd!!.firstVisiblePosition
            outState.putInt(KEY_LIST_POSITION_OFFSET_END, mYearPickerViewEnd!!.firstPositionOffset)
        }
        outState.putInt(KEY_LIST_POSITION, listPosition)
        outState.putInt(KEY_LIST_POSITION_END, listPositionEnd)
        outState.putSerializable(KEY_MIN_DATE, mMinDate)
        outState.putSerializable(KEY_MAX_DATE, mMaxDate)
        outState.putSerializable(KEY_MIN_DATE_SELECTABLE, mMinSelectableDate)
        outState.putSerializable(KEY_HIGHLIGHTED_DAYS, highlightedDays)
        outState.putSerializable(KEY_SELECTABLE_DAYS, selectableDays)
        outState.putBoolean(KEY_THEME_DARK, mThemeDark)
        outState.putInt(KEY_ACCENT, accentColor)
        outState.putBoolean(KEY_VIBRATE, mVibrate)
        outState.putBoolean(KEY_DISMISS, mDismissOnPause)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView: ")
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        val view = inflater.inflate(R.layout.sdrp_dialog, container)

        mDayOfWeekView = view.findViewById(R.id.date_picker_header)
        mDayOfWeekViewEnd = view.findViewById(R.id.date_picker_header_end)
        mMonthAndDayView = view.findViewById<View>(R.id.date_picker_month_and_day) as LinearLayout
        mMonthAndDayViewEnd = view.findViewById<View>(R.id.date_picker_month_and_day_end) as LinearLayout
        mMonthAndDayView!!.setOnClickListener(this)
        mMonthAndDayViewEnd!!.setOnClickListener(this)

        mSelectedMonthTextView = view.findViewById(R.id.date_picker_month)
        mSelectedMonthTextViewEnd = view.findViewById(R.id.date_picker_month_end)

        mSelectedDayTextView = view.findViewById(R.id.date_picker_day)
        mSelectedDayTextViewEnd = view.findViewById(R.id.date_picker_day_end)

        mYearView = view.findViewById(R.id.date_picker_year)
        mYearViewEnd = view.findViewById(R.id.date_picker_year_end)
        mYearView!!.setOnClickListener(this)
        mYearViewEnd!!.setOnClickListener(this)

        mDurationView = view.findViewById<View>(R.id.date_picker_duration_layout) as LinearLayout
        mDurationView!!.setOnClickListener(this)
        mDurationTextView = view.findViewById(R.id.date_picker_duration_days)
        mDurationEditText = view.findViewById(R.id.date_picker_duration_days_et)
        // disable soft keyboard popup when edittext is selected
        mDurationEditText!!.setRawInputType(InputType.TYPE_CLASS_TEXT)
        mDurationEditText!!.setTextIsSelectable(true)
        mDurationDayTextView = view.findViewById(R.id.tv_duration_day)
        mDurationArrow = view.findViewById(R.id.arrow_start)
        mDurationArrow!!.setOnClickListener(this)
        mDurationArrowEnd = view.findViewById(R.id.arrow_end)
        mDurationArrowEnd!!.setOnClickListener(this)

        viewList = ArrayList()
        viewList?.add(MONTH_AND_DAY_VIEW, mMonthAndDayView)
        viewList?.add(YEAR_VIEW, mYearView)
        viewList?.add(MONTH_AND_DAY_VIEW_END, mMonthAndDayViewEnd)
        viewList?.add(YEAR_VIEW_END, mYearViewEnd)
        viewList?.add(DURATION_VIEW, mDurationView)

        var listPosition = -1
        var listPositionOffset = 0
        var listPositionEnd = -1
        var listPositionOffsetEnd = 0
        var currentView = MONTH_AND_DAY_VIEW
        if (savedInstanceState != null) {
            mWeekStart = savedInstanceState.getInt(KEY_WEEK_START)
            mMinYear = savedInstanceState.getInt(KEY_YEAR_START)
            mMaxYear = savedInstanceState.getInt(KEY_YEAR_END)
            currentView = savedInstanceState.getInt(KEY_CURRENT_VIEW)
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION)
            listPositionOffset = savedInstanceState.getInt(KEY_LIST_POSITION_OFFSET)
            listPositionEnd = savedInstanceState.getInt(KEY_LIST_POSITION_END)
            listPositionOffsetEnd = savedInstanceState.getInt(KEY_LIST_POSITION_OFFSET_END)
            mMinDate = savedInstanceState.getSerializable(KEY_MIN_DATE) as Calendar?
            mMaxDate = savedInstanceState.getSerializable(KEY_MAX_DATE) as Calendar?
            mMinSelectableDate = savedInstanceState.getSerializable(KEY_MIN_DATE_SELECTABLE) as Calendar?
            highlightedDays = savedInstanceState.getSerializable(KEY_HIGHLIGHTED_DAYS) as Array<Calendar>?
            selectableDays = savedInstanceState.getSerializable(KEY_SELECTABLE_DAYS) as Array<Calendar>?
            mThemeDark = savedInstanceState.getBoolean(KEY_THEME_DARK)
            accentColor = savedInstanceState.getInt(KEY_ACCENT)
            mVibrate = savedInstanceState.getBoolean(KEY_VIBRATE)
            mDismissOnPause = savedInstanceState.getBoolean(KEY_DISMISS)
        }

        val activity = activity
        mDayPickerView = SimpleDayPickerView(activity!!, this)
        mYearPickerView = YearPickerView(activity, this)
        mDayPickerViewEnd = SimpleDayPickerView(activity, this)
        mYearPickerViewEnd = YearPickerView(activity, this)
        mNumberPadView = NumberPadView(activity, this)


        val res = resources
        mDayPickerDescription = res.getString(R.string.mdtp_day_picker_description)
        mSelectDay = res.getString(R.string.mdtp_select_day)
        mYearPickerDescription = res.getString(R.string.mdtp_year_picker_description)
        mSelectYear = res.getString(R.string.mdtp_select_year)

        val bgColorResource = if (mThemeDark)
            R.color.mdtp_date_picker_view_animator_dark_theme
        else
            R.color.mdtp_date_picker_view_animator
        view.setBackgroundColor(ResourcesCompat.getColor(activity.resources!!, bgColorResource, activity.theme))

        if (mThemeDark) {
            view.findViewById<View>(R.id.hyphen).setBackgroundColor(ResourcesCompat.getColor(activity.resources!!, R.color.date_picker_selector_unselected_dark_theme, activity.theme))

            Utils.setMultiTextColorList(ResourcesCompat.getColorStateList(activity.resources!!, R.color.sdrp_selector_dark, activity.theme),
                    mDayOfWeekView, mDayOfWeekViewEnd,
                    mSelectedMonthTextView, mSelectedMonthTextViewEnd,
                    mSelectedDayTextView, mSelectedDayTextViewEnd,
                    mYearView, mYearViewEnd, mDurationTextView,
                    mDurationDayTextView, mDurationArrow, mDurationArrowEnd,
                    mDurationEditText, view.findViewById(R.id.tv_duration))
        }

        mAnimator = view.findViewById<View>(R.id.animator) as AccessibleDateAnimator

        mAnimator!!.addView(mDayPickerView)
        mAnimator!!.addView(mYearPickerView)
        mAnimator!!.addView(mDayPickerViewEnd)
        mAnimator!!.addView(mYearPickerViewEnd)
        mAnimator!!.addView(mNumberPadView)
        mAnimator!!.setDateMillis(mCalendar.timeInMillis)
        val animation = AlphaAnimation(0.0f, 1.0f)
        animation.duration = ANIMATION_DURATION.toLong()
        mAnimator!!.inAnimation = animation
        val animation2 = AlphaAnimation(1.0f, 0.0f)
        animation2.duration = ANIMATION_DURATION.toLong()
        mAnimator!!.outAnimation = animation2

        val okButton = view.findViewById<MaterialButton>(R.id.ok)
        okButton.setOnClickListener {
            tryVibrate()
            if (mCallBack != null) {
                mCallBack!!.onDateRangeSet(this@DateRangePickerFragment,
                        mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                        mCalendar.get(Calendar.DAY_OF_MONTH), mCalendarEnd.get(Calendar.YEAR),
                        mCalendarEnd.get(Calendar.MONTH), mCalendarEnd.get(Calendar.DAY_OF_MONTH))
            }
            dismiss()
        }
        okButton.setTypeface(TypefaceHelper.get(activity, "Roboto-Medium"))

        val cancelButton = view.findViewById<MaterialButton>(R.id.cancel)
        cancelButton.setOnClickListener {
            tryVibrate()
            dialog?.cancel()
        }
        cancelButton.setTypeface(TypefaceHelper.get(activity, "Roboto-Medium"))
        cancelButton.visibility = if (isCancelable) View.VISIBLE else View.GONE

        //If an accent color has not been set manually, try and get it from the context
        if (accentColor == -1) {
            val accentColor = Utils.getAccentColorFromThemeIfAvailable(activity)
            if (accentColor != -1) {
                this.accentColor = accentColor
            }
        }
        if (accentColor != -1) {
            if (mDayOfWeekView != null)
                mDayOfWeekView!!.setBackgroundColor(accentColor)
            if (mDayOfWeekViewEnd != null)
                mDayOfWeekViewEnd!!.setBackgroundColor(accentColor)

            view.findViewById<View>(R.id.layout_container).setBackgroundColor(accentColor)
            view.findViewById<View>(R.id.day_picker_selected_date_layout).setBackgroundColor(accentColor)
            view.findViewById<View>(R.id.day_picker_selected_date_layout_end).setBackgroundColor(accentColor)
            mDurationView!!.setBackgroundColor(accentColor)
            mDurationEditText!!.highlightColor = Utils.darkenColor(accentColor)
            mDurationEditText!!.background.setColorFilter(Utils.darkenColor(accentColor), PorterDuff.Mode.SRC_ATOP)
            okButton.setTextColor(accentColor)
            cancelButton.setTextColor(accentColor)
            mYearPickerView!!.setAccentColor(accentColor)
            mDayPickerView!!.setAccentColor(accentColor)
            mYearPickerViewEnd!!.setAccentColor(accentColor)
            mDayPickerViewEnd!!.setAccentColor(accentColor)
        }

        updateDisplay(false)
        setCurrentView(currentView)

        if (listPosition != -1) {
            if (currentView == MONTH_AND_DAY_VIEW) {
                mDayPickerView!!.postSetSelection(listPosition)
            } else if (currentView == YEAR_VIEW) {
                mYearPickerView!!.postSetSelectionFromTop(listPosition, listPositionOffset)
            }
        }

        if (listPositionEnd != -1) {
            if (currentView == MONTH_AND_DAY_VIEW_END) {
                mDayPickerViewEnd!!.postSetSelection(listPositionEnd)
            } else if (currentView == YEAR_VIEW_END) {
                mYearPickerViewEnd!!.postSetSelectionFromTop(listPositionEnd, listPositionOffsetEnd)
            }
        }

        mHapticFeedbackController = HapticFeedbackController(activity)

        return view
    }

    override fun onResume() {
        super.onResume()
        mHapticFeedbackController!!.start()
    }

    override fun onPause() {
        super.onPause()
        mHapticFeedbackController!!.stop()
        if (mDismissOnPause) dismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (mOnCancelListener != null) mOnCancelListener!!.onCancel(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (mOnDismissListener != null) mOnDismissListener!!.onDismiss(dialog)
    }

    private fun setCurrentView(viewIndex: Int) {
        val millis = mCalendar.timeInMillis
        val millisEnd = mCalendarEnd.timeInMillis

        if (viewIndex != DURATION_VIEW) {
            if (mCurrentView != viewIndex) {
                setViewSelected(viewList!![viewIndex])
                mAnimator!!.displayedChild = viewIndex
                mDurationTextView!!.visibility = View.VISIBLE
                mDurationEditText!!.visibility = View.GONE
                mDurationArrow!!.visibility = View.GONE
                mDurationArrowEnd!!.visibility = View.GONE
            }
        }

        when (viewIndex) {
            MONTH_AND_DAY_VIEW -> {
                mMinSelectableDate = mMinDate
                mDayPickerView!!.onDateChanged()

                var flags = DateUtils.FORMAT_SHOW_DATE
                val dayString = DateUtils.formatDateTime(activity, millis, flags)
                mAnimator!!.contentDescription = "$mDayPickerDescription: $dayString"
                Utils.tryAccessibilityAnnounce(mAnimator, mSelectDay)
            }
            MONTH_AND_DAY_VIEW_END -> {
                mMinSelectableDate = mCalendar
                mDayPickerViewEnd!!.onDateChanged()

                val flags = DateUtils.FORMAT_SHOW_DATE
                val dayStringEnd = DateUtils.formatDateTime(activity, millisEnd, flags)
                mAnimator!!.contentDescription = "$mDayPickerDescription: $dayStringEnd"
                Utils.tryAccessibilityAnnounce(mAnimator, mSelectDay)
            }
            YEAR_VIEW -> {
                mMinSelectableDate = mMinDate
                mYearPickerView!!.onDateChanged()
                mYearPickerView!!.refreshYearAdapter()

                val yearString = YEAR_FORMAT.format(millis)
                mAnimator!!.contentDescription = "$mYearPickerDescription: $yearString"
                Utils.tryAccessibilityAnnounce(mAnimator, mSelectYear)
            }
            YEAR_VIEW_END -> {
                mMinSelectableDate = mCalendar
                mYearPickerViewEnd!!.onDateChanged()
                mYearPickerViewEnd!!.refreshYearAdapter()

                val yearStringEnd = YEAR_FORMAT.format(millisEnd)
                mAnimator!!.contentDescription = "$mYearPickerDescription: $yearStringEnd"
                Utils.tryAccessibilityAnnounce(mAnimator, mSelectYear)
            }
            DURATION_VIEW -> {
                if (mCurrentView == YEAR_VIEW || mCurrentView == MONTH_AND_DAY_VIEW
                        || mDurationArrow!!.visibility == View.VISIBLE) {
                    setViewSelected(mMonthAndDayView, mYearView, mDurationView)
                    mDurationArrow!!.visibility = View.GONE
                    mDurationArrowEnd!!.visibility = View.VISIBLE
                } else if (mCurrentView == YEAR_VIEW_END || mCurrentView == MONTH_AND_DAY_VIEW_END
                        || mDurationArrowEnd!!.visibility == View.VISIBLE) {
                    setViewSelected(mMonthAndDayViewEnd, mYearViewEnd, mDurationView)
                    mDurationArrow!!.visibility = View.VISIBLE
                    mDurationArrowEnd!!.visibility = View.GONE
                }
                mAnimator!!.displayedChild = DURATION_VIEW
                mDurationTextView!!.visibility = View.GONE
                mDurationEditText!!.visibility = View.VISIBLE
                mDurationEditText!!.requestFocus()
                mDurationEditText!!.setText(Utils.daysBetween(mCalendar, mCalendarEnd).toString())
                mDurationEditText!!.selectAll()
            }
        }//TODO Accessibility
        mCurrentView = viewIndex
    }

    private fun setViewSelected(vararg views: View?) {
        mMonthAndDayView!!.isSelected = false
        mMonthAndDayViewEnd!!.isSelected = false
        mYearView!!.isSelected = false
        mYearViewEnd!!.isSelected = false
        mDurationView!!.isSelected = false
        for (view in views) {
            view?.isSelected = true
            if (view !== mDurationView) { // disable DurationView animation
                val pulseAnimator = Utils.getPulseAnimator(view, 0.9f, 1.05f)
                if (mDelayAnimation) {
                    pulseAnimator.startDelay = ANIMATION_DELAY.toLong()
                    mDelayAnimation = false
                }
                pulseAnimator.start()
            }
        }
    }

    private fun updateDisplay(announce: Boolean) {
        if (mDayOfWeekView != null && mDayOfWeekViewEnd != null) {
            mDayOfWeekView!!.text = mCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG,
                    Locale.getDefault())!!.toUpperCase(Locale.getDefault())
            mDayOfWeekViewEnd!!.text = mCalendarEnd.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG,
                    Locale.getDefault())!!.toUpperCase(Locale.getDefault())
        }

        mSelectedMonthTextView!!.text = mCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                Locale.getDefault())!!.toUpperCase(Locale.getDefault())
        mSelectedMonthTextViewEnd!!.text = mCalendarEnd.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                Locale.getDefault())!!.toUpperCase(Locale.getDefault())
        mSelectedDayTextView!!.text = DAY_FORMAT.format(mCalendar.time)
        mSelectedDayTextViewEnd!!.text = DAY_FORMAT.format(mCalendarEnd.time)
        mYearView!!.text = YEAR_FORMAT.format(mCalendar.time)
        mYearViewEnd!!.text = YEAR_FORMAT.format(mCalendarEnd.time)
        mDuration = Utils.daysBetween(mCalendar, mCalendarEnd)
        mDurationTextView!!.text = mDuration.toString()
        mDurationDayTextView!!.text = if (mDuration > 1) getString(R.string.days) else getString(R.string.day)

        // Accessibility.
        val millis = mCalendar.timeInMillis
        val millisEnd = mCalendarEnd.timeInMillis
        mAnimator!!.setDateMillis(millis)
        var flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR
        val monthAndDayText = DateUtils.formatDateTime(activity, millis, flags)
        val monthAndDayTextEnd = DateUtils.formatDateTime(activity, millisEnd, flags)
        mMonthAndDayView!!.contentDescription = monthAndDayText
        mMonthAndDayViewEnd!!.contentDescription = monthAndDayTextEnd

        if (announce) {
            flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
            val fullDateText = DateUtils.formatDateTime(activity, millis, flags)
            //            String fullDateTextEnd = DateUtils.formatDateTime(getActivity(), millisEnd, flags);
            Utils.tryAccessibilityAnnounce(mAnimator, fullDateText)
        }
    }

    /**
     * Set whether the device should vibrate when touching fields
     *
     * @param vibrate true if the device should vibrate when touching a field
     */
    fun vibrate(vibrate: Boolean) {
        mVibrate = vibrate
    }

    /**
     * Set whether the picker should dismiss itself when being paused or whether it should try to survive an orientation change
     *
     * @param dismissOnPause true if the dialog should dismiss itself when it's pausing
     */
    fun dismissOnPause(dismissOnPause: Boolean) {
        mDismissOnPause = dismissOnPause
    }

    /**
     * Set whether the dark theme should be used
     *
     * @param themeDark true if the dark theme should be used, false if the default theme should be used
     */
    fun setThemeDark(themeDark: Boolean) {
        mThemeDark = themeDark
    }

    /**
     * Returns true when the dark theme should be used
     *
     * @return true if the dark theme should be used, false if the default theme should be used
     */
    override fun isThemeDark(): Boolean {
        return mThemeDark
    }

    fun setFirstDayOfWeek(startOfWeek: Int, startWeekEnd: Int) {
        require(!(startOfWeek < Calendar.SUNDAY || startOfWeek > Calendar.SATURDAY)) { "Value must be between Calendar.SUNDAY and " + "Calendar.SATURDAY" }
        mWeekStart = startOfWeek

        if (mDayPickerView != null) {
            mDayPickerView!!.onChange()
        }

        if (mDayPickerViewEnd != null) {
            mDayPickerViewEnd!!.onChange()
        }
    }

    fun setYearRange(startYear: Int, endYear: Int) {
        require(endYear >= startYear) { "Year end must be larger than or equal to year start" }

        mMinYear = startYear
        mMaxYear = endYear
        if (mDayPickerView != null && mDayPickerViewEnd != null) {
            mDayPickerView!!.onChange()
            mDayPickerViewEnd!!.onChange()
        }
    }

    /**
     * Sets the minimal date supported by this DatePicker. Dates before (but not including) the
     * specified date will be disallowed from being selected.
     *
     * @param calendar a Calendar object set to the year, month, day desired as the mindate.
     */
    fun setMinDate(calendar: Calendar) {
        mMinDate = calendar
        if (mDayPickerView != null && mDayPickerViewEnd != null) {
            mDayPickerView!!.onChange()
            mDayPickerViewEnd!!.onChange()
        }
    }

    /**
     * @return The minimal date supported by this DatePicker. Null if it has not been set.
     */
    override fun getMinDate(): Calendar? {
        return mMinDate
    }

    /**
     * @return The minimal date can be selected by this DatePicker. return mMinDate if
     * mMonthAndDayView is showing.
     */
    override fun getMinSelectableDate(): Calendar? {
        return mMinSelectableDate
    }

    /**
     * Sets the minimal date supported by this DatePicker. Dates after (but not including) the
     * specified date will be disallowed from being selected.
     *
     * @param calendar a Calendar object set to the year, month, day desired as the maxdate.
     */
    fun setMaxDate(calendar: Calendar) {
        mMaxDate = calendar

        if (mDayPickerView != null && mDayPickerViewEnd != null) {
            mDayPickerView!!.onChange()
            mDayPickerViewEnd!!.onChange()
        }
    }

    /**
     * @return The maximal date supported by this DatePicker. Null if it has not been set.
     */
    override fun getMaxDate(): Calendar? {
        return mMaxDate
    }


    // update highlight days
    private fun updateHighlightDays() {
        val highlightList = ArrayList<Calendar>()
        for (i in 0 until Utils.daysBetween(mCalendar, mCalendarEnd) + 1) {
            val c = Calendar.getInstance()
            c.time = mCalendar.time
            c.add(Calendar.DAY_OF_YEAR, i)
            highlightList.add(c)
        }
        val calendars = highlightList.toTypedArray()
        setHighlightedDays(calendars)
    }

    /**
     * Sets an array of dates which should be highlighted when the picker is drawn
     *
     * @param highlightedDays an Array of Calendar objects containing the dates to be highlighted
     */

    fun setHighlightedDays(highlightedDays: Array<Calendar>) {
        // Sort the array to optimize searching over it later on
        Arrays.sort(highlightedDays)
        this.highlightedDays = highlightedDays
    }

    /**
     * @return The list of dates, as Calendar Objects, which should be highlighted. null is no dates should be highlighted
     */
    override fun getHighlightedDays(): Array<Calendar>? {
        return highlightedDays
    }

    /**
     * Set's a list of days which are the only valid selections.
     * Setting this value will take precedence over using setMinDate() and setMaxDate()
     *
     * @param selectableDays an Array of Calendar Objects containing the selectable dates
     */
    fun setSelectableDays(selectableDays: Array<Calendar>) {
        // Sort the array to optimize searching over it later on
        Arrays.sort(selectableDays)
        this.selectableDays = selectableDays
    }

    /**
     * @return an Array of Calendar objects containing the list with selectable items. null if no restriction is set
     */
    override fun getSelectableDays(): Array<Calendar>? {
        return selectableDays
    }


    fun setOnDateSetListener(listener: OnDateRangeSetListener) {
        mCallBack = listener
    }

    fun setOnCancelListener(onCancelListener: DialogInterface.OnCancelListener) {
        mOnCancelListener = onCancelListener
    }

    fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener) {
        mOnDismissListener = onDismissListener
    }

    // If the newly selected month / year does not contain the currently selected day number,
    // change the selected day number to the last day of the selected month or year.
    //      e.g. Switching from Mar to Apr when Mar 31 is selected -> Apr 30
    //      e.g. Switching from 2012 to 2013 when Feb 29, 2012 is selected -> Feb 28, 2013
    private fun adjustDayInMonthIfNeeded(calendar: Calendar) {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (day > daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, daysInMonth)
        }
    }

    override fun onClick(v: View) {
        tryVibrate()
        val i = v.id
        if (i == R.id.date_picker_year) {
            setCurrentView(YEAR_VIEW)

        } else if (i == R.id.date_picker_year_end) {
            setCurrentView(YEAR_VIEW_END)

        } else if (i == R.id.date_picker_month_and_day) {
            setCurrentView(MONTH_AND_DAY_VIEW)

        } else if (i == R.id.date_picker_month_and_day_end) {
            setCurrentView(MONTH_AND_DAY_VIEW_END)

        } else if (i == R.id.date_picker_duration_layout || i == R.id.arrow_start || i == R.id.arrow_end) {
            setCurrentView(DURATION_VIEW)

        }
    }

    override fun onYearSelected(year: Int) {
        updatePickers()
        if (mCurrentView == YEAR_VIEW) {
            adjustDayInMonthIfNeeded(mCalendar)
            mCalendar.set(Calendar.YEAR, year)
            //make sure start date always after min date and before max date
            if (minDate != null && mCalendar.before(minDate)) {
                mCalendar.time = minDate!!.time
            } else if (maxDate != null && mCalendar.after(maxDate)) {
                mCalendar.time = maxDate!!.time
            }
            if (mCalendar.after(mCalendarEnd)) {
                //make sure end date always after start date
                mCalendarEnd.time = mCalendar.time
            }
            setCurrentView(MONTH_AND_DAY_VIEW)
        } else if (mCurrentView == YEAR_VIEW_END) {
            adjustDayInMonthIfNeeded(mCalendarEnd)
            mCalendarEnd.set(Calendar.YEAR, year)
            //make sure end date always after min date and before max date
            if (minDate != null && mCalendarEnd.before(minDate)) {
                mCalendarEnd.time = minDate!!.time
            } else if (maxDate != null && mCalendarEnd.after(maxDate)) {
                mCalendarEnd.time = maxDate!!.time
            }
            if (mCalendar.after(mCalendarEnd)) {
                //make sure end date always after start date
                mCalendarEnd.time = mCalendar.time
            }
            setCurrentView(MONTH_AND_DAY_VIEW_END)
        }
        updateHighlightDays()
        updateDisplay(true)
    }

    override fun onDayOfMonthSelected(year: Int, month: Int, day: Int) {
        if (mCurrentView == MONTH_AND_DAY_VIEW) {
            mCalendar.set(Calendar.YEAR, year)
            mCalendar.set(Calendar.MONTH, month)
            mCalendar.set(Calendar.DAY_OF_MONTH, day)
            if (mCalendar.after(mCalendarEnd)) {
                mCalendarEnd.time = mCalendar.time
            }
            // jump to end day selector
            setCurrentView(MONTH_AND_DAY_VIEW_END)
        } else if (mCurrentView == MONTH_AND_DAY_VIEW_END) {
            mCalendarEnd.set(Calendar.YEAR, year)
            mCalendarEnd.set(Calendar.MONTH, month)
            mCalendarEnd.set(Calendar.DAY_OF_MONTH, day)
        }
        updatePickers()
        updateHighlightDays()

        updateDisplay(true)
    }

    override fun onDurationChanged(num: Int) {
        if (num >= 0) {
            val limitDay = Calendar.getInstance()
            val limitDuration: Int
            if (mMonthAndDayView!!.isSelected) {
                limitDay.set(1900, 0, 1)
                limitDuration = Utils.daysBetween(limitDay, mCalendarEnd) + 1
            } else {
                limitDay.set(2100, 11, 31)
                limitDuration = Utils.daysBetween(mCalendar, limitDay)
            }
            if (mDurationEditText!!.hasSelection()) {
                mDuration = num
            } else {
                mDuration = if (mDuration * 10 + num > limitDuration) limitDuration else mDuration * 10 + num
            }
        } else if (num == -1) { //del
            mDuration = if (mDuration > 0) mDuration / 10 else mDuration
        } else if (num == -2) { // delete all
            mDuration = 0
        }
        mDurationEditText!!.setText(mDuration.toString())
        mDurationEditText!!.setSelection(mDuration.toString().length)
        if (mMonthAndDayView!!.isSelected) {
            mCalendar.time = mCalendarEnd.time
            mCalendar.add(Calendar.DATE, -mDuration)
        } else {
            mCalendarEnd.time = mCalendar.time
            mCalendarEnd.add(Calendar.DATE, mDuration)
        }

        updateHighlightDays()
        updateDisplay(true)
    }

    private fun updatePickers() {
        for (listener in mListeners) listener.onDateChanged()
    }

    override fun getSelectedDay(): MonthAdapter.CalendarDay {
        return if (mYearView!!.isSelected || mMonthAndDayView!!.isSelected) {
            MonthAdapter.CalendarDay(mCalendar)
        } else {
            MonthAdapter.CalendarDay(mCalendarEnd)
        }
    }

    override fun getMinYear(): Int {
        if (selectableDays != null) return selectableDays!![0].get(Calendar.YEAR)
        // Ensure no years can be selected outside of the given minimum date
        return if (mMinDate != null && mMinDate!!.get(Calendar.YEAR) > mMinYear)
            mMinDate!!.get(Calendar.YEAR)
        else
            mMinYear
    }

    override fun getMinSelectableYear(): Int {
        if (selectableDays != null) return selectableDays!![0].get(Calendar.YEAR)
        // Ensure no years can be selected outside of the given minimum date
        return if (mMinSelectableDate != null && mMinSelectableDate!!.get(Calendar.YEAR) > mMinYear)
            mMinSelectableDate!!.get(Calendar.YEAR)
        else
            mMinYear
    }

    override fun getMaxYear(): Int {
        if (selectableDays != null)
            return selectableDays!![selectableDays!!.size - 1].get(Calendar.YEAR)
        // Ensure no years can be selected outside of the given maximum date
        return if (mMaxDate != null && mMaxDate!!.get(Calendar.YEAR) < mMaxYear)
            mMaxDate!!.get(Calendar.YEAR)
        else
            mMaxYear
    }

    override fun getFirstDayOfWeek(): Int {
        return mWeekStart
    }

    override fun registerOnDateChangedListener(listener: OnDateChangedListener) {
        mListeners.add(listener)
    }

    override fun unregisterOnDateChangedListener(listener: OnDateChangedListener) {
        mListeners.remove(listener)
    }

    override fun tryVibrate() {
        if (mVibrate) mHapticFeedbackController!!.tryVibrate()
    }

    companion object {

        private val TAG = "DateRangePickerFragment"

        private val UNINITIALIZED = -1
        private val MONTH_AND_DAY_VIEW = 0
        private val YEAR_VIEW = 1
        private val MONTH_AND_DAY_VIEW_END = 2
        private val YEAR_VIEW_END = 3
        private val DURATION_VIEW = 4

        private val KEY_SELECTED_YEAR = "selected_year"
        private val KEY_SELECTED_YEAR_END = "selected_year_end"
        private val KEY_SELECTED_MONTH = "selected_month"
        private val KEY_SELECTED_MONTH_END = "selected_month_end"
        private val KEY_SELECTED_DAY = "selected_day"
        private val KEY_SELECTED_DAY_END = "selected_day_end"
        private val KEY_DURATION_DAYS = "duration_days"
        private val KEY_LIST_POSITION = "list_position"
        private val KEY_LIST_POSITION_END = "list_position_end"
        private val KEY_WEEK_START = "week_start"
        private val KEY_YEAR_START = "year_start"
        private val KEY_YEAR_END = "year_end"
        private val KEY_CURRENT_VIEW = "current_view"
        private val KEY_LIST_POSITION_OFFSET = "list_position_offset"
        private val KEY_LIST_POSITION_OFFSET_END = "list_position_offset_end"
        private val KEY_MIN_DATE = "min_date"
        private val KEY_MIN_DATE_SELECTABLE = "min_date_end"
        private val KEY_MAX_DATE = "max_date"
        private val KEY_HIGHLIGHTED_DAYS = "highlighted_days"
        private val KEY_SELECTABLE_DAYS = "selectable_days"
        private val KEY_THEME_DARK = "theme_dark"
        private val KEY_ACCENT = "accent"
        private val KEY_VIBRATE = "vibrate"
        private val KEY_DISMISS = "dismiss"

        private val DEFAULT_START_YEAR = 1900
        private val DEFAULT_END_YEAR = 2100

        private val ANIMATION_DURATION = 300
        private val ANIMATION_DELAY = 500

        private val YEAR_FORMAT = SimpleDateFormat("yyyy", Locale.getDefault())
        private val DAY_FORMAT = SimpleDateFormat("dd", Locale.getDefault())

        /**
         * @param callBack    How the parent is notified that the date is set.
         * @param year        The initial year of the dialog.
         * @param monthOfYear The initial month of the dialog.
         * @param dayOfMonth  The initial day of the dialog.
         */
        fun newInstance(callBack: OnDateRangeSetListener, year: Int,
                        monthOfYear: Int,
                        dayOfMonth: Int): DateRangePickerFragment {
            val ret = DateRangePickerFragment()
            ret.initialize(callBack, year, monthOfYear, dayOfMonth)
            return ret
        }

        /**
         * @param callBack How the parent is notified that the date is set.
         * the initial date is set to today
         */
        fun newInstance(callBack: OnDateRangeSetListener): DateRangePickerFragment {
            val ret = DateRangePickerFragment()
            val todayCal = Calendar.getInstance()
            ret.initialize(callBack, todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH),
                    todayCal.get(Calendar.DAY_OF_MONTH))
            return ret
        }
    }
}// Empty constructor required for dialog fragment.
