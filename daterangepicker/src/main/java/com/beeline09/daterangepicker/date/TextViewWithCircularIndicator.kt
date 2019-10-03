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
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import com.beeline09.daterangepicker.R
import com.google.android.material.textview.MaterialTextView

/**
 * A text view which, when pressed or activated, displays a colored circle around the text.
 */
class TextViewWithCircularIndicator(context: Context, attrs: AttributeSet) : MaterialTextView(context, attrs) {

    private var mCirclePaint = Paint()

    private val mRadius: Int
    private var mCircleColor: Int = 0
    private val mItemIsSelectedText: String

    private var mDrawCircle: Boolean = false

    init {
        val res = context.resources
        mCircleColor = ResourcesCompat.getColor(res, R.color.mdtp_accent_color, context.theme)
//        mCircleColor = res.getColor(R.color.mdtp_accent_color)
        mRadius = res.getDimensionPixelOffset(R.dimen.mdtp_month_select_circle_radius)
        mItemIsSelectedText = context.resources.getString(R.string.mdtp_item_is_selected)

        init()
    }

    private fun init() {
        mCirclePaint.isFakeBoldText = true
        mCirclePaint.isAntiAlias = true
        mCirclePaint.color = mCircleColor
        mCirclePaint.textAlign = Align.CENTER
        mCirclePaint.style = Style.FILL
        mCirclePaint.alpha = SELECTED_CIRCLE_ALPHA
    }

    fun setAccentColor(color: Int, isDarkTheme: Boolean) {
        mCircleColor = color
        mCirclePaint.color = mCircleColor
        setTextColor(createTextColor(color, isDarkTheme))
    }

    /**
     * Programmatically set the color state list (see sdrp_year_selector)
     *
     * @param accentColor pressed state text color
     * @return ColorStateList with pressed state
     */
    private fun createTextColor(accentColor: Int): ColorStateList {
        val states = arrayOf(intArrayOf(android.R.attr.state_pressed), // pressed
                intArrayOf(android.R.attr.state_selected), // selected
                intArrayOf())
        val colors = intArrayOf(accentColor, Color.WHITE, Color.BLACK)
        return ColorStateList(states, colors)
    }

    /**
     * Programmatically set the color state list (see sdrp_year_selector)
     *
     * @param accentColor pressed state text color
     * @param isDarkTheme set color based on theme
     * @return ColorStateList with pressed state
     */
    private fun createTextColor(accentColor: Int, isDarkTheme: Boolean): ColorStateList {
        val states = arrayOf(intArrayOf(android.R.attr.state_pressed), // pressed
                intArrayOf(android.R.attr.state_selected), // selected
                intArrayOf())
        val colors = if (isDarkTheme)
            intArrayOf(accentColor, Color.BLACK, Color.WHITE)
        else
            intArrayOf(accentColor, Color.WHITE, Color.BLACK)
        return ColorStateList(states, colors)
    }

    fun drawIndicator(drawCircle: Boolean) {
        mDrawCircle = drawCircle
    }

    public override fun onDraw(canvas: Canvas) {
        if (mDrawCircle) {
            val width = width
            val height = height
            val radius = Math.min(width, height) / 2
            canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), radius.toFloat(), mCirclePaint)
        }
        isSelected = mDrawCircle
        super.onDraw(canvas)
    }

    @SuppressLint("GetContentDescriptionOverride")
    override fun getContentDescription(): CharSequence {
        val itemText = text
        return if (mDrawCircle) {
            String.format(mItemIsSelectedText, itemText)
        } else {
            itemText
        }
    }

    companion object {

        private val SELECTED_CIRCLE_ALPHA = 255
    }
}
