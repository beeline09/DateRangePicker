package com.beeline09.daterangepicker.date

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TableLayout
import androidx.core.content.res.ResourcesCompat
import com.beeline09.daterangepicker.R
import com.google.android.material.button.MaterialButton
import java.util.*

class NumberPadView : TableLayout, View.OnClickListener {
    private var mContext: Context? = null
    private var mController: DateRangePickerController? = null
    private var mTextColor: Int = 0

    constructor(context: Context, controller: DateRangePickerController) : super(context) {
        mContext = context
        mController = controller
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mContext = context
        init()
    }

    private fun init() {
        val darkTheme = mController?.isThemeDark
        mTextColor = if (darkTheme == true) {
            ResourcesCompat.getColor(resources, R.color.mdtp_date_picker_text_normal_dark_theme, mContext?.theme)
        } else {
            ResourcesCompat.getColor(resources, R.color.mdtp_date_picker_text_normal, mContext?.theme)
        }
        val view = LayoutInflater.from(mContext).inflate(R.layout.sdrp_number_pad, this)
        val btnNum0 = view.findViewById<MaterialButton>(R.id.btn_zero)
        val btnNum1 = view.findViewById<MaterialButton>(R.id.btn_one)
        val btnNum2 = view.findViewById<MaterialButton>(R.id.btn_two)
        val btnNum3 = view.findViewById<MaterialButton>(R.id.btn_three)
        val btnNum4 = view.findViewById<MaterialButton>(R.id.btn_four)
        val btnNum5 = view.findViewById<MaterialButton>(R.id.btn_five)
        val btnNum6 = view.findViewById<MaterialButton>(R.id.btn_six)
        val btnNum7 = view.findViewById<MaterialButton>(R.id.btn_seven)
        val btnNum8 = view.findViewById<MaterialButton>(R.id.btn_eight)
        val btnNum9 = view.findViewById<MaterialButton>(R.id.btn_nine)
        val btnDel = view.findViewById<MaterialButton>(R.id.btn_delete)
        val buttons = ArrayList(listOf<MaterialButton>(btnNum0, btnNum1, btnNum2,
                btnNum3, btnNum4, btnNum5, btnNum6, btnNum7, btnNum8, btnNum9, btnDel))
        setMultiButtonsTextColor(buttons)
        setMultiBtnsOnClickListener(buttons)
        btnDel.setOnLongClickListener {
            mController?.onDurationChanged(-2)
            true
        }
    }

    private fun setMultiBtnsOnClickListener(buttons: ArrayList<MaterialButton>) {
        for (btn in buttons) {
            btn.setOnClickListener(this)
        }
    }

    private fun setMultiButtonsTextColor(buttons: ArrayList<MaterialButton>) {
        for (btn in buttons) {
            btn.setTextColor(mTextColor)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_zero -> mController?.onDurationChanged(0)
            R.id.btn_one -> mController?.onDurationChanged(1)
            R.id.btn_two -> mController?.onDurationChanged(2)
            R.id.btn_three -> mController?.onDurationChanged(3)
            R.id.btn_four -> mController?.onDurationChanged(4)
            R.id.btn_five -> mController?.onDurationChanged(5)
            R.id.btn_six -> mController?.onDurationChanged(6)
            R.id.btn_seven -> mController?.onDurationChanged(7)
            R.id.btn_eight -> mController?.onDurationChanged(8)
            R.id.btn_nine -> mController?.onDurationChanged(9)
            R.id.btn_delete -> mController?.onDurationChanged(-1)
        }
    }
}